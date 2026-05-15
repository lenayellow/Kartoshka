package handlers

import (
	"encoding/json"
	"net/http"
	"net/http/httptest"
	"strings"
	"testing"
	"time"

	"github.com/go-chi/chi/v5"
	"github.com/lena/kartoshka-backend/internal/auth"
	"github.com/lena/kartoshka-backend/internal/models"
)

func makeAuthRouter(h *AuthHandler) *chi.Mux {
	r := chi.NewRouter()
	r.Post("/auth/email/register", h.EmailRegister)
	r.Post("/auth/email/login", h.EmailLogin)
	r.Post("/auth/refresh", h.Refresh)
	r.Post("/auth/logout", h.Logout)
	return r
}

func newAuthHandler(us *fakeUserStore, ts *fakeTokenStore) *AuthHandler {
	return &AuthHandler{users: us, tokens: ts, logger: silentLogger()}
}

func authPost(t *testing.T, router http.Handler, path, body string) *httptest.ResponseRecorder {
	t.Helper()
	req := httptest.NewRequest(http.MethodPost, path, strings.NewReader(body))
	req.Header.Set("Content-Type", "application/json")
	rr := httptest.NewRecorder()
	router.ServeHTTP(rr, req)
	return rr
}

func hasKey(t *testing.T, rr *httptest.ResponseRecorder, key string) {
	t.Helper()
	var m map[string]interface{}
	if err := json.NewDecoder(rr.Body).Decode(&m); err != nil {
		t.Fatalf("response is not JSON: %v", err)
	}
	if _, ok := m[key]; !ok {
		t.Errorf("expected key %q in response, got: %v", key, m)
	}
}

// 1. Register — happy path
func TestEmailRegister_OK(t *testing.T) {
	router := makeAuthRouter(newAuthHandler(newFakeUserStore(), newFakeTokenStore()))
	rr := authPost(t, router, "/auth/email/register",
		`{"email":"a@b.com","password":"12345678","name":"Лена"}`)
	if rr.Code != http.StatusCreated {
		t.Fatalf("want 201, got %d: %s", rr.Code, rr.Body)
	}
	hasKey(t, rr, "access_token")
}

// 2. Register — missing fields
func TestEmailRegister_MissingFields(t *testing.T) {
	router := makeAuthRouter(newAuthHandler(newFakeUserStore(), newFakeTokenStore()))
	rr := authPost(t, router, "/auth/email/register", `{}`)
	if rr.Code != http.StatusBadRequest {
		t.Fatalf("want 400, got %d", rr.Code)
	}
}

// 3. Register — weak password
func TestEmailRegister_WeakPassword(t *testing.T) {
	router := makeAuthRouter(newAuthHandler(newFakeUserStore(), newFakeTokenStore()))
	rr := authPost(t, router, "/auth/email/register",
		`{"email":"a@b.com","password":"123","name":"Лена"}`)
	if rr.Code != http.StatusBadRequest {
		t.Fatalf("want 400, got %d", rr.Code)
	}
}

// 4. Register — email already taken
func TestEmailRegister_EmailTaken(t *testing.T) {
	us := newFakeUserStore()
	us.byEmail["a@b.com"] = &models.User{UserID: "u1", Email: "a@b.com"}
	router := makeAuthRouter(newAuthHandler(us, newFakeTokenStore()))
	rr := authPost(t, router, "/auth/email/register",
		`{"email":"a@b.com","password":"12345678","name":"Лена"}`)
	if rr.Code != http.StatusConflict {
		t.Fatalf("want 409, got %d", rr.Code)
	}
}

// 5. Login — happy path
func TestEmailLogin_OK(t *testing.T) {
	us := newFakeUserStore()
	u, _ := us.Create(nil, "", "a@b.com", "Лена")
	hash, _ := auth.HashPassword("12345678")
	us.SetPasswordHash(nil, u.UserID, hash)
	// keep byEmail in sync after SetPasswordHash mutated the struct in-place
	us.byEmail["a@b.com"] = us.users[u.UserID]

	router := makeAuthRouter(newAuthHandler(us, newFakeTokenStore()))
	rr := authPost(t, router, "/auth/email/login",
		`{"email":"a@b.com","password":"12345678"}`)
	if rr.Code != http.StatusOK {
		t.Fatalf("want 200, got %d: %s", rr.Code, rr.Body)
	}
	hasKey(t, rr, "access_token")
}

// 6. Login — wrong password
func TestEmailLogin_WrongPassword(t *testing.T) {
	us := newFakeUserStore()
	u, _ := us.Create(nil, "", "a@b.com", "Лена")
	hash, _ := auth.HashPassword("12345678")
	us.SetPasswordHash(nil, u.UserID, hash)
	us.byEmail["a@b.com"] = us.users[u.UserID]

	router := makeAuthRouter(newAuthHandler(us, newFakeTokenStore()))
	rr := authPost(t, router, "/auth/email/login",
		`{"email":"a@b.com","password":"wrongpass"}`)
	if rr.Code != http.StatusUnauthorized {
		t.Fatalf("want 401, got %d", rr.Code)
	}
}

// 7. Login — user not found
func TestEmailLogin_UserNotFound(t *testing.T) {
	router := makeAuthRouter(newAuthHandler(newFakeUserStore(), newFakeTokenStore()))
	rr := authPost(t, router, "/auth/email/login",
		`{"email":"nobody@b.com","password":"12345678"}`)
	if rr.Code != http.StatusUnauthorized {
		t.Fatalf("want 401, got %d", rr.Code)
	}
}

// 8. Refresh — happy path
func TestRefresh_OK(t *testing.T) {
	ts := newFakeTokenStore()
	token, _ := ts.Save(nil, "user-1", time.Now().Add(time.Hour))

	router := makeAuthRouter(newAuthHandler(newFakeUserStore(), ts))
	body, _ := json.Marshal(map[string]string{"refresh_token": token})
	rr := authPost(t, router, "/auth/refresh", string(body))
	if rr.Code != http.StatusOK {
		t.Fatalf("want 200, got %d: %s", rr.Code, rr.Body)
	}
	hasKey(t, rr, "access_token")
}

// 9. Refresh — invalid token
func TestRefresh_InvalidToken(t *testing.T) {
	router := makeAuthRouter(newAuthHandler(newFakeUserStore(), newFakeTokenStore()))
	rr := authPost(t, router, "/auth/refresh",
		`{"refresh_token":"nosuchtoken"}`)
	if rr.Code != http.StatusUnauthorized {
		t.Fatalf("want 401, got %d", rr.Code)
	}
}

// 10. Logout — always 204
func TestLogout_OK(t *testing.T) {
	router := makeAuthRouter(newAuthHandler(newFakeUserStore(), newFakeTokenStore()))
	body, _ := json.Marshal(map[string]string{"refresh_token": "anytoken"})
	rr := authPost(t, router, "/auth/logout", string(body))
	if rr.Code != http.StatusNoContent {
		t.Fatalf("want 204, got %d", rr.Code)
	}
}

