package handlers

import (
	"encoding/json"
	"fmt"
	"log/slog"
	"net/http"
	"os"
	"time"

	"github.com/go-chi/chi/v5"

	"github.com/lena/kartoshka-backend/internal/middleware"
	"github.com/lena/kartoshka-backend/internal/notifications"
	"github.com/lena/kartoshka-backend/internal/repository"
)

type InvitationHandler struct {
	invitations *repository.InvitationRepo
	lists       *repository.ListRepo
	logger      *slog.Logger
}

func NewInvitationHandler(invitations *repository.InvitationRepo, lists *repository.ListRepo, logger *slog.Logger) *InvitationHandler {
	return &InvitationHandler{invitations: invitations, lists: lists, logger: logger}
}

// POST /lists/{list_id}/invite — создать инвайт, вернуть deep-link
func (h *InvitationHandler) Create(w http.ResponseWriter, r *http.Request) {
	userID := middleware.GetUserID(r)
	listID := chi.URLParam(r, "list_id")

	if role, err := h.lists.GetMemberRole(r.Context(), listID, userID); err != nil || role == "" {
		http.Error(w, "список не найден", http.StatusNotFound)
		return
	}

	var req struct {
		InviteeEmail string `json:"invitee_email"`
	}
	// email необязателен — игнорируем ошибку парсинга
	_ = json.NewDecoder(r.Body).Decode(&req)

	inv, err := h.invitations.Create(r.Context(), listID, userID, req.InviteeEmail)
	if err != nil {
		http.Error(w, "ошибка создания инвайта", http.StatusInternalServerError)
		return
	}

	deepLink := fmt.Sprintf("kartoshka://invite/%s", inv.InviteToken)
	webLink := fmt.Sprintf("%s/invite/%s", appBaseURL(), inv.InviteToken)

	// Если указан email — попытаться отправить приглашение.
	// Если SMTP падает — НЕ валим запрос: инвайт уже создан, ссылку возвращаем клиенту,
	// чтобы он мог скопировать её и отправить вручную.
	emailSent := false
	if req.InviteeEmail != "" {
		body := fmt.Sprintf(`<html><body>
<p>Вас пригласили в список покупок в приложении Супер Списки.</p>
<p><a href="%s">Принять приглашение</a></p>
<p>Ссылка действительна 7 дней.</p>
</body></html>`, webLink)
		if err := notifications.SendEmail(req.InviteeEmail, "Приглашение в список — Супер Списки", body); err != nil {
			h.logger.Error("invite email send failed", "list_id", listID, "err", err)
		} else {
			emailSent = true
		}
	}

	writeJSON(w, http.StatusCreated, map[string]any{
		"invite_token": inv.InviteToken,
		"deep_link":    deepLink,
		"web_link":     webLink,
		"email_sent":   emailSent,
	})
}

// GET /invite/{invite_token} — публичный: инфо об инвайте (без авторизации)
func (h *InvitationHandler) GetInfo(w http.ResponseWriter, r *http.Request) {
	token := chi.URLParam(r, "invite_token")

	info, err := h.invitations.GetInfoByToken(r.Context(), token)
	if err != nil || info == nil {
		http.Error(w, "инвайт не найден", http.StatusNotFound)
		return
	}

	if info.Status == "expired" || (info.Status == "pending" && time.Now().After(info.ExpiresAt)) {
		// Помечаем как истёкший если ещё не помечен
		if info.Status == "pending" {
			_ = h.invitations.MarkExpired(r.Context(), token)
			info.Status = "expired"
		}
	}

	writeJSON(w, http.StatusOK, info)
}

// POST /invite/{invite_token}/accept — принять инвайт (требует авторизации)
func (h *InvitationHandler) Accept(w http.ResponseWriter, r *http.Request) {
	userID := middleware.GetUserID(r)
	token := chi.URLParam(r, "invite_token")

	inv, err := h.invitations.GetByToken(r.Context(), token)
	if err != nil || inv == nil {
		http.Error(w, "инвайт не найден", http.StatusNotFound)
		return
	}

	if inv.Status == "accepted" {
		// Уже принят — проверяем что пользователь действительно является участником
		writeJSON(w, http.StatusOK, map[string]string{
			"message": "вы уже участник этого списка",
			"list_id": inv.ListID,
		})
		return
	}

	if inv.Status == "expired" || time.Now().After(inv.ExpiresAt) {
		if inv.Status != "expired" {
			_ = h.invitations.MarkExpired(r.Context(), token)
		}
		http.Error(w, "срок действия приглашения истёк", http.StatusGone)
		return
	}

	// Если пользователь уже участник — вернуть 200 (идемпотентно)
	existingRole, err := h.lists.GetMemberRole(r.Context(), inv.ListID, userID)
	if err != nil {
		http.Error(w, "ошибка проверки членства", http.StatusInternalServerError)
		return
	}
	if existingRole != "" {
		writeJSON(w, http.StatusOK, map[string]string{
			"message": "вы уже участник этого списка",
			"list_id": inv.ListID,
		})
		return
	}

	if err := h.invitations.Accept(r.Context(), token, inv.ListID, userID); err != nil {
		http.Error(w, "ошибка принятия приглашения", http.StatusInternalServerError)
		return
	}

	writeJSON(w, http.StatusOK, map[string]string{
		"message": "вы добавлены в список",
		"list_id": inv.ListID,
	})
}

func appBaseURL() string {
	if url := os.Getenv("APP_BASE_URL"); url != "" {
		return url
	}
	return "http://localhost:8080"
}
