package repository

import (
	"context"

	"github.com/google/uuid"
	"github.com/ydb-platform/ydb-go-sdk/v3/table"
	"github.com/ydb-platform/ydb-go-sdk/v3/table/result/named"
	"github.com/ydb-platform/ydb-go-sdk/v3/table/types"

	"github.com/lena/kartoshka-backend/internal/models"
)

type CardRepo struct {
	db *DB
}

func NewCardRepo(db *DB) *CardRepo {
	return &CardRepo{db: db}
}

// GetAllByUser возвращает все карты лояльности пользователя.
func (r *CardRepo) GetAllByUser(ctx context.Context, userID string) ([]models.LoyaltyCard, error) {
	var cards []models.LoyaltyCard
	err := r.db.Driver.Table().Do(ctx, func(ctx context.Context, s table.Session) error {
		_, res, err := s.Execute(ctx, table.OnlineReadOnlyTxControl(),
			`DECLARE $user_id AS Utf8;
			 SELECT card_id, user_id, name, barcode_value, barcode_format, color
			 FROM loyalty_cards WHERE user_id = $user_id`,
			table.NewQueryParameters(
				table.ValueParam("$user_id", types.TextValue(userID)),
			),
		)
		if err != nil {
			return err
		}
		defer res.Close()
		for res.NextResultSet(ctx) {
			for res.NextRow() {
				c := models.LoyaltyCard{}
				if err = res.ScanNamed(
					named.Required("card_id", &c.CardID),
					named.Required("user_id", &c.UserID),
					named.Required("name", &c.Name),
					named.Required("barcode_value", &c.BarcodeValue),
					named.Required("barcode_format", &c.BarcodeFormat),
					named.Required("color", &c.Color),
				); err != nil {
					return err
				}
				cards = append(cards, c)
			}
		}
		return res.Err()
	})
	return cards, err
}

// Create добавляет карту лояльности для пользователя.
func (r *CardRepo) Create(ctx context.Context, userID, name, barcodeValue string, barcodeFormat int32, color int64) (*models.LoyaltyCard, error) {
	card := &models.LoyaltyCard{
		CardID:        uuid.New().String(),
		UserID:        userID,
		Name:          name,
		BarcodeValue:  barcodeValue,
		BarcodeFormat: barcodeFormat,
		Color:         color,
	}
	err := r.db.Driver.Table().Do(ctx, func(ctx context.Context, s table.Session) error {
		_, _, err := s.Execute(ctx, table.SerializableReadWriteTxControl(table.CommitTx()),
			`DECLARE $card_id AS Utf8;
			 DECLARE $user_id AS Utf8;
			 DECLARE $name AS Utf8;
			 DECLARE $barcode_value AS Utf8;
			 DECLARE $barcode_format AS Int32;
			 DECLARE $color AS Int64;
			 UPSERT INTO loyalty_cards (card_id, user_id, name, barcode_value, barcode_format, color)
			 VALUES ($card_id, $user_id, $name, $barcode_value, $barcode_format, $color)`,
			table.NewQueryParameters(
				table.ValueParam("$card_id", types.TextValue(card.CardID)),
				table.ValueParam("$user_id", types.TextValue(userID)),
				table.ValueParam("$name", types.TextValue(name)),
				table.ValueParam("$barcode_value", types.TextValue(barcodeValue)),
				table.ValueParam("$barcode_format", types.Int32Value(barcodeFormat)),
				table.ValueParam("$color", types.Int64Value(color)),
			),
		)
		return err
	})
	if err != nil {
		return nil, err
	}
	return card, nil
}

// Delete удаляет карту. Срабатывает только если карта принадлежит данному пользователю.
func (r *CardRepo) Delete(ctx context.Context, cardID, userID string) error {
	return r.db.Driver.Table().Do(ctx, func(ctx context.Context, s table.Session) error {
		_, _, err := s.Execute(ctx, table.SerializableReadWriteTxControl(table.CommitTx()),
			`DECLARE $card_id AS Utf8;
			 DECLARE $user_id AS Utf8;
			 DELETE FROM loyalty_cards WHERE card_id = $card_id AND user_id = $user_id`,
			table.NewQueryParameters(
				table.ValueParam("$card_id", types.TextValue(cardID)),
				table.ValueParam("$user_id", types.TextValue(userID)),
			),
		)
		return err
	})
}
