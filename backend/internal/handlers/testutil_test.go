package handlers

import (
	"context"
	"errors"
	"io"
	"log/slog"
	"net/http"
	"strings"
	"time"

	"github.com/go-chi/chi/v5"
	"github.com/google/uuid"
	"github.com/lena/kartoshka-backend/internal/middleware"
	"github.com/lena/kartoshka-backend/internal/models"
)

// ── fakeItemStore ────────────────────────────────────────────────────────────

type fakeItemStore struct {
	items map[string]*models.Item
	err   error
}

func newFakeItemStore() *fakeItemStore {
	return &fakeItemStore{items: make(map[string]*models.Item)}
}

func (f *fakeItemStore) GetAllByList(_ context.Context, listID string) ([]models.Item, error) {
	if f.err != nil {
		return nil, f.err
	}
	var out []models.Item
	for _, it := range f.items {
		if it.ListID == listID {
			out = append(out, *it)
		}
	}
	return out, nil
}

func (f *fakeItemStore) GetByID(_ context.Context, itemID string) (*models.Item, error) {
	if f.err != nil {
		return nil, f.err
	}
	return f.items[itemID], nil
}

func (f *fakeItemStore) Create(_ context.Context, listID, userID, name, tags, note, categoryID string, sortIndex int32) (*models.Item, error) {
	if f.err != nil {
		return nil, f.err
	}
	it := &models.Item{
		ItemID:    uuid.New().String(),
		ListID:    listID,
		Name:      name,
		AddedBy:   userID,
		UpdatedBy: userID,
		UpdatedAt: time.Now(),
	}
	if tags != "" {
		it.Tags = &tags
	}
	if note != "" {
		it.Note = &note
	}
	if categoryID != "" {
		it.CategoryID = &categoryID
	}
	if sortIndex != 0 {
		it.SortIndex = &sortIndex
	}
	f.items[it.ItemID] = it
	return it, nil
}

func (f *fakeItemStore) Update(_ context.Context, itemID, userID, name, _, _, _ string) (*models.Item, error) {
	if f.err != nil {
		return nil, f.err
	}
	it, ok := f.items[itemID]
	if !ok {
		return nil, nil
	}
	it.Name = name
	it.UpdatedBy = userID
	it.UpdatedAt = time.Now()
	return it, nil
}

func (f *fakeItemStore) SoftDelete(_ context.Context, _, _ string) error                    { return f.err }
func (f *fakeItemStore) Check(_ context.Context, _, _, _, _ string) error                   { return f.err }
func (f *fakeItemStore) Uncheck(_ context.Context, _, _ string) error                       { return f.err }
func (f *fakeItemStore) Move(_ context.Context, _, _, _ string) error                       { return f.err }
func (f *fakeItemStore) Reorder(_ context.Context, _ []models.ReorderItem) error            { return f.err }
func (f *fakeItemStore) UpdatePhotoURL(_ context.Context, _, _ string) error                { return f.err }
func (f *fakeItemStore) GetRecent(_ context.Context, _ string) ([]models.PurchaseHistoryEntry, error) {
	return []models.PurchaseHistoryEntry{}, f.err
}

// ── fakeListStore ────────────────────────────────────────────────────────────

type fakeListStore struct {
	lists   map[string]*models.List
	members map[string]map[string]string // listID → userID → role
	err     error
}

func newFakeListStore() *fakeListStore {
	return &fakeListStore{
		lists:   make(map[string]*models.List),
		members: make(map[string]map[string]string),
	}
}

func (f *fakeListStore) addMember(listID, userID, role string) {
	if f.members[listID] == nil {
		f.members[listID] = make(map[string]string)
	}
	f.members[listID][userID] = role
}

func (f *fakeListStore) GetMemberRole(_ context.Context, listID, userID string) (string, error) {
	if f.err != nil {
		return "", f.err
	}
	return f.members[listID][userID], nil
}

