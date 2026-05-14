package handlers

import (
	"context"
	"encoding/json"
	"fmt"
	"log/slog"
	"net/http"
	"os"
	"time"

	"github.com/lena/kartoshka-backend/internal/apierror"
	"github.com/lena/kartoshka-backend/internal/auth"
	"github.com/lena/kartoshka-backend/internal/models"
	"github.com/lena/kartoshka-backend/internal/notifications"
	"github.com/lena/kartoshka-backend/internal/repository"
)

type AuthHandler struct {
	users  *repository.UserRepo
	tokens *repository.TokenRepo
	logger *slog.Logger
}

func NewAuthHandler(users *repository.UserRepo, tokens *repository.TokenRepo, logger *slog.Logger) *AuthHandler {
	return &AuthHandler{users: users, tokens: tokens, logger: logger}
}

// POST /auth/yandex — обмен OAuth-кода на JWT-пару
func (h *AuthHandler) YandexLogin(w http.ResponseWriter, r *http.Request) {
	var req struct {
		Code string `json:"code"`
	}
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil || req.Code == "" {
		apierror.Write(w, r, http.StatusBadRequest, apierror.CodeBadRequest, "OAuth code is required", "")
		return
	}

	profile, err := auth.ExchangeYandexCode(r.Context(), req.Code)
	if err != nil {
		apierror.Write(w, r, http.StatusBadGateway, apierror.CodeYandexAuthFailed, "Yandex authentication failed", err.Error())
		return
	}

	user, err := h.users.GetByYandexUID(r.Context(), profile.UID)
	if err != nil {
		apierror.Write(w, r, http.StatusInternalServerError, apierror.CodeInternal, "Internal error", err.Error())
		return
	}
	if user == nil {
		user, err = h.users.Create(r.Context(), profile.UID, profile.DefaultEmail, profile.RealName)
		if err != nil {
			apierror.Write(w, r, http.StatusInternalServerError, apierror.CodeInternal, "Failed to create user", err.Error())
			return
		}
	}

	pair, err := h.issueTokenPair(r.Context(), user.UserID)
	if err != nil {
		apierror.Write(w, r, http.StatusInternalServerError, apierror.CodeInternal, "Failed to issue token", err.Error())
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
		apierror.Write(w, r, http.StatusBadRequest, apierror.CodeBadRequest, "Invalid request format", err.Error())
		return
	}
	if req.Email == "" || req.Password == "" || req.Name == "" {
		apierror.Write(w, r, http.StatusBadRequest, apierror.CodeBadRequest, "email, password and name are required", "")
		return
	}
	if len(req.Password) < 8 {
		apierror.Write(w, r, http.StatusBadRequest, apierror.CodeWeakPassword, "Password must be at least 8 characters", "")
		return
	}

	existing, err := h.users.GetByEmail(r.Context(), req.Email)
	if err != nil {
		apierror.Write(w, r, http.StatusInternalServerError, apierror.CodeInternal, "Internal error", err.Error())
		return
	}
	if existing != nil {
		apierror.Write(w, r, http.StatusConflict, apierror.CodeEmailTaken, "Email already registered", "")
		return
	}

	user, err := h.users.Create(r.Context(), "", req.Email, req.Name)
	if err != nil {
		apierror.Write(w, r, http.StatusInternalServerError, apierror.CodeInternal, "Failed to create user", err.Error())
		return
	}

	hash, err := auth.HashPassword(req.Password)
	if err != nil {
		apierror.Write(w, r, http.StatusInternalServerError, apierror.CodeInternal, "Internal error", err.Error())
		return
	}
	if err := h.users.SetPasswordHash(r.Context(), user.UserID, hash); err != nil {
		apierror.Write(w, r, http.StatusInternalServerError, apierror.CodeInternal, "Internal error", err.Error())
		return
	}
	if err := h.users.SetVerified(r.Context(), user.UserID); err != nil {
		apierror.Write(w, r, http.StatusInternalServerError, apierror.CodeInternal, "Internal error", err.Error())
		return
	}

	pair, err := h.issueTokenPair(r.Context(), user.UserID)
	if err != nil {
		apierror.Write(w, r, http.StatusInternalServerError, apierror.CodeInternal, "Failed to issue token", err.Error())
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
		apierror.Write(w, r, http.StatusBadRequest, apierror.CodeBadRequest, "email and password are required", "")
		return
	}

	user, err := h.users.GetByEmail(r.Context(), req.Email)
	if err != nil {
		apierror.Write(w, r, http.StatusInternalServerError, apierror.CodeInternal, "Internal error", err.Error())
		return
	}
	if user == nil || user.PasswordHash == "" || !auth.CheckPassword(user.PasswordHash, req.Password) {
		apierror.Write(w, r, http.StatusUnauthorized, apierror.CodeInvalidCredentials, "Invalid email or password", "")
		return
	}

	pair, err := h.issueTokenPair(r.Context(), user.UserID)
	if err != nil {
		apierror.Write(w, r, http.StatusInternalServerError, apierror.CodeInternal, "Failed to issue token", err.Error())
		return
	}
	writeJSON(w, http.StatusOK, pair)
}

