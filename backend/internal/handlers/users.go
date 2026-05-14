package handlers

import (
	"encoding/json"
	"fmt"
	"io"
	"log/slog"
	"net/http"

	"github.com/google/uuid"

	"github.com/lena/kartoshka-backend/internal/apierror"
	"github.com/lena/kartoshka-backend/internal/middleware"
	"github.com/lena/kartoshka-backend/internal/repository"
	"github.com/lena/kartoshka-backend/internal/storage"
)

type UserHandler struct {
	users  *repository.UserRepo
	tokens *repository.PushTokenRepo
	store  *storage.S3Client // nil если S3 не настроен
	logger *slog.Logger
}

func NewUserHandler(users *repository.UserRepo, tokens *repository.PushTokenRepo, store *storage.S3Client, logger *slog.Logger) *UserHandler {
	return &UserHandler{users: users, tokens: tokens, store: store, logger: logger}
}

// GET /users/me
func (h *UserHandler) GetMe(w http.ResponseWriter, r *http.Request) {
	userID := middleware.GetUserID(r)
	user, err := h.users.GetByID(r.Context(), userID)
	if err != nil || user == nil {
		apierror.Write(w, r, http.StatusNotFound, apierror.CodeNotFound, "User not found", "")
		return
	}
	writeJSON(w, http.StatusOK, user.Public())
}

// PUT /users/me
func (h *UserHandler) UpdateMe(w http.ResponseWriter, r *http.Request) {
	userID := middleware.GetUserID(r)
	var req struct {
		Name string `json:"name"`
	}
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil || req.Name == "" {
		apierror.Write(w, r, http.StatusBadRequest, apierror.CodeBadRequest, "name is required", "")
		return
	}
	if err := h.users.UpdateName(r.Context(), userID, req.Name); err != nil {
		apierror.Write(w, r, http.StatusInternalServerError, apierror.CodeInternal, "Failed to update profile", err.Error())
		return
	}
	user, err := h.users.GetByID(r.Context(), userID)
	if err != nil || user == nil {
		apierror.Write(w, r, http.StatusInternalServerError, apierror.CodeInternal, "Failed to read profile", "")
		return
	}
	writeJSON(w, http.StatusOK, user.Public())
}

// POST /users/me/avatar — загрузить аватарку (multipart/form-data, поле "avatar")
func (h *UserHandler) UploadAvatar(w http.ResponseWriter, r *http.Request) {
	if h.store == nil {
		apierror.Write(w, r, http.StatusNotImplemented, apierror.CodeUnavailable, "File upload not configured", "")
		return
	}
	userID := middleware.GetUserID(r)

	if err := r.ParseMultipartForm(5 << 20); err != nil {
		apierror.Write(w, r, http.StatusBadRequest, apierror.CodeBadRequest, "File too large (max 5 MB)", err.Error())
		return
	}
	file, header, err := r.FormFile("avatar")
	if err != nil {
		apierror.Write(w, r, http.StatusBadRequest, apierror.CodeBadRequest, "avatar field is required", err.Error())
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

	key := fmt.Sprintf("avatars/%s/%s.jpg", userID, uuid.New().String())
	url, err := h.store.UploadFile(r.Context(), key, contentType, data)
	if err != nil {
		apierror.Write(w, r, http.StatusInternalServerError, apierror.CodeInternal, "Failed to upload avatar", err.Error())
		return
	}

	if err := h.users.UpdateAvatarURL(r.Context(), userID, url); err != nil {
		apierror.Write(w, r, http.StatusInternalServerError, apierror.CodeInternal, "Failed to update profile", err.Error())
		return
	}
	writeJSON(w, http.StatusOK, map[string]string{"avatar_url": url})
}

// POST /users/me/push-token — сохранить FCM/RuStore токен устройства
func (h *UserHandler) SavePushToken(w http.ResponseWriter, r *http.Request) {
	userID := middleware.GetUserID(r)
	var req struct {
		DeviceID     string `json:"device_id"`
		FCMToken     string `json:"fcm_token"`
		RuStoreToken string `json:"rustore_token"`
	}
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil || req.DeviceID == "" {
		apierror.Write(w, r, http.StatusBadRequest, apierror.CodeBadRequest, "device_id is required", "")
		return
	}
	if err := h.tokens.Save(r.Context(), userID, req.DeviceID, req.FCMToken, req.RuStoreToken); err != nil {
		apierror.Write(w, r, http.StatusInternalServerError, apierror.CodeInternal, "Failed to save push token", err.Error())
		return
	}
	w.WriteHeader(http.StatusNoContent)
}
