package main

import (
	"bytes"
	"context"
	"encoding/base64"
	"io"
	"net/http"
	"net/http/httptest"
	"net/url"
	"os"
	"strings"
	"sync"
	"time"

	"github.com/go-chi/chi/v5"
	"github.com/joho/godotenv"

	"github.com/lena/kartoshka-backend/internal/handlers"
	"github.com/lena/kartoshka-backend/internal/logging"
	"github.com/lena/kartoshka-backend/internal/middleware"
	"github.com/lena/kartoshka-backend/internal/notifications"
	"github.com/lena/kartoshka-backend/internal/repository"
	"github.com/lena/kartoshka-backend/internal/storage"
)

type FunctionRequest struct {
	HttpMethod      string            `json:"httpMethod"`
	Path            string            `json:"path"`
	QueryParameters map[string]string `json:"queryStringParameters"`
	Headers         map[string]string `json:"headers"`
	Body            string            `json:"body"`
	IsBase64Encoded bool              `json:"isBase64Encoded"`
}

type FunctionResponse struct {
	StatusCode int               `json:"statusCode"`
	Headers    map[string]string `json:"headers"`
	Body       string            `json:"body"`
}

var (
	once          sync.Once
	cachedHandler http.Handler
)

func Handler(ctx context.Context, req FunctionRequest) (*FunctionResponse, error) {
	once.Do(func() {
		cachedHandler = BuildRouter()
	})

	rawURL := "http://localhost" + req.Path
	if len(req.QueryParameters) > 0 {
		q := url.Values{}
		for k, v := range req.QueryParameters {
			q.Set(k, v)
		}
		rawURL += "?" + q.Encode()
	}

	var bodyReader io.Reader
	if req.Body != "" {
		if req.IsBase64Encoded {
			decoded, err := base64.StdEncoding.DecodeString(req.Body)
			if err != nil {
				return nil, err
			}
			bodyReader = bytes.NewReader(decoded)
		} else {
			bodyReader = strings.NewReader(req.Body)
		}
	}

	httpReq, err := http.NewRequestWithContext(ctx, req.HttpMethod, rawURL, bodyReader)
	if err != nil {
		return nil, err
	}
	for k, v := range req.Headers {
		httpReq.Header.Set(k, v)
	}

	w := httptest.NewRecorder()
	cachedHandler.ServeHTTP(w, httpReq)

	result := w.Result()
	defer result.Body.Close()
	body, _ := io.ReadAll(result.Body)

	headers := make(map[string]string, len(result.Header))
	for k, vals := range result.Header {
		if len(vals) > 0 {
			headers[k] = vals[0]
		}
	}

	return &FunctionResponse{
		StatusCode: result.StatusCode,
		Headers:    headers,
		Body:       string(body),
	}, nil
}

