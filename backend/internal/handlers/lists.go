package handlers

import (
	"encoding/json"
	"errors"
	"log/slog"
	"net/http"

	"github.com/go-chi/chi/v5"

	"github.com/lena/kartoshka-backend/internal/middleware"
	"github.com/lena/kartoshka-backend/internal/models"
	"github.com/lena/kartoshka-backend/internal/repository"
)

type ListHandler struct {
	lists  *repository.ListRepo
	logger *slog.Logger
}

func NewListHandler(lists *repository.ListRepo, logger *slog.Logger) *ListHandler {
	return &ListHandler{lists: lists, logger: logger}
}

// GET /lists
func (h *ListHandler) GetAll(w http.ResponseWriter, r *http.Request) {
	userID := middleware.GetUserID(r)
	lists, err := h.lists.GetAllForUser(r.Context(), userID)
	if err != nil {
		h.logger.Error("get lists failed", "user_id", userID, "err", err)
		http.Error(w, "ошибка базы данных", http.StatusInternalServerError)
		return
	}
	if lists == nil {
		lists = []models.List{}
	}
	writeJSON(w, http.StatusOK, lists)
}

// POST /lists
func (h *ListHandler) Create(w http.ResponseWriter, r *http.Request) {
	userID := middleware.GetUserID(r)
	var req struct {
		ListID     string `json:"list_id"`
		Title      string `json:"title"`
		ColorValue int64  `json:"color_value"`
		Position   int32  `json:"position"`
	}
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil || req.Title == "" {
		http.Error(w, "поле title обязательно", http.StatusBadRequest)
		return
	}

	list, err := h.lists.Create(r.Context(), userID, req.ListID, req.Title, req.ColorValue, req.Position)
	if err != nil {
		http.Error(w, "ошибка создания списка", http.StatusInternalServerError)
		return
	}
	writeJSON(w, http.StatusCreated, list)
}

// GET /lists/{list_id}
func (h *ListHandler) GetOne(w http.ResponseWriter, r *http.Request) {
	userID := middleware.GetUserID(r)
	listID := chi.URLParam(r, "list_id")

	if role, err := h.lists.GetMemberRole(r.Context(), listID, userID); err != nil || role == "" {
		http.Error(w, "список не найден", http.StatusNotFound)
		return
	}

	list, err := h.lists.GetByID(r.Context(), listID)
	if err != nil || list == nil {
		http.Error(w, "список не найден", http.StatusNotFound)
		return
	}
	writeJSON(w, http.StatusOK, list)
}

// PUT /lists/{list_id}
func (h *ListHandler) Update(w http.ResponseWriter, r *http.Request) {
	userID := middleware.GetUserID(r)
	listID := chi.URLParam(r, "list_id")

	role, err := h.lists.GetMemberRole(r.Context(), listID, userID)
	if err != nil || role == "" {
		http.Error(w, "список не найден", http.StatusNotFound)
		return
	}

	var req struct {
		Title            string `json:"title"`
		ColorValue       int64  `json:"color_value"`
		Position         int32  `json:"position"`
		CategoryOrder    string `json:"category_order"`
		HiddenCategories string `json:"hidden_categories"`
	}
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil || req.Title == "" {
		http.Error(w, "поле title обязательно", http.StatusBadRequest)
		return
	}

	if err := h.lists.Update(r.Context(), listID,
		req.Title, req.ColorValue, req.Position,
		req.CategoryOrder, req.HiddenCategories,
	); err != nil {
		http.Error(w, "ошибка обновления", http.StatusInternalServerError)
		return
	}

	list, err := h.lists.GetByID(r.Context(), listID)
	if err != nil || list == nil {
		http.Error(w, "ошибка чтения после обновления", http.StatusInternalServerError)
		return
	}
	writeJSON(w, http.StatusOK, list)
}

// DELETE /lists/{list_id}
func (h *ListHandler) Delete(w http.ResponseWriter, r *http.Request) {
	userID := middleware.GetUserID(r)
	listID := chi.URLParam(r, "list_id")

	role, err := h.lists.GetMemberRole(r.Context(), listID, userID)
	if err != nil || role == "" {
		http.Error(w, "список не найден", http.StatusNotFound)
		return
	}
	if role != "owner" {
		http.Error(w, "только владелец может удалить список", http.StatusForbidden)
		return
	}

	if err := h.lists.Delete(r.Context(), listID, userID); err != nil {
		http.Error(w, "ошибка удаления", http.StatusInternalServerError)
		return
	}
	w.WriteHeader(http.StatusNoContent)
}

// GET /lists/{list_id}/members
func (h *ListHandler) GetMembers(w http.ResponseWriter, r *http.Request) {
	userID := middleware.GetUserID(r)
	listID := chi.URLParam(r, "list_id")

	if role, err := h.lists.GetMemberRole(r.Context(), listID, userID); err != nil || role == "" {
		http.Error(w, "список не найден", http.StatusNotFound)
		return
	}

	members, err := h.lists.GetMembers(r.Context(), listID)
	if err != nil {
		http.Error(w, "ошибка базы данных", http.StatusInternalServerError)
		return
	}
	writeJSON(w, http.StatusOK, members)
}

// DELETE /lists/{list_id}/members/{user_id}
func (h *ListHandler) RemoveMember(w http.ResponseWriter, r *http.Request) {
	userID := middleware.GetUserID(r)
	listID := chi.URLParam(r, "list_id")
	targetUserID := chi.URLParam(r, "user_id")

	role, err := h.lists.GetMemberRole(r.Context(), listID, userID)
	if err != nil || role == "" {
		http.Error(w, "список не найден", http.StatusNotFound)
		return
	}
	if role != "owner" {
		http.Error(w, "только владелец может удалять участников", http.StatusForbidden)
		return
	}
	if targetUserID == userID {
		http.Error(w, "владелец не может покинуть список", http.StatusBadRequest)
		return
	}

	targetRole, err := h.lists.GetMemberRole(r.Context(), listID, targetUserID)
	if err != nil || targetRole == "" {
		http.Error(w, "участник не найден", http.StatusNotFound)
		return
	}
	if targetRole == "owner" {
		http.Error(w, "нельзя удалить владельца", http.StatusBadRequest)
		return
	}

	if err := h.lists.RemoveMember(r.Context(), listID, targetUserID); err != nil {
		if errors.Is(err, repository.ErrForbidden) {
			http.Error(w, "нет прав", http.StatusForbidden)
			return
		}
		http.Error(w, "ошибка удаления участника", http.StatusInternalServerError)
		return
	}
	w.WriteHeader(http.StatusNoContent)
}
