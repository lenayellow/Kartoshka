package repository

import (
	"context"
	"time"

	"github.com/google/uuid"
	"github.com/ydb-platform/ydb-go-sdk/v3/table"
	"github.com/ydb-platform/ydb-go-sdk/v3/table/result/named"
	"github.com/ydb-platform/ydb-go-sdk/v3/table/types"

	"github.com/lena/kartoshka-backend/internal/models"
)

type UserRepo struct {
	db *DB
}

func NewUserRepo(db *DB) *UserRepo {
	return &UserRepo{db: db}
}

func (r *UserRepo) GetByYandexUID(ctx context.Context, yandexUID string) (*models.User, error) {
	var user *models.User
	err := r.db.Driver.Table().Do(ctx, func(ctx context.Context, s table.Session) error {
		_, res, err := s.Execute(ctx, table.OnlineReadOnlyTxControl(),
			`DECLARE $yandex_uid AS Utf8;
			 SELECT user_id, yandex_uid, email, name, avatar_url,
			        password_hash, is_verified, created_at, updated_at
			 FROM users WHERE yandex_uid = $yandex_uid`,
			table.NewQueryParameters(
				table.ValueParam("$yandex_uid", types.TextValue(yandexUID)),
			),
		)
		if err != nil {
			return err
		}
		defer res.Close()
		if res.NextResultSet(ctx) && res.NextRow() {
			u, err := scanUser(res)
			if err != nil {
				return err
			}
			user = u
		}
		return res.Err()
	})
	return user, err
}

func (r *UserRepo) GetByEmail(ctx context.Context, email string) (*models.User, error) {
	var user *models.User
	err := r.db.Driver.Table().Do(ctx, func(ctx context.Context, s table.Session) error {
		_, res, err := s.Execute(ctx, table.OnlineReadOnlyTxControl(),
			`DECLARE $email AS Utf8;
			 SELECT user_id, yandex_uid, email, name, avatar_url,
			        password_hash, is_verified, created_at, updated_at
			 FROM users WHERE email = $email`,
			table.NewQueryParameters(
				table.ValueParam("$email", types.TextValue(email)),
			),
		)
		if err != nil {
			return err
		}
		defer res.Close()
		if res.NextResultSet(ctx) && res.NextRow() {
			u, err := scanUser(res)
			if err != nil {
				return err
			}
			user = u
		}
		return res.Err()
	})
	return user, err
}

func (r *UserRepo) GetByID(ctx context.Context, userID string) (*models.User, error) {
	var user *models.User
	err := r.db.Driver.Table().Do(ctx, func(ctx context.Context, s table.Session) error {
		_, res, err := s.Execute(ctx, table.OnlineReadOnlyTxControl(),
			`DECLARE $user_id AS Utf8;
			 SELECT user_id, yandex_uid, email, name, avatar_url,
			        password_hash, is_verified, created_at, updated_at
			 FROM users WHERE user_id = $user_id`,
			table.NewQueryParameters(
				table.ValueParam("$user_id", types.TextValue(userID)),
			),
		)
		if err != nil {
			return err
		}
		defer res.Close()
		if res.NextResultSet(ctx) && res.NextRow() {
			u, err := scanUser(res)
			if err != nil {
				return err
			}
			user = u
		}
		return res.Err()
	})
	return user, err
}

func (r *UserRepo) Create(ctx context.Context, yandexUID, email, name string) (*models.User, error) {
	now := time.Now()
	user := &models.User{
		UserID:    uuid.New().String(),
		YandexUID: yandexUID,
		Email:     email,
		Name:      name,
		CreatedAt: now,
		UpdatedAt: now,
	}
	err := r.db.Driver.Table().Do(ctx, func(ctx context.Context, s table.Session) error {
		_, _, err := s.Execute(ctx, table.SerializableReadWriteTxControl(table.CommitTx()),
			`DECLARE $user_id AS Utf8;
			 DECLARE $yandex_uid AS Utf8;
			 DECLARE $email AS Utf8;
			 DECLARE $name AS Utf8;
			 DECLARE $created_at AS Datetime;
			 DECLARE $updated_at AS Datetime;
			 UPSERT INTO users (user_id, yandex_uid, email, name, created_at, updated_at)
			 VALUES ($user_id, $yandex_uid, $email, $name, $created_at, $updated_at)`,
			table.NewQueryParameters(
				table.ValueParam("$user_id", types.TextValue(user.UserID)),
				table.ValueParam("$yandex_uid", types.TextValue(yandexUID)),
				table.ValueParam("$email", types.TextValue(email)),
				table.ValueParam("$name", types.TextValue(name)),
				table.ValueParam("$created_at", types.DatetimeValueFromTime(now)),
				table.ValueParam("$updated_at", types.DatetimeValueFromTime(now)),
			),
		)
		return err
	})
	if err != nil {
		return nil, err
	}
	return user, nil
}

