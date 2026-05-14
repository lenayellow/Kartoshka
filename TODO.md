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

### 2. POST /lists/{id}/items возвращает 500 для некоторых товаров

Наблюдалось при добавлении товаров "Багет", "Пицца". Тело ответа:
"ошибка создания товара". Логов сервера нет, нужны structured logs
(slog в JSON) со стороны Go-функции, чтобы понять причину.

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
