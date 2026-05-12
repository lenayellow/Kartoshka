package repository

import (
	"context"
	"time"

	"github.com/ydb-platform/ydb-go-sdk/v3/table"
	"github.com/ydb-platform/ydb-go-sdk/v3/table/result/named"
	"github.com/ydb-platform/ydb-go-sdk/v3/table/types"

	"github.com/lena/kartoshka-backend/internal/models"
)

type EventsRepo struct {
	db *DB
}

func NewEventsRepo(db *DB) *EventsRepo {
	return &EventsRepo{db: db}
}

// GetChangedItems возвращает все товары списка, изменённые после since.
// Включает is_deleted=true — клиент знает какие товары удалены.
func (r *EventsRepo) GetChangedItems(ctx context.Context, listID string, since time.Time) ([]models.Item, error) {
	var items []models.Item
	err := r.db.Driver.Table().Do(ctx, func(ctx context.Context, s table.Session) error {
		_, res, err := s.Execute(ctx, table.OnlineReadOnlyTxControl(),
			`DECLARE $list_id AS Utf8;
			 DECLARE $since AS Datetime;
			 SELECT item_id, list_id, name, tags, note, category_id, photo_url,
			        is_deleted, added_by, updated_by, updated_at, sort_index
			 FROM items
			 WHERE list_id = $list_id AND updated_at > $since
			 ORDER BY updated_at ASC`,
			table.NewQueryParameters(
				table.ValueParam("$list_id", types.TextValue(listID)),
				table.ValueParam("$since", types.DatetimeValueFromTime(since)),
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

// GetChangedMembers возвращает участников списка, присоединившихся после since.
func (r *EventsRepo) GetChangedMembers(ctx context.Context, listID string, since time.Time) ([]models.ListMember, error) {
	var members []models.ListMember
	err := r.db.Driver.Table().Do(ctx, func(ctx context.Context, s table.Session) error {
		_, res, err := s.Execute(ctx, table.OnlineReadOnlyTxControl(),
			`DECLARE $list_id AS Utf8;
			 DECLARE $since AS Datetime;
			 SELECT lm.user_id, lm.role, lm.joined_at, u.name, u.email
			 FROM list_members AS lm
			 JOIN users AS u ON lm.user_id = u.user_id
			 WHERE lm.list_id = $list_id AND lm.joined_at > $since
			 ORDER BY lm.joined_at ASC`,
			table.NewQueryParameters(
				table.ValueParam("$list_id", types.TextValue(listID)),
				table.ValueParam("$since", types.DatetimeValueFromTime(since)),
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
