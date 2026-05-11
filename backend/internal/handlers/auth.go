package handlers

import (
	"context"
	"encoding/json"
	"fmt"
	"net/http"
	"os"
	"time"

	"github.com/lena/kartoshka-backend/internal/auth"
	"github.com/lena/kartoshka-backend/internal/models"
	"github.com/lena/kartoshka-backend/internal/notifications"
	"github.com/lena/kartoshka-backend/internal/repository"
)

type AuthHandler struct {
	users  *repository.UserRepo
	tokens *repository.TokenRepo
}

func NewAuthHandler(users *repository.UserRepo, tokens *repository.TokenRepo) *AuthHandler {
	return &AuthHandler{users: users, tokens: tokens}
}

// POST /auth/yandex — обмен OAuth-кода на JWT-пару
func (h *AuthHandler) YandexLogin(w http.ResponseWriter, r *http.Request) {
	var req struct {
		Code string `json:"code"`
	}
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil || req.Code == "" {
		http.Error(w, "поле code обязательно", http.StatusBadRequest)
		return
	}

	profile, err := auth.ExchangeYandexCode(r.Context(), req.Code)
	if err != nil {
		http.Error(w, "ошибка Yandex OAuth: "+err.Error(), http.StatusBadGateway)
		return
	}

	user, err := h.users.GetByYandexUID(r.Context(), profile.UID)
	if err != nil {
		http.Error(w, "ошибка базы данных", http.StatusInternalServerError)
		return
	}
	if user == nil {
		user, err = h.users.Create(r.Context(), profile.UID, profile.DefaultEmail, profile.RealName)
		if err != nil {
			http.Error(w, "ошибка создания пользователя", http.StatusInternalServerError)
			return
		}
	}

	pair, err := h.issueTokenPair(r.Context(), user.UserID)
	if err != nil {
		http.Error(w, "ошибка выдачи токена", http.StatusInternalServerError)
		return
	}
	writeJSON(w, http.StatusOK, pair)
}

// POST /auth/email/register — регистрация по email
func (h *AuthHandler) EmailRegister(w http.ResponseWriter, r *http.Request) {
	var req struct {
		Email    string `json:"email"`
		Password string `json:"password"`
		Name     string `json:"name"`
	}
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		http.Error(w, "неверный формат запроса", http.StatusBadRequest)
		return
	}
	if req.Email == "" || req.Password == "" || req.Name == "" {
		http.Error(w, "email, password и name обязательны", http.StatusBadRequest)
		return
	}
	if len(req.Password) < 8 {
		http.Error(w, "пароль должен быть не менее 8 символов", http.StatusBadRequest)
		return
	}

	existing, err := h.users.GetByEmail(r.Context(), req.Email)
	if err != nil {
		http.Error(w, "ошибка базы данных", http.StatusInternalServerError)
		return
	}
	if existing != nil {
		http.Error(w, "пользователь с таким email уже существует", http.StatusConflict)
		return
	}

	user, err := h.users.Create(r.Context(), "", req.Email, req.Name)
	if err != nil {
		http.Error(w, "ошибка создания пользователя", http.StatusInternalServerError)
		return
	}

	hash, err := auth.HashPassword(req.Password)
	if err != nil {
		http.Error(w, "ошибка хеширования пароля", http.StatusInternalServerError)
		return
	}
	if err := h.users.SetPasswordHash(r.Context(), user.UserID, hash); err != nil {
		http.Error(w, "ошибка сохранения пароля", http.StatusInternalServerError)
		return
	}
	if err := h.users.SetVerified(r.Context(), user.UserID); err != nil {
		http.Error(w, "ошибка подтверждения пользователя", http.StatusInternalServerError)
		return
	}

	pair, err := h.issueTokenPair(r.Context(), user.UserID)
	if err != nil {
		http.Error(w, "ошибка выдачи токена", http.StatusInternalServerError)
		return
	}
	writeJSON(w, http.StatusCreated, pair)
}

