package models

type TokenPair struct {
	AccessToken  string `json:"access_token"`
	RefreshToken string `json:"refresh_token"`
}

// YandexProfile — данные профиля от Yandex ID API
type YandexProfile struct {
	UID             string `json:"id"`
	Login           string `json:"login"`
	DefaultEmail    string `json:"default_email"`
	RealName        string `json:"real_name"`
	DefaultAvatarID string `json:"default_avatar_id"`
}
