package middleware

import (
	"net/http"
	"time"

	"github.com/go-chi/httprate"
	"github.com/lena/kartoshka-backend/internal/apierror"
)

func RateLimitByIP(limit int, window time.Duration) func(http.Handler) http.Handler {
	return httprate.Limit(limit, window,
		httprate.WithKeyFuncs(httprate.KeyByIP),
		httprate.WithLimitHandler(func(w http.ResponseWriter, r *http.Request) {
			apierror.Write(w, r, http.StatusTooManyRequests,
				apierror.CodeTooManyRequests, "Too many requests, slow down", "")
		}),
	)
}

func RateLimitByUserID(limit int, window time.Duration) func(http.Handler) http.Handler {
	return httprate.Limit(limit, window,
		httprate.WithKeyFuncs(func(r *http.Request) (string, error) {
			return GetUserID(r), nil
		}),
		httprate.WithLimitHandler(func(w http.ResponseWriter, r *http.Request) {
			apierror.Write(w, r, http.StatusTooManyRequests,
				apierror.CodeTooManyRequests, "Too many invites, try again later", "")
		}),
	)
}
