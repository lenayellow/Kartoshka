package handlers

import (
	"bytes"
	"encoding/json"
	"net/http"
	"net/http/httptest"
	"testing"

	"github.com/go-chi/chi/v5"
	"github.com/lena/kartoshka-backend/internal/models"
)

func newItemRouter(h *ItemHandler) http.Handler {
	r := chi.NewRouter()
	r.Get("/lists/{list_id}/items", h.GetAll)
	r.Post("/lists/{list_id}/items", h.Create)
	r.Delete("/lists/{list_id}/items/{item_id}", h.Delete)
	r.Post("/lists/{list_id}/items/{item_id}/check", h.Check)
	r.Post("/lists/{list_id}/items/{item_id}/uncheck", h.Uncheck)
	r.Post("/lists/{list_id}/items/{item_id}/move", h.Move)
	r.Patch("/lists/{list_id}/items/reorder", h.Reorder)
	r.Get("/lists/{list_id}/recent", h.GetRecent)
	return r
}

func TestItemHandler_GetAll(t *testing.T) {
	items := newFakeItemStore()
	lists := newFakeListStore()
	lists.addMember("list-1", "user-1", "editor")
	items.items["item-1"] = &models.Item{ItemID: "item-1", ListID: "list-1", Name: "Milk"}

	router := newItemRouter(&ItemHandler{items: items, lists: lists, logger: silentLogger()})

	t.Run("200 returns items for member", func(t *testing.T) {
		req := withUser(httptest.NewRequest(http.MethodGet, "/lists/list-1/items", nil), "user-1")
		rr := httptest.NewRecorder()
		router.ServeHTTP(rr, req)
		if rr.Code != http.StatusOK {
			t.Fatalf("want 200, got %d", rr.Code)
		}
		var got []models.Item
		if err := json.NewDecoder(rr.Body).Decode(&got); err != nil {
			t.Fatal(err)
		}
		if len(got) != 1 || got[0].Name != "Milk" {
			t.Fatalf("unexpected body: %+v", got)
		}
	})

	t.Run("404 for non-member", func(t *testing.T) {
		req := withUser(httptest.NewRequest(http.MethodGet, "/lists/list-1/items", nil), "stranger")
		rr := httptest.NewRecorder()
		router.ServeHTTP(rr, req)
		if rr.Code != http.StatusNotFound {
			t.Fatalf("want 404, got %d", rr.Code)
		}
	})
}

func TestItemHandler_Create(t *testing.T) {
	items := newFakeItemStore()
	lists := newFakeListStore()
	lists.addMember("list-1", "user-1", "editor")

	router := newItemRouter(&ItemHandler{items: items, lists: lists, logger: silentLogger()})

	t.Run("201 creates item", func(t *testing.T) {
		body := jsonBody(t, map[string]string{"name": "Bread"})
		req := withUser(httptest.NewRequest(http.MethodPost, "/lists/list-1/items", body), "user-1")
		req.Header.Set("Content-Type", "application/json")
		rr := httptest.NewRecorder()
		router.ServeHTTP(rr, req)
		if rr.Code != http.StatusCreated {
			t.Fatalf("want 201, got %d: %s", rr.Code, rr.Body.String())
		}
		var got models.Item
		if err := json.NewDecoder(rr.Body).Decode(&got); err != nil {
			t.Fatal(err)
		}
		if got.Name != "Bread" {
			t.Fatalf("want Name=Bread, got %q", got.Name)
		}
		if got.ItemID == "" {
			t.Fatal("ItemID must be set")
		}
	})

	t.Run("400 on empty name", func(t *testing.T) {
		body := jsonBody(t, map[string]string{"name": ""})
		req := withUser(httptest.NewRequest(http.MethodPost, "/lists/list-1/items", body), "user-1")
		req.Header.Set("Content-Type", "application/json")
		rr := httptest.NewRecorder()
		router.ServeHTTP(rr, req)
		if rr.Code != http.StatusBadRequest {
			t.Fatalf("want 400, got %d", rr.Code)
		}
	})

	t.Run("404 for non-member", func(t *testing.T) {
		body := jsonBody(t, map[string]string{"name": "Milk"})
		req := withUser(httptest.NewRequest(http.MethodPost, "/lists/list-1/items", body), "stranger")
		req.Header.Set("Content-Type", "application/json")
		rr := httptest.NewRecorder()
		router.ServeHTTP(rr, req)
		if rr.Code != http.StatusNotFound {
			t.Fatalf("want 404, got %d", rr.Code)
		}
	})
}

