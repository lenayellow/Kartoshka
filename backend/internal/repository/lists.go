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

type ListRepo struct {
	db *DB
}

func NewListRepo(db *DB) *ListRepo {
	return &ListRepo{db: db}
}

// GetMemberRole возвращает роль пользователя в списке ("owner"/"editor") или "" если не участник.
func (r *ListRepo) GetMemberRole(ctx context.Context, listID, userID string) (string, error) {
	var role string
	err := r.db.Driver.Table().Do(ctx, func(ctx context.Context, s table.Session) error {
		_, res, err := s.Execute(ctx, table.OnlineReadOnlyTxControl(),
			`DECLARE $list_id AS Utf8;
			 DECLARE $user_id AS Utf8;
			 SELECT role FROM list_members
			 WHERE list_id = $list_id AND user_id = $user_id`,
			table.NewQueryParameters(
				table.ValueParam("$list_id", types.TextValue(listID)),
				table.ValueParam("$user_id", types.TextValue(userID)),
			),
		)
		if err != nil {
			return err
		}
		defer res.Close()
		if res.NextResultSet(ctx) && res.NextRow() {
			if err = res.ScanNamed(named.Required("role", &role)); err != nil {
				return err
			}
		}
		return res.Err()
	})
	return role, err
}

// GetAllForUser возвращает все списки, в которых пользователь является участником.
func (r *ListRepo) GetAllForUser(ctx context.Context, userID string) ([]models.List, error) {
	var lists []models.List
	err := r.db.Driver.Table().Do(ctx, func(ctx context.Context, s table.Session) error {
		_, res, err := s.Execute(ctx, table.OnlineReadOnlyTxControl(),
			`DECLARE $user_id AS Utf8;
			 SELECT l.list_id AS list_id, l.owner_id AS owner_id, l.title AS title,
			        l.color_value AS color_value, l.position AS position,
			        l.category_order AS category_order, l.hidden_categories AS hidden_categories,
			        l.created_at AS created_at, l.updated_at AS updated_at
			 FROM list_members AS lm
			 JOIN lists AS l ON lm.list_id = l.list_id
			 WHERE lm.user_id = $user_id
			 ORDER BY l.position ASC`,
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
				l, err := scanList(res)
				if err != nil {
					return err
				}
				lists = append(lists, *l)
			}
		}
		return res.Err()
	})
	return lists, err
}

// GetByID возвращает список по ID. Не проверяет членство — делает хендлер.
func (r *ListRepo) GetByID(ctx context.Context, listID string) (*models.List, error) {
	var list *models.List
	err := r.db.Driver.Table().Do(ctx, func(ctx context.Context, s table.Session) error {
		_, res, err := s.Execute(ctx, table.OnlineReadOnlyTxControl(),
			`DECLARE $list_id AS Utf8;
			 SELECT list_id, owner_id, title, color_value, position,
			        category_order, hidden_categories, created_at, updated_at
			 FROM lists WHERE list_id = $list_id`,
			table.NewQueryParameters(
				table.ValueParam("$list_id", types.TextValue(listID)),
			),
		)
		if err != nil {
			return err
		}
		defer res.Close()
		if res.NextResultSet(ctx) && res.NextRow() {
			l, err := scanList(res)
			if err != nil {
				return err
			}
			list = l
		}
		return res.Err()
	})
	return list, err
}

