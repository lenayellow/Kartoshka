# Claude Code Prompt — Бэкенд для «Картошки» (миграция local-first → Yandex Cloud)

## Контекст

Приложение называется **Картошка** (`com.lena.kartoshka`) — Android-приложение для совместных списков покупок.
Язык: Kotlin, UI: Jetpack Compose + Material3, локальная БД: Room.
Приложение работает **только в России**, целевая аудитория — российские пользователи.

Сейчас приложение работает **local-first**: всё хранится в Room на устройстве, синхронизации нет.
**Задача**: построить бэкенд на Yandex Cloud и подключить к нему приложение,
не ломая существующую архитектуру.

---

## Реальная структура Android-проекта (то, что уже написано)

### Модели данных

```kotlin
// ShoppingList.kt
data class ShoppingList(val id: String, val name: String, val itemCount: Int, val color: Color)

// Item.kt
data class Item(
    val id: String,
    val name: String,
    val tags: Set<ItemTag> = emptySet(),  // URGENT, ON_SALE, IF_CONVENIENT
    val note: String = "",
    val categoryId: String? = null,
    val imagePath: String? = null         // локальный путь к файлу
)

// LoyaltyCard.kt — Room Entity
@Entity(tableName = "loyalty_cards")
data class LoyaltyCard(
    @PrimaryKey val id: String,
    val name: String,
    @ColumnInfo(name = "barcode_value") val barcodeValue: String,
    @ColumnInfo(name = "barcode_format") val barcodeFormat: Int,  // ZXing BarcodeFormat ordinal
    val color: Long
)
```

### Room-таблицы (существующие)
- `shopping_lists` — id, name, color_value, position
- `items` — id, list_id (FK), name, tags (CSV строка), note, category_id, image_path
- `purchase_history` — id, item_name, list_id, purchased_at (timestamp)
- `loyalty_cards` — id, name, barcode_value, barcode_format, color

### AppRepository (интерфейс, с которым работает UI)

```kotlin
class AppRepository(private val db: KartoshkaDatabase) {
    fun observeLists(): Flow<List<ShoppingList>>
    suspend fun insertList(list: ShoppingList)
    suspend fun updateList(list: ShoppingList)
    suspend fun deleteList(id: String)

    fun observeItems(listId: String): Flow<List<Item>>
    suspend fun insertItem(item: Item, listId: String)
    suspend fun insertItems(listId: String, items: List<Item>)
    suspend fun updateItem(item: Item, listId: String)
    suspend fun deleteItem(itemId: String)
    suspend fun moveItem(item: Item, toListId: String)
    suspend fun recordPurchase(item: Item, listId: String)

    fun observeLoyaltyCards(): Flow<List<LoyaltyCard>>
    suspend fun insertLoyaltyCard(card: LoyaltyCard)
    suspend fun deleteLoyaltyCard(id: String)
}
```

### SortRepository (интерфейс с заменяемой реализацией)

```kotlin
interface SortRepository {
    fun observeCategoryOrder(): Flow<List<String>>
    fun observeHiddenCategories(): Flow<Set<String>>
    suspend fun saveCategoryOrder(ids: List<String>)
    suspend fun saveHiddenCategories(ids: Set<String>)
}
// Сейчас: LocalSortRepository (DataStore)
// После миграции: CloudSortRepository (API) — UI не меняется
```

### Экраны и их зависимости
- `MyListsScreen` — читает `AppRepository.observeLists()`
- `ListDetailScreen` — читает `observeItems()`, `observeLoyaltyCards()`, пишет через insert/update/delete
- `NewListScreen` — цвет обложки выбирается из `coverColors` (10 цветов из Color.kt), НЕТ загрузки изображений
- `ListSettingsScreen` — удаление списка, открывает NewListScreen для редактирования названия/цвета
- `ShareScreen` — UI есть, логика не реализована (TODO: send invite)
- `ProfileScreen` — смена аватара (кроп + сохранение в filesDir), тема, выход — всё локально
- `CardScannerFlow` / `CardDisplaySheet` — полностью готовы, работают локально
- `IdeasScreen` — рецепты, полностью локальные (sampleRecipes in-memory), в бэкенде не нужны на MVP

### Что хранится в filesDir локально
- `avatar.jpg` — аватар пользователя
- `item_{id}.jpg` — фото товара (путь хранится в `Item.imagePath`)