func BuildRouter() http.Handler {
	_ = godotenv.Load()
	logger := logging.NewLogger()

	ctx, cancel := context.WithTimeout(context.Background(), 10*time.Second)
	defer cancel()

	db, err := repository.Connect(ctx)
	if err != nil {
		logger.Error("ydb open failed", "err", err)
		os.Exit(1)
	}

	if err := db.Ping(ctx); err != nil {
		logger.Error("ydb ping failed", "err", err)
		os.Exit(1)
	}

	users := repository.NewUserRepo(db)
	tokens := repository.NewTokenRepo(db)
	lists := repository.NewListRepo(db)
	items := repository.NewItemRepo(db)
	cards := repository.NewCardRepo(db)
	pushTokens := repository.NewPushTokenRepo(db)

	store, err := storage.NewS3Client()
	if err != nil {
		logger.Error("s3 init failed", "err", err)
		os.Exit(1)
	}
	if store == nil {
		logger.Warn("s3 not configured, media unavailable")
	}

	invitations := repository.NewInvitationRepo(db)
	events := repository.NewEventsRepo(db)

	notifier := notifications.NewNotifier(pushTokens, logger)

	authHandler := handlers.NewAuthHandler(users, tokens, logger)
	inviteHandler := handlers.NewInvitationHandler(invitations, lists, users, logger)
	syncHandler := handlers.NewSyncHandler(events, lists, logger)
	userHandler := handlers.NewUserHandler(users, pushTokens, store, logger)
	listHandler := handlers.NewListHandler(lists, logger)
	itemHandler := handlers.NewItemHandler(items, lists, store, notifier, logger)
	cardHandler := handlers.NewCardHandler(cards, logger)

	authRateLimit := middleware.RateLimitByIP(10, time.Minute)
	inviteRateLimit := middleware.RateLimitByUserID(30, time.Hour)

	r := chi.NewRouter()
	r.Use(middleware.RequestID)
	r.Use(middleware.Logging(logger))
	r.Use(middleware.Recovery(logger))
	r.Use(middleware.RequestTimeout(25 * time.Second))

	r.With(authRateLimit).Post("/auth/yandex", authHandler.YandexLogin)
	r.With(authRateLimit).Post("/auth/refresh", authHandler.Refresh)
	r.Post("/auth/logout", authHandler.Logout)
	r.With(authRateLimit).Post("/auth/email/register", authHandler.EmailRegister)
	r.With(authRateLimit).Post("/auth/email/login", authHandler.EmailLogin)
	r.Get("/auth/email/confirm", authHandler.ConfirmEmail)
	r.With(authRateLimit).Post("/auth/email/forgot", authHandler.ForgotPassword)
	r.Get("/auth/email/reset", authHandler.ResetPasswordForm)
	r.Post("/auth/email/reset", authHandler.ResetPassword)

	r.Get("/invite/{invite_token}", inviteHandler.GetInfo)

	r.Group(func(r chi.Router) {
		r.Use(middleware.Bearer)

		r.Get("/users/me", userHandler.GetMe)
		r.Put("/users/me", userHandler.UpdateMe)
		r.Post("/users/me/avatar", userHandler.UploadAvatar)
		r.Post("/users/me/push-token", userHandler.SavePushToken)

		r.Get("/lists", listHandler.GetAll)
		r.Post("/lists", listHandler.Create)
		r.Get("/lists/{list_id}", listHandler.GetOne)
		r.Put("/lists/{list_id}", listHandler.Update)
		r.Delete("/lists/{list_id}", listHandler.Delete)
		r.Get("/lists/{list_id}/members", listHandler.GetMembers)
		r.Delete("/lists/{list_id}/members/{user_id}", listHandler.RemoveMember)

		r.Get("/lists/{list_id}/items", itemHandler.GetAll)
		r.Post("/lists/{list_id}/items", itemHandler.Create)
		r.Patch("/lists/{list_id}/items/reorder", itemHandler.Reorder)
		r.Put("/lists/{list_id}/items/{item_id}", itemHandler.Update)
		r.Delete("/lists/{list_id}/items/{item_id}", itemHandler.Delete)
		r.Post("/lists/{list_id}/items/{item_id}/photo", itemHandler.UploadPhoto)
		r.Post("/lists/{list_id}/items/{item_id}/check", itemHandler.Check)
		r.Post("/lists/{list_id}/items/{item_id}/uncheck", itemHandler.Uncheck)
		r.Post("/lists/{list_id}/items/{item_id}/move", itemHandler.Move)
		r.Get("/lists/{list_id}/recent", itemHandler.GetRecent)

		r.Get("/lists/{list_id}/events", syncHandler.GetEvents)

		r.With(inviteRateLimit).Post("/lists/{list_id}/invite", inviteHandler.Create)
		r.Post("/invite/{invite_token}/accept", inviteHandler.Accept)

		r.Get("/cards", cardHandler.GetAll)
		r.Post("/cards", cardHandler.Create)
		r.Delete("/cards/{card_id}", cardHandler.Delete)
	})

	return r
}

func main() {}