// Create создаёт список и добавляет создателя как owner.
// Если listID не пустой — использует его; иначе генерирует UUID.
func (r *ListRepo) Create(ctx context.Context, ownerID, listID, title string, colorValue int64, position int32) (*models.List, error) {
	now := time.Now()
	if listID == "" {
		listID = uuid.New().String()
	}
	list := &models.List{
		ListID:     listID,
		OwnerID:    ownerID,
		Title:      title,
		ColorValue: colorValue,
		Position:   position,
		CreatedAt:  now,
		UpdatedAt:  now,
	}

	err := r.db.Driver.Table().Do(ctx, func(ctx context.Context, s table.Session) error {
		_, _, err := s.Execute(ctx, table.SerializableReadWriteTxControl(table.CommitTx()),
			`DECLARE $list_id AS Utf8;
			 DECLARE $owner_id AS Utf8;
			 DECLARE $title AS Utf8;
			 DECLARE $color_value AS Int64;
			 DECLARE $position AS Int32;
			 DECLARE $created_at AS Datetime;
			 DECLARE $updated_at AS Datetime;
			 UPSERT INTO lists (list_id, owner_id, title, color_value, position, created_at, updated_at)
			 VALUES ($list_id, $owner_id, $title, $color_value, $position, $created_at, $updated_at)`,
			table.NewQueryParameters(
				table.ValueParam("$list_id", types.TextValue(list.ListID)),
				table.ValueParam("$owner_id", types.TextValue(ownerID)),
				table.ValueParam("$title", types.TextValue(title)),
				table.ValueParam("$color_value", types.Int64Value(colorValue)),
				table.ValueParam("$position", types.Int32Value(position)),
				table.ValueParam("$created_at", types.DatetimeValueFromTime(now)),
				table.ValueParam("$updated_at", types.DatetimeValueFromTime(now)),
			),
		)
		return err
	})
	if err != nil {
		return nil, err
	}

	err = r.db.Driver.Table().Do(ctx, func(ctx context.Context, s table.Session) error {
		_, _, err := s.Execute(ctx, table.SerializableReadWriteTxControl(table.CommitTx()),
			`DECLARE $list_id AS Utf8;
			 DECLARE $user_id AS Utf8;
			 DECLARE $joined_at AS Datetime;
			 UPSERT INTO list_members (list_id, user_id, role, joined_at)
			 VALUES ($list_id, $user_id, "owner", $joined_at)`,
			table.NewQueryParameters(
				table.ValueParam("$list_id", types.TextValue(list.ListID)),
				table.ValueParam("$user_id", types.TextValue(ownerID)),
				table.ValueParam("$joined_at", types.DatetimeValueFromTime(now)),
			),
		)
		return err
	})
	if err != nil {
		return nil, err
	}

	return list, nil
}

// Update обновляет поля списка.
func (r *ListRepo) Update(ctx context.Context, listID, title string, colorValue int64, position int32, categoryOrder, hiddenCategories string) error {
	now := time.Now()
	return r.db.Driver.Table().Do(ctx, func(ctx context.Context, s table.Session) error {
		_, _, err := s.Execute(ctx, table.SerializableReadWriteTxControl(table.CommitTx()),
			`DECLARE $list_id AS Utf8;
			 DECLARE $title AS Utf8;
			 DECLARE $color_value AS Int64;
			 DECLARE $position AS Int32;
			 DECLARE $category_order AS Utf8;
			 DECLARE $hidden_categories AS Utf8;
			 DECLARE $updated_at AS Datetime;
			 UPDATE lists SET title = $title, color_value = $color_value,
			   position = $position, category_order = $category_order,
			   hidden_categories = $hidden_categories, updated_at = $updated_at
			 WHERE list_id = $list_id`,
			table.NewQueryParameters(
				table.ValueParam("$list_id", types.TextValue(listID)),
				table.ValueParam("$title", types.TextValue(title)),
				table.ValueParam("$color_value", types.Int64Value(colorValue)),
				table.ValueParam("$position", types.Int32Value(position)),
				table.ValueParam("$category_order", types.TextValue(categoryOrder)),
				table.ValueParam("$hidden_categories", types.TextValue(hiddenCategories)),
				table.ValueParam("$updated_at", types.DatetimeValueFromTime(now)),
			),
		)
		return err
	})
}

