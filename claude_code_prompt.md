# Claude Code Prompt — Backend для Android-приложения «Картошка»

## Контекст проекта

Android-приложение для списков покупок «Картошка» — уже полностью реализовано как **local-first** (Kotlin / Jetpack Compose, Room/SQLite, без сервера).
Сейчас наступает момент перехода к **облачному backend**: синхронизация между устройствами, совместные списки, авторизация.

Приложение работает **только в России**, целевой магазин — **RuStore** (package: `com.lena.kartoshka`).

---

## Функциональность приложения (актуальное состояние UI)

### Экраны:

1. **Мои списки (главный экран)**
   - Список карточек: название, цвет-обложка, счётчик товаров
   - Кнопка «Создать список»
   - Раздел «Предложения» — шаблоны для быстрого создания нового списка (Аптека, Подарки на новый год, Продукты, Работа, Путешествие, День рождения и др.)

2. **Создание / редактирование списка**
   - Ввод названия
   - Выбор цвета из палитры (10 цветов)

3. **Детальный экран списка**
   - Товары в виде карточек, сгруппированные по категориям
   - Тег приоритета на карточке (Срочно / Акция / Если удобно)
   - Строка добавления товара + кнопка «+»
   - Кнопка сортировки списка
   - Нажатие на карточку = товар куплен (удаляется из списка, записывается в историю покупок)
   - Раздел **«Ваши карты»** (горизонтальный скролл) — показывает все карты лояльности пользователя
     - Карты глобальные (принадлежат пользователю, не конкретному списку), отображаются в каждом списке
     - Нажатие на карту → показывает штрихкод / QR (CardDisplaySheet, bottom sheet) для считывания на кассе
     - Кнопка «+» в ряду карт → открывает **сканер штрихкодов** (CardScannerScreen через камеру) → карта сохраняется
     - Удаление карты — из CardDisplaySheet

4. **Редактирование товара**
   - Название
   - Заметка (описание, количество — свободный текст)
   - Теги приоритета: **Срочно** / **Акция** / **Если удобно** (можно выбрать несколько)
   - Категория (из 12 фиксированных категорий на русском)
   - Фото товара (локальный файл)

5. **Настройки списка**
   - Переименование
   - Смена цвета
   - Настройки сортировки (экран SortScreen)
   - Удаление списка (с подтверждением)

6. **Сортировка списка (SortScreen)**
   - Настройки порядка отображения товаров

7. **Идеи / Рецепты (IdeasScreen)**
   - Карточки рецептов с обложкой, автором, тегом, лайками
   - Внутри рецепта — список ингредиентов с количеством
   - Кнопка «Добавить ингредиенты в список»
   - Флаг `isProbablyOwned` на ингредиентах (скорее всего уже есть дома)
   - Рецепты пока захардкожены; в будущем — подгружаются с сервера

8. **Профиль (ProfileScreen)**
   - Аватар с кроппером (выбор из галереи → ImageCropScreen)
   - Переключатель темы (тёмная / светлая)
   - Управление списками (открывает настройки конкретного списка)
   - «Рекомендовать приложение» → шаринг через Telegram / WhatsApp / ВКонтакте
   - «Оценить в RuStore»
   - «Поддержать разработчика» → СБП QR-код
   - Заглушки (готовый UI, логика появится с бэкендом):
     - Сменить пароль
     - Сменить email
     - Удалить аккаунт
     - Выйти

9. **Поделиться списком (ShareScreen)**
    - Пока заглушка; появится после реализации бэкенда

### Категории товаров (12 штук, фиксированный список на сервере):
Фрукты и овощи, Хлеб и выпечка, Молоко и сыр, Мясо и рыба, Крупы и макароны,
Заморозка, Напитки, Снеки и сладкое, Консервы и соусы, Бытовая химия,
Уход за собой, Детские товары

---

## Технический стек бэкенда

### Облако и база данных

- **Провайдер**: Yandex Cloud (Россия, 152-ФЗ, данные хранятся в РФ)
- **База данных**: **YDB Serverless** (Yandex Database в serverless-режиме)
  - Оплата за запросы (RU) и хранение, не за поднятые серверы
  - Бесплатный уровень: 1 ГБ хранения + 1 млн RU/мес
  - YQL (диалект SQL, совместимый с ANSI SQL)
  - SDK: ydb-go-sdk / ydb-python-sdk / Node.js SDK
- **Хранилище файлов**: Yandex Object Storage (S3-совместимое)
  - Для: аватарок пользователей, фотографий товаров
  - При миграции: локальные `image_path` в Room → URL в Object Storage
