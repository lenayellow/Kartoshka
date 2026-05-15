package handlers

import (
	"encoding/json"
	"log/slog"
	"net/http"

	"github.com/go-chi/chi/v5"

	"github.com/lena/kartoshka-backend/internal/apierror"
	"github.com/lena/kartoshka-backend/internal/middleware"
	"github.com/lena/kartoshka-backend/internal/models"
	"github.com/lena/kartoshka-backend/internal/repository"
)

type CardHandler struct {
	cards  cardStore
	logger *slog.Logger
}

func NewCardHandler(cards *repository.CardRepo, logger *slog.Logger) *CardHandler {
	return &CardHandler{cards: cards, logger: logger}
}

// GET /cards
func (h *CardHandler) GetAll(w http.ResponseWriter, r *http.Request) {
	userID := middleware.GetUserID(r)

	cards, err := h.cards.GetAllByUser(r.Context(), userID)
	if err != nil {
		apierror.Write(w, r, http.StatusInternalServerError, apierror.CodeInternal, "Internal error", err.Error())
		return
	}
	if cards == nil {
		cards = []models.LoyaltyCard{}
	}
	writeJSON(w, http.StatusOK, cards)
}

// POST /cards
func (h *CardHandler) Create(w http.ResponseWriter, r *http.Request) {
	userID := middleware.GetUserID(r)

	var req struct {
		Name          string `json:"name"`
		BarcodeValue  string `json:"barcode_value"`
		BarcodeFormat int32  `json:"barcode_format"`
		Color         int64  `json:"color"`
	}
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		apierror.Write(w, r, http.StatusBadRequest, apierror.CodeBadRequest, "Invalid request format", err.Error())
		return
	}
	if req.Name == "" || req.BarcodeValue == "" {
		apierror.Write(w, r, http.StatusBadRequest, apierror.CodeBadRequest, "name and barcode_value are required", "")
		return
	}

	card, err := h.cards.Create(r.Context(), userID,
		req.Name, req.BarcodeValue, req.BarcodeFormat, req.Color)
	if err != nil {
		apierror.Write(w, r, http.StatusInternalServerError, apierror.CodeInternal, "Failed to create card", err.Error())
		return
	}
	writeJSON(w, http.StatusCreated, card)
}

// DELETE /cards/{card_id}
func (h *CardHandler) Delete(w http.ResponseWriter, r *http.Request) {
	userID := middleware.GetUserID(r)
	cardID := chi.URLParam(r, "card_id")

	if err := h.cards.Delete(r.Context(), cardID, userID); err != nil {
		apierror.Write(w, r, http.StatusInternalServerError, apierror.CodeInternal, "Failed to delete card", err.Error())
		return
	}
	w.WriteHeader(http.StatusNoContent)
}
