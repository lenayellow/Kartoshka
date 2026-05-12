package models

import "time"

type List struct {
	ListID           string    `json:"list_id"`
	OwnerID          string    `json:"owner_id"`
	Title            string    `json:"title"`
	ColorValue       int64     `json:"color_value"`
	Position         int32     `json:"position"`
	CategoryOrder    string    `json:"category_order"`    // JSON-строка: ["cat1","cat3"]
	HiddenCategories string    `json:"hidden_categories"` // JSON-строка: ["cat4"]
	CreatedAt        time.Time `json:"created_at"`
	UpdatedAt        time.Time `json:"updated_at"`
}

type ListMember struct {
	ListID    string    `json:"list_id"`
	UserID    string    `json:"user_id"`
	Role      string    `json:"role"` // "owner" | "editor"
	JoinedAt  time.Time `json:"joined_at"`
	UserName  string    `json:"name"`
	UserEmail string    `json:"email"`
	AvatarUrl string    `json:"avatar_url"`
}
