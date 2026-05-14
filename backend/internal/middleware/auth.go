package middleware

import (
	"context"
	"net/http"
	"strings"

	"github.com/lena/kartoshka-backend/internal/apierror"
	"github.com/lena/kartoshka-backend/internal/auth"
)

type contextKey string

const UserIDKey contextKey = "userID"

// Bearer проверяет заголовок Authorization: Bearer <token> и кладёт userID в контекст.
func Bearer(next http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		header := r.Header.Get("Authorization")
		if !strings.HasPrefix(header, "Bearer ") {
			apierror.Write(w, r, http.StatusUnauthorized, apierror.CodeUnauthorized, "Authentication required", "")
			return
		}
		userID, err := auth.ParseAccessToken(strings.TrimPrefix(header, "Bearer "))
		if err != nil {
			apierror.Write(w, r, http.StatusUnauthorized, apierror.CodeUnauthorized, "Invalid or expired token", "")
			return
		}
		if a := getAttrs(r.Context()); a != nil {
			a.UserID = userID
		}
		ctx := context.WithValue(r.Context(), UserIDKey, userID)
		next.ServeHTTP(w, r.WithContext(ctx))
	})
}

// GetUserID достаёт userID из контекста запроса.
func GetUserID(r *http.Request) string {
	v, _ := r.Context().Value(UserIDKey).(string)
	return v
}
