package handlers

import (
	"context"
	"time"

	"github.com/lena/kartoshka-backend/internal/models"
)

type itemStore interface {
	GetAllByList(ctx context.Context, listID string) ([]models.Item, error)
	GetByID(ctx context.Context, itemID string) (*models.Item, error)
	Create(ctx context.Context, listID, userID, name, tags, note, categoryID string, sortIndex int32) (*models.Item, error)
	Update(ctx context.Context, itemID, userID, name, tags, note, categoryID string) (*models.Item, error)
	SoftDelete(ctx context.Context, itemID, userID string) error
	Check(ctx context.Context, itemID, itemName, listID, userID string) error
	Uncheck(ctx context.Context, itemID, userID string) error
	Move(ctx context.Context, itemID, targetListID, userID string) error
	Reorder(ctx context.Context, updates []models.ReorderItem) error
	GetRecent(ctx context.Context, listID string) ([]models.PurchaseHistoryEntry, error)
	UpdatePhotoURL(ctx context.Context, itemID, photoURL string) error
}

type listStore interface {
	GetMemberRole(ctx context.Context, listID, userID string) (string, error)
	GetAllForUser(ctx context.Context, userID string) ([]models.List, error)
	GetByID(ctx context.Context, listID string) (*models.List, error)
	Create(ctx context.Context, ownerID, listID, title string, colorValue int64, position int32) (*models.List, error)
	Update(ctx context.Context, listID, title string, colorValue int64, position int32, categoryOrder, hiddenCategories string) error
	Delete(ctx context.Context, listID, userID string) error
	GetMembers(ctx context.Context, listID string) ([]models.ListMember, error)
	RemoveMember(ctx context.Context, listID, userID string) error
}

type userStore interface {
	GetByYandexUID(ctx context.Context, yandexUID string) (*models.User, error)
	GetByEmail(ctx context.Context, email string) (*models.User, error)
	GetByID(ctx context.Context, userID string) (*models.User, error)
	Create(ctx context.Context, yandexUID, email, name string) (*models.User, error)
	SetPasswordHash(ctx context.Context, userID, hash string) error
	SetVerified(ctx context.Context, userID string) error
	UpdateName(ctx context.Context, userID, name string) error
	UpdateAvatarURL(ctx context.Context, userID, avatarURL string) error
}

type tokenStore interface {
	Save(ctx context.Context, userID string, expiresAt time.Time) (string, error)
	Get(ctx context.Context, token string) (userID string, expiresAt time.Time, err error)
	Delete(ctx context.Context, token string) error
}

type invitationStore interface {
	Create(ctx context.Context, listID, inviterID, inviteeEmail string) (*models.Invitation, error)
	GetByToken(ctx context.Context, token string) (*models.Invitation, error)
	GetInfoByToken(ctx context.Context, token string) (*models.InviteInfo, error)
	Accept(ctx context.Context, token, listID, userID string) error
	GetPendingByListAndEmail(ctx context.Context, listID, email string) (*models.Invitation, error)
	MarkExpired(ctx context.Context, token string) error
}

type cardStore interface {
	GetAllByUser(ctx context.Context, userID string) ([]models.LoyaltyCard, error)
	Create(ctx context.Context, userID, name, barcodeValue string, barcodeFormat int32, color int64) (*models.LoyaltyCard, error)
	Delete(ctx context.Context, cardID, userID string) error
}

type pushTokenStore interface {
	Save(ctx context.Context, userID, deviceID, fcmToken, rustoreToken string) error
}

type eventsStore interface {
	GetChangedItems(ctx context.Context, listID string, since time.Time) ([]models.Item, error)
	GetChangedMembers(ctx context.Context, listID string, since time.Time) ([]models.ListMember, error)
}

type mediaStorage interface {
	UploadFile(ctx context.Context, key, contentType string, data []byte) (string, error)
}