func (f *fakeListStore) GetAllForUser(_ context.Context, userID string) ([]models.List, error) {
	if f.err != nil {
		return nil, f.err
	}
	var out []models.List
	for listID, roles := range f.members {
		if _, ok := roles[userID]; ok {
			if l, ok := f.lists[listID]; ok {
				out = append(out, *l)
			}
		}
	}
	return out, nil
}

func (f *fakeListStore) GetByID(_ context.Context, listID string) (*models.List, error) {
	if f.err != nil {
		return nil, f.err
	}
	return f.lists[listID], nil
}

func (f *fakeListStore) Create(_ context.Context, ownerID, listID, title string, colorValue int64, position int32) (*models.List, error) {
	if f.err != nil {
		return nil, f.err
	}
	l := &models.List{
		ListID:     listID,
		OwnerID:    ownerID,
		Title:      title,
		ColorValue: colorValue,
		Position:   position,
	}
	f.lists[listID] = l
	f.addMember(listID, ownerID, "owner")
	return l, nil
}

func (f *fakeListStore) Update(_ context.Context, _, _ string, _ int64, _ int32, _, _ string) error {
	return f.err
}

func (f *fakeListStore) Delete(_ context.Context, _, _ string) error { return f.err }

func (f *fakeListStore) GetMembers(_ context.Context, listID string) ([]models.ListMember, error) {
	if f.err != nil {
		return nil, f.err
	}
	var out []models.ListMember
	for uid, role := range f.members[listID] {
		out = append(out, models.ListMember{ListID: listID, UserID: uid, Role: role})
	}
	return out, nil
}

func (f *fakeListStore) RemoveMember(_ context.Context, _, _ string) error { return f.err }

// ── fakeUserStore ────────────────────────────────────────────────────────────

type fakeUserStore struct {
	users       map[string]*models.User
	byEmail     map[string]*models.User
	byYandexUID map[string]*models.User
	err         error
}

func newFakeUserStore() *fakeUserStore {
	return &fakeUserStore{
		users:       make(map[string]*models.User),
		byEmail:     make(map[string]*models.User),
		byYandexUID: make(map[string]*models.User),
	}
}

func (f *fakeUserStore) GetByEmail(_ context.Context, email string) (*models.User, error) {
	if f.err != nil {
		return nil, f.err
	}
	return f.byEmail[email], nil
}

func (f *fakeUserStore) GetByYandexUID(_ context.Context, uid string) (*models.User, error) {
	if f.err != nil {
		return nil, f.err
	}
	return f.byYandexUID[uid], nil
}

func (f *fakeUserStore) GetByID(_ context.Context, userID string) (*models.User, error) {
	if f.err != nil {
		return nil, f.err
	}
	return f.users[userID], nil
}

func (f *fakeUserStore) Create(_ context.Context, yandexUID, email, name string) (*models.User, error) {
	if f.err != nil {
		return nil, f.err
	}
	u := &models.User{
		UserID:    uuid.New().String(),
		YandexUID: yandexUID,
		Email:     email,
		Name:      name,
	}
	f.users[u.UserID] = u
	f.byEmail[email] = u
	if yandexUID != "" {
		f.byYandexUID[yandexUID] = u
	}
	return u, nil
}

func (f *fakeUserStore) SetPasswordHash(_ context.Context, userID, hash string) error {
	if f.err != nil {
		return f.err
	}
	if u, ok := f.users[userID]; ok {
		u.PasswordHash = hash
	}
	return nil
}

func (f *fakeUserStore) SetVerified(_ context.Context, _ string) error    { return f.err }
func (f *fakeUserStore) UpdateName(_ context.Context, _, _ string) error  { return f.err }
func (f *fakeUserStore) UpdateAvatarURL(_ context.Context, _, _ string) error { return f.err }

// ── fakeTokenStore ───────────────────────────────────────────────────────────

type tokenEntry struct {
	userID    string
	expiresAt time.Time
}

type fakeTokenStore struct {
	tokens map[string]tokenEntry
	err    error
}

func newFakeTokenStore() *fakeTokenStore {
	return &fakeTokenStore{tokens: make(map[string]tokenEntry)}
}

