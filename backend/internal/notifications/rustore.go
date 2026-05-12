package notifications

import (
	"context"
	"encoding/json"
	"fmt"
	"io"
	"net/http"
	"os"
	"strings"
)

// SendRuStore отправляет push-уведомление через RuStore Push API.
func SendRuStore(ctx context.Context, deviceToken, title, body string) error {
	projectID := os.Getenv("RUSTORE_PROJECT_ID")
	serviceToken := os.Getenv("RUSTORE_SERVICE_TOKEN")

	if projectID == "" || serviceToken == "" {
		return fmt.Errorf("RuStore Push не настроен")
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

	endpoint := fmt.Sprintf("https://vkpns.rustore.ru/api/v1/projects/%s/messages:send", projectID)
	req, err := http.NewRequestWithContext(ctx, http.MethodPost, endpoint, strings.NewReader(string(payload)))
	if err != nil {
		return err
	}
	req.Header.Set("Authorization", serviceToken)
	req.Header.Set("Content-Type", "application/json")

	resp, err := http.DefaultClient.Do(req)
	if err != nil {
		return fmt.Errorf("RuStore запрос: %w", err)
	}
	defer resp.Body.Close()
	if resp.StatusCode != http.StatusOK {
		b, _ := io.ReadAll(resp.Body)
		return fmt.Errorf("RuStore ошибка %d: %s", resp.StatusCode, b)
	}
	return nil
}
