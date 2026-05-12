package notifications

import (
	"context"
	"crypto/rsa"
	"crypto/x509"
	"encoding/json"
	"encoding/pem"
	"fmt"
	"io"
	"net/http"
	"net/url"
	"os"
	"strings"
	"sync"
	"time"

	"github.com/golang-jwt/jwt/v4"
)

type serviceAccountKey struct {
	PrivateKey  string `json:"private_key"`
	ClientEmail string `json:"client_email"`
	TokenURI    string `json:"token_uri"`
}

var fcmCache struct {
	mu        sync.Mutex
	token     string
	expiresAt time.Time
}

func getFCMAccessToken(ctx context.Context) (string, error) {
	fcmCache.mu.Lock()
	defer fcmCache.mu.Unlock()

	if fcmCache.token != "" && time.Now().Before(fcmCache.expiresAt) {
		return fcmCache.token, nil
	}

	keyFile := os.Getenv("FCM_SERVICE_ACCOUNT_JSON")
	if keyFile == "" {
		return "", fmt.Errorf("FCM_SERVICE_ACCOUNT_JSON не задан")
	}

	data, err := os.ReadFile(keyFile)
	if err != nil {
		return "", fmt.Errorf("чтение ключа FCM: %w", err)
	}

	var saKey serviceAccountKey
	if err := json.Unmarshal(data, &saKey); err != nil {
		return "", fmt.Errorf("парсинг ключа FCM: %w", err)
	}

	block, _ := pem.Decode([]byte(saKey.PrivateKey))
	if block == nil {
		return "", fmt.Errorf("некорректный PEM ключ FCM")
	}
	raw, err := x509.ParsePKCS8PrivateKey(block.Bytes)
	if err != nil {
		return "", fmt.Errorf("парсинг RSA ключа FCM: %w", err)
	}
	rsaKey, ok := raw.(*rsa.PrivateKey)
	if !ok {
		return "", fmt.Errorf("FCM ключ не является RSA")
	}

	tokenURI := saKey.TokenURI
	if tokenURI == "" {
		tokenURI = "https://oauth2.googleapis.com/token"
	}

	now := time.Now()
	jwtToken := jwt.NewWithClaims(jwt.SigningMethodRS256, jwt.MapClaims{
		"iss":   saKey.ClientEmail,
		"scope": "https://www.googleapis.com/auth/firebase.messaging",
		"aud":   tokenURI,
		"exp":   now.Add(time.Hour).Unix(),
		"iat":   now.Unix(),
	})
	signed, err := jwtToken.SignedString(rsaKey)
	if err != nil {
		return "", fmt.Errorf("подпись JWT FCM: %w", err)
	}

	resp, err := http.PostForm(tokenURI, url.Values{
		"grant_type": {"urn:ietf:params:oauth:grant-type:jwt-bearer"},
		"assertion":  {signed},
	})
	if err != nil {
		return "", fmt.Errorf("запрос токена FCM: %w", err)
	}
	defer resp.Body.Close()
	body, _ := io.ReadAll(resp.Body)
	if resp.StatusCode != http.StatusOK {
		return "", fmt.Errorf("ошибка токена FCM %d: %s", resp.StatusCode, body)
	}

	var result struct {
		AccessToken string `json:"access_token"`
		ExpiresIn   int    `json:"expires_in"`
	}
	if err := json.Unmarshal(body, &result); err != nil {
		return "", fmt.Errorf("парсинг токена FCM: %w", err)
	}

	fcmCache.token = result.AccessToken
	fcmCache.expiresAt = now.Add(time.Duration(result.ExpiresIn-60) * time.Second)
	return result.AccessToken, nil
}

// SendFCM отправляет push-уведомление через FCM HTTP v1 API.
func SendFCM(ctx context.Context, deviceToken, title, body string) error {
	projectID := os.Getenv("FCM_PROJECT_ID")
	if projectID == "" {
		return fmt.Errorf("FCM_PROJECT_ID не задан")
	}

	accessToken, err := getFCMAccessToken(ctx)
	if err != nil {
		return err
	}

	payload, _ := json.Marshal(map[string]interface{}{
		"message": map[string]interface{}{
			"token": deviceToken,
			"notification": map[string]string{
				"title": title,
				"body":  body,
			},
		},
	})

	endpoint := fmt.Sprintf("https://fcm.googleapis.com/v1/projects/%s/messages:send", projectID)
	req, err := http.NewRequestWithContext(ctx, http.MethodPost, endpoint, strings.NewReader(string(payload)))
	if err != nil {
		return err
	}
	req.Header.Set("Authorization", "Bearer "+accessToken)
	req.Header.Set("Content-Type", "application/json")

	resp, err := http.DefaultClient.Do(req)
	if err != nil {
		return fmt.Errorf("FCM запрос: %w", err)
	}
	defer resp.Body.Close()
	if resp.StatusCode != http.StatusOK {
		b, _ := io.ReadAll(resp.Body)
		return fmt.Errorf("FCM ошибка %d: %s", resp.StatusCode, b)
	}
	return nil
}