func TestItemHandler_Delete(t *testing.T) {
	items := newFakeItemStore()
	lists := newFakeListStore()
	lists.addMember("list-1", "user-1", "editor")
	items.items["item-1"] = &models.Item{ItemID: "item-1", ListID: "list-1", Name: "Milk"}

	router := newItemRouter(&ItemHandler{items: items, lists: lists, logger: silentLogger()})

	t.Run("204 on success", func(t *testing.T) {
		req := withUser(httptest.NewRequest(http.MethodDelete, "/lists/list-1/items/item-1", nil), "user-1")
		rr := httptest.NewRecorder()
		router.ServeHTTP(rr, req)
		if rr.Code != http.StatusNoContent {
			t.Fatalf("want 204, got %d", rr.Code)
		}
	})

	t.Run("404 for non-member", func(t *testing.T) {
		req := withUser(httptest.NewRequest(http.MethodDelete, "/lists/list-1/items/item-1", nil), "stranger")
		rr := httptest.NewRecorder()
		router.ServeHTTP(rr, req)
		if rr.Code != http.StatusNotFound {
			t.Fatalf("want 404, got %d", rr.Code)
		}
	})
}

func TestItemHandler_Check(t *testing.T) {
	items := newFakeItemStore()
	lists := newFakeListStore()
	lists.addMember("list-1", "user-1", "editor")
	items.items["item-1"] = &models.Item{ItemID: "item-1", ListID: "list-1", Name: "Milk"}

	router := newItemRouter(&ItemHandler{items: items, lists: lists, logger: silentLogger()})

	t.Run("204 on success", func(t *testing.T) {
		req := withUser(httptest.NewRequest(http.MethodPost, "/lists/list-1/items/item-1/check", nil), "user-1")
		rr := httptest.NewRecorder()
		router.ServeHTTP(rr, req)
		if rr.Code != http.StatusNoContent {
			t.Fatalf("want 204, got %d", rr.Code)
		}
	})

	t.Run("404 for non-member", func(t *testing.T) {
		req := withUser(httptest.NewRequest(http.MethodPost, "/lists/list-1/items/item-1/check", nil), "stranger")
		rr := httptest.NewRecorder()
		router.ServeHTTP(rr, req)
		if rr.Code != http.StatusNotFound {
			t.Fatalf("want 404, got %d", rr.Code)
		}
	})

	t.Run("404 for unknown item", func(t *testing.T) {
		req := withUser(httptest.NewRequest(http.MethodPost, "/lists/list-1/items/no-such-item/check", nil), "user-1")
		rr := httptest.NewRecorder()
		router.ServeHTTP(rr, req)
		if rr.Code != http.StatusNotFound {
			t.Fatalf("want 404, got %d", rr.Code)
		}
	})
}

func TestItemHandler_Uncheck(t *testing.T) {
	items := newFakeItemStore()
	lists := newFakeListStore()
	lists.addMember("list-1", "user-1", "editor")
	items.items["item-1"] = &models.Item{ItemID: "item-1", ListID: "list-1", Name: "Milk"}

	router := newItemRouter(&ItemHandler{items: items, lists: lists, logger: silentLogger()})

	t.Run("200 returns updated item", func(t *testing.T) {
		req := withUser(httptest.NewRequest(http.MethodPost, "/lists/list-1/items/item-1/uncheck", nil), "user-1")
		rr := httptest.NewRecorder()
		router.ServeHTTP(rr, req)
		if rr.Code != http.StatusOK {
			t.Fatalf("want 200, got %d", rr.Code)
		}
		var got models.Item
		if err := json.NewDecoder(rr.Body).Decode(&got); err != nil {
			t.Fatal(err)
		}
		if got.ItemID != "item-1" {
			t.Fatalf("unexpected item: %+v", got)
		}
	})

	t.Run("404 for non-member", func(t *testing.T) {
		req := withUser(httptest.NewRequest(http.MethodPost, "/lists/list-1/items/item-1/uncheck", nil), "stranger")
		rr := httptest.NewRecorder()
		router.ServeHTTP(rr, req)
		if rr.Code != http.StatusNotFound {
			t.Fatalf("want 404, got %d", rr.Code)
		}
	})
}

