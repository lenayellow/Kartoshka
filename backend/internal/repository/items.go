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

type ItemRepo struct {
	db *DB
}

func NewItemRepo(db *DB) *ItemRepo {
	return &ItemRepo{db: db}
}

// GetAllByList возвращает все активные (не удалённые) товары списка.
func (r *ItemRepo) GetAllByList(ctx context.Context, listID string) ([]models.Item, error) {
	var items []models.Item
	err := r.db.Driver.Table().Do(ctx, func(ctx context.Context, s table.Session) error {
		_, res, err := s.Execute(ctx, table.OnlineReadOnlyTxControl(),
			`DECLARE $list_id AS Utf8;
			 SELECT item_id, list_id, name, tags, note, category_id, photo_url,
			        is_deleted, added_by, updated_by, updated_at, sort_index
			 FROM items
			 WHERE list_id = $list_id AND is_deleted = false
			 ORDER BY sort_index ASC`,
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
				item, err := scanItem(res)
				if err != nil {
					return err
				}
				items = append(items, *item)
			}
		}
		return res.Err()
	})
	return items, err
}

// GetByID возвращает товар по ID (включая удалённые).
func (r *ItemRepo) GetByID(ctx context.Context, itemID string) (*models.Item, error) {
	var item *models.Item
	err := r.db.Driver.Table().Do(ctx, func(ctx context.Context, s table.Session) error {
		_, res, err := s.Execute(ctx, table.OnlineReadOnlyTxControl(),
			`DECLARE $item_id AS Utf8;
			 SELECT item_id, list_id, name, tags, note, category_id, photo_url,
			        is_deleted, added_by, updated_by, updated_at, sort_index
			 FROM items WHERE item_id = $item_id`,
			table.NewQueryParameters(
				table.ValueParam("$item_id", types.TextValue(itemID)),
			),
		)
		if err != nil {
			return err
		}
		defer res.Close()
		if res.NextResultSet(ctx) && res.NextRow() {
			i, err := scanItem(res)
			if err != nil {
				return err
			}
			item = i
		}
		return res.Err()
	})
	return item, err
}

// Create добавляет новый товар в список.
func (r *ItemRepo) Create(ctx context.Context, listID, userID, name, tags, note, categoryID string, sortIndex int32) (*models.Item, error) {
	now := time.Now()
	itemID := uuid.New().String()

	err := r.db.Driver.Table().Do(ctx, func(ctx context.Context, s table.Session) error {
		_, _, err := s.Execute(ctx, table.SerializableReadWriteTxControl(table.CommitTx()),
			`DECLARE $item_id AS Utf8;
			 DECLARE $list_id AS Utf8;
			 DECLARE $name AS Utf8;
			 DECLARE $tags AS Utf8;
			 DECLARE $note AS Utf8;
			 DECLARE $category_id AS Utf8;
			 DECLARE $added_by AS Utf8;
			 DECLARE $updated_by AS Utf8;
			 DECLARE $updated_at AS Datetime;
			 DECLARE $sort_index AS Int32;
			 UPSERT INTO items
			   (item_id, list_id, name, tags, note, category_id,
			    is_deleted, added_by, updated_by, updated_at, sort_index)
			 VALUES
			   ($item_id, $list_id, $name, $tags, $note, $category_id,
			    false, $added_by, $updated_by, $updated_at, $sort_index)`,
			table.NewQueryParameters(
				table.ValueParam("$item_id", types.TextValue(itemID)),
				table.ValueParam("$list_id", types.TextValue(listID)),
				table.ValueParam("$name", types.TextValue(name)),
				table.ValueParam("$tags", types.TextValue(tags)),
				table.ValueParam("$note", types.TextValue(note)),
				table.ValueParam("$category_id", types.TextValue(categoryID)),
				table.ValueParam("$added_by", types.TextValue(userID)),
				table.ValueParam("$updated_by", types.TextValue(userID)),
				table.ValueParam("$updated_at", types.DatetimeValueFromTime(now)),
				table.ValueParam("$sort_index", types.Int32Value(sortIndex)),
			),
		)
		return err
	})
	if err != nil {
		return nil, err
	}
	return r.GetByID(ctx, itemID)
}

