package middleware

import (
	"context"
	"net/http"
	"time"
)

// RequestTimeout adds a deadline to every request context.
// All downstream code using r.Context() (YDB queries, S3 calls) inherits this deadline.
func RequestTimeout(d time.Duration) func(http.Handler) http.Handler {
	return func(next http.Handler) http.Handler {
		return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
			ctx, cancel := context.WithTimeout(r.Context(), d)
			defer cancel()
			next.ServeHTTP(w, r.WithContext(ctx))
		})
	}
}
