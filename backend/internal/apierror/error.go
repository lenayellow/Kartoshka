package apierror

import (
	"encoding/json"
	"net/http"
	"os"
)

// APIError is the structured error returned to clients.
type APIError struct {
	Code      string `json:"code"`
	Message   string `json:"message"`
	RequestID string `json:"request_id,omitempty"`
	Detail    string `json:"detail,omitempty"`
}

// Body is the JSON envelope: {"error": {...}}.
type Body struct {
	Error APIError `json:"error"`
}

// Error codes — single source of truth shared with the Android client.
const (
	// Generic
	CodeInternal    = "internal_error"
	CodeBadRequest  = "bad_request"
	CodeUnauthorized = "unauthorized"
	CodeForbidden   = "forbidden"
	CodeNotFound    = "not_found"
	CodeConflict    = "conflict"
	CodeUnavailable = "service_unavailable"

	// Auth
	CodeInvalidCredentials  = "invalid_credentials"
	CodeEmailTaken          = "email_taken"
	CodeEmailNotFound       = "email_not_found"
	CodeInvalidRefreshToken = "invalid_refresh_token"
	CodeYandexAuthFailed    = "yandex_auth_failed"
	CodeWeakPassword        = "weak_password"

	// Lists & Items
	CodeListNotFound    = "list_not_found"
	CodeListAccessDenied = "list_access_denied"
	CodeItemNotFound    = "item_not_found"
	CodeItemCreateFailed = "item_create_failed"

	// Invitations
	CodeInviteSelfForbidden = "invite_self_forbidden"
	CodeInviteAlreadySent   = "invite_already_sent"
	CodeInviteAlreadyMember = "invite_already_member"
	CodeInviteUserNotFound  = "invite_user_not_found"
	CodeInviteExpired       = "invite_expired"
	CodeInviteNotFound      = "invite_not_found"
)

// Write sends a JSON error response. It reads request_id from the
// X-Request-Id response header (set by RequestID middleware). detail is
// included only when APP_ENV != "production".
func Write(w http.ResponseWriter, _ *http.Request, httpStatus int, code, message, detail string) {
	reqID := w.Header().Get("X-Request-Id")
	e := APIError{
		Code:      code,
		Message:   message,
		RequestID: reqID,
	}
	if os.Getenv("APP_ENV") != "production" {
		e.Detail = detail
	}
	w.Header().Set("Content-Type", "application/json; charset=utf-8")
	w.WriteHeader(httpStatus)
	json.NewEncoder(w).Encode(Body{Error: e})
}
