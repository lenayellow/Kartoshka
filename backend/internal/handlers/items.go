package handlers

import (
	"context"
	"encoding/json"
	"fmt"
	"io"
	"log/slog"
	"net/http"
	"time"

	"github.com/go-chi/chi/v5"
	"github.com/google/uuid"

	"github.com/lena/kartoshka-backend/internal/apierror"
	"github.com/lena/kartoshka-backend/internal/middleware"
	"github.com/lena/kartoshka-backend/internal/models"
	"github.com/lena/kartoshka-backend/internal/notifications"
	"github.com/lena/kartoshka-backend/internal/repository"
	"github.com/lena/kartoshka-backend/internal/storage"
)

type ItemHandler struct {
	items    *repository.ItemRepo
	lists    *repository.ListRepo
	store    *storage.S3Client       // nil если S3 не настроен
	notifier *notifications.Notifier // nil если push не настроен
	logger   *slog.Logger
}

func NewItemHandler(items *repository.ItemRepo, lists *repository.ListRepo, store *storage.S3Client, notifier *notifications.Notifier, logger *slog.Logger) *ItemHandler {
	return &ItemHandler{items: items, lists: lists, store: store, notifier: notifier, logger: logger}
}

// GET /lists/{list_id}/items
func (h *ItemHandler) GetAll(w http.ResponseWriter, r *http.Request) {
	userID := middleware.GetUserID(r)
	listID := chi.URLParam(r, "list_id")

	if role, err := h.lists.GetMemberRole(r.Context(), listID, userID); err != nil || role == "" {
		apierror.Write(w, r, http.StatusNotFound, apierror.CodeListNotFound, "List not found", "")
		return
	}

	items, err := h.items.GetAllByList(r.Context(), listID)
	if err != nil {
		apierror.Write(w, r, http.StatusInternalServerError, apierror.CodeInternal, "Internal error", err.Error())
		return
	}
	if items == nil {
		items = []models.Item{}
	}
	writeJSON(w, http.StatusOK, items)
}

// POST /lists/{list_id}/items
func (h *ItemHandler) Create(w http.ResponseWriter, r *http.Request) {
	userID := middleware.GetUserID(r)
	listID := chi.URLParam(r, "list_id")

	if role, err := h.lists.GetMemberRole(r.Context(), listID, userID); err != nil || role == "" {
		apierror.Write(w, r, http.StatusNotFound, apierror.CodeListNotFound, "List not found", "")
		return
	}

	var req struct {
		Name       string `json:"name"`
		Tags       string `json:"tags"`
		Note       string `json:"note"`
		CategoryID string `json:"category_id"`
		SortIndex  int32  `json:"sort_index"`
	}
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil || req.Name == "" {
		apierror.Write(w, r, http.StatusBadRequest, apierror.CodeBadRequest, "Item name is required", "")
		return
	}

	item, err := h.items.Create(r.Context(), listID, userID,
		req.Name, req.Tags, req.Note, req.CategoryID, req.SortIndex)
	if err != nil {
		h.logger.Error("item create failed", "err", err, "list_id", listID)
		apierror.Write(w, r, http.StatusInternalServerError, apierror.CodeItemCreateFailed, "Failed to create item", err.Error())
		return
	}
	writeJSON(w, http.StatusCreated, item)
	if h.notifier != nil {
		go func() {
			ctx, cancel := context.WithTimeout(context.Background(), 30*time.Second)
			defer cancel()
			h.notifier.NotifyListChanged(ctx, listID, userID,
				"Супер Списки", req.Name+" добавлен в список")
		}()
	}
}

// PUT /lists/{list_id}/items/{item_id}
func (h *ItemHandler) Update(w http.ResponseWriter, r *http.Request) {
	userID := middleware.GetUserID(r)
	listID := chi.URLParam(r, "list_id")
	itemID := chi.URLParam(r, "item_id")

	if role, err := h.lists.GetMemberRole(r.Context(), listID, userID); err != nil || role == "" {
		apierror.Write(w, r, http.StatusNotFound, apierror.CodeListNotFound, "List not found", "")
		return
	}

	var req struct {
		Name       string `json:"name"`
		Tags       string `json:"tags"`
		Note       string `json:"note"`
		CategoryID string `json:"category_id"`
	}
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil || req.Name == "" {
		apierror.Write(w, r, http.StatusBadRequest, apierror.CodeBadRequest, "Item name is required", "")
		return
	}

	item, err := h.items.Update(r.Context(), itemID, userID,
		req.Name, req.Tags, req.Note, req.CategoryID)
	if err != nil {
		apierror.Write(w, r, http.StatusInternalServerError, apierror.CodeInternal, "Failed to update item", err.Error())
		return
	}
	writeJSON(w, http.StatusOK, item)
	if h.notifier != nil {
		go func() {
			ctx, cancel := context.WithTimeout(context.Background(), 30*time.Second)
			defer cancel()
			h.notifier.NotifyListChanged(ctx, listID, userID,
				"Супер Списки", req.Name+" изменён")
		}()
	}
}