func (r *UserRepo) SetPasswordHash(ctx context.Context, userID, hash string) error {
	now := time.Now()
	return r.db.Driver.Table().Do(ctx, func(ctx context.Context, s table.Session) error {
		_, _, err := s.Execute(ctx, table.SerializableReadWriteTxControl(table.CommitTx()),
			`DECLARE $user_id AS Utf8;
			 DECLARE $password_hash AS Utf8;
			 DECLARE $updated_at AS Datetime;
			 UPDATE users SET password_hash = $password_hash, updated_at = $updated_at
			 WHERE user_id = $user_id`,
			table.NewQueryParameters(
				table.ValueParam("$user_id", types.TextValue(userID)),
				table.ValueParam("$password_hash", types.TextValue(hash)),
				table.ValueParam("$updated_at", types.DatetimeValueFromTime(now)),
			),
		)
		return err
	})
}

func (r *UserRepo) SetVerified(ctx context.Context, userID string) error {
	now := time.Now()
	return r.db.Driver.Table().Do(ctx, func(ctx context.Context, s table.Session) error {
		_, _, err := s.Execute(ctx, table.SerializableReadWriteTxControl(table.CommitTx()),
			`DECLARE $user_id AS Utf8;
			 DECLARE $updated_at AS Datetime;
			 UPDATE users SET is_verified = true, updated_at = $updated_at
			 WHERE user_id = $user_id`,
			table.NewQueryParameters(
				table.ValueParam("$user_id", types.TextValue(userID)),
				table.ValueParam("$updated_at", types.DatetimeValueFromTime(now)),
			),
		)
		return err
	})
}

// UpdateName обновляет имя пользователя.
func (r *UserRepo) UpdateName(ctx context.Context, userID, name string) error {
	now := time.Now()
	return r.db.Driver.Table().Do(ctx, func(ctx context.Context, s table.Session) error {
		_, _, err := s.Execute(ctx, table.SerializableReadWriteTxControl(table.CommitTx()),
			`DECLARE $user_id AS Utf8;
			 DECLARE $name AS Utf8;
			 DECLARE $updated_at AS Datetime;
			 UPDATE users SET name = $name, updated_at = $updated_at
			 WHERE user_id = $user_id`,
			table.NewQueryParameters(
				table.ValueParam("$user_id", types.TextValue(userID)),
				table.ValueParam("$name", types.TextValue(name)),
				table.ValueParam("$updated_at", types.DatetimeValueFromTime(now)),
			),
		)
		return err
	})
}

// UpdateAvatarURL обновляет ссылку на аватарку пользователя.
func (r *UserRepo) UpdateAvatarURL(ctx context.Context, userID, avatarURL string) error {
	now := time.Now()
	return r.db.Driver.Table().Do(ctx, func(ctx context.Context, s table.Session) error {
		_, _, err := s.Execute(ctx, table.SerializableReadWriteTxControl(table.CommitTx()),
			`DECLARE $user_id AS Utf8;
			 DECLARE $avatar_url AS Utf8;
			 DECLARE $updated_at AS Datetime;
			 UPDATE users SET avatar_url = $avatar_url, updated_at = $updated_at
			 WHERE user_id = $user_id`,
			table.NewQueryParameters(
				table.ValueParam("$user_id", types.TextValue(userID)),
				table.ValueParam("$avatar_url", types.TextValue(avatarURL)),
				table.ValueParam("$updated_at", types.DatetimeValueFromTime(now)),
			),
		)
		return err
	})
}

// scanUser считывает строку результата в структуру User.
func scanUser(res interface {
	ScanNamed(...named.Value) error
}) (*models.User, error) {
	u := &models.User{}
	var yandexUID, avatarURL, passwordHash *string
	var isVerified *bool
	err := res.ScanNamed(
		named.Required("user_id", &u.UserID),
		named.Optional("yandex_uid", &yandexUID),
		named.Required("email", &u.Email),
		named.Required("name", &u.Name),
		named.Optional("avatar_url", &avatarURL),
		named.Optional("password_hash", &passwordHash),
		named.Optional("is_verified", &isVerified),
		named.Required("created_at", &u.CreatedAt),
		named.Required("updated_at", &u.UpdatedAt),
	)
	if yandexUID != nil {
		u.YandexUID = *yandexUID
	}
	if avatarURL != nil {
		u.AvatarURL = *avatarURL
	}
	if passwordHash != nil {
		u.PasswordHash = *passwordHash
	}
	if isVerified != nil {
		u.IsVerified = *isVerified
	}
	return u, err
}
