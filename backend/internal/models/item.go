package models

import "time"

type Item struct {
	ItemID     string    `json:"item_id"`
	ListID     string    `json:"list_id"`
	Name       string    `json:"name"`
	Tags       *string   `json:"tags"`        // Optional<Utf8> in YDB
	Note       *string   `json:"note"`
	CategoryID *string   `json:"category_id"` // Optional<Utf8> in YDB
	PhotoURL   *string   `json:"photo_url"`   // Optional<Utf8> in YDB
	IsDeleted  bool      `json:"is_deleted"`
	AddedBy    string    `json:"added_by"`
	UpdatedBy  string    `json:"updated_by"`
	UpdatedAt  time.Time `json:"updated_at"`
	SortIndex  *int32    `json:"sort_index"`  // Optional<Int32> in YDB
}

type PurchaseHistoryEntry struct {
	ID          string    `json:"id"`
	ItemName    string    `json:"item_name"`
	ListID      string    `json:"list_id"`
	PurchasedAt time.Time `json:"purchased_at"`
	UserID      string    `json:"user_id"`
}

type ReorderItem struct {
	ItemID    string `json:"item_id"`
	SortIndex int32  `json:"sort_index"`
}
