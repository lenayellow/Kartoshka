package middleware

import (
	"fmt"
	"log/slog"
	"net/http"
	"runtime/debug"

	"github.com/lena/kartoshka-backend/internal/apierror"
)

// Recovery replaces chimiddleware.Recoverer. Logs panic value + full stack
// trace via slog, then returns a structured JSON 500 error to the client.
func Recovery(logger *slog.Logger) func(http.Handler) http.Handler {
	return func(next http.Handler) http.Handler {
		return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
			defer func() {
				if rec := recover(); rec != nil {
					logger.Error("panic recovered",
						"panic", rec,
						"stack", string(debug.Stack()),
						"request_id", GetRequestID(r.Context()),
					)
					apierror.Write(w, r, http.StatusInternalServerError,
						apierror.CodeInternal, "internal server error",
						fmt.Sprintf("%v", rec))
				}
			}()
			next.ServeHTTP(w, r)
		})
	}
}
