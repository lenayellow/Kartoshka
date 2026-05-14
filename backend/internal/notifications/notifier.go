package notifications

import (
	"context"
	"log/slog"

	"github.com/lena/kartoshka-backend/internal/repository"
)

type Notifier struct {
	tokens *repository.PushTokenRepo
	logger *slog.Logger
}

func NewNotifier(tokens *repository.PushTokenRepo, logger *slog.Logger) *Notifier {
	return &Notifier{tokens: tokens, logger: logger}
}

// NotifyListChanged рассылает push-уведомление всем участникам списка кроме инициатора.
// Вызывать в горутине — ошибки только логируются.
func (n *Notifier) NotifyListChanged(ctx context.Context, listID, senderUserID, title, body string) {
	tokenInfos, err := n.tokens.GetByListMembers(ctx, listID)
	if err != nil {
		n.logger.Error("push token fetch failed", "list_id", listID, "err", err)
		return
	}
	for _, info := range tokenInfos {
		if info.UserID == senderUserID {
			continue
		}
		Send(ctx, info.FCMToken, info.RuStoreToken, title, body)
	}
}