---

## Что нужно построить: Бэкенд на Yandex Cloud

### Инфраструктура

| Сервис | Назначение |
|--------|-----------|
| **YDB Serverless** | Основная БД (списки, товары, пользователи, карты лояльности) |
| **Yandex Object Storage** | Аватарки, фото товаров (не обложки — они цветовые, не файлы) |
| **Serverless Containers** | HTTP API (Go) — не Functions, нужен долгий коннект для polling |
| **Yandex API Gateway** | Единая точка входа, авторизация по JWT |
| **SourceCraft** | Git-репозиторий бэкенда |

### Аутентификация

**Основной метод: Yandex ID (OAuth 2.0)**
- Флоу: Custom Chrome Tab → Yandex OAuth → код → бэкенд меняет код на токены Яндекса → выдаёт свой JWT
- Яндекс возвращает: `uid`, `login`, `default_email`, `real_name`, `default_avatar_id`
- Документация: https://yandex.ru/dev/id/doc/dg/oauth/concepts/about.html

**Запасной метод: Email + пароль**
- bcrypt для хранения паролей
- Подтверждение email через SMTP (smtp.yandex.ru)

**НЕ нужно**: Apple Sign In, Google Sign In (нестабилен в РФ)

**Сессии**: JWT access token (15 мин) + refresh token (30 дней)
- На Android: refresh хранить в `EncryptedSharedPreferences` или Android Keystore

### Push-уведомления

- **FCM** (Firebase Cloud Messaging) — основной канал, HTTP v1 API
- **RuStore Push** — обязателен для публикации в RuStore
- Архитектура: при изменении товара → Cloud Function пробует FCM → при неудаче RuStore Push

---

## Схема БД (YDB, YQL)

### `users`
```sql
CREATE TABLE users (
    user_id    UTF8     NOT NULL,  -- UUID
    yandex_uid UTF8,               -- nullable для email-юзеров
    email      UTF8     NOT NULL,
    name       UTF8     NOT NULL,
    avatar_url UTF8,               -- Object Storage URL
    created_at Datetime NOT NULL,
    updated_at Datetime NOT NULL,
    PRIMARY KEY (user_id)
);
```

### `lists`
```sql
CREATE TABLE lists (
    list_id      UTF8     NOT NULL,
    owner_id     UTF8     NOT NULL,  -- FK → users.user_id
    title        UTF8     NOT NULL,
    color_value  Int64    NOT NULL,  -- цвет в формате ARGB Long (как в Android Color)
    position     Int32    NOT NULL DEFAULT 0,
    country_code UTF8,
    category_order UTF8,            -- JSON: ["cat1","cat3","cat2",...]
    hidden_categories UTF8,         -- JSON: ["cat4","cat7"]
    created_at   Datetime NOT NULL,
    updated_at   Datetime NOT NULL,
    PRIMARY KEY (list_id)
);
-- Примечание: обложки — только цвет (Long), не файлы. NewListScreen выбирает из 10 фиксированных цветов.
```

### `list_members`
```sql
CREATE TABLE list_members (
    list_id   UTF8     NOT NULL,
    user_id   UTF8     NOT NULL,
    role      UTF8     NOT NULL,  -- 'owner' | 'editor'
    joined_at Datetime NOT NULL,
    PRIMARY KEY (list_id, user_id)
);
```

### `items`
```sql
CREATE TABLE items (
    item_id     UTF8     NOT NULL,
    list_id     UTF8     NOT NULL,
    name        UTF8     NOT NULL,
    tags        UTF8,              -- CSV: "URGENT,ON_SALE" — точно как в ItemEntity.tags
    note        UTF8,              -- поле "Количество, описание"
    category_id UTF8,              -- "cat1".."cat12" или null
    photo_url   UTF8,              -- Object Storage URL (заменяет imagePath)
    is_deleted  Bool     NOT NULL DEFAULT false,  -- soft delete
    added_by    UTF8     NOT NULL,
    updated_by  UTF8     NOT NULL,
    updated_at  Datetime NOT NULL,
    sort_index  Int32,
    PRIMARY KEY (item_id)
);
CREATE INDEX idx_items_list ON items (list_id, is_deleted, category_id);
```