- **Вычисления**: Yandex Cloud Functions (serverless) или Serverless Containers
- **API Gateway**: Yandex API Gateway (OpenAPI 3.0)
- **Репозиторий**: GitHub

### Аутентификация (приоритет)

**Основной способ входа: Yandex ID (OAuth 2.0)**
- У большинства российских пользователей уже есть аккаунт Яндекса
- OAuth-флоу: открываем WebView/Custom Tab → пользователь логинится → получаем access_token + refresh_token
- Yandex ID возвращает: `uid`, `login`, `default_email`, `real_name`, `default_avatar_id`
- Документация: https://yandex.ru/dev/id/doc/dg/oauth/concepts/about.html

**Дополнительный способ: Email + пароль**
- Для пользователей без Яндекс-аккаунта
- Подтверждение email через Yandex Send (или любой SMTP)
- Хранить: bcrypt-хэш пароля, не сам пароль

**Опционально в будущем**: VK ID

**Apple Sign In**: НЕ нужен (только Android)
**Google Sign In**: избегать как основного (нестабильность Firebase/Google в РФ)

**Сессии**: JWT (access token ~15 мин + refresh token ~30 дней), хранить refresh в Android Keystore

### Push-уведомления

**Основной канал: Firebase Cloud Messaging (FCM)**
- Пока работает в РФ для отправки серверных пушей
- Использовать: HTTP v1 API (не legacy)

**Дополнительно для совместимости с RuStore**: RuStore Push SDK
- Документация: https://help.rustore.ru/rustore/for_developers/developer-documentation/sdk_push-notifications

**Архитектура**: бэкенд отправляет пуш через Cloud Function, которая пробует FCM, при неудаче — RuStore Push.

### Триггеры пуш-уведомлений
- Партнёр добавил/удалил товар в общем списке
- Вас добавили в список
- Кто-то принял приглашение в список

---

## Схема базы данных (YDB — таблицы)

> YDB использует YQL. Первичные ключи обязательны. UUID v4 для ID.
> Таблицы `shopping_lists`, `items`, `purchase_history`, `loyalty_cards` уже существуют в Room на устройстве — это точная основа для YDB-схемы.

### `users` *(новая таблица, только в облаке)*
```sql
CREATE TABLE users (
    user_id     UTF8     NOT NULL,  -- UUID
    yandex_uid  UTF8,               -- uid от Yandex ID OAuth (null для email-юзеров)
    email       UTF8     NOT NULL,
    name        UTF8     NOT NULL,
    avatar_url  UTF8,               -- ссылка на Object Storage (заменяет локальный avatar_path)
    created_at  Datetime NOT NULL,
    updated_at  Datetime NOT NULL,
    PRIMARY KEY (user_id)
);
```

### `shopping_lists`
```sql
CREATE TABLE shopping_lists (
    id          UTF8    NOT NULL,   -- UUID
    owner_id    UTF8    NOT NULL,   -- FK → users.user_id
    name        UTF8    NOT NULL,
    color_value Int64   NOT NULL,   -- ARGB Long (совпадает с Room: colorValue)
    position    Int32   NOT NULL,   -- порядок в списке у пользователя
    created_at  Datetime NOT NULL,
    updated_at  Datetime NOT NULL,
    PRIMARY KEY (id)
);
```

### `list_members` *(новая таблица, только в облаке)*
```sql
CREATE TABLE list_members (
    list_id   UTF8    NOT NULL,
    user_id   UTF8    NOT NULL,
    role      UTF8    NOT NULL,   -- 'owner' | 'editor'
    joined_at Datetime NOT NULL,
    PRIMARY KEY (list_id, user_id)
);
```

### `items`
```sql
CREATE TABLE items (
    id          UTF8    NOT NULL,   -- UUID
    list_id     UTF8    NOT NULL,   -- FK → shopping_lists.id
    name        UTF8    NOT NULL,
    tags        UTF8    NOT NULL DEFAULT "",
    -- comma-separated enum: "URGENT", "ON_SALE", "IF_CONVENIENT"
    -- соответствует Room: ItemTag enum (URGENT, ON_SALE, IF_CONVENIENT)
    note        UTF8    NOT NULL DEFAULT "",
    category_id UTF8,               -- "cat1"…"cat12" (см. категории выше)
    image_url   UTF8,               -- ссылка на Object Storage (в Room было image_path)
    added_by    UTF8    NOT NULL,   -- FK → users.user_id (новое поле для синхронизации)
    updated_by  UTF8    NOT NULL,   -- FK → users.user_id
    updated_at  Datetime NOT NULL,
    PRIMARY KEY (id)
);
-- Индекс для быстрой выборки по списку:
-- CREATE INDEX idx_items_by_list ON items (list_id, category_id);
```

