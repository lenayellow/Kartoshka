package models

type LoyaltyCard struct {
	CardID        string `json:"card_id"`
	UserID        string `json:"user_id"`
	Name          string `json:"name"`
	BarcodeValue  string `json:"barcode_value"`
	BarcodeFormat int32  `json:"barcode_format"` // ZXing BarcodeFormat ordinal (как в LoyaltyCard.kt)
	Color         int64  `json:"color"`           // ARGB Long (как в LoyaltyCard.kt)
}
