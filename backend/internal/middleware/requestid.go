package middleware

import (
	"context"
	"net/http"

	"github.com/google/uuid"
)

const RequestIDKey contextKey = "request_id"

// requestAttrs is a mutable bag attached to each request context via a pointer.
// All middleware in the chain can mutate the same struct so that Logging,
// which wraps everything, can read fields (user_id, request_id) set by later
// middleware (e.g. Bearer) after next.ServeHTTP returns.
type requestAttrs struct {
	RequestID string
	UserID    string
}

const attrsKey contextKey = "attrs"

func getAttrs(ctx context.Context) *requestAttrs {
	a, _ := ctx.Value(attrsKey).(*requestAttrs)
	return a
}

// RequestID reads X-Request-Id from the incoming request or generates a new UUID,
// writes it back in the response header, and stores it in the request context.
func RequestID(next http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		id := r.Header.Get("X-Request-Id")
		if id == "" {
			id = uuid.NewString()
		}
		w.Header().Set("X-Request-Id", id)
		attrs := &requestAttrs{RequestID: id}
		ctx := context.WithValue(r.Context(), attrsKey, attrs)
		next.ServeHTTP(w, r.WithContext(ctx))
	})
}

// GetRequestID returns the request ID stored in the context.
func GetRequestID(ctx context.Context) string {
	if a := getAttrs(ctx); a != nil {
		return a.RequestID
	}
	return ""
}