// DELETE /lists/{list_id}/items/{item_id}
func (h *ItemHandler) Delete(w http.ResponseWriter, r *http.Request) {
	userID := middleware.GetUserID(r)
	listID := chi.URLParam(r, "list_id")
	itemID := chi.URLParam(r, "item_id")

	if role, err := h.lists.GetMemberRole(r.Context(), listID, userID); err != nil || role == "" {
		apierror.Write(w, r, http.StatusNotFound, apierror.CodeListNotFound, "List not found", "")
		return
	}

	if err := h.items.SoftDelete(r.Context(), itemID, userID); err != nil {
		apierror.Write(w, r, http.StatusInternalServerError, apierror.CodeInternal, "Failed to delete item", err.Error())
		return
	}
	w.WriteHeader(http.StatusNoContent)
	if h.notifier != nil {
		go func() {
			ctx, cancel := context.WithTimeout(context.Background(), 30*time.Second)
			defer cancel()
			h.notifier.NotifyListChanged(ctx, listID, userID,
				"Супер Списки", "Товар удалён из списка")
		}()
	}
}

// POST /lists/{list_id}/items/{item_id}/photo — загрузить фото товара (multipart, поле "photo")
func (h *ItemHandler) UploadPhoto(w http.ResponseWriter, r *http.Request) {
	if h.store == nil {
		apierror.Write(w, r, http.StatusNotImplemented, apierror.CodeUnavailable, "File upload not configured", "")
		return
	}
	userID := middleware.GetUserID(r)
	listID := chi.URLParam(r, "list_id")
	itemID := chi.URLParam(r, "item_id")

	if role, err := h.lists.GetMemberRole(r.Context(), listID, userID); err != nil || role == "" {
		apierror.Write(w, r, http.StatusNotFound, apierror.CodeListNotFound, "List not found", "")
		return
	}

	if err := r.ParseMultipartForm(10 << 20); err != nil {
		apierror.Write(w, r, http.StatusBadRequest, apierror.CodeBadRequest, "File too large (max 10 MB)", err.Error())
		return
	}
	file, header, err := r.FormFile("photo")
	if err != nil {
		apierror.Write(w, r, http.StatusBadRequest, apierror.CodeBadRequest, "photo field is required", err.Error())
		return
	}
	defer file.Close()

	data, err := io.ReadAll(file)
	if err != nil {
		apierror.Write(w, r, http.StatusInternalServerError, apierror.CodeInternal, "Failed to read file", err.Error())
		return
	}

	contentType := header.Header.Get("Content-Type")
	if contentType == "" {
		contentType = "image/jpeg"
	}

	key := fmt.Sprintf("items/%s/%s/%s.jpg", listID, itemID, uuid.New().String())
	url, err := h.store.UploadFile(r.Context(), key, contentType, data)
	if err != nil {
		apierror.Write(w, r, http.StatusInternalServerError, apierror.CodeInternal, "Failed to upload photo", err.Error())
		return
	}

	if err := h.items.UpdatePhotoURL(r.Context(), itemID, url); err != nil {
		apierror.Write(w, r, http.StatusInternalServerError, apierror.CodeInternal, "Failed to update item", err.Error())
		return
	}

	item, err := h.items.GetByID(r.Context(), itemID)
	if err != nil || item == nil {
		apierror.Write(w, r, http.StatusNotFound, apierror.CodeItemNotFound, "Item not found", "")
		return
	}
	writeJSON(w, http.StatusOK, item)
}

// POST /lists/{list_id}/items/{item_id}/check
func (h *ItemHandler) Check(w http.ResponseWriter, r *http.Request) {
	userID := middleware.GetUserID(r)
	listID := chi.URLParam(r, "list_id")
	itemID := chi.URLParam(r, "item_id")

	if role, err := h.lists.GetMemberRole(r.Context(), listID, userID); err != nil || role == "" {
		apierror.Write(w, r, http.StatusNotFound, apierror.CodeListNotFound, "List not found", "")
		return
	}

	item, err := h.items.GetByID(r.Context(), itemID)
	if err != nil || item == nil {
		apierror.Write(w, r, http.StatusNotFound, apierror.CodeItemNotFound, "Item not found", "")
		return
	}

	if err := h.items.Check(r.Context(), itemID, item.Name, listID, userID); err != nil {
		apierror.Write(w, r, http.StatusInternalServerError, apierror.CodeInternal, "Internal error", err.Error())
		return
	}
	w.WriteHeader(http.StatusNoContent)
	if h.notifier != nil {
		go func() {
			ctx, cancel := context.WithTimeout(context.Background(), 30*time.Second)
			defer cancel()
			h.notifier.NotifyListChanged(ctx, listID, userID,
				"Супер Списки", item.Name+" куплен")
		}()
	}
}

