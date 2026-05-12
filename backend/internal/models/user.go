package models

import "time"

type User struct {
	UserID       string
	YandexUID    string
	Email        string
	Name         string
	AvatarURL    string
	PasswordHash string // пусто для Yandex-пользователей
	IsVerified   bool   // для email-пользователей; Yandex-пользователи всегда считаются верифицированными
	CreatedAt    time.Time
	UpdatedAt    time.Time
}

// UserResponse — публичное представление пользователя без чувствительных полей.
type UserResponse struct {
	UserID    string    `json:"user_id"`
	Email     string    `json:"email"`
	Name      string    `json:"name"`
	AvatarURL string    `json:"avatar_url"`
	CreatedAt time.Time `json:"created_at"`
	UpdatedAt time.Time `json:"updated_at"`
}

func (u *User) Public() UserResponse {
	return UserResponse{
		UserID:    u.UserID,
		Email:     u.Email,
		Name:      u.Name,
		AvatarURL: u.AvatarURL,
		CreatedAt: u.CreatedAt,
		UpdatedAt: u.UpdatedAt,
	}
}
