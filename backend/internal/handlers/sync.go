package handlers

import (
	"log/slog"
	"net/http"
	"strconv"
	"time"

	"github.com/go-chi/chi/v5"

	"github.com/lena/kartoshka-backend/internal/apierror"
	"github.com/lena/kartoshka-backend/internal/middleware"
	"github.com/lena/kartoshka-backend/internal/models"
	"github.com/lena/kartoshka-backend/internal/repository"
)

type SyncHandler struct {
	events eventsStore
	lists  listStore
	logger *slog.Logger
}

func NewSyncHandler(events *repository.EventsRepo, lists *repository.ListRepo, logger *slog.Logger) *SyncHandler {
	return &SyncHandler{events: events, lists: lists, logger: logger}
}

type eventsResponse struct {
	ServerTime     int64               `json:"server_time"`
	ItemsChanged   []models.Item       `json:"items_changed"`
	MembersChanged []models.ListMember `json:"members_changed"`
}

// GET /lists/{list_id}/events?since={unix_timestamp}
func (h *SyncHandler) GetEvents(w http.ResponseWriter, r *http.Request) {
	userID := middleware.GetUserID(r)
	listID := chi.URLParam(r, "list_id")

	if role, err := h.lists.GetMemberRole(r.Context(), listID, userID); err != nil || role == "" {
		apierror.Write(w, r, http.StatusNotFound, apierror.CodeListNotFound, "List not found", "")
		return
	}

	since := parseSince(r.URL.Query().Get("since"))

	items, err := h.events.GetChangedItems(r.Context(), listID, since)
	if err != nil {
		apierror.Write(w, r, http.StatusInternalServerError, apierror.CodeInternal, "Failed to fetch item events", err.Error())
		return
	}

	members, err := h.events.GetChangedMembers(r.Context(), listID, since)
	if err != nil {
		apierror.Write(w, r, http.StatusInternalServerError, apierror.CodeInternal, "Failed to fetch member events", err.Error())
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
func parseSince(s string) time.Time {
	if s != "" && s != "0" {
		if ts, err := strconv.ParseInt(s, 10, 64); err == nil && ts > 0 {
			return time.Unix(ts, 0).UTC()
		}
	}
	return time.Date(2000, 1, 1, 0, 0, 0, 0, time.UTC)
}
