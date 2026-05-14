package handlers

import (
	"encoding/json"
	"errors"
	"log/slog"
	"net/http"

	"github.com/go-chi/chi/v5"

	"github.com/lena/kartoshka-backend/internal/apierror"
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
		apierror.Write(w, r, http.StatusInternalServerError, apierror.CodeInternal, "Internal error", err.Error())
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
		apierror.Write(w, r, http.StatusBadRequest, apierror.CodeBadRequest, "List title is required", "")
		return
	}

	list, err := h.lists.Create(r.Context(), userID, req.ListID, req.Title, req.ColorValue, req.Position)
	if err != nil {
		apierror.Write(w, r, http.StatusInternalServerError, apierror.CodeInternal, "Failed to create list", err.Error())
		return
	}
	writeJSON(w, http.StatusCreated, list)
}

// GET /lists/{list_id}
func (h *ListHandler) GetOne(w http.ResponseWriter, r *http.Request) {
	userID := middleware.GetUserID(r)
	listID := chi.URLParam(r, "list_id")

	if role, err := h.lists.GetMemberRole(r.Context(), listID, userID); err != nil || role == "" {
		apierror.Write(w, r, http.StatusNotFound, apierror.CodeListNotFound, "List not found", "")
		return
	}

	list, err := h.lists.GetByID(r.Context(), listID)
	if err != nil || list == nil {
		apierror.Write(w, r, http.StatusNotFound, apierror.CodeListNotFound, "List not found", "")
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
		apierror.Write(w, r, http.StatusNotFound, apierror.CodeListNotFound, "List not found", "")
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
		apierror.Write(w, r, http.StatusBadRequest, apierror.CodeBadRequest, "List title is required", "")
		return
	}

	if err := h.lists.Update(r.Context(), listID,
		req.Title, req.ColorValue, req.Position,
		req.CategoryOrder, req.HiddenCategories,
	); err != nil {
		apierror.Write(w, r, http.StatusInternalServerError, apierror.CodeInternal, "Failed to update list", err.Error())
		return
	}

	list, err := h.lists.GetByID(r.Context(), listID)
	if err != nil || list == nil {
		apierror.Write(w, r, http.StatusInternalServerError, apierror.CodeInternal, "Failed to read list after update", "")
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
		apierror.Write(w, r, http.StatusNotFound, apierror.CodeListNotFound, "List not found", "")
		return
	}
	if role != "owner" {
		apierror.Write(w, r, http.StatusForbidden, apierror.CodeListAccessDenied, "Only the owner can delete this list", "")
		return
	}

	if err := h.lists.Delete(r.Context(), listID, userID); err != nil {
		apierror.Write(w, r, http.StatusInternalServerError, apierror.CodeInternal, "Failed to delete list", err.Error())
		return
	}
	w.WriteHeader(http.StatusNoContent)
}

// GET /lists/{list_id}/members
func (h *ListHandler) GetMembers(w http.ResponseWriter, r *http.Request) {
	userID := middleware.GetUserID(r)
	listID := chi.URLParam(r, "list_id")

	if role, err := h.lists.GetMemberRole(r.Context(), listID, userID); err != nil || role == "" {
		apierror.Write(w, r, http.StatusNotFound, apierror.CodeListNotFound, "List not found", "")
		return
	}

	members, err := h.lists.GetMembers(r.Context(), listID)
	if err != nil {
		apierror.Write(w, r, http.StatusInternalServerError, apierror.CodeInternal, "Internal error", err.Error())
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
		apierror.Write(w, r, http.StatusNotFound, apierror.CodeListNotFound, "List not found", "")
		return
	}
	if role != "owner" {
		apierror.Write(w, r, http.StatusForbidden, apierror.CodeListAccessDenied, "Only the owner can remove members", "")
		return
	}
	if targetUserID == userID {
		apierror.Write(w, r, http.StatusBadRequest, apierror.CodeBadRequest, "Owner cannot leave the list", "")
		return
	}

	targetRole, err := h.lists.GetMemberRole(r.Context(), listID, targetUserID)
	if err != nil || targetRole == "" {
		apierror.Write(w, r, http.StatusNotFound, apierror.CodeNotFound, "Member not found", "")
		return
	}
	if targetRole == "owner" {
		apierror.Write(w, r, http.StatusBadRequest, apierror.CodeBadRequest, "Cannot remove the owner", "")
		return
	}

	if err := h.lists.RemoveMember(r.Context(), listID, targetUserID); err != nil {
		if errors.Is(err, repository.ErrForbidden) {
			apierror.Write(w, r, http.StatusForbidden, apierror.CodeForbidden, "Insufficient permissions", "")
			return
		}
		apierror.Write(w, r, http.StatusInternalServerError, apierror.CodeInternal, "Failed to remove member", err.Error())
		return
	}
	w.WriteHeader(http.StatusNoContent)
}
