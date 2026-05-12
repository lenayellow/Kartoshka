package repository

import (
	"context"
	"time"

	"github.com/ydb-platform/ydb-go-sdk/v3/table"
	"github.com/ydb-platform/ydb-go-sdk/v3/table/result/named"
	"github.com/ydb-platform/ydb-go-sdk/v3/table/types"
)

type PushTokenRepo struct {
	db *DB
}

func NewPushTokenRepo(db *DB) *PushTokenRepo {
	return &PushTokenRepo{db: db}
}

// PushTokenInfo — токены одного устройства пользователя.
type PushTokenInfo struct {
	UserID       string
	FCMToken     string
	RuStoreToken string
}

// GetByListMembers возвращает push-токены всех участников списка у которых они есть.
func (r *PushTokenRepo) GetByListMembers(ctx context.Context, listID string) ([]PushTokenInfo, error) {
	var infos []PushTokenInfo
	err := r.db.Driver.Table().Do(ctx, func(ctx context.Context, s table.Session) error {
		_, res, err := s.Execute(ctx, table.OnlineReadOnlyTxControl(),
			`DECLARE $list_id AS Utf8;
			 SELECT pt.user_id, pt.fcm_token, pt.rustore_token
			 FROM push_tokens AS pt
			 JOIN list_members AS lm ON pt.user_id = lm.user_id
			 WHERE lm.list_id = $list_id`,
			table.NewQueryParameters(
				table.ValueParam("$list_id", types.TextValue(listID)),
			),
		)
		if err != nil {
			return err
		}
		defer res.Close()
		for res.NextResultSet(ctx) {
			for res.NextRow() {
				info := PushTokenInfo{}
				if err = res.ScanNamed(
					named.Required("user_id", &info.UserID),
					named.Optional("fcm_token", &info.FCMToken),
					named.Optional("rustore_token", &info.RuStoreToken),
				); err != nil {
					return err
				}
				infos = append(infos, info)
			}
		}
		return res.Err()
	})
	return infos, err
}

// Save сохраняет или обновляет push-токены устройства.
func (r *PushTokenRepo) Save(ctx context.Context, userID, deviceID, fcmToken, rustoreToken string) error {
	now := time.Now()
	return r.db.Driver.Table().Do(ctx, func(ctx context.Context, s table.Session) error {
		_, _, err := s.Execute(ctx, table.SerializableReadWriteTxControl(table.CommitTx()),
			`DECLARE $user_id AS Utf8;
			 DECLARE $device_id AS Utf8;
			 DECLARE $fcm_token AS Utf8;
			 DECLARE $rustore_token AS Utf8;
			 DECLARE $updated_at AS Datetime;
			 UPSERT INTO push_tokens (user_id, device_id, fcm_token, rustore_token, updated_at)
			 VALUES ($user_id, $device_id, $fcm_token, $rustore_token, $updated_at)`,
			table.NewQueryParameters(
				table.ValueParam("$user_id", types.TextValue(userID)),
				table.ValueParam("$device_id", types.TextValue(deviceID)),
				table.ValueParam("$fcm_token", types.TextValue(fcmToken)),
				table.ValueParam("$rustore_token", types.TextValue(rustoreToken)),
				table.ValueParam("$updated_at", types.DatetimeValueFromTime(now)),
			),
		)
		return err
	})
}
