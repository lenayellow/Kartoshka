package handlers

import (
	"context"
	"io"
	"log/slog"
	"net/http"
	"time"

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

// ── shared test helpers ──────────────────────────────────────────────────────

func withUser(r *http.Request, userID string) *http.Request {
	return r.WithContext(context.WithValue(r.Context(), middleware.UserIDKey, userID))
}

func silentLogger() *slog.Logger {
	return slog.New(slog.NewTextHandler(io.Discard, nil))
}
