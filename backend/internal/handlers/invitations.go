package handlers

import (
	"encoding/json"
	"fmt"
	"log/slog"
	"net/http"
	"os"
	"time"

	"github.com/go-chi/chi/v5"

	"github.com/lena/kartoshka-backend/internal/apierror"
	"github.com/lena/kartoshka-backend/internal/middleware"
	"github.com/lena/kartoshka-backend/internal/notifications"
	"github.com/lena/kartoshka-backend/internal/repository"
)

type InvitationHandler struct {
	invitations invitationStore
	lists       listStore
	users       userStore
	logger      *slog.Logger
}

func NewInvitationHandler(invitations *repository.InvitationRepo, lists *repository.ListRepo, users *repository.UserRepo, logger *slog.Logger) *InvitationHandler {
	return &InvitationHandler{invitations: invitations, lists: lists, users: users, logger: logger}
}

// POST /lists/{list_id}/invite — создать инвайт, вернуть deep-link
func (h *InvitationHandler) Create(w http.ResponseWriter, r *http.Request) {
	userID := middleware.GetUserID(r)
	listID := chi.URLParam(r, "list_id")

	if role, err := h.lists.GetMemberRole(r.Context(), listID, userID); err != nil || role == "" {
		apierror.Write(w, r, http.StatusNotFound, apierror.CodeListNotFound, "List not found", "")
		return
	}

	var req struct {
		InviteeEmail string `json:"invitee_email"`
	}
	// email необязателен — игнорируем ошибку парсинга
	_ = json.NewDecoder(r.Body).Decode(&req)

	if req.InviteeEmail != "" {
		invitee, err := h.users.GetByEmail(r.Context(), req.InviteeEmail)
		if err != nil {
			apierror.Write(w, r, http.StatusInternalServerError, apierror.CodeInternal, "Internal error", err.Error())
			return
		}
		if invitee == nil {
			apierror.Write(w, r, http.StatusNotFound, apierror.CodeInviteUserNotFound, "User not found", "")
			return
		}
		if invitee.UserID == userID {
			apierror.Write(w, r, http.StatusConflict, apierror.CodeInviteSelfForbidden, "Cannot invite yourself", "")
			return
		}
		existingRole, err := h.lists.GetMemberRole(r.Context(), listID, invitee.UserID)
		if err != nil {
			apierror.Write(w, r, http.StatusInternalServerError, apierror.CodeInternal, "Internal error", err.Error())
			return
		}
		if existingRole != "" {
			apierror.Write(w, r, http.StatusConflict, apierror.CodeInviteAlreadyMember, "User is already a member", "")
			return
		}
		pending, err := h.invitations.GetPendingByListAndEmail(r.Context(), listID, req.InviteeEmail)
		if err != nil {
			apierror.Write(w, r, http.StatusInternalServerError, apierror.CodeInternal, "Internal error", err.Error())
			return
		}
		if pending != nil {
			apierror.Write(w, r, http.StatusConflict, apierror.CodeInviteAlreadySent, "Invitation already pending", "")
			return
		}
	}

	inv, err := h.invitations.Create(r.Context(), listID, userID, req.InviteeEmail)
	if err != nil {
		apierror.Write(w, r, http.StatusInternalServerError, apierror.CodeInternal, "Failed to create invitation", err.Error())
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
		apierror.Write(w, r, http.StatusNotFound, apierror.CodeInviteNotFound, "Invitation not found", "")
		return
	}

	if info.Status == "expired" || (info.Status == "pending" && time.Now().After(info.ExpiresAt)) {
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
		apierror.Write(w, r, http.StatusNotFound, apierror.CodeInviteNotFound, "Invitation not found", "")
		return
	}

	if inv.Status == "accepted" {
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
		apierror.Write(w, r, http.StatusGone, apierror.CodeInviteExpired, "Invitation has expired", "")
		return
	}

	existingRole, err := h.lists.GetMemberRole(r.Context(), inv.ListID, userID)
	if err != nil {
		apierror.Write(w, r, http.StatusInternalServerError, apierror.CodeInternal, "Internal error", err.Error())
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
		apierror.Write(w, r, http.StatusInternalServerError, apierror.CodeInternal, "Failed to accept invitation", err.Error())
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