// Update обновляет поля товара и возвращает обновлённую запись.
func (r *ItemRepo) Update(ctx context.Context, itemID, userID, name, tags, note, categoryID string) (*models.Item, error) {
	now := time.Now()
	err := r.db.Driver.Table().Do(ctx, func(ctx context.Context, s table.Session) error {
		_, _, err := s.Execute(ctx, table.SerializableReadWriteTxControl(table.CommitTx()),
			`DECLARE $item_id AS Utf8;
			 DECLARE $name AS Utf8;
			 DECLARE $tags AS Utf8;
			 DECLARE $note AS Utf8;
			 DECLARE $category_id AS Utf8;
			 DECLARE $updated_by AS Utf8;
			 DECLARE $updated_at AS Datetime;
			 UPDATE items
			 SET name = $name, tags = $tags, note = $note,
			     category_id = $category_id, updated_by = $updated_by, updated_at = $updated_at
			 WHERE item_id = $item_id`,
			table.NewQueryParameters(
				table.ValueParam("$item_id", types.TextValue(itemID)),
				table.ValueParam("$name", types.TextValue(name)),
				table.ValueParam("$tags", types.TextValue(tags)),
				table.ValueParam("$note", types.TextValue(note)),
				table.ValueParam("$category_id", types.TextValue(categoryID)),
				table.ValueParam("$updated_by", types.TextValue(userID)),
				table.ValueParam("$updated_at", types.DatetimeValueFromTime(now)),
			),
		)
		return err
	})
	if err != nil {
		return nil, err
	}
	return r.GetByID(ctx, itemID)
}

// SoftDelete помечает товар как удалённый.
func (r *ItemRepo) SoftDelete(ctx context.Context, itemID, userID string) error {
	now := time.Now()
	return r.db.Driver.Table().Do(ctx, func(ctx context.Context, s table.Session) error {
		_, _, err := s.Execute(ctx, table.SerializableReadWriteTxControl(table.CommitTx()),
			`DECLARE $item_id AS Utf8;
			 DECLARE $updated_by AS Utf8;
			 DECLARE $updated_at AS Datetime;
			 UPDATE items
			 SET is_deleted = true, updated_by = $updated_by, updated_at = $updated_at
			 WHERE item_id = $item_id`,
			table.NewQueryParameters(
				table.ValueParam("$item_id", types.TextValue(itemID)),
				table.ValueParam("$updated_by", types.TextValue(userID)),
				table.ValueParam("$updated_at", types.DatetimeValueFromTime(now)),
			),
		)
		return err
	})
}

// Check отмечает товар как купленный: soft delete + запись в purchase_history.
func (r *ItemRepo) Check(ctx context.Context, itemID, itemName, listID, userID string) error {
	now := time.Now()

	if err := r.SoftDelete(ctx, itemID, userID); err != nil {
		return err
	}

	return r.db.Driver.Table().Do(ctx, func(ctx context.Context, s table.Session) error {
		_, _, err := s.Execute(ctx, table.SerializableReadWriteTxControl(table.CommitTx()),
			`DECLARE $id AS Utf8;
			 DECLARE $item_name AS Utf8;
			 DECLARE $list_id AS Utf8;
			 DECLARE $purchased_at AS Datetime;
			 DECLARE $user_id AS Utf8;
			 UPSERT INTO purchase_history (id, item_name, list_id, purchased_at, user_id)
			 VALUES ($id, $item_name, $list_id, $purchased_at, $user_id)`,
			table.NewQueryParameters(
				table.ValueParam("$id", types.TextValue(uuid.New().String())),
				table.ValueParam("$item_name", types.TextValue(itemName)),
				table.ValueParam("$list_id", types.TextValue(listID)),
				table.ValueParam("$purchased_at", types.DatetimeValueFromTime(now)),
				table.ValueParam("$user_id", types.TextValue(userID)),
			),
		)
		return err
	})
}

// Uncheck возвращает soft-deleted товар обратно в список.
func (r *ItemRepo) Uncheck(ctx context.Context, itemID, userID string) error {
	now := time.Now()
	return r.db.Driver.Table().Do(ctx, func(ctx context.Context, s table.Session) error {
		_, _, err := s.Execute(ctx, table.SerializableReadWriteTxControl(table.CommitTx()),
			`DECLARE $item_id AS Utf8;
			 DECLARE $updated_by AS Utf8;
			 DECLARE $updated_at AS Datetime;
			 UPDATE items
			 SET is_deleted = false, updated_by = $updated_by, updated_at = $updated_at
			 WHERE item_id = $item_id`,
			table.NewQueryParameters(
				table.ValueParam("$item_id", types.TextValue(itemID)),
				table.ValueParam("$updated_by", types.TextValue(userID)),
				table.ValueParam("$updated_at", types.DatetimeValueFromTime(now)),
			),
		)
		return err
	})
}