### `purchase_history`
```sql
CREATE TABLE purchase_history (
    id           UTF8     NOT NULL,
    item_name    UTF8     NOT NULL,
    list_id      UTF8     NOT NULL,
    purchased_at Datetime NOT NULL,
    user_id      UTF8     NOT NULL,
    PRIMARY KEY (id)
);
CREATE INDEX idx_history_list ON purchase_history (list_id, purchased_at);
```

### `loyalty_cards`
```sql
CREATE TABLE loyalty_cards (
    card_id        UTF8 NOT NULL,
    user_id        UTF8 NOT NULL,  -- карты привязаны к пользователю, не к списку
    name           UTF8 NOT NULL,
    barcode_value  UTF8 NOT NULL,
    barcode_format Int32 NOT NULL, -- ZXing BarcodeFormat ordinal (как в LoyaltyCard.kt)
    color          Int64 NOT NULL, -- ARGB Long (как в LoyaltyCard.kt)
    PRIMARY KEY (card_id)
);
```

### `invitations`
```sql
CREATE TABLE invitations (
    invite_token  UTF8     NOT NULL,
    list_id       UTF8     NOT NULL,
    inviter_id    UTF8     NOT NULL,
    invitee_email UTF8,
    status        UTF8     NOT NULL,  -- 'pending' | 'accepted' | 'expired'
    created_at    Datetime NOT NULL,
    expires_at    Datetime NOT NULL,
    PRIMARY KEY (invite_token)
);
```

### `push_tokens`
```sql
CREATE TABLE push_tokens (
    user_id       UTF8     NOT NULL,
    device_id     UTF8     NOT NULL,
    fcm_token     UTF8,
    rustore_token UTF8,
    updated_at    Datetime NOT NULL,
    PRIMARY KEY (user_id, device_id)
);
```

---

## REST API

### Auth
```
POST /auth/yandex          — обмен OAuth-кода на JWT
POST /auth/email/register  — регистрация по email
POST /auth/email/login     — вход по email
POST /auth/refresh         — обновление access token
POST /auth/logout          — инвалидация refresh token
```

### Users
```
GET  /users/me             — профиль текущего пользователя
PUT  /users/me             — обновление имени
POST /users/me/avatar      — загрузка аватарки → Object Storage (замена filesDir/avatar.jpg)
POST /users/me/push-token  — сохранить FCM/RuStore токен
```

### Lists
```
GET    /lists                    — все списки пользователя (где он member)
POST   /lists                    — создать список
GET    /lists/{list_id}          — детали списка
PUT    /lists/{list_id}          — обновить название/цвет/порядок/настройки
DELETE /lists/{list_id}          — удалить список (только owner)
GET    /lists/{list_id}/members  — участники
DELETE /lists/{list_id}/members/{user_id}
```

### Items
```
GET    /lists/{list_id}/items                    — все активные товары
POST   /lists/{list_id}/items                    — добавить товар
PUT    /lists/{list_id}/items/{item_id}          — обновить (имя, note, tags, category_id)
DELETE /lists/{list_id}/items/{item_id}          — soft delete
POST   /lists/{list_id}/items/{item_id}/photo    — загрузить фото → Object Storage
POST   /lists/{list_id}/items/{item_id}/check    — отметить как купленный (→ purchase_history)
POST   /lists/{list_id}/items/{item_id}/uncheck  — вернуть в список
POST   /lists/{list_id}/items/{item_id}/move     — перенести в другой список
PATCH  /lists/{list_id}/items/reorder            — обновить sort_index
GET    /lists/{list_id}/recent                   — история покупок (для "Недавно использованные")
```

### Loyalty Cards
```
GET    /cards              — все карты текущего пользователя
POST   /cards              — добавить карту (name, barcode_value, barcode_format, color)
DELETE /cards/{card_id}    — удалить карту
```

### Invitations (расшаривание, ShareScreen)
```
POST /lists/{list_id}/invite        — создать инвайт (email или deep-link)
GET  /invite/{invite_token}         — публичный: инфо об инвайте
POST /invite/{invite_token}/accept  — принять (пользователь становится member)
```

### Sync (polling для MVP)
```
GET /lists/{list_id}/events?since={unix_timestamp}
  — возвращает список изменений с указанного момента
  — клиент вызывает каждые 15–30 сек когда список открыт
  — структура ответа: { updated_at, items_changed: [...], members_changed: [...] }
```

