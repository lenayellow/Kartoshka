package handlers

import (
	"encoding/json"
	"fmt"
	"io"
	"net/http"

	"github.com/google/uuid"

	"github.com/lena/kartoshka-backend/internal/middleware"
	"github.com/lena/kartoshka-backend/internal/repository"
	"github.com/lena/kartoshka-backend/internal/storage"
)

type UserHandler struct {
	users  *repository.UserRepo
	tokens *repository.PushTokenRepo
	store  *storage.S3Client // nil если S3 не настроен
}

func NewUserHandler(users *repository.UserRepo, tokens *repository.PushTokenRepo, store *storage.S3Client) *UserHandler {
	return &UserHandler{users: users, tokens: tokens, store: store}
}

// GET /users/me
func (h *UserHandler) GetMe(w http.ResponseWriter, r *http.Request) {
	userID := middleware.GetUserID(r)
	user, err := h.users.GetByID(r.Context(), userID)
	if err != nil || user == nil {
		http.Error(w, "пользователь не найден", http.StatusNotFound)
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
		http.Error(w, "поле name обязательно", http.StatusBadRequest)
		return
	}
	if err := h.users.UpdateName(r.Context(), userID, req.Name); err != nil {
		http.Error(w, "ошибка обновления", http.StatusInternalServerError)
		return
	}
	user, err := h.users.GetByID(r.Context(), userID)
	if err != nil || user == nil {
		http.Error(w, "ошибка чтения профиля", http.StatusInternalServerError)
		return
	}
	writeJSON(w, http.StatusOK, user.Public())
}

// POST /users/me/avatar — загрузить аватарку (multipart/form-data, поле "avatar")
func (h *UserHandler) UploadAvatar(w http.ResponseWriter, r *http.Request) {
	if h.store == nil {
		http.Error(w, "загрузка файлов не настроена (S3)", http.StatusNotImplemented)
		return
	}
	userID := middleware.GetUserID(r)

	if err := r.ParseMultipartForm(5 << 20); err != nil {
		http.Error(w, "файл слишком большой (макс. 5 МБ)", http.StatusBadRequest)
		return
	}
	file, header, err := r.FormFile("avatar")
	if err != nil {
		http.Error(w, "поле avatar обязательно", http.StatusBadRequest)
		return
	}
	defer file.Close()

	data, err := io.ReadAll(file)
	if err != nil {
		http.Error(w, "ошибка чтения файла", http.StatusInternalServerError)
		return
	}

	contentType := header.Header.Get("Content-Type")
	if contentType == "" {
		contentType = "image/jpeg"
	}

	key := fmt.Sprintf("avatars/%s/%s.jpg", userID, uuid.New().String())
	url, err := h.store.UploadFile(r.Context(), key, contentType, data)
	if err != nil {
		http.Error(w, "ошибка загрузки файла", http.StatusInternalServerError)
		return
	}

	if err := h.users.UpdateAvatarURL(r.Context(), userID, url); err != nil {
		http.Error(w, "ошибка обновления профиля", http.StatusInternalServerError)
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
		http.Error(w, "поле device_id обязательно", http.StatusBadRequest)
		return
	}
	if err := h.tokens.Save(r.Context(), userID, req.DeviceID, req.FCMToken, req.RuStoreToken); err != nil {
		http.Error(w, "ошибка сохранения токена", http.StatusInternalServerError)
		return
	}
	w.WriteHeader(http.StatusNoContent)
}
