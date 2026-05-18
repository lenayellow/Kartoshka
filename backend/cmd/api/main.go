package main

import (
	"context"
	"log/slog"
	"net/http"
	"os"
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

func main() {
	_ = godotenv.Load()
	logger := logging.NewLogger()
	slog.SetDefault(logger)

	ctx, cancel := context.WithTimeout(context.Background(), 10*time.Second)
	defer cancel()

	db, err := repository.Connect(ctx)
	if err != nil {
		logger.Error("ydb open failed", "err", err)
		os.Exit(1)
	}
	defer db.Close(context.Background())

	if err := db.Ping(ctx); err != nil {
		logger.Error("ydb ping failed", "err", err)
		os.Exit(1)
	}
	logger.Info("ydb connected")

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

	// Auth
	r.With(authRateLimit).Post("/auth/yandex", authHandler.YandexLogin)
	r.With(authRateLimit).Post("/auth/refresh", authHandler.Refresh)
	r.Post("/auth/logout", authHandler.Logout)
	r.With(authRateLimit).Post("/auth/email/register", authHandler.EmailRegister)
	r.With(authRateLimit).Post("/auth/email/login", authHandler.EmailLogin)
	r.Get("/auth/email/confirm", authHandler.ConfirmEmail)
	r.With(authRateLimit).Post("/auth/email/forgot", authHandler.ForgotPassword)
	r.Get("/auth/email/reset", authHandler.ResetPasswordForm)
	r.Post("/auth/email/reset", authHandler.ResetPassword)

	// Публичный маршрут инвайтов (без авторизации)
	r.Get("/invite/{invite_token}", inviteHandler.GetInfo)

	// Защищённые маршруты
	r.Group(func(r chi.Router) {
		r.Use(middleware.Bearer)

		// Users
		r.Get("/users/me", userHandler.GetMe)
		r.Put("/users/me", userHandler.UpdateMe)
		r.Post("/users/me/avatar", userHandler.UploadAvatar)
		r.Post("/users/me/push-token", userHandler.SavePushToken)

		// Lists
		r.Get("/lists", listHandler.GetAll)
		r.Post("/lists", listHandler.Create)
		r.Get("/lists/{list_id}", listHandler.GetOne)
		r.Put("/lists/{list_id}", listHandler.Update)
		r.Delete("/lists/{list_id}", listHandler.Delete)
		r.Get("/lists/{list_id}/members", listHandler.GetMembers)
		r.Delete("/lists/{list_id}/members/{user_id}", listHandler.RemoveMember)

		// Items
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

		// Sync
		r.Get("/lists/{list_id}/events", syncHandler.GetEvents)

		// Invitations
		r.With(inviteRateLimit).Post("/lists/{list_id}/invite", inviteHandler.Create)
		r.Post("/invite/{invite_token}/accept", inviteHandler.Accept)

		// Loyalty Cards
		r.Get("/cards", cardHandler.GetAll)
		r.Post("/cards", cardHandler.Create)
		r.Delete("/cards/{card_id}", cardHandler.Delete)
	})

	port := os.Getenv("PORT")
	if port == "" {
		port = "8080"
	}
	logger.Info("server started", "port", port)
	if err := http.ListenAndServe(":"+port, r); err != nil {
		logger.Error("server failed", "err", err)
		os.Exit(1)
	}
}