// Move переносит товар в другой список.
func (r *ItemRepo) Move(ctx context.Context, itemID, targetListID, userID string) error {
	now := time.Now()
	return r.db.Driver.Table().Do(ctx, func(ctx context.Context, s table.Session) error {
		_, _, err := s.Execute(ctx, table.SerializableReadWriteTxControl(table.CommitTx()),
			`DECLARE $item_id AS Utf8;
			 DECLARE $list_id AS Utf8;
			 DECLARE $updated_by AS Utf8;
			 DECLARE $updated_at AS Datetime;
			 UPDATE items
			 SET list_id = $list_id, updated_by = $updated_by, updated_at = $updated_at
			 WHERE item_id = $item_id`,
			table.NewQueryParameters(
				table.ValueParam("$item_id", types.TextValue(itemID)),
				table.ValueParam("$list_id", types.TextValue(targetListID)),
				table.ValueParam("$updated_by", types.TextValue(userID)),
				table.ValueParam("$updated_at", types.DatetimeValueFromTime(now)),
			),
		)
		return err
	})
}

// Reorder обновляет sort_index для переданных товаров.
func (r *ItemRepo) Reorder(ctx context.Context, updates []models.ReorderItem) error {
	now := time.Now()
	for _, u := range updates {
		err := r.db.Driver.Table().Do(ctx, func(ctx context.Context, s table.Session) error {
			_, _, err := s.Execute(ctx, table.SerializableReadWriteTxControl(table.CommitTx()),
				`DECLARE $item_id AS Utf8;
				 DECLARE $sort_index AS Int32;
				 DECLARE $updated_at AS Datetime;
				 UPDATE items
				 SET sort_index = $sort_index, updated_at = $updated_at
				 WHERE item_id = $item_id`,
				table.NewQueryParameters(
					table.ValueParam("$item_id", types.TextValue(u.ItemID)),
					table.ValueParam("$sort_index", types.Int32Value(u.SortIndex)),
					table.ValueParam("$updated_at", types.DatetimeValueFromTime(now)),
				),
			)
			return err
		})
		if err != nil {
			return err
		}
	}
	return nil
}

// GetRecent возвращает историю покупок по списку (последние 50).
func (r *ItemRepo) GetRecent(ctx context.Context, listID string) ([]models.PurchaseHistoryEntry, error) {
	var entries []models.PurchaseHistoryEntry
	err := r.db.Driver.Table().Do(ctx, func(ctx context.Context, s table.Session) error {
		_, res, err := s.Execute(ctx, table.OnlineReadOnlyTxControl(),
			`DECLARE $list_id AS Utf8;
			 SELECT id, item_name, list_id, purchased_at, user_id
			 FROM purchase_history
			 WHERE list_id = $list_id
			 ORDER BY purchased_at DESC
			 LIMIT 50`,
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
				e := models.PurchaseHistoryEntry{}
				if err = res.ScanNamed(
					named.Required("id", &e.ID),
					named.Required("item_name", &e.ItemName),
					named.Required("list_id", &e.ListID),
					named.Required("purchased_at", &e.PurchasedAt),
					named.Required("user_id", &e.UserID),
				); err != nil {
					return err
				}
				entries = append(entries, e)
			}
		}
		return res.Err()
	})
	return entries, err
}

// UpdatePhotoURL сохраняет URL фото товара.
func (r *ItemRepo) UpdatePhotoURL(ctx context.Context, itemID, photoURL string) error {
	now := time.Now()
	return r.db.Driver.Table().Do(ctx, func(ctx context.Context, s table.Session) error {
		_, _, err := s.Execute(ctx, table.SerializableReadWriteTxControl(table.CommitTx()),
			`DECLARE $item_id AS Utf8;
			 DECLARE $photo_url AS Utf8;
			 DECLARE $updated_at AS Datetime;
			 UPDATE items SET photo_url = $photo_url, updated_at = $updated_at
			 WHERE item_id = $item_id`,
			table.NewQueryParameters(
				table.ValueParam("$item_id", types.TextValue(itemID)),
				table.ValueParam("$photo_url", types.TextValue(photoURL)),
				table.ValueParam("$updated_at", types.DatetimeValueFromTime(now)),
			),
		)
		return err
	})
}

func scanItem(res interface {
	ScanNamed(...named.Value) error
}) (*models.Item, error) {
	i := &models.Item{}
	err := res.ScanNamed(
		named.Required("item_id", &i.ItemID),
		named.Required("list_id", &i.ListID),
		named.Required("name", &i.Name),
		named.Optional("tags", &i.Tags),
		named.Optional("note", &i.Note),
		named.Optional("category_id", &i.CategoryID),
		named.Optional("photo_url", &i.PhotoURL),
		named.Required("is_deleted", &i.IsDeleted),
		named.Required("added_by", &i.AddedBy),
		named.Required("updated_by", &i.UpdatedBy),
		named.Required("updated_at", &i.UpdatedAt),
		named.Optional("sort_index", &i.SortIndex),
	)
	return i, err
}
