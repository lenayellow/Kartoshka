package notifications

import "context"

// Send отправляет push-уведомление: сначала пробует FCM, при ошибке — RuStore.
// Не возвращает ошибку — сбои уведомлений не должны влиять на основной поток.
func Send(ctx context.Context, fcmToken, rustoreToken, title, body string) {
	if fcmToken != "" {
		if err := SendFCM(ctx, fcmToken, title, body); err == nil {
			return
		}
	}
	if rustoreToken != "" {
		_ = SendRuStore(ctx, rustoreToken, title, body)
	}
}
