## Sprint 4 — Security audit

### [Auto Backup security] Отключить бэкап EncryptedSharedPreferences с токенами

`android:allowBackup=true` (дефолт) синхронизирует все SharedPreferences,
включая `kartoshka_tokens` с access/refresh токенами, в Google Drive.

Последствия:
- Утечка credentials через third-party облако для приложения с auth через Яндекс ID
- Возможный compliance issue с 152-ФЗ
- Объясняет, почему пользователь оставался залогинен после переустановки
  приложения (наблюдалось при тестировании в Sprint 2.4)

Решение:
1. В `AndroidManifest.xml` на тег `<application>` добавить:
   - `android:fullBackupContent="@xml/backup_rules"` (API 23+)
   - `android:dataExtractionRules="@xml/data_extraction_rules"` (API 31+)
2. Создать `res/xml/backup_rules.xml`:
   - Разрешить бэкап Room БД (`kartoshka.db`) — данные списков восстанавливаются
   - Запретить бэкап `kartoshka_tokens` — токены без Keystore-ключа всё равно
     бесполезны, но хранить их в облаке не следует
3. Создать `res/xml/data_extraction_rules.xml` (то же самое для API 31+)

---

## Sprint 5 — Backend

### Go-тесты
Сейчас 0 тестов, 0% покрытие. Написать юнит-тесты для auth (JWT, bcrypt),
handlers (через httptest), repository (интеграционные с YDB).

### CORS middleware
Нужен для веб-клиента и landing-страницы приглашений (`/invite/{token}`).
Добавить `github.com/go-chi/cors` или собственный middleware в middleware-цепочку.

### Rate Limiting middleware
- 60 req/min на `user_id` для аутентифицированных endpoint'ов
- 10 req/min на IP для публичных endpoint'ов (`/auth/*`, `/invite/*`)

### Yandex Cloud Functions деплой
Бэкенд сейчас работает только локально. Нужно:
- `serverless.yaml` с описанием функций
- Entrypoint-обёртка `func Handler(ctx, event)` поверх chi-роутера
- `deploy.sh` / `deploy.bat` скрипт через `yc` CLI

---

## Sprint 5 — Backend validation bugs

### 1. POST /lists/{id}/invite не валидирует invitee_email

Принимает любой email и возвращает 200 OK, в том числе:
- email несуществующего в системе пользователя
- email текущего пользователя (самоприглашение)
- email уже-участника списка

Должен возвращать:
- 404 если пользователь с таким email не зарегистрирован
- 409 с serverMessage="self_invite" если приглашает сам себя
- 409 с serverMessage="already_member" если пользователь уже участник
- 409 с serverMessage="already_invited" если уже есть pending invite

Android-сторона уже умеет показывать корректные сообщения для всех
четырёх случаев (см. ShareScreen.kt sendInvite + NetworkError.Conflict).

### 2. POST /lists/{id}/items возвращает 500 для ЛЮБОГО нового товара

Ранее наблюдалось только на "Багет"/"Пицца" — это была ложная гипотеза.
На самом деле 500 воспроизводится для каждого нового товара в UUID-списке.

Корневая причина: UPSERT в `ItemRepo.Create` не включает `photo_url`
в список колонок → в YDB это поле остаётся NULL. Затем `Create` вызывает
`GetByID(ctx, itemID)` который запускает `scanItem()`. В строке
`backend/internal/repository/items.go:353`:

    named.Optional("photo_url", &i.PhotoURL)

SDK пытается scan-нуть `NULL (Optional<Utf8>)` в `string` (не `*string`)
→ ошибка типа в YDB Go SDK v3.73.1. UPSERT успешен (~335ms), падает
именно `GetByID` (~340ms), итого ~675ms латентности и 500.

Сервер не логирует ошибку: `h.logger.Error` не вызывается в
`ItemHandler.Create` перед `apierror.Write`.

Фикс (на выбор):
- Вариант А: изменить `models.Item` — nullable поля как указатели:
  `PhotoURL *string`, `Tags *string`, `Note *string`,
  `CategoryID *string`, `SortIndex *int32`.
- Вариант Б: добавить `photo_url` в UPSERT с `types.NullValue(...)`,
  чтобы YDB получал явный `Optional<Utf8>`, а не имплицитный NULL.
- Плюс в обоих случаях: добавить `h.logger.Error("item create failed",
  "err", err, "list_id", listID)` в `ItemHandler.Create`.

### 3. Seed-списки вызывают бесконечный retry в WorkManager после логина

Seed-списки (id "1"–"5") создаются локально в Room когда пользователь
не залогинен. После логина они остаются в Room, и при попытке добавить
в них товар бэкенд возвращает 500 — списка нет в YDB.

С Sprint 3.4 (pending_ops + WorkManager) это вызовет бесконечный retry:
500 классифицируется как ServerError → isRetryable() = true → Worker
добавляет retry, retry_count растёт до 10, только тогда операция
удаляется. Всё это время WorkManager будет периодически будить приложение.

Фикс (на выбор):
- При логине удалить seed-списки из Room и заменить реальными данными
  с сервера (syncLists() уже это частично делает, но seed-данные могут
  остаться если у пользователя нет своих списков на сервере).
- Либо пометить seed-списки флагом `is_local_only` в ShoppingListEntity
  и не вызывать API для операций с ними.

---

## Future features (post-MVP)

### List push messages (Sprint 5+)

Allow users to send a short push notification to all members of a shared list.

UI: bottom sheet with 3 preset messages + 1 custom short message field.

Preset examples:
- "Я обновил список"
- "Список пуст, я всё купил"
- "Не забудь зайти в магазин"

Backend: POST /lists/{id}/notify with body { type: "preset"|"custom", text: string }
→ fanout to all list_members via FCM/RuStore.