// POST /lists/{list_id}/items/{item_id}/uncheck
func (h *ItemHandler) Uncheck(w http.ResponseWriter, r *http.Request) {
	userID := middleware.GetUserID(r)
	listID := chi.URLParam(r, "list_id")
	itemID := chi.URLParam(r, "item_id")

	if role, err := h.lists.GetMemberRole(r.Context(), listID, userID); err != nil || role == "" {
		apierror.Write(w, r, http.StatusNotFound, apierror.CodeListNotFound, "List not found", "")
		return
	}

	if err := h.items.Uncheck(r.Context(), itemID, userID); err != nil {
		apierror.Write(w, r, http.StatusInternalServerError, apierror.CodeInternal, "Internal error", err.Error())
		return
	}

	item, err := h.items.GetByID(r.Context(), itemID)
	if err != nil || item == nil {
		apierror.Write(w, r, http.StatusNotFound, apierror.CodeItemNotFound, "Item not found", "")
		return
	}
	writeJSON(w, http.StatusOK, item)
}

// POST /lists/{list_id}/items/{item_id}/move
func (h *ItemHandler) Move(w http.ResponseWriter, r *http.Request) {
	userID := middleware.GetUserID(r)
	listID := chi.URLParam(r, "list_id")
	itemID := chi.URLParam(r, "item_id")

	var req struct {
		TargetListID string `json:"target_list_id"`
	}
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil || req.TargetListID == "" {
		apierror.Write(w, r, http.StatusBadRequest, apierror.CodeBadRequest, "target_list_id is required", "")
		return
	}

	if role, err := h.lists.GetMemberRole(r.Context(), listID, userID); err != nil || role == "" {
		apierror.Write(w, r, http.StatusNotFound, apierror.CodeListNotFound, "List not found", "")
		return
	}
	if role, err := h.lists.GetMemberRole(r.Context(), req.TargetListID, userID); err != nil || role == "" {
		apierror.Write(w, r, http.StatusNotFound, apierror.CodeListNotFound, "Target list not found", "")
		return
	}

	if err := h.items.Move(r.Context(), itemID, req.TargetListID, userID); err != nil {
		apierror.Write(w, r, http.StatusInternalServerError, apierror.CodeInternal, "Failed to move item", err.Error())
		return
	}

	item, err := h.items.GetByID(r.Context(), itemID)
	if err != nil || item == nil {
		apierror.Write(w, r, http.StatusNotFound, apierror.CodeItemNotFound, "Item not found", "")
		return
	}
	writeJSON(w, http.StatusOK, item)
}

// PATCH /lists/{list_id}/items/reorder
func (h *ItemHandler) Reorder(w http.ResponseWriter, r *http.Request) {
	userID := middleware.GetUserID(r)
	listID := chi.URLParam(r, "list_id")

	if role, err := h.lists.GetMemberRole(r.Context(), listID, userID); err != nil || role == "" {
		apierror.Write(w, r, http.StatusNotFound, apierror.CodeListNotFound, "List not found", "")
		return
	}

	var req struct {
		Items []models.ReorderItem `json:"items"`
	}
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil || len(req.Items) == 0 {
		apierror.Write(w, r, http.StatusBadRequest, apierror.CodeBadRequest, "items field is required", "")
		return
	}

	if err := h.items.Reorder(r.Context(), req.Items); err != nil {
		apierror.Write(w, r, http.StatusInternalServerError, apierror.CodeInternal, "Failed to reorder items", err.Error())
		return
	}
	w.WriteHeader(http.StatusNoContent)
}

// GET /lists/{list_id}/recent
func (h *ItemHandler) GetRecent(w http.ResponseWriter, r *http.Request) {
	userID := middleware.GetUserID(r)
	listID := chi.URLParam(r, "list_id")

	if role, err := h.lists.GetMemberRole(r.Context(), listID, userID); err != nil || role == "" {
		apierror.Write(w, r, http.StatusNotFound, apierror.CodeListNotFound, "List not found", "")
		return
	}

	entries, err := h.items.GetRecent(r.Context(), listID)
	if err != nil {
		apierror.Write(w, r, http.StatusInternalServerError, apierror.CodeInternal, "Internal error", err.Error())
		return
	}
	if entries == nil {
		entries = []models.PurchaseHistoryEntry{}
	}
	writeJSON(w, http.StatusOK, entries)
}