func (f *fakeTokenStore) Save(_ context.Context, userID string, expiresAt time.Time) (string, error) {
	if f.err != nil {
		return "", f.err
	}
	token := uuid.New().String()
	f.tokens[token] = tokenEntry{userID: userID, expiresAt: expiresAt}
	return token, nil
}

func (f *fakeTokenStore) Get(_ context.Context, token string) (string, time.Time, error) {
	if f.err != nil {
		return "", time.Time{}, f.err
	}
	e, ok := f.tokens[token]
	if !ok {
		return "", time.Time{}, errors.New("not found")
	}
	return e.userID, e.expiresAt, nil
}

func (f *fakeTokenStore) Delete(_ context.Context, token string) error {
	delete(f.tokens, token)
	return nil
}

// ── fakeInvitationStore ──────────────────────────────────────────────────────

type fakeInvitationStore struct {
	pending *models.Invitation
	byToken *models.Invitation
	info    *models.InviteInfo
	err     error
}

func (f *fakeInvitationStore) Create(_ context.Context, listID, inviterID, inviteeEmail string) (*models.Invitation, error) {
	if f.err != nil {
		return nil, f.err
	}
	return &models.Invitation{
		InviteToken:  uuid.New().String(),
		ListID:       listID,
		InviterID:    inviterID,
		InviteeEmail: inviteeEmail,
		Status:       "pending",
		ExpiresAt:    time.Now().Add(7 * 24 * time.Hour),
	}, nil
}

func (f *fakeInvitationStore) GetByToken(_ context.Context, _ string) (*models.Invitation, error) {
	return f.byToken, f.err
}

func (f *fakeInvitationStore) GetInfoByToken(_ context.Context, _ string) (*models.InviteInfo, error) {
	if f.err != nil {
		return nil, f.err
	}
	if f.byToken != nil {
		return &models.InviteInfo{
			InviteToken: f.byToken.InviteToken,
			Status:      f.byToken.Status,
			ExpiresAt:   f.byToken.ExpiresAt,
		}, nil
	}
	return f.info, nil
}

func (f *fakeInvitationStore) GetPendingByListAndEmail(_ context.Context, _, _ string) (*models.Invitation, error) {
	return f.pending, f.err
}

func (f *fakeInvitationStore) Accept(_ context.Context, _, _, _ string) error { return f.err }
func (f *fakeInvitationStore) MarkExpired(_ context.Context, _ string) error  { return f.err }

// ── router helpers ───────────────────────────────────────────────────────────

func makeFakeListStoreWithRoles(roles map[string]string) *fakeListStore {
	fs := newFakeListStore()
	for key, role := range roles {
		parts := strings.SplitN(key, ":", 2)
		if len(parts) == 2 {
			fs.addMember(parts[0], parts[1], role)
		}
	}
	return fs
}

func makeListRouter(h *ListHandler) *chi.Mux {
	r := chi.NewRouter()
	r.Get("/lists", h.GetAll)
	r.Post("/lists", h.Create)
	r.Get("/lists/{list_id}", h.GetOne)
	r.Put("/lists/{list_id}", h.Update)
	r.Delete("/lists/{list_id}", h.Delete)
	r.Get("/lists/{list_id}/members", h.GetMembers)
	r.Delete("/lists/{list_id}/members/{user_id}", h.RemoveMember)
	return r
}

func makeInvitationRouter(h *InvitationHandler) *chi.Mux {
	r := chi.NewRouter()
	r.Post("/lists/{list_id}/invite", h.Create)
	r.Get("/invite/{invite_token}", h.GetInfo)
	r.Post("/invite/{invite_token}/accept", h.Accept)
	return r
}

// ── shared test helpers ──────────────────────────────────────────────────────

func withUser(r *http.Request, userID string) *http.Request {
	return r.WithContext(context.WithValue(r.Context(), middleware.UserIDKey, userID))
}

func silentLogger() *slog.Logger {
	return slog.New(slog.NewTextHandler(io.Discard, nil))
}
