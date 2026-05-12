package repository

import (
	"context"
	"fmt"
	"time"

	"github.com/google/uuid"
	"github.com/ydb-platform/ydb-go-sdk/v3/table"
	"github.com/ydb-platform/ydb-go-sdk/v3/table/result/named"
	"github.com/ydb-platform/ydb-go-sdk/v3/table/types"
)

type TokenRepo struct {
	db *DB
}

func NewTokenRepo(db *DB) *TokenRepo {
	return &TokenRepo{db: db}
}

// Save сохраняет refresh-токен и возвращает его значение.
func (r *TokenRepo) Save(ctx context.Context, userID string, expiresAt time.Time) (string, error) {
	token := uuid.New().String()
	err := r.db.Driver.Table().Do(ctx, func(ctx context.Context, s table.Session) error {
		_, _, err := s.Execute(ctx, table.SerializableReadWriteTxControl(table.CommitTx()),
			`DECLARE $token AS Utf8;
			 DECLARE $user_id AS Utf8;
			 DECLARE $expires_at AS Datetime;
			 UPSERT INTO refresh_tokens (token, user_id, expires_at)
			 VALUES ($token, $user_id, $expires_at)`,
			table.NewQueryParameters(
				table.ValueParam("$token", types.TextValue(token)),
				table.ValueParam("$user_id", types.TextValue(userID)),
				table.ValueParam("$expires_at", types.DatetimeValueFromTime(expiresAt)),
			),
		)
		return err
	})
	if err != nil {
		return "", err
	}
	return token, nil
}

// Get возвращает userID и срок действия токена.
func (r *TokenRepo) Get(ctx context.Context, token string) (userID string, expiresAt time.Time, err error) {
	err = r.db.Driver.Table().Do(ctx, func(ctx context.Context, s table.Session) error {
		_, res, err := s.Execute(ctx, table.OnlineReadOnlyTxControl(),
			`DECLARE $token AS Utf8;
			 SELECT user_id, expires_at FROM refresh_tokens WHERE token = $token`,
			table.NewQueryParameters(
				table.ValueParam("$token", types.TextValue(token)),
			),
		)
		if err != nil {
			return err
		}
		defer res.Close()

		if !res.NextResultSet(ctx) || !res.NextRow() {
			return fmt.Errorf("token not found")
		}
		if err = res.ScanNamed(
			named.Required("user_id", &userID),
			named.Required("expires_at", &expiresAt),
		); err != nil {
			return err
		}
		return res.Err()
	})
	return
}

// Delete удаляет refresh-токен (logout / ротация).
func (r *TokenRepo) Delete(ctx context.Context, token string) error {
	return r.db.Driver.Table().Do(ctx, func(ctx context.Context, s table.Session) error {
		_, _, err := s.Execute(ctx, table.SerializableReadWriteTxControl(table.CommitTx()),
			`DECLARE $token AS Utf8;
			 DELETE FROM refresh_tokens WHERE token = $token`,
			table.NewQueryParameters(
				table.ValueParam("$token", types.TextValue(token)),
			),
		)
		return err
	})
}
