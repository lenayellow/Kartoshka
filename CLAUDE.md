# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

Kartoshka ("Super Списки") is a local-first collaborative shopping list app for Android. Users create lists, add items as cards, and tap a card to mark it done. Lists can be shared with other users. Loyalty cards (with barcodes) are also supported.

## Tech Stack

### Android (Kotlin)
- **UI:** Jetpack Compose + Material 3
- **Local DB:** Room (SQLite)
- **Prefs/tokens:** DataStore + AndroidX Security Crypto (encrypted)
- **Networking:** Retrofit 2 + OkHttp + Gson
- **Image loading:** Coil
- **Barcode scanning:** CameraX + ML Kit
- **Barcode rendering:** ZXing
- **Drag-and-drop:** `sh.calvin.reorderable`
- **Min SDK:** 26 (Android 8.0), Target SDK: 34, JVM: 17

### Backend (Go)
- **Language:** Go 1.22, module `github.com/lena/kartoshka-backend`
- **Router:** Chi v5
- **Database:** Yandex YDB (serverless, gRPC)
- **Auth:** JWT (access + refresh), Yandex OAuth, email/password
- **Storage:** Yandex Object Storage (S3-compatible, via AWS SDK v2)
- **Push:** Firebase Cloud Messaging + RuStore
- **Email:** Yandex SMTP
- **Config:** `backend/.env`

## Build & Run

### Android
```bash
./gradlew assembleDebug          # build debug APK
./gradlew test                   # unit tests
./gradlew test --tests "com.lena.kartoshka.SomeTest"  # single test class
./gradlew connectedAndroidTest   # instrumented tests on emulator
./gradlew lint
```

### Backend (from repo root on Windows)
```bash
start-dev.bat   # kills port 8080, builds Go binary, sets up adb reverse tunnel, starts server
```
Or manually:
```bash
cd backend
go build -o api.exe ./cmd/api
./api.exe
```
Backend runs on port 8080. The adb reverse tunnel makes `localhost:8080` reachable from the emulator.

### YDB migrations
```bash
cd backend
python run_migration.py   # runs YQL migrations in migrations/
```

Always verify Android changes on the emulator before marking a task done.

## Architecture

### Android layers
```
ui/screens/        Compose screens (one file per screen)
ui/theme/          Material 3 color/typography tokens (dark primary)
data/
  AppRepository    Core business logic — reads/writes Room, calls SyncRepository
  SyncRepository   Talks to the Go backend; called async from AppRepository
  KartoshkaDatabase  Room database
  TokenStore       Encrypted JWT storage (DataStore + Security Crypto)
  UserPrefsRepository  Theme, avatar, last-used list
network/
  ApiService       Retrofit interface (all endpoints)
  ApiClient        OkHttp setup, Bearer token interceptor
  ApiModels        Request/response DTOs
```

**Data flow:** Local Room DB is the source of truth. `AppRepository` writes to Room immediately, then calls `SyncRepository` in a background coroutine to propagate to the server. On launch (when logged in), the app syncs from the server via `/lists/{id}/events`.

**Offline / logged-out mode:** When no user is logged in, the app seeds Room with 5 sample lists (Russian names). These are replaced by real server data after login.

### Backend layers
```
cmd/api/main.go      Entry point, route registration
internal/
  handlers/          HTTP handlers (auth, user, list, item, card, invitation, sync)
  repository/        YDB data access (users, tokens, lists, items, cards, invitations, events, push_tokens)
  models/            Shared data structs
  auth/              JWT generation/validation, Yandex OAuth, email/password hashing
  storage/           S3 client (avatars, item photos)
  notifications/     FCM + RuStore push, Yandex SMTP email
migrations/          YQL schema files (run via run_migration.py)
```

### Screens
1. **AuthScreen** — Yandex OAuth + email login/register
2. **MyListsScreen** — all lists; drag to reorder
3. **ListDetailScreen** — items in a list; tap to remove
4. **NewListScreen** — create list with name and color
5. **ListSettingsScreen** — rename, recolor, delete
6. **ListMembersScreen** — invite and manage members
7. **ShareScreen** — share after creating a list
8. **IdeasScreen** — purchase history suggestions
9. **ProfileScreen** — avatar, name, logout
10. **CardDisplaySheet / CardScannerScreen** — loyalty cards with barcodes

## Design Rules

- Dark theme is primary (Material 3, no light theme)
- No checkboxes — interactions are card-based (tap card to complete/remove)
- Empty states use illustrations
- All user-visible strings in `strings.xml` (English + Russian) — never hardcode

## Working With the Human

- The user is a **product designer with no coding background**. Use plain English; define jargon briefly if unavoidable.
- Make **small, reviewable increments** — one change at a time, no bundling.
- **Do not add features that were not asked for.** If an improvement seems useful, ask first.
- **When in doubt, ask** — do not assume.
- Prefer **simple over clever**.

## Git Workflow

Main branch: `main`. Two remotes:
- `origin` → Sourcecraft (primary)
- `github` → GitHub

**Push order:** always push to `origin` first, then `github`.

Rules:
1. **Never `git commit`** unless explicitly told "commit it" or "commit this increment."
2. **Never `git push`** unless explicitly asked to push.
3. **Never `git reset --hard` or `git restore`** without explicit approval.
4. At the start of each increment, propose a plan first. Code only after approval.
5. After finishing an increment, report: files changed (`git status`), what to test on the emulator, and a suggested commit message. Then stop and wait.
6. On "looks good, commit it": run `git add .` and `git commit -m "..."` and show output.
7. On "broken": do NOT commit. Diagnose, propose a fix, wait for approval.