// GET /auth/email/confirm?token=... — подтверждение email по ссылке из письма
func (h *AuthHandler) ConfirmEmail(w http.ResponseWriter, r *http.Request) {
	token := r.URL.Query().Get("token")
	if token == "" {
		apierror.Write(w, r, http.StatusBadRequest, apierror.CodeBadRequest, "Confirmation token missing", "")
		return
	}

	userID, err := auth.ParseConfirmToken(token)
	if err != nil {
		apierror.Write(w, r, http.StatusBadRequest, apierror.CodeBadRequest, "Invalid or expired confirmation link", err.Error())
		return
	}

	if err := h.users.SetVerified(r.Context(), userID); err != nil {
		apierror.Write(w, r, http.StatusInternalServerError, apierror.CodeInternal, "Internal error", err.Error())
		return
	}

	w.Header().Set("Content-Type", "text/html; charset=utf-8")
	fmt.Fprint(w, `<html><body style="font-family:sans-serif;text-align:center;padding:60px">
<h2>Email подтверждён!</h2>
<p>Можете вернуться в приложение и войти.</p>
</body></html>`)
}

// POST /auth/email/forgot — запрос письма для сброса пароля
func (h *AuthHandler) ForgotPassword(w http.ResponseWriter, r *http.Request) {
	var req struct {
		Email string `json:"email"`
	}
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil || req.Email == "" {
		apierror.Write(w, r, http.StatusBadRequest, apierror.CodeBadRequest, "email is required", "")
		return
	}

	user, err := h.users.GetByEmail(r.Context(), req.Email)
	if err != nil {
		apierror.Write(w, r, http.StatusInternalServerError, apierror.CodeInternal, "Internal error", err.Error())
		return
	}
	if user == nil || user.PasswordHash == "" {
		apierror.Write(w, r, http.StatusNotFound, apierror.CodeEmailNotFound, "No account with this email", "")
		return
	}

	token, err := auth.GenerateResetToken(user.UserID)
	if err != nil {
		apierror.Write(w, r, http.StatusInternalServerError, apierror.CodeInternal, "Internal error", err.Error())
		return
	}

	baseURL := os.Getenv("APP_BASE_URL")
	resetURL := baseURL + "/auth/email/reset?token=" + token

	go func() {
		if err := notifications.SendResetEmail(user.Email, user.Name, resetURL); err != nil {
			h.logger.Error("reset email send failed", "err", err)
		}
	}()

	w.WriteHeader(http.StatusNoContent)
}

