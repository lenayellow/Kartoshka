package notifications

import (
	"context"
	"fmt"

	"github.com/lena/kartoshka-backend/internal/repository"
)

type Notifier struct {
	tokens *repository.PushTokenRepo
}

func NewNotifier(tokens *repository.PushTokenRepo) *Notifier {
	return &Notifier{tokens: tokens}
}

// NotifyListChanged рассылает push-уведомление всем участникам списка кроме инициатора.
// Вызывать в горутине — ошибки только логируются.
func (n *Notifier) NotifyListChanged(ctx context.Context, listID, senderUserID, title, body string) {
	tokenInfos, err := n.tokens.GetByListMembers(ctx, listID)
	if err != nil {
		fmt.Printf("push: ошибка получения токенов listID=%s: %v\n", listID, err)
		return
	}
	for _, info := range tokenInfos {
		if info.UserID == senderUserID {
			continue
		}
		Send(ctx, info.FCMToken, info.RuStoreToken, title, body)
	}
}