// POST /auth/email/login — вход по email и паролю
func (h *AuthHandler) EmailLogin(w http.ResponseWriter, r *http.Request) {
	var req struct {
		Email    string `json:"email"`
		Password string `json:"password"`
	}
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil || req.Email == "" || req.Password == "" {
		http.Error(w, "email и password обязательны", http.StatusBadRequest)
		return
	}

	user, err := h.users.GetByEmail(r.Context(), req.Email)
	if err != nil {
		http.Error(w, "ошибка базы данных", http.StatusInternalServerError)
		return
	}
	if user == nil || user.PasswordHash == "" || !auth.CheckPassword(user.PasswordHash, req.Password) {
		http.Error(w, "неверный email или пароль", http.StatusUnauthorized)
		return
	}

	pair, err := h.issueTokenPair(r.Context(), user.UserID)
	if err != nil {
		http.Error(w, "ошибка выдачи токена", http.StatusInternalServerError)
		return
	}
	writeJSON(w, http.StatusOK, pair)
}

// GET /auth/email/confirm?token=... — подтверждение email по ссылке из письма
func (h *AuthHandler) ConfirmEmail(w http.ResponseWriter, r *http.Request) {
	token := r.URL.Query().Get("token")
	if token == "" {
		http.Error(w, "токен отсутствует", http.StatusBadRequest)
		return
	}

	userID, err := auth.ParseConfirmToken(token)
	if err != nil {
		http.Error(w, "недействительная или устаревшая ссылка", http.StatusBadRequest)
		return
	}

	if err := h.users.SetVerified(r.Context(), userID); err != nil {
		http.Error(w, "ошибка подтверждения", http.StatusInternalServerError)
		return
	}

	w.Header().Set("Content-Type", "text/html; charset=utf-8")
	fmt.Fprint(w, `<html><body style="font-family:sans-serif;text-align:center;padding:60px">
<h2>Email подтверждён!</h2>
<p>Можете вернуться в приложение и войти.</p>
</body></html>`)
}

// POST /auth/refresh — обновление access token по refresh token
func (h *AuthHandler) Refresh(w http.ResponseWriter, r *http.Request) {
	var req struct {
		RefreshToken string `json:"refresh_token"`
	}
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil || req.RefreshToken == "" {
		http.Error(w, "поле refresh_token обязательно", http.StatusBadRequest)
		return
	}

	userID, expiresAt, err := h.tokens.Get(r.Context(), req.RefreshToken)
	if err != nil || time.Now().After(expiresAt) {
		http.Error(w, "токен недействителен", http.StatusUnauthorized)
		return
	}

	_ = h.tokens.Delete(r.Context(), req.RefreshToken)

	pair, err := h.issueTokenPair(r.Context(), userID)
	if err != nil {
		http.Error(w, "ошибка выдачи токена", http.StatusInternalServerError)
		return
	}
	writeJSON(w, http.StatusOK, pair)
}

// POST /auth/logout — инвалидация refresh token
func (h *AuthHandler) Logout(w http.ResponseWriter, r *http.Request) {
	var req struct {
		RefreshToken string `json:"refresh_token"`
	}
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil || req.RefreshToken == "" {
		http.Error(w, "поле refresh_token обязательно", http.StatusBadRequest)
		return
	}
	_ = h.tokens.Delete(r.Context(), req.RefreshToken)
	w.WriteHeader(http.StatusNoContent)
}

func (h *AuthHandler) issueTokenPair(ctx context.Context, userID string) (*models.TokenPair, error) {
	accessToken, err := auth.GenerateAccessToken(userID)
	if err != nil {
		return nil, err
	}
	refreshTTL, err := time.ParseDuration(os.Getenv("JWT_REFRESH_TTL"))
	if err != nil {
		refreshTTL = 30 * 24 * time.Hour
	}
	refreshToken, err := h.tokens.Save(ctx, userID, time.Now().Add(refreshTTL))
	if err != nil {
		return nil, err
	}
	return &models.TokenPair{
		AccessToken:  accessToken,
		RefreshToken: refreshToken,
	}, nil
}

func writeJSON(w http.ResponseWriter, status int, v interface{}) {
	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(status)
	json.NewEncoder(w).Encode(v)
}
