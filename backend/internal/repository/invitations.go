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

const inviteTTL = 7 * 24 * time.Hour

type InvitationRepo struct {
	db *DB
}

func NewInvitationRepo(db *DB) *InvitationRepo {
	return &InvitationRepo{db: db}
}

// Create создаёт инвайт и возвращает его.
func (r *InvitationRepo) Create(ctx context.Context, listID, inviterID, inviteeEmail string) (*models.Invitation, error) {
	now := time.Now()
	inv := &models.Invitation{
		InviteToken:  uuid.New().String(),
		ListID:       listID,
		InviterID:    inviterID,
		InviteeEmail: inviteeEmail,
		Status:       "pending",
		CreatedAt:    now,
		ExpiresAt:    now.Add(inviteTTL),
	}

	err := r.db.Driver.Table().Do(ctx, func(ctx context.Context, s table.Session) error {
		_, _, err := s.Execute(ctx, table.SerializableReadWriteTxControl(table.CommitTx()),
			`DECLARE $invite_token AS Utf8;
			 DECLARE $list_id AS Utf8;
			 DECLARE $inviter_id AS Utf8;
			 DECLARE $invitee_email AS Utf8;
			 DECLARE $status AS Utf8;
			 DECLARE $created_at AS Datetime;
			 DECLARE $expires_at AS Datetime;
			 UPSERT INTO invitations
			   (invite_token, list_id, inviter_id, invitee_email, status, created_at, expires_at)
			 VALUES
			   ($invite_token, $list_id, $inviter_id, $invitee_email, $status, $created_at, $expires_at)`,
			table.NewQueryParameters(
				table.ValueParam("$invite_token", types.TextValue(inv.InviteToken)),
				table.ValueParam("$list_id", types.TextValue(listID)),
				table.ValueParam("$inviter_id", types.TextValue(inviterID)),
				table.ValueParam("$invitee_email", types.TextValue(inviteeEmail)),
				table.ValueParam("$status", types.TextValue("pending")),
				table.ValueParam("$created_at", types.DatetimeValueFromTime(now)),
				table.ValueParam("$expires_at", types.DatetimeValueFromTime(inv.ExpiresAt)),
			),
		)
		return err
	})
	if err != nil {
		return nil, err
	}
	return inv, nil
}

// GetByToken возвращает инвайт по токену.
func (r *InvitationRepo) GetByToken(ctx context.Context, token string) (*models.Invitation, error) {
	var inv *models.Invitation
	err := r.db.Driver.Table().Do(ctx, func(ctx context.Context, s table.Session) error {
		_, res, err := s.Execute(ctx, table.OnlineReadOnlyTxControl(),
			`DECLARE $invite_token AS Utf8;
			 SELECT invite_token, list_id, inviter_id, invitee_email,
			        status, created_at, expires_at
			 FROM invitations WHERE invite_token = $invite_token`,
			table.NewQueryParameters(
				table.ValueParam("$invite_token", types.TextValue(token)),
			),
		)
		if err != nil {
			return err
		}
		defer res.Close()
		if res.NextResultSet(ctx) && res.NextRow() {
			i := models.Invitation{}
			if err = res.ScanNamed(
				named.Required("invite_token", &i.InviteToken),
				named.Required("list_id", &i.ListID),
				named.Required("inviter_id", &i.InviterID),
				named.Optional("invitee_email", &i.InviteeEmail),
				named.Required("status", &i.Status),
				named.Required("created_at", &i.CreatedAt),
				named.Required("expires_at", &i.ExpiresAt),
			); err != nil {
				return err
			}
			inv = &i
		}
		return res.Err()
	})
	return inv, err
}

// GetInfoByToken возвращает публичную информацию об инвайте (название списка, имя пригласившего).
func (r *InvitationRepo) GetInfoByToken(ctx context.Context, token string) (*models.InviteInfo, error) {
	var info *models.InviteInfo
	err := r.db.Driver.Table().Do(ctx, func(ctx context.Context, s table.Session) error {
		_, res, err := s.Execute(ctx, table.OnlineReadOnlyTxControl(),
			`DECLARE $invite_token AS Utf8;
			 SELECT i.invite_token, i.status, i.expires_at,
			        l.title AS list_title, u.name AS inviter_name
			 FROM invitations AS i
			 JOIN lists AS l ON i.list_id = l.list_id
			 JOIN users AS u ON i.inviter_id = u.user_id
			 WHERE i.invite_token = $invite_token`,
			table.NewQueryParameters(
				table.ValueParam("$invite_token", types.TextValue(token)),
			),
		)
		if err != nil {
			return err
		}
		defer res.Close()
		if res.NextResultSet(ctx) && res.NextRow() {
			inf := models.InviteInfo{}
			if err = res.ScanNamed(
				named.Required("invite_token", &inf.InviteToken),
				named.Required("status", &inf.Status),
				named.Required("expires_at", &inf.ExpiresAt),
				named.Required("list_title", &inf.ListTitle),
				named.Required("inviter_name", &inf.InviterName),
			); err != nil {
				return err
			}
			info = &inf
		}
		return res.Err()
	})
	return info, err
}

