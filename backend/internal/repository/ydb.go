package repository

import (
	"context"
	"fmt"
	"os"

	"github.com/ydb-platform/ydb-go-sdk/v3"
	yc "github.com/ydb-platform/ydb-go-yc"
)

type DB struct {
	Driver *ydb.Driver
}

// Connect открывает соединение с YDB Serverless.
// Читает YDB_ENDPOINT, YDB_DATABASE, YDB_SA_KEY_FILE из переменных окружения.
func Connect(ctx context.Context) (*DB, error) {
	endpoint := os.Getenv("YDB_ENDPOINT")
	database := os.Getenv("YDB_DATABASE")
	saKeyFile := os.Getenv("YDB_SA_KEY_FILE")

	if endpoint == "" || database == "" || saKeyFile == "" {
		return nil, fmt.Errorf("требуются переменные окружения: YDB_ENDPOINT, YDB_DATABASE, YDB_SA_KEY_FILE")
	}

	dsn := endpoint + database

	driver, err := ydb.Open(ctx, dsn,
		yc.WithServiceAccountKeyFileCredentials(saKeyFile),
	)
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
