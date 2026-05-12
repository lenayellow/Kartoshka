package models

import "time"

type Invitation struct {
	InviteToken  string    `json:"invite_token"`
	ListID       string    `json:"list_id"`
	InviterID    string    `json:"inviter_id"`
	InviteeEmail string    `json:"invitee_email,omitempty"`
	Status       string    `json:"status"` // "pending" | "accepted" | "expired"
	CreatedAt    time.Time `json:"created_at"`
	ExpiresAt    time.Time `json:"expires_at"`
}

// InviteInfo — публичная информация для GET /invite/{token} (без авторизации).
type InviteInfo struct {
	InviteToken string    `json:"invite_token"`
	ListTitle   string    `json:"list_title"`
	InviterName string    `json:"inviter_name"`
	Status      string    `json:"status"`
	ExpiresAt   time.Time `json:"expires_at"`
}
