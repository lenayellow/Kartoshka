# Sprint 5.5 — Serverless Containers deploy
# Builder: golang:1.23-alpine (соответствует go.mod, статический бинарник без glibc)
# Runtime: distroless/static-debian12:nonroot
#   - ~2 MB, без shell, без пакетного менеджера
#   - UID 65532 (nonroot), снижает attack surface
#   - Fallback при недоступности gcr.io из РФ: alpine:3.20 + явный USER appuser
# Контекст РФ: cr.yandex/mirror/ не существует на 2026-05-16; Docker Hub и gcr.io
# работают напрямую. Если ситуация изменится — пересобрать с alpine fallback.

FROM golang:1.23-alpine AS builder
WORKDIR /build
COPY go.mod go.sum ./
RUN go mod download
COPY . .
RUN CGO_ENABLED=0 GOOS=linux GOARCH=amd64 \
    go build -trimpath -ldflags="-s -w" -o api ./cmd/api

FROM gcr.io/distroless/static-debian12:nonroot
WORKDIR /app
COPY --from=builder /build/api .
EXPOSE 8080
ENTRYPOINT ["/app/api"]