### `purchase_history`
```sql
CREATE TABLE purchase_history (
    id           Int64    NOT NULL,  -- autoincrement заменяем на UUID в облаке
    user_id      UTF8     NOT NULL,  -- FK → users.user_id (новое поле)
    item_name    UTF8     NOT NULL,  -- соответствует Room: item_name
    list_id      UTF8     NOT NULL,  -- соответствует Room: list_id
    purchased_at Datetime NOT NULL,  -- в Room хранится как Unix ms (Long)
    PRIMARY KEY (id)
);
-- Индекс для «недавно использованных»:
-- CREATE INDEX idx_history_by_list ON purchase_history (list_id, purchased_at DESC);
```

### `loyalty_cards`
```sql
CREATE TABLE loyalty_cards (
    id             UTF8    NOT NULL,  -- UUID; совпадает с Room
    user_id        UTF8    NOT NULL,  -- FK → users.user_id (новое поле)
    name           UTF8    NOT NULL,  -- совпадает с Room: name
    barcode_value  UTF8    NOT NULL,  -- совпадает с Room: barcode_value
    barcode_format Int32   NOT NULL,  -- int из ZXing BarcodeFormat; совпадает с Room
    color          Int64   NOT NULL,  -- ARGB Long; совпадает с Room
    PRIMARY KEY (id)
);
```

### `invitations` *(новая таблица, только в облаке)*
```sql
CREATE TABLE invitations (
    invite_token  UTF8    NOT NULL,  -- UUID, уникальный токен в ссылке
    list_id       UTF8    NOT NULL,
    inviter_id    UTF8    NOT NULL,
    invitee_email UTF8,              -- если приглашение на email
    status        UTF8    NOT NULL,  -- 'pending' | 'accepted' | 'expired'
    created_at    Datetime NOT NULL,
    expires_at    Datetime NOT NULL,
    PRIMARY KEY (invite_token)
);
```

### `push_tokens`
```sql
CREATE TABLE push_tokens (
    user_id       UTF8    NOT NULL,
    device_id     UTF8    NOT NULL,
    fcm_token     UTF8,
    rustore_token UTF8,
    updated_at    Datetime NOT NULL,
    PRIMARY KEY (user_id, device_id)
);
```

---

## API — эндпоинты (REST)

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
POST /users/me/avatar      — загрузка аватарки (→ Object Storage)
POST /users/me/push-token  — сохранить FCM/RuStore токен устройства
DELETE /users/me           — удалить аккаунт
```

### Lists
```
GET    /lists              — все списки пользователя (где он member)
POST   /lists              — создать список
GET    /lists/{list_id}    — детали списка
PUT    /lists/{list_id}    — обновить название / цвет / позицию
DELETE /lists/{list_id}    — удалить список (только owner)
```

### Members
```
GET    /lists/{list_id}/members             — участники списка
DELETE /lists/{list_id}/members/{user_id}   — удалить участника
```

### Invitations
```
POST /lists/{list_id}/invite        — создать приглашение (email или deep-link)
GET  /invite/{invite_token}         — инфо о приглашении (публичный эндпоинт)
POST /invite/{invite_token}/accept  — принять приглашение
```

### Items
```
GET    /lists/{list_id}/items              — все товары списка
POST   /lists/{list_id}/items             — добавить товар
PUT    /lists/{list_id}/items/{item_id}   — обновить (заметка, теги, категория, и т.д.)
DELETE /lists/{list_id}/items/{item_id}   — удалить товар (= куплен или вручную)
POST   /lists/{list_id}/items/{item_id}/photo — загрузить фото товара (→ Object Storage)
```

### Purchase history (история покупок / «недавно использованные»)
```
GET  /lists/{list_id}/history          — недавно купленные товары в этом списке
POST /lists/{list_id}/history          — записать покупку (при удалении товара)
```

### Loyalty cards
```
GET    /loyalty-cards              — карты лояльности пользователя
POST   /loyalty-cards             — добавить карту
DELETE /loyalty-cards/{card_id}   — удалить карту
```

### Catalog (каталог товаров для подсказок и поиска)
```
GET /catalog/search?q={query}&lang=ru   — поиск по названию
GET /catalog/categories                 — список 12 категорий
```

---

## Реальное время (синхронизация между участниками)

YDB serverless не имеет встроенных WebSocket-подписок.

### Вариант A — Polling (просто, подходит для MVP)
- Клиент делает GET /lists/{list_id}/items каждые 10–30 секунд
- Сервер возвращает `updated_at` списка; клиент сравнивает и запрашивает данные только при изменении
- Плюс: нет постоянного соединения, работает из любой сети
- Минус: задержка до 30 секунд

### Вариант B — SSE (лучше для совместной работы)
- `GET /lists/{list_id}/events?since={timestamp}` держит соединение до события
- Реализовать через Serverless Containers (у Functions есть лимит по времени)

**Рекомендация**: начать с Polling на MVP, перейти на SSE при росте MAU.

---

## Структура проекта (бэкенд)

```
backend/
├── cmd/
│   └── api/
│       └── main.go
├── internal/
│   ├── auth/                  # JWT, Yandex OAuth, email auth
│   ├── handlers/
│   │   ├── lists.go
│   │   ├── items.go
│   │   ├── users.go
│   │   ├── invitations.go
│   │   ├── history.go
│   │   ├── loyalty_cards.go
│   │   └── catalog.go
│   ├── middleware/            # auth middleware, logging, CORS
│   ├── models/                # структуры данных
│   ├── repository/            # слой работы с YDB
│   │   ├── ydb.go
│   │   ├── users.go
│   │   ├── lists.go
│   │   ├── items.go
│   │   ├── history.go
│   │   └── loyalty_cards.go
│   ├── notifications/         # FCM + RuStore Push
│   └── storage/               # Yandex Object Storage (S3)
├── migrations/                # YQL-скрипты создания таблиц
├── deploy/
│   └── serverless/
└── docs/
    └── openapi.yaml