// Delete удаляет список, помечает все его товары удалёнными и удаляет участников.
// Вызывать только если userID — owner.
func (r *ListRepo) Delete(ctx context.Context, listID, userID string) error {
	now := time.Now()

	// Шаг 1: soft delete всех товаров списка
	err := r.db.Driver.Table().Do(ctx, func(ctx context.Context, s table.Session) error {
		_, _, err := s.Execute(ctx, table.SerializableReadWriteTxControl(table.CommitTx()),
			`DECLARE $list_id AS Utf8;
			 DECLARE $updated_by AS Utf8;
			 DECLARE $updated_at AS Datetime;
			 UPDATE items
			 SET is_deleted = true, updated_by = $updated_by, updated_at = $updated_at
			 WHERE list_id = $list_id AND is_deleted = false`,
			table.NewQueryParameters(
				table.ValueParam("$list_id", types.TextValue(listID)),
				table.ValueParam("$updated_by", types.TextValue(userID)),
				table.ValueParam("$updated_at", types.DatetimeValueFromTime(now)),
			),
		)
		return err
	})
	if err != nil {
		return err
	}

	// Шаг 2: удалить всех участников
	err = r.db.Driver.Table().Do(ctx, func(ctx context.Context, s table.Session) error {
		_, _, err := s.Execute(ctx, table.SerializableReadWriteTxControl(table.CommitTx()),
			`DECLARE $list_id AS Utf8;
			 DELETE FROM list_members WHERE list_id = $list_id`,
			table.NewQueryParameters(
				table.ValueParam("$list_id", types.TextValue(listID)),
			),
		)
		return err
	})
	if err != nil {
		return err
	}

	// Шаг 3: удалить сам список
	return r.db.Driver.Table().Do(ctx, func(ctx context.Context, s table.Session) error {
		_, _, err := s.Execute(ctx, table.SerializableReadWriteTxControl(table.CommitTx()),
			`DECLARE $list_id AS Utf8;
			 DELETE FROM lists WHERE list_id = $list_id`,
			table.NewQueryParameters(
				table.ValueParam("$list_id", types.TextValue(listID)),
			),
		)
		return err
	})
}

// GetMembers возвращает участников списка с именами и email.
func (r *ListRepo) GetMembers(ctx context.Context, listID string) ([]models.ListMember, error) {
	var members []models.ListMember
	err := r.db.Driver.Table().Do(ctx, func(ctx context.Context, s table.Session) error {
		_, res, err := s.Execute(ctx, table.OnlineReadOnlyTxControl(),
			`DECLARE $list_id AS Utf8;
			 SELECT lm.user_id AS user_id, lm.role AS role, lm.joined_at AS joined_at,
			        u.name AS name, u.email AS email, u.avatar_url AS avatar_url
			 FROM list_members AS lm
			 JOIN users AS u ON lm.user_id = u.user_id
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
				m := models.ListMember{ListID: listID}
				if err = res.ScanNamed(
					named.Required("user_id", &m.UserID),
					named.Required("role", &m.Role),
					named.Required("joined_at", &m.JoinedAt),
					named.Required("name", &m.UserName),
					named.Required("email", &m.UserEmail),
					named.OptionalWithDefault("avatar_url", &m.AvatarUrl),
				); err != nil {
					return err
				}
				members = append(members, m)
			}
		}
		return res.Err()
	})
	return members, err
}

// RemoveMember удаляет участника из списка.
func (r *ListRepo) RemoveMember(ctx context.Context, listID, userID string) error {
	return r.db.Driver.Table().Do(ctx, func(ctx context.Context, s table.Session) error {
		_, _, err := s.Execute(ctx, table.SerializableReadWriteTxControl(table.CommitTx()),
			`DECLARE $list_id AS Utf8;
			 DECLARE $user_id AS Utf8;
			 DELETE FROM list_members WHERE list_id = $list_id AND user_id = $user_id`,
			table.NewQueryParameters(
				table.ValueParam("$list_id", types.TextValue(listID)),
				table.ValueParam("$user_id", types.TextValue(userID)),
			),
		)
		return err
	})
}

func scanList(res interface {
	ScanNamed(...named.Value) error
}) (*models.List, error) {
	l := &models.List{}
	err := res.ScanNamed(
		named.Required("list_id", &l.ListID),
		named.Required("owner_id", &l.OwnerID),
		named.Required("title", &l.Title),
		named.Required("color_value", &l.ColorValue),
		named.Required("position", &l.Position),
		named.OptionalWithDefault("category_order", &l.CategoryOrder),
		named.OptionalWithDefault("hidden_categories", &l.HiddenCategories),
		named.Required("created_at", &l.CreatedAt),
		named.Required("updated_at", &l.UpdatedAt),
	)
	return l, err
}
