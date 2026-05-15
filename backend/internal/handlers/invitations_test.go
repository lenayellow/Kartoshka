package handlers

import (
	"net/http"
	"net/http/httptest"
	"strings"
	"testing"
	"time"

	"github.com/lena/kartoshka-backend/internal/models"
)

func newInvitationHandler(is *fakeInvitationStore, ls *fakeListStore, us *fakeUserStore) *InvitationHandler {
	return &InvitationHandler{invitations: is, lists: ls, users: us, logger: silentLogger()}
}

func invitePost(t *testing.T, router http.Handler, path, body string, userID string) *httptest.ResponseRecorder {
	t.Helper()
	req := httptest.NewRequest(http.MethodPost, path, strings.NewReader(body))
	req.Header.Set("Content-Type", "application/json")
	req = withUser(req, userID)
	rr := httptest.NewRecorder()
	router.ServeHTTP(rr, req)
	return rr
}

// 1. Self-invite → 409
func TestInviteCreate_SelfInvite(t *testing.T) {
	us := newFakeUserStore()
	us.byEmail["self@test.com"] = &models.User{UserID: "user-1"}
	ls := makeFakeListStoreWithRoles(map[string]string{"list-1:user-1": "editor"})
	router := makeInvitationRouter(newInvitationHandler(&fakeInvitationStore{}, ls, us))

	rr := invitePost(t, router, "/lists/list-1/invite", `{"invitee_email":"self@test.com"}`, "user-1")
	if rr.Code != http.StatusConflict {
		t.Fatalf("want 409, got %d: %s", rr.Code, rr.Body)
	}
}

// 2. Invitee is already a member → 409
func TestInviteCreate_AlreadyMember(t *testing.T) {
	us := newFakeUserStore()
	us.byEmail["other@test.com"] = &models.User{UserID: "user-2"}
	ls := makeFakeListStoreWithRoles(map[string]string{
		"list-1:user-1": "editor",
		"list-1:user-2": "editor",
	})
	router := makeInvitationRouter(newInvitationHandler(&fakeInvitationStore{}, ls, us))

	rr := invitePost(t, router, "/lists/list-1/invite", `{"invitee_email":"other@test.com"}`, "user-1")
	if rr.Code != http.StatusConflict {
		t.Fatalf("want 409, got %d: %s", rr.Code, rr.Body)
	}
}

// 3. Invitation already pending → 409
func TestInviteCreate_AlreadySent(t *testing.T) {
	us := newFakeUserStore()
	us.byEmail["other@test.com"] = &models.User{UserID: "user-2"}
	ls := makeFakeListStoreWithRoles(map[string]string{"list-1:user-1": "editor"})
	is := &fakeInvitationStore{
		pending: &models.Invitation{InviteToken: "tok", ListID: "list-1", Status: "pending"},
	}
	router := makeInvitationRouter(newInvitationHandler(is, ls, us))

	rr := invitePost(t, router, "/lists/list-1/invite", `{"invitee_email":"other@test.com"}`, "user-1")
	if rr.Code != http.StatusConflict {
		t.Fatalf("want 409, got %d: %s", rr.Code, rr.Body)
	}
}

// 4. Invitee email not registered → 404
func TestInviteCreate_UserNotFound(t *testing.T) {
	ls := makeFakeListStoreWithRoles(map[string]string{"list-1:user-1": "editor"})
	router := makeInvitationRouter(newInvitationHandler(&fakeInvitationStore{}, ls, newFakeUserStore()))

	rr := invitePost(t, router, "/lists/list-1/invite", `{"invitee_email":"nobody@test.com"}`, "user-1")
	if rr.Code != http.StatusNotFound {
		t.Fatalf("want 404, got %d", rr.Code)
	}
}

// 5. No email — anonymous deep-link invite → 201 with deep_link
func TestInviteCreate_NoEmail_OK(t *testing.T) {
	ls := makeFakeListStoreWithRoles(map[string]string{"list-1:user-1": "editor"})
	router := makeInvitationRouter(newInvitationHandler(&fakeInvitationStore{}, ls, newFakeUserStore()))

	rr := invitePost(t, router, "/lists/list-1/invite", `{}`, "user-1")
	if rr.Code != http.StatusCreated {
		t.Fatalf("want 201, got %d: %s", rr.Code, rr.Body)
	}
	if !strings.Contains(rr.Body.String(), "deep_link") {
		t.Errorf("expected 'deep_link' in body, got: %s", rr.Body)
	}
}

// 6. Accept — happy path → 200 with list_id
func TestInviteAccept_OK(t *testing.T) {
	is := &fakeInvitationStore{byToken: &models.Invitation{
		InviteToken: "tok-1",
		ListID:      "list-1",
		Status:      "pending",
		ExpiresAt:   time.Now().Add(time.Hour),
	}}
	ls := makeFakeListStoreWithRoles(map[string]string{})
	router := makeInvitationRouter(newInvitationHandler(is, ls, newFakeUserStore()))

	req := httptest.NewRequest(http.MethodPost, "/invite/tok-1/accept", nil)
	req = withUser(req, "user-1")
	rr := httptest.NewRecorder()
	router.ServeHTTP(rr, req)

	if rr.Code != http.StatusOK {
		t.Fatalf("want 200, got %d: %s", rr.Code, rr.Body)
	}
	if !strings.Contains(rr.Body.String(), "list_id") {
		t.Errorf("expected 'list_id' in body, got: %s", rr.Body)
	}
}

// 7. Accept — expired invitation → 410
func TestInviteAccept_Expired(t *testing.T) {
	is := &fakeInvitationStore{byToken: &models.Invitation{
		InviteToken: "tok-1",
		ListID:      "list-1",
		Status:      "expired",
		ExpiresAt:   time.Now().Add(-time.Hour),
	}}
	ls := makeFakeListStoreWithRoles(map[string]string{})
	router := makeInvitationRouter(newInvitationHandler(is, ls, newFakeUserStore()))

	req := httptest.NewRequest(http.MethodPost, "/invite/tok-1/accept", nil)
	req = withUser(req, "user-1")
	rr := httptest.NewRecorder()
	router.ServeHTTP(rr, req)

	if rr.Code != http.StatusGone {
		t.Fatalf("want 410, got %d", rr.Code)
	}
}

// 8. Accept — user is already a member → 200 with "уже участник"
func TestInviteAccept_AlreadyMember(t *testing.T) {
	is := &fakeInvitationStore{byToken: &models.Invitation{
		InviteToken: "tok-1",
		ListID:      "list-1",
		Status:      "pending",
		ExpiresAt:   time.Now().Add(time.Hour),
	}}
	ls := makeFakeListStoreWithRoles(map[string]string{"list-1:user-1": "editor"})
	router := makeInvitationRouter(newInvitationHandler(is, ls, newFakeUserStore()))

	req := httptest.NewRequest(http.MethodPost, "/invite/tok-1/accept", nil)
	req = withUser(req, "user-1")
	rr := httptest.NewRecorder()
	router.ServeHTTP(rr, req)

	if rr.Code != http.StatusOK {
		t.Fatalf("want 200, got %d: %s", rr.Code, rr.Body)
	}
	if !strings.Contains(rr.Body.String(), "уже участник") {
		t.Errorf("expected 'уже участник' in body, got: %s", rr.Body)
	}
}
