package handlers

import (
	"net/http"
	"strconv"
	"time"

	"github.com/go-chi/chi/v5"

	"github.com/lena/kartoshka-backend/internal/middleware"
	"github.com/lena/kartoshka-backend/internal/models"
	"github.com/lena/kartoshka-backend/internal/repository"
)

type SyncHandler struct {
	events *repository.EventsRepo
	lists  *repository.ListRepo
}

func NewSyncHandler(events *repository.EventsRepo, lists *repository.ListRepo) *SyncHandler {
	return &SyncHandler{events: events, lists: lists}
}

type eventsResponse struct {
	ServerTime     int64               `json:"server_time"`      // Unix timestamp — использовать как следующий since
	ItemsChanged   []models.Item       `json:"items_changed"`    // включает is_deleted=true
	MembersChanged []models.ListMember `json:"members_changed"`
}

// GET /lists/{list_id}/events?since={unix_timestamp}
// since=0 или отсутствует → вернуть все данные
func (h *SyncHandler) GetEvents(w http.ResponseWriter, r *http.Request) {
	userID := middleware.GetUserID(r)
	listID := chi.URLParam(r, "list_id")

	if role, err := h.lists.GetMemberRole(r.Context(), listID, userID); err != nil || role == "" {
		http.Error(w, "список не найден", http.StatusNotFound)
		return
	}

	since := parseSince(r.URL.Query().Get("since"))

	items, err := h.events.GetChangedItems(r.Context(), listID, since)
	if err != nil {
		http.Error(w, "ошибка получения изменений товаров", http.StatusInternalServerError)
		return
	}

	members, err := h.events.GetChangedMembers(r.Context(), listID, since)
	if err != nil {
		http.Error(w, "ошибка получения изменений участников", http.StatusInternalServerError)
		return
	}

	if items == nil {
		items = []models.Item{}
	}
	if members == nil {
		members = []models.ListMember{}
	}

	writeJSON(w, http.StatusOK, eventsResponse{
		ServerTime:     time.Now().Unix(),
		ItemsChanged:   items,
		MembersChanged: members,
	})
}

// parseSince разбирает Unix timestamp из строки.
// Если строка пустая, "0" или невалидная — возвращает 2000-01-01 (начало диапазона YDB Datetime).
func parseSince(s string) time.Time {
	if s != "" && s != "0" {
		if ts, err := strconv.ParseInt(s, 10, 64); err == nil && ts > 0 {
			return time.Unix(ts, 0).UTC()
		}
	}
	// Минимальная дата в диапазоне YDB Datetime — возвращает все данные
	return time.Date(2000, 1, 1, 0, 0, 0, 0, time.UTC)
}
