package auth

import (
	"context"
	"encoding/json"
	"fmt"
	"io"
	"net/http"
	"net/url"
	"os"
	"strings"

	"github.com/lena/kartoshka-backend/internal/httpclient"
	"github.com/lena/kartoshka-backend/internal/models"
)

var yandexClient = httpclient.YandexOAuth()

type yandexTokenResp struct {
	AccessToken string `json:"access_token"`
}

// ExchangeYandexCode обменивает OAuth-код на профиль пользователя Яндекса.
func ExchangeYandexCode(ctx context.Context, code string) (*models.YandexProfile, error) {
	data := url.Values{
		"grant_type":    {"authorization_code"},
		"code":          {code},
		"client_id":     {os.Getenv("YANDEX_CLIENT_ID")},
		"client_secret": {os.Getenv("YANDEX_CLIENT_SECRET")},
	}

	req, err := http.NewRequestWithContext(ctx, http.MethodPost,
		"https://oauth.yandex.ru/token", strings.NewReader(data.Encode()))
	if err != nil {
		return nil, err
	}
	req.Header.Set("Content-Type", "application/x-www-form-urlencoded")

	resp, err := yandexClient.Do(req)
	if err != nil {
		return nil, fmt.Errorf("yandex token request: %w", err)
	}
	defer resp.Body.Close()
	body, _ := io.ReadAll(resp.Body)

	if resp.StatusCode != http.StatusOK {
		return nil, fmt.Errorf("yandex token %d: %s", resp.StatusCode, body)
	}

	var tokenResp yandexTokenResp
	if err := json.Unmarshal(body, &tokenResp); err != nil {
		return nil, fmt.Errorf("parse yandex token: %w", err)
	}

	return fetchYandexProfile(ctx, tokenResp.AccessToken)
}

func fetchYandexProfile(ctx context.Context, yandexToken string) (*models.YandexProfile, error) {
	req, err := http.NewRequestWithContext(ctx, http.MethodGet,
		"https://login.yandex.ru/info?format=json", nil)
	if err != nil {
		return nil, err
	}
	req.Header.Set("Authorization", "OAuth "+yandexToken)

	resp, err := yandexClient.Do(req)
	if err != nil {
		return nil, fmt.Errorf("yandex info request: %w", err)
	}
	defer resp.Body.Close()
	body, _ := io.ReadAll(resp.Body)

	if resp.StatusCode != http.StatusOK {
		return nil, fmt.Errorf("yandex info %d: %s", resp.StatusCode, body)
	}

	var profile models.YandexProfile
	if err := json.Unmarshal(body, &profile); err != nil {
		return nil, fmt.Errorf("parse yandex profile: %w", err)
	}
	return &profile, nil
}