---

## Стратегия миграции Android-клиента

### Принцип: минимальные изменения в UI-слое

UI работает через `AppRepository` и `SortRepository` — это интерфейсы/классы-обёртки.
Нужно добавить облачные реализации, которые UI вызывает так же, как локальные.

### Шаг 1: Сетевой слой (добавить, не трогая Room)

```
app/src/main/kotlin/com/lena/kartoshka/
├── data/
│   ├── remote/
│   │   ├── KartoshkaApi.kt          -- Retrofit/Ktor интерфейс
│   │   ├── dto/                     -- DTO классы для API
│   │   ├── AuthInterceptor.kt       -- добавляет Bearer токен
│   │   └── TokenManager.kt          -- хранит JWT в EncryptedSharedPreferences
│   ├── AppRepository.kt             -- РАСШИРИТЬ: добавить облачные вызовы
│   └── sort/
│       ├── SortRepository.kt        -- интерфейс (не менять)
│       ├── LocalSortRepository.kt   -- оставить как fallback
│       └── CloudSortRepository.kt   -- новый: синхронизирует с /lists/{id}
```

### Шаг 2: Замена `imagePath` на `imageUrl`

`Item.imagePath` — локальный путь к файлу (`/data/data/.../files/item_X.jpg`).
После миграции: `Item.imageUrl` — URL из Object Storage.
Переходный период: поддерживать оба поля, при синхронизации загружать файл в Object Storage
и заменять локальный путь на URL.

### Шаг 3: Аутентификация

Добавить экран входа (`LoginScreen.kt`) перед `MainActivity`.
В `MainActivity.onCreate` проверять наличие JWT → если нет, показывать `LoginScreen`.

Кнопки входа:
- «Войти через Яндекс» → Custom Chrome Tab → OAuth → `POST /auth/yandex`
- «Войти по email» → диалог с полями email/пароль → `POST /auth/email/login`

### Шаг 4: Расшаривание (ShareScreen)

`ShareScreen.kt` уже существует, кнопка «Отправить приглашение» помечена TODO.
Нужно: `POST /lists/{list_id}/invite` → получить deep-link → передать в системный share sheet.
Deep-link формат: `kartoshka://invite/{token}` или `https://kartoshka.app/invite/{token}`
Добавить intent-filter в `AndroidManifest.xml` для обработки входящих инвайтов.

---

## Структура бэкенд-проекта (Go)

```
backend/
├── cmd/api/main.go
├── internal/
│   ├── auth/
│   │   ├── yandex.go        -- Yandex ID OAuth
│   │   ├── email.go         -- email + bcrypt
│   │   └── jwt.go           -- выдача/проверка JWT
│   ├── handlers/
│   │   ├── auth.go
│   │   ├── users.go
│   │   ├── lists.go
│   │   ├── items.go
│   │   ├── cards.go         -- loyalty cards
│   │   ├── invitations.go
│   │   └── sync.go          -- /events polling
│   ├── middleware/
│   │   ├── auth.go          -- Bearer JWT проверка
│   │   ├── cors.go
│   │   └── ratelimit.go     -- для auth эндпоинтов
│   ├── models/              -- Go-структуры, зеркало Kotlin-моделей
│   ├── repository/
│   │   ├── ydb.go           -- подключение YDB
│   │   ├── users.go
│   │   ├── lists.go
│   │   ├── items.go
│   │   ├── cards.go
│   │   └── invitations.go
│   ├── notifications/
│   │   ├── fcm.go           -- FCM HTTP v1 API
│   │   └── rustore.go       -- RuStore Push API
│   └── storage/
│       └── s3.go            -- Yandex Object Storage
├── migrations/              -- YQL скрипты CREATE TABLE
├── deploy/
│   └── container.yaml       -- Serverless Container конфиг
└── docs/openapi.yaml
```

---

## Ключевые маппинги: Kotlin ↔ Go ↔ YDB

| Kotlin | Go | YDB |
|--------|-----|-----|
| `Set<ItemTag>` (URGENT,ON_SALE) | `string` (CSV) | `UTF8` (CSV) |
| `Color` (ARGB Long) | `int64` | `Int64` |
| `BarcodeFormat.ordinal` (Int) | `int32` | `Int32` |
| `imagePath` (локальный путь) | `photo_url` (S3 URL) | `UTF8` |
| `ShoppingListEntity.color_value` | `color_value int64` | `Int64` |