```

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
FCM_SERVER_KEY=...
FCM_PROJECT_ID=...

# RuStore Push
RUSTORE_PROJECT_ID=...
RUSTORE_SERVICE_TOKEN=...

# Email (Yandex Send или SMTP)
SMTP_HOST=smtp.yandex.ru
SMTP_PORT=465
SMTP_USER=noreply@yourdomain.ru
SMTP_PASS=...

# Server
PORT=8080
ENV=production
```

---

## MVP-порядок реализации

1. **Инициализация YDB**: подключение, создание всех таблиц из `migrations/`
2. **Auth через Yandex ID**: OAuth-флоу, выдача JWT, refresh
3. **Auth по email**: регистрация, логин, подтверждение email
4. **CRUD списков**: создать, получить мои списки, редактировать, удалить
5. **CRUD товаров**: добавить, обновить, удалить, получить по списку
6. **История покупок**: записать покупку, получить «недавно использованные»
7. **CRUD карт лояльности**: добавить, получить, удалить
8. **Расшаривание**: приглашение → ссылка/email → принять → стать member
9. **Загрузка медиа**: фото товаров и аватарки в Object Storage
10. **Push-уведомления**: FCM при изменении товара другим участником
11. **Поиск товаров**: каталог + история использования

---

## Важные требования

- **152-ФЗ**: все данные хранятся в Yandex Cloud (Россия), не передаются за рубеж
- **Авторизация на уровне данных**: пользователь видит только списки, в которых он `list_member`
- **Оптимистичные обновления**: API должен возвращать обновлённую сущность в ответе на PUT
- **Идемпотентность**: PUT-запросы должны быть идемпотентными
- **Конфликты**: если два участника одновременно меняют один товар — last-write-wins по `updated_at`
- **Rate limiting**: на auth-эндпоинты (защита от брутфорса)
- **Миграция данных**: при первом входе пользователя — предложить импорт локальных данных из Room в облако

---

## Технологии для реализации бэкенда (рекомендую Go)

**Язык**: Go 1.22+

**Основные библиотеки**:
```
github.com/ydb-platform/ydb-go-sdk/v3   — YDB клиент
github.com/go-chi/chi/v5                 — HTTP router
github.com/golang-jwt/jwt/v5             — JWT
github.com/aws/aws-sdk-go-v2             — S3/Object Storage
golang.org/x/crypto/bcrypt               — хэширование паролей
github.com/google/uuid                   — генерация UUID
```

**Альтернатива**: Python (FastAPI) или Node.js (Fastify). YDB SDK есть для всех трёх.

---

## Первый шаг

```bash
# 1. Создать проект в Yandex Cloud Console
# 2. Создать YDB serverless базу данных
# 3. Создать сервисный аккаунт с ролью ydb.editor
# 4. Скачать ключ сервисного аккаунта (JSON)
# 5. Инициализировать Go-модуль
go mod init github.com/yourname/kartoshka-backend
go get github.com/ydb-platform/ydb-go-sdk/v3
```

Начнём с создания таблиц (`migrations/`) и базового подключения к YDB.
