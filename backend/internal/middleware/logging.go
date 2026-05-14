package middleware

import (
	"log/slog"
	"net/http"
	"time"
)

type responseWriter struct {
	http.ResponseWriter
	status int
}

func (rw *responseWriter) WriteHeader(code int) {
	rw.status = code
	rw.ResponseWriter.WriteHeader(code)
}

// Logging replaces chimiddleware.Logger with structured slog output.
// Logs method, path, status, latency_ms, request_id and user_id per request.
// user_id is populated by the Bearer middleware via requestAttrs mutation,
// so it is present for authenticated routes and empty for public ones.
func Logging(logger *slog.Logger) func(http.Handler) http.Handler {
	return func(next http.Handler) http.Handler {
		return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
			start := time.Now()
			rw := &responseWriter{ResponseWriter: w, status: http.StatusOK}
			next.ServeHTTP(rw, r)
			attrs := getAttrs(r.Context())
			reqID, userID := "", ""
			if attrs != nil {
				reqID = attrs.RequestID
				userID = attrs.UserID
			}
			logger.Info("request",
				"method", r.Method,
				"path", r.URL.Path,
				"status", rw.status,
				"latency_ms", time.Since(start).Milliseconds(),
				"request_id", reqID,
				"user_id", userID,
			)
		})
	}
}