---

## Переменные окружения

```env
# YDB
YDB_ENDPOINT=grpcs://ydb.serverless.yandexcloud.net:2135
YDB_DATABASE=/ru-central1/b1g.../etn...
YDB_SA_KEY_FILE=./sa-key.json

# Yandex ID OAuth
YANDEX_CLIENT_ID=...
YANDEX_CLIENT_SECRET=...
YANDEX_REDIRECT_URI=kartoshka://auth/yandex/callback

# JWT
JWT_SECRET=...
JWT_ACCESS_TTL=15m
JWT_REFRESH_TTL=720h

# Yandex Object Storage
S3_ENDPOINT=https://storage.yandexcloud.net
S3_BUCKET=kartoshka-media
S3_ACCESS_KEY=...
S3_SECRET_KEY=...

# FCM
FCM_PROJECT_ID=...
FCM_SERVICE_ACCOUNT_JSON=./fcm-sa.json

# RuStore Push
RUSTORE_PROJECT_ID=...
RUSTORE_SERVICE_TOKEN=...

# Email
SMTP_HOST=smtp.yandex.ru
SMTP_PORT=465
SMTP_USER=noreply@yourdomain.ru
SMTP_PASS=...

PORT=8080
ENV=production
```

---

## Важные требования

- **152-ФЗ**: все данные — только Yandex Cloud (Россия)
- **Авторизация на уровне данных**: пользователь видит только списки, где он `list_member`
- **Оптимистичные обновления**: PUT/POST возвращают обновлённую сущность целиком
- **Soft delete для items**: поле `is_deleted = true`, физически не удалять
- **last-write-wins**: при конфликте побеждает больший `updated_at`
- **Rate limiting**: на все `/auth/*` эндпоинты (защита от брутфорса)
- **Карты лояльности**: привязаны к `user_id`, не к `list_id` (как в текущем коде)

---

## MVP-порядок реализации

### Бэкенд (Go)
1. YDB: подключение + все `CREATE TABLE` в `migrations/`
2. Auth: Yandex ID OAuth + JWT выдача/refresh
3. Auth: Email + пароль + подтверждение
4. CRUD списков + участники
5. CRUD товаров (включая soft delete и `updated_by`)
6. Карты лояльности (CRUD)
7. Загрузка медиа в Object Storage (аватарки + фото товаров)
8. Расшаривание: инвайты → deep-link → принятие
9. Polling `/events` для синхронизации
10. Push-уведомления: FCM + RuStore при изменении списка

### Android-клиент (Kotlin) — параллельно или после бэкенда
1. `TokenManager` + `AuthInterceptor` + `KartoshkaApi` (Retrofit)
2. `LoginScreen` + Yandex OAuth Custom Tab
3. Расширить `AppRepository`: при успешном ответе API → записать в Room (offline cache)
4. `CloudSortRepository` (заменяет `LocalSortRepository` через DI)
5. Загрузка фото в Object Storage (заменить `imagePath` на `imageUrl`)
6. Обработка deep-link инвайтов в `AndroidManifest` + `MainActivity`
7. Polling в `ListDetailScreen` (повторный запрос `/events` каждые 15 сек)
8. Push: добавить FCM SDK + `POST /users/me/push-token` при старте

---

## Начать с

```bash
# Создать структуру проекта
mkdir -p backend/{cmd/api,internal/{auth,handlers,middleware,models,repository,notifications,storage},migrations,deploy,docs}
cd backend
go mod init github.com/lena/kartoshka-backend

# Зависимости
go get github.com/ydb-platform/ydb-go-sdk/v3
go get github.com/go-chi/chi/v5
go get github.com/golang-jwt/jwt/v5
go get github.com/aws/aws-sdk-go-v2/service/s3
go get golang.org/x/crypto/bcrypt
go get github.com/google/uuid
```

Первый файл: `migrations/001_init.yql` — все CREATE TABLE.
Второй: `internal/repository/ydb.go` — подключение и пинг.
Третий: `internal/auth/jwt.go` + `internal/auth/yandex.go`.
```