func TestItemHandler_Move(t *testing.T) {
	items := newFakeItemStore()
	lists := newFakeListStore()
	lists.addMember("list-1", "user-1", "editor")
	lists.addMember("list-2", "user-1", "editor")
	items.items["item-1"] = &models.Item{ItemID: "item-1", ListID: "list-1", Name: "Milk"}

	router := newItemRouter(&ItemHandler{items: items, lists: lists, logger: silentLogger()})

	t.Run("200 on success", func(t *testing.T) {
		body := jsonBody(t, map[string]string{"target_list_id": "list-2"})
		req := withUser(httptest.NewRequest(http.MethodPost, "/lists/list-1/items/item-1/move", body), "user-1")
		req.Header.Set("Content-Type", "application/json")
		rr := httptest.NewRecorder()
		router.ServeHTTP(rr, req)
		if rr.Code != http.StatusOK {
			t.Fatalf("want 200, got %d: %s", rr.Code, rr.Body.String())
		}
	})

	t.Run("400 on missing target_list_id", func(t *testing.T) {
		body := jsonBody(t, map[string]string{})
		req := withUser(httptest.NewRequest(http.MethodPost, "/lists/list-1/items/item-1/move", body), "user-1")
		req.Header.Set("Content-Type", "application/json")
		rr := httptest.NewRecorder()
		router.ServeHTTP(rr, req)
		if rr.Code != http.StatusBadRequest {
			t.Fatalf("want 400, got %d", rr.Code)
		}
	})

	t.Run("404 when target list not accessible", func(t *testing.T) {
		body := jsonBody(t, map[string]string{"target_list_id": "list-99"})
		req := withUser(httptest.NewRequest(http.MethodPost, "/lists/list-1/items/item-1/move", body), "user-1")
		req.Header.Set("Content-Type", "application/json")
		rr := httptest.NewRecorder()
		router.ServeHTTP(rr, req)
		if rr.Code != http.StatusNotFound {
			t.Fatalf("want 404, got %d", rr.Code)
		}
	})
}

func TestItemHandler_Reorder(t *testing.T) {
	items := newFakeItemStore()
	lists := newFakeListStore()
	lists.addMember("list-1", "user-1", "editor")

	router := newItemRouter(&ItemHandler{items: items, lists: lists, logger: silentLogger()})

	t.Run("204 on success", func(t *testing.T) {
		body := jsonBody(t, map[string]any{
			"items": []map[string]any{{"item_id": "item-1", "sort_index": 0}},
		})
		req := withUser(httptest.NewRequest(http.MethodPatch, "/lists/list-1/items/reorder", body), "user-1")
		req.Header.Set("Content-Type", "application/json")
		rr := httptest.NewRecorder()
		router.ServeHTTP(rr, req)
		if rr.Code != http.StatusNoContent {
			t.Fatalf("want 204, got %d: %s", rr.Code, rr.Body.String())
		}
	})

	t.Run("400 on empty items list", func(t *testing.T) {
		body := jsonBody(t, map[string]any{"items": []any{}})
		req := withUser(httptest.NewRequest(http.MethodPatch, "/lists/list-1/items/reorder", body), "user-1")
		req.Header.Set("Content-Type", "application/json")
		rr := httptest.NewRecorder()
		router.ServeHTTP(rr, req)
		if rr.Code != http.StatusBadRequest {
			t.Fatalf("want 400, got %d", rr.Code)
		}
	})
}

func TestItemHandler_GetRecent(t *testing.T) {
	items := newFakeItemStore()
	lists := newFakeListStore()
	lists.addMember("list-1", "user-1", "editor")

	router := newItemRouter(&ItemHandler{items: items, lists: lists, logger: silentLogger()})

	t.Run("200 returns empty slice for member", func(t *testing.T) {
		req := withUser(httptest.NewRequest(http.MethodGet, "/lists/list-1/recent", nil), "user-1")
		rr := httptest.NewRecorder()
		router.ServeHTTP(rr, req)
		if rr.Code != http.StatusOK {
			t.Fatalf("want 200, got %d", rr.Code)
		}
		var got []models.PurchaseHistoryEntry
		if err := json.NewDecoder(rr.Body).Decode(&got); err != nil {
			t.Fatal(err)
		}
		if got == nil {
			t.Fatal("want non-nil slice")
		}
	})

	t.Run("404 for non-member", func(t *testing.T) {
		req := withUser(httptest.NewRequest(http.MethodGet, "/lists/list-1/recent", nil), "stranger")
		rr := httptest.NewRecorder()
		router.ServeHTTP(rr, req)
		if rr.Code != http.StatusNotFound {
			t.Fatalf("want 404, got %d", rr.Code)
		}
	})
}

// ── local helper ─────────────────────────────────────────────────────────────

func jsonBody(t *testing.T, v any) *bytes.Reader {
	t.Helper()
	b, err := json.Marshal(v)
	if err != nil {
		t.Fatal(err)
	}
	return bytes.NewReader(b)
}
