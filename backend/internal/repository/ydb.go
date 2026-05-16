package repository

import (
	"context"
	"fmt"
	"log/slog"
	"os"

	"github.com/ydb-platform/ydb-go-sdk/v3"
	yc "github.com/ydb-platform/ydb-go-yc"
)

type DB struct {
	Driver *ydb.Driver
}

// Connect открывает соединение с YDB Serverless.
// Читает YDB_ENDPOINT, YDB_DATABASE (обязательные).
// Auth: YDB_SA_KEY_FILE задан → ключ SA (локальная разработка);
// пусто → metadata-сервис Yandex Cloud (Serverless Containers/VM с SA).
func Connect(ctx context.Context) (*DB, error) {
	endpoint := os.Getenv("YDB_ENDPOINT")
	database := os.Getenv("YDB_DATABASE")

	if endpoint == "" || database == "" {
		return nil, fmt.Errorf("требуются переменные окружения: YDB_ENDPOINT, YDB_DATABASE")
	}

	dsn := endpoint + database

	var creds ydb.Option
	if saKeyFile := os.Getenv("YDB_SA_KEY_FILE"); saKeyFile != "" {
		if _, err := os.Stat(saKeyFile); err != nil {
			return nil, fmt.Errorf("YDB_SA_KEY_FILE=%s, но файл не найден: %w", saKeyFile, err)
		}
		slog.Info("ydb auth mode", "mode", "service_account_key_file", "path", saKeyFile)
		creds = yc.WithServiceAccountKeyFileCredentials(saKeyFile)
	} else {
		slog.Info("ydb auth mode", "mode", "metadata")
		creds = yc.WithMetadataCredentials()
	}

	driver, err := ydb.Open(ctx, dsn, creds)
	if err != nil {
		return nil, fmt.Errorf("ydb.Open: %w", err)
	}

	return &DB{Driver: driver}, nil
}

// Ping проверяет живость соединения.
func (db *DB) Ping(ctx context.Context) error {
	_, err := db.Driver.Discovery().WhoAmI(ctx)
	return err
}

// Close закрывает соединение с базой.
func (db *DB) Close(ctx context.Context) error {
	return db.Driver.Close(ctx)
}