// Accept добавляет пользователя в список как editor и помечает инвайт принятым.
func (r *InvitationRepo) Accept(ctx context.Context, token, listID, userID string) error {
	now := time.Now()

	// Шаг 1: добавить в list_members
	err := r.db.Driver.Table().Do(ctx, func(ctx context.Context, s table.Session) error {
		_, _, err := s.Execute(ctx, table.SerializableReadWriteTxControl(table.CommitTx()),
			`DECLARE $list_id AS Utf8;
			 DECLARE $user_id AS Utf8;
			 DECLARE $joined_at AS Datetime;
			 UPSERT INTO list_members (list_id, user_id, role, joined_at)
			 VALUES ($list_id, $user_id, "editor", $joined_at)`,
			table.NewQueryParameters(
				table.ValueParam("$list_id", types.TextValue(listID)),
				table.ValueParam("$user_id", types.TextValue(userID)),
				table.ValueParam("$joined_at", types.DatetimeValueFromTime(now)),
			),
		)
		return err
	})
	if err != nil {
		return err
	}

	// Шаг 2: обновить статус инвайта
	return r.db.Driver.Table().Do(ctx, func(ctx context.Context, s table.Session) error {
		_, _, err := s.Execute(ctx, table.SerializableReadWriteTxControl(table.CommitTx()),
			`DECLARE $invite_token AS Utf8;
			 UPDATE invitations SET status = "accepted"
			 WHERE invite_token = $invite_token`,
			table.NewQueryParameters(
				table.ValueParam("$invite_token", types.TextValue(token)),
			),
		)
		return err
	})
}

// GetPendingByListAndEmail возвращает активный pending-инвайт для данного
// email на данный список, или nil если такого нет.
func (r *InvitationRepo) GetPendingByListAndEmail(ctx context.Context, listID, email string) (*models.Invitation, error) {
	var inv *models.Invitation
	err := r.db.Driver.Table().Do(ctx, func(ctx context.Context, s table.Session) error {
		_, res, err := s.Execute(ctx, table.OnlineReadOnlyTxControl(),
			`DECLARE $list_id AS Utf8;
			 DECLARE $invitee_email AS Utf8;
			 SELECT invite_token, list_id, inviter_id, invitee_email,
			        status, created_at, expires_at
			 FROM invitations
			 WHERE list_id = $list_id
			   AND invitee_email = $invitee_email
			   AND status = "pending"`,
			table.NewQueryParameters(
				table.ValueParam("$list_id", types.TextValue(listID)),
				table.ValueParam("$invitee_email", types.TextValue(email)),
			),
		)
		if err != nil {
			return err
		}
		defer res.Close()
		if res.NextResultSet(ctx) && res.NextRow() {
			i := models.Invitation{}
			if err = res.ScanNamed(
				named.Required("invite_token", &i.InviteToken),
				named.Required("list_id", &i.ListID),
				named.Required("inviter_id", &i.InviterID),
				named.Optional("invitee_email", &i.InviteeEmail),
				named.Required("status", &i.Status),
				named.Required("created_at", &i.CreatedAt),
				named.Required("expires_at", &i.ExpiresAt),
			); err != nil {
				return err
			}
			inv = &i
		}
		return res.Err()
	})
	return inv, err
}

// MarkExpired помечает инвайт как истёкший.
func (r *InvitationRepo) MarkExpired(ctx context.Context, token string) error {
	return r.db.Driver.Table().Do(ctx, func(ctx context.Context, s table.Session) error {
		_, _, err := s.Execute(ctx, table.SerializableReadWriteTxControl(table.CommitTx()),
			`DECLARE $invite_token AS Utf8;
			 UPDATE invitations SET status = "expired"
			 WHERE invite_token = $invite_token`,
			table.NewQueryParameters(
				table.ValueParam("$invite_token", types.TextValue(token)),
			),
		)
		return err
	})
}