// GET /auth/email/reset?token=... — HTML-форма ввода нового пароля
func (h *AuthHandler) ResetPasswordForm(w http.ResponseWriter, r *http.Request) {
	token := r.URL.Query().Get("token")
	w.Header().Set("Content-Type", "text/html; charset=utf-8")
	if token == "" {
		w.WriteHeader(http.StatusBadRequest)
		fmt.Fprint(w, `<html><body style="font-family:sans-serif;text-align:center;padding:60px"><h2>Ссылка недействительна</h2></body></html>`)
		return
	}
	if _, err := auth.ParseResetToken(token); err != nil {
		fmt.Fprint(w, `<html><body style="font-family:sans-serif;text-align:center;padding:60px">
<h2>Ссылка устарела</h2>
<p>Запросите сброс пароля ещё раз в приложении.</p>
</body></html>`)
		return
	}
	fmt.Fprintf(w, `<html><body style="font-family:sans-serif;max-width:400px;margin:60px auto;padding:0 20px">
<h2>Новый пароль</h2>
<form method="POST" action="/auth/email/reset">
  <input type="hidden" name="token" value="%s">
  <input type="password" name="password" placeholder="Новый пароль (мин. 8 символов)"
         style="width:100%%;padding:12px;margin:12px 0;box-sizing:border-box;font-size:16px;border:1px solid #ccc;border-radius:6px">
  <button type="submit" style="background:#4CAF50;color:white;padding:12px 24px;border:none;border-radius:6px;font-size:16px;cursor:pointer;width:100%%">
    Сохранить пароль
  </button>
</form>
</body></html>`, token)
}

// POST /auth/email/reset — сохранение нового пароля (из HTML-формы)
func (h *AuthHandler) ResetPassword(w http.ResponseWriter, r *http.Request) {
	if err := r.ParseForm(); err != nil {
		apierror.Write(w, r, http.StatusBadRequest, apierror.CodeBadRequest, "Invalid request", err.Error())
		return
	}
	token := r.FormValue("token")
	password := r.FormValue("password")

	w.Header().Set("Content-Type", "text/html; charset=utf-8")
	if token == "" || len(password) < 8 {
		w.WriteHeader(http.StatusBadRequest)
		fmt.Fprint(w, `<html><body style="font-family:sans-serif;text-align:center;padding:60px">
<h2>Ошибка</h2><p>Пароль должен быть не менее 8 символов.</p>
</body></html>`)
		return
	}

	userID, err := auth.ParseResetToken(token)
	if err != nil {
		fmt.Fprint(w, `<html><body style="font-family:sans-serif;text-align:center;padding:60px">
<h2>Ссылка устарела</h2>
<p>Запросите сброс пароля ещё раз в приложении.</p>
</body></html>`)
		return
	}

	hash, err := auth.HashPassword(password)
	if err != nil {
		apierror.Write(w, r, http.StatusInternalServerError, apierror.CodeInternal, "Internal error", err.Error())
		return
	}
	if err := h.users.SetPasswordHash(r.Context(), userID, hash); err != nil {
		apierror.Write(w, r, http.StatusInternalServerError, apierror.CodeInternal, "Internal error", err.Error())
		return
	}

	fmt.Fprint(w, `<html><body style="font-family:sans-serif;text-align:center;padding:60px">
<h2>Пароль обновлён!</h2>
<p>Вернитесь в приложение и войдите с новым паролем.</p>
</body></html>`)
}

// POST /auth/refresh — обновление access token по refresh token
func (h *AuthHandler) Refresh(w http.ResponseWriter, r *http.Request) {
	var req struct {
		RefreshToken string `json:"refresh_token"`
	}
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil || req.RefreshToken == "" {
		apierror.Write(w, r, http.StatusBadRequest, apierror.CodeBadRequest, "refresh_token is required", "")
		return
	}

	userID, expiresAt, err := h.tokens.Get(r.Context(), req.RefreshToken)
	if err != nil || time.Now().After(expiresAt) {
		apierror.Write(w, r, http.StatusUnauthorized, apierror.CodeInvalidRefreshToken, "Refresh token is invalid or expired", "")
		return
	}

	_ = h.tokens.Delete(r.Context(), req.RefreshToken)

	pair, err := h.issueTokenPair(r.Context(), userID)
	if err != nil {
		apierror.Write(w, r, http.StatusInternalServerError, apierror.CodeInternal, "Failed to issue token", err.Error())
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
		apierror.Write(w, r, http.StatusBadRequest, apierror.CodeBadRequest, "refresh_token is required", "")
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
