package handlers

import (
	"net/http"
	"net/http/httptest"
	"strings"
	"testing"
)

// 1. GET /lists — happy path
func TestListGetAll_OK(t *testing.T) {
	h := &ListHandler{lists: newFakeListStore(), logger: silentLogger()}
	router := makeListRouter(h)

	req := httptest.NewRequest(http.MethodGet, "/lists", nil)
	req = withUser(req, "user-1")
	rr := httptest.NewRecorder()
	router.ServeHTTP(rr, req)

	if rr.Code != http.StatusOK {
		t.Fatalf("want 200, got %d: %s", rr.Code, rr.Body)
	}
	if !strings.Contains(rr.Body.String(), "[") {
		t.Errorf("expected JSON array in body, got: %s", rr.Body)
	}
}

// 2. POST /lists — happy path
func TestListCreate_OK(t *testing.T) {
	h := &ListHandler{lists: newFakeListStore(), logger: silentLogger()}
	router := makeListRouter(h)

	req := httptest.NewRequest(http.MethodPost, "/lists",
		strings.NewReader(`{"title":"Продукты","color_value":123,"list_id":"l-1"}`))
	req.Header.Set("Content-Type", "application/json")
	req = withUser(req, "user-1")
	rr := httptest.NewRecorder()
	router.ServeHTTP(rr, req)

	if rr.Code != http.StatusCreated {
		t.Fatalf("want 201, got %d: %s", rr.Code, rr.Body)
	}
	if !strings.Contains(rr.Body.String(), "Продукты") {
		t.Errorf("expected 'Продукты' in body, got: %s", rr.Body)
	}
}

// 3. POST /lists — missing title
func TestListCreate_NoTitle(t *testing.T) {
	h := &ListHandler{lists: newFakeListStore(), logger: silentLogger()}
	router := makeListRouter(h)

	req := httptest.NewRequest(http.MethodPost, "/lists", strings.NewReader(`{}`))
	req.Header.Set("Content-Type", "application/json")
	req = withUser(req, "user-1")
	rr := httptest.NewRecorder()
	router.ServeHTTP(rr, req)

	if rr.Code != http.StatusBadRequest {
		t.Fatalf("want 400, got %d", rr.Code)
	}
}

// 4. GET /lists/{list_id} — user is not a member
func TestListGetOne_NotMember(t *testing.T) {
	h := &ListHandler{lists: newFakeListStore(), logger: silentLogger()}
	router := makeListRouter(h)

	req := httptest.NewRequest(http.MethodGet, "/lists/list-1", nil)
	req = withUser(req, "user-1")
	rr := httptest.NewRecorder()
	router.ServeHTTP(rr, req)

	if rr.Code != http.StatusNotFound {
		t.Fatalf("want 404, got %d", rr.Code)
	}
}

// 5. DELETE /lists/{list_id} — owner can delete
func TestListDelete_OK(t *testing.T) {
	ls := makeFakeListStoreWithRoles(map[string]string{"list-1:user-1": "owner"})
	h := &ListHandler{lists: ls, logger: silentLogger()}
	router := makeListRouter(h)

	req := httptest.NewRequest(http.MethodDelete, "/lists/list-1", nil)
	req = withUser(req, "user-1")
	rr := httptest.NewRecorder()
	router.ServeHTTP(rr, req)

	if rr.Code != http.StatusNoContent {
		t.Fatalf("want 204, got %d: %s", rr.Code, rr.Body)
	}
}

// 6. DELETE /lists/{list_id} — non-owner gets 403
func TestListDelete_NotOwner(t *testing.T) {
	ls := makeFakeListStoreWithRoles(map[string]string{"list-1:user-1": "editor"})
	h := &ListHandler{lists: ls, logger: silentLogger()}
	router := makeListRouter(h)

	req := httptest.NewRequest(http.MethodDelete, "/lists/list-1", nil)
	req = withUser(req, "user-1")
	rr := httptest.NewRecorder()
	router.ServeHTTP(rr, req)

	if rr.Code != http.StatusForbidden {
		t.Fatalf("want 403, got %d", rr.Code)
	}
}

// 7. DELETE /lists/{list_id}/members/{user_id} — owner cannot remove themselves
func TestListRemoveMember_CannotRemoveSelf(t *testing.T) {
	ls := makeFakeListStoreWithRoles(map[string]string{"list-1:user-1": "owner"})
	h := &ListHandler{lists: ls, logger: silentLogger()}
	router := makeListRouter(h)

	req := httptest.NewRequest(http.MethodDelete, "/lists/list-1/members/user-1", nil)
	req = withUser(req, "user-1")
	rr := httptest.NewRecorder()
	router.ServeHTTP(rr, req)

	if rr.Code != http.StatusBadRequest {
		t.Fatalf("want 400, got %d", rr.Code)
	}
}
