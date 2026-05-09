# Claude Code Prompt — Backend для Android-приложения «Списки покупок»

## Контекст проекта

Я разрабатываю Android-приложение для совместных списков покупок (аналог OurGroceries / Bring!).
Приложение работает **только в России**, целевая аудитория — российские пользователи.
Сейчас делаю только **Android-версию** (Kotlin / Jetpack Compose или Flutter — уточню).

Приложение уже имеет UI — нужно построить **бэкенд с нуля**.

---

## Функциональность приложения (из готового UI)

### Экраны и сценарии:

1. **Главный экран (Мои списки)**
   - Список карточек со списками покупок
   - Каждая карточка: название, картинка-обложка, счётчик товаров, аватарки участников, кнопка «+ пользователь»
   - Кнопка «Создать список»
   - Раздел «Рекомендации для добавления в список» (готовые шаблоны)

2. **Создание нового списка**
   - Ввод названия
   - Выбор обложки из галереи тематических картинок
   - Загрузка своей картинки

3. **Экран списка (детальный)**
   - Товары в виде плиток с иконками, сгруппированы по категориям
   - Метка NEW на новых товарах
   - Раздел «Недавно использованные» товары
   - Раздел «Ваши карты» (карты лояльности: Coop, Migros, IKEA и др.)
   - Раздел «Подходящие товары по акции»
   - Строка поиска / добавления товара «Мне нужно...» + кнопка «+»
   - Кнопка «Сортировка списка»
   - Аватарки участников в шапке

4. **Редактирование товара**
   - Название товара (например, «Bananas»)
   - Поле «Количество, описание»
   - Быстрые теги: количество (1, 2, 3, 4, 5, 8, 10, 1kg), атрибуты (Bio, Ripe)
   - Теги приоритета: Срочные, Акция, Если удобно
   - Настройки: Изменить иконку, Добавить фото, Изменить категорию, Перенести товары
   - Блок «Последнее изменение»: аватарка + имя пользователя + время

5. **Настройки списка**
   - Обложка с кнопкой редактирования
   - Настройки: Сортировка, Страна и язык
   - Пользователи списка: аватарка + имя + email
   - Управление пользователями
   - Кнопка «Удалить этот список» (красная, с подтверждением)

6. **Поделиться списком**
   - Способы: WhatsApp, SMS, Другое
   - ИЛИ: поле ввода email/контакта + кнопка «Отправить приглашение»

7. **Меню параметров списка (bottom sheet)**
   - Предложения покупок
   - Отправить список
   - Печать списка
   - Рекомендовать другу
   - Настройки списка
   - Карты

8. **Категории товаров**
   - Fruits & Vegetables, Bread & Pastries, Milk & Cheese, Meat & Fish,
     Ingredients & Spices, Frozen & Convenience, Grain Products, Snacks & Sweets,
     Beverages, Household, Care & Health, Pet Supplies, Home & Garden, Own Items

---

## Технический стек бэкенда

### Облако и база данных

- **Провайдер**: Yandex Cloud (Россия, 152-ФЗ, данные хранятся в РФ)
- **База данных**: **YDB Serverless** (Yandex Database в serverless-режиме)
  - Оплата за запросы (RU) и хранение, не за поднятые серверы
  - Бесплатный уровень: 1 ГБ хранения + 1 млн RU/мес
  - YQL (диалект SQL, совместимый с ANSI SQL)
  - SDK: ydb-go-sdk / ydb-python-sdk / ydb-java-sdk / Node.js SDK
- **Хранилище файлов**: Yandex Object Storage (S3-совместимое)
  - Для: обложек списков, фотографий товаров, аватарок пользователей
- **Вычисления**: Yandex Cloud Functions (serverless) или Serverless Containers
- **API Gateway**: Yandex API Gateway (OpenAPI 3.0)
- **Репозиторий**: Yandex Cloud Source (SourceCraft) или GitHub

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

**Опционально в будущем**: VK ID (российская альтернатива)

**Apple Sign In**: НЕ нужен (только Android)
**Google Sign In**: избегать как основного (нестабильность Firebase/Google в РФ)

**Сессии**: JWT (access token ~15 мин + refresh token ~30 дней), хранить refresh в Android Keystore

### Push-уведомления

**Основной канал: Firebase Cloud Messaging (FCM)**
- Пока работает в РФ для отправки серверных пушей (даже если Firebase DB нестабилен)
- Сервер → FCM API → Android-устройство
- Использовать: HTTP v1 API (не legacy)

**Дополнительно для совместимости с RuStore**: RuStore Push SDK
- Необходим для публикации в RuStore (российский магазин приложений)
- Документация: https://help.rustore.ru/rustore/for_developers/developer-documentation/sdk_push-notifications

**Архитектура**: бэкенд отправляет пуш через Cloud Function, которая пробует FCM, при неудаче — RuStore Push.

### Триггеры пуш-уведомлений
- Партнёр добавил/удалил товар в общем списке
- Вас добавили в список
- Кто-то принял приглашение в список
- Цена на товар снизилась (если интегрировать акции)

---

## Схема базы данных (YDB — таблицы)

> YDB использует YQL. Первичные ключи обязательны. Рекомендуется использовать UUID v4 для ID.

### `users`
```sql
CREATE TABLE users (
    user_id     UTF8    NOT NULL,  -- UUID
    yandex_uid  UTF8,              -- uid от Yandex ID OAuth (nullable для email-юзеров)
    email       UTF8    NOT NULL,
    name        UTF8    NOT NULL,
    avatar_url  UTF8,              -- ссылка на Object Storage
    created_at  Datetime NOT NULL,
    updated_at  Datetime NOT NULL,
    PRIMARY KEY (user_id)
);
```

### `lists`
```sql
CREATE TABLE lists (
    list_id      UTF8    NOT NULL,  -- UUID
    owner_id     UTF8    NOT NULL,  -- FK → users.user_id
    title        UTF8    NOT NULL,
    cover_url    UTF8,              -- ссылка на Object Storage
    country_code UTF8,             -- для локализации категорий
    sort_order   UTF8,             -- 'category' | 'manual' | 'alphabetical'
    created_at   Datetime NOT NULL,
    updated_at   Datetime NOT NULL,
    PRIMARY KEY (list_id)
);
```

### `list_members`
```sql
CREATE TABLE list_members (
    list_id   UTF8    NOT NULL,
    user_id   UTF8    NOT NULL,
    role      UTF8    NOT NULL,  -- 'owner' | 'editor'
    joined_at Datetime NOT NULL,
    PRIMARY KEY (list_id, user_id)
);
```

### `items`
```sql
CREATE TABLE items (
    item_id      UTF8    NOT NULL,  -- UUID
    list_id      UTF8    NOT NULL,  -- FK → lists.list_id
    name         UTF8    NOT NULL,
    quantity     UTF8,              -- "2", "1kg", "500g" — строка для гибкости
    description  UTF8,
    category     UTF8,             -- 'fruits_vegetables' | 'dairy' | ...
    icon_name    UTF8,             -- имя иконки из встроенного набора
    photo_url    UTF8,             -- ссылка на Object Storage
    is_checked   Bool    NOT NULL,
    priority     UTF8,             -- 'urgent' | 'on_sale' | 'if_convenient' | null
    is_bio       Bool,
    added_by     UTF8    NOT NULL,  -- FK → users.user_id
    updated_by   UTF8    NOT NULL,  -- FK → users.user_id (для "Последнее изменение")
    updated_at   Datetime NOT NULL,
    sort_index   Int32,
    PRIMARY KEY (item_id)
);
-- Индекс для быстрого получения товаров по списку:
-- CREATE INDEX idx_items_by_list ON items (list_id, is_checked, category);
```

### `invitations`
```sql
CREATE TABLE invitations (
    invite_token UTF8    NOT NULL,  -- UUID, уникальный токен в ссылке
    list_id      UTF8    NOT NULL,
    inviter_id   UTF8    NOT NULL,
    invitee_email UTF8,             -- если приглашение на email
    status       UTF8    NOT NULL,  -- 'pending' | 'accepted' | 'expired'
    created_at   Datetime NOT NULL,
    expires_at   Datetime NOT NULL,
    PRIMARY KEY (invite_token)
);
```

### `loyalty_cards`
```sql
CREATE TABLE loyalty_cards (
    card_id    UTF8 NOT NULL,
    user_id    UTF8 NOT NULL,
    store_name UTF8 NOT NULL,  -- 'Coop', 'Migros', 'IKEA', 'Лента', ...
    card_number UTF8,
    barcode_url UTF8,
    PRIMARY KEY (card_id)
);
```

### `push_tokens`
```sql
CREATE TABLE push_tokens (
    user_id    UTF8 NOT NULL,
    device_id  UTF8 NOT NULL,
    fcm_token  UTF8,
    rustore_token UTF8,
    updated_at Datetime NOT NULL,
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
PUT  /users/me             — обновление имени/аватарки
POST /users/me/avatar      — загрузка аватарки (→ Object Storage)
POST /users/me/push-token  — сохранить FCM/RuStore токен устройства
```

### Lists
```
GET    /lists              — все списки пользователя (где он member)
POST   /lists              — создать список
GET    /lists/{list_id}    — детали списка
PUT    /lists/{list_id}    — обновить название/обложку/настройки
DELETE /lists/{list_id}    — удалить список (только owner)
POST   /lists/{list_id}/cover — загрузить обложку (→ Object Storage)
```

### Members
```
GET    /lists/{list_id}/members             — участники списка
DELETE /lists/{list_id}/members/{user_id}   — удалить участника
```

### Invitations
```
POST /lists/{list_id}/invite        — создать приглашение (email или ссылка)
GET  /invite/{invite_token}         — инфо о приглашении (публичный эндпоинт)
POST /invite/{invite_token}/accept  — принять приглашение
```

### Items
```
GET    /lists/{list_id}/items              — все товары списка
POST   /lists/{list_id}/items             — добавить товар
PUT    /lists/{list_id}/items/{item_id}   — обновить товар (кол-во, описание, чекбокс, и т.д.)
DELETE /lists/{list_id}/items/{item_id}   — удалить товар
POST   /lists/{list_id}/items/{item_id}/photo — загрузить фото товара
PATCH  /lists/{list_id}/items/reorder     — обновить sort_index для нескольких товаров
```

### Catalog (встроенный каталог товаров для поиска и подсказок)
```
GET /catalog/search?q={query}&lang={ru|en}  — поиск товаров по названию
GET /catalog/categories                     — список категорий
GET /catalog/recent?list_id={list_id}       — недавно использованные товары
```

---

## Реальное время (синхронизация между участниками)

Так как YDB serverless не имеет встроенных WebSocket-подписок (в отличие от Firebase Realtime DB),
реализовать синхронизацию можно двумя способами:

### Вариант A — Polling (просто, подходит для MVP)
- Клиент делает GET /lists/{list_id}/items каждые 10–30 секунд
- При изменении — сервер возвращает `updated_at` списка, клиент сравнивает
- Плюс: нет постоянного соединения, работает из любой сети
- Минус: задержка до 30 секунд

### Вариант B — Long Polling или SSE (лучше для совместной работы)
- Эндпоинт `GET /lists/{list_id}/events?since={timestamp}` держит соединение до события
- При изменении любым участником — сервер закрывает соединение с данными изменения
- Клиент сразу переподключается
- Можно реализовать через Serverless Containers (не Functions, у которых лимит по времени)

**Рекомендация**: начать с Polling (Вариант A) на MVP, перейти на SSE при росте MAU.

---

## Структура проекта (бэкенд)

```
backend/
├── cmd/
│   └── api/
│       └── main.go            # точка входа
├── internal/
│   ├── auth/                  # JWT, Yandex OAuth, email auth
│   ├── handlers/              # HTTP-хэндлеры по доменам
│   │   ├── lists.go
│   │   ├── items.go
│   │   ├── users.go
│   │   ├── invitations.go
│   │   └── catalog.go
│   ├── middleware/            # auth middleware, logging, CORS
│   ├── models/                # структуры данных
│   ├── repository/            # слой работы с YDB
│   │   ├── ydb.go             # инициализация YDB-клиента
│   │   ├── users.go
│   │   ├── lists.go
│   │   └── items.go
│   ├── notifications/         # FCM + RuStore Push
│   └── storage/               # Yandex Object Storage (S3)
├── migrations/                # YQL-скрипты создания таблиц
├── deploy/
│   └── serverless/            # конфиги для Cloud Functions / Containers
└── docs/
    └── openapi.yaml           # спецификация API
```

---

## Переменные окружения

```env
# YDB
YDB_ENDPOINT=grpcs://ydb.serverless.yandexcloud.net:2135
YDB_DATABASE=/ru-central1/b1g.../etn...
YDB_SA_KEY_FILE=./sa-key.json       # ключ сервисного аккаунта

# Yandex ID OAuth
YANDEX_CLIENT_ID=...
YANDEX_CLIENT_SECRET=...

# JWT
JWT_SECRET=...
JWT_ACCESS_TTL=15m
JWT_REFRESH_TTL=720h

# Yandex Object Storage
S3_ENDPOINT=https://storage.yandexcloud.net
S3_BUCKET=shoppingapp-media
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

## Что нужно реализовать в первую очередь (MVP-порядок)

1. **Инициализация YDB**: подключение, создание всех таблиц из migrations/
2. **Auth через Yandex ID**: OAuth-флоу, выдача JWT, refresh
3. **Auth по email**: регистрация, логин, подтверждение email
4. **CRUD списков**: создать, получить все мои списки, редактировать, удалить
5. **CRUD товаров**: добавить, обновить (чекбокс!), удалить, получить список товаров
6. **Расшаривание**: создать приглашение → ссылка/email → принять → стать member
7. **Загрузка медиа**: обложки списков и фото товаров в Object Storage
8. **Push-уведомления**: FCM при изменении товара другим участником
9. **Поиск товаров**: каталог + история использования

---

## Важные требования

- **152-ФЗ**: все данные хранятся в Yandex Cloud (Россия), не передаются за рубеж
- **Авторизация на уровне данных**: пользователь видит только списки, в которых он `list_member`
- **Оптимистичные обновления**: API должен возвращать обновлённую сущность в ответе на PUT/PATCH
- **Soft delete**: не удалять товары физически сразу — помечать deleted_at для истории
- **Идемпотентность**: PUT-запросы должны быть идемпотентными
- **Обработка конфликтов**: если два участника одновременно меняют один товар — last-write-wins + updated_at
- **Rate limiting**: на auth-эндпоинты (защита от брутфорса)

---

## Технологии для реализации бэкенда (рекомендую Go)

**Язык**: Go 1.22+
- Отличный официальный YDB SDK (ydb-go-sdk)
- Нативная поддержка в Yandex Cloud Functions
- Быстрый старт в контейнере

**Основные библиотеки**:
```
github.com/ydb-platform/ydb-go-sdk/v3   — YDB клиент
github.com/go-chi/chi/v5                 — HTTP router
github.com/golang-jwt/jwt/v5             — JWT
github.com/aws/aws-sdk-go-v2             — S3/Object Storage
golang.org/x/crypto/bcrypt               — хэширование паролей
github.com/google/uuid                   — генерация UUID
```

**Альтернатива**: Python (FastAPI) или Node.js (Fastify) — если удобнее.
YDB SDK есть для всех трёх.

---

## Следующий шаг — с чего начать

```bash
# 1. Создать проект в Yandex Cloud Console
# 2. Создать YDB serverless базу данных
# 3. Создать сервисный аккаунт с ролью ydb.editor
# 4. Скачать ключ сервисного аккаунта (JSON)
# 5. Инициализировать Go-модуль
go mod init github.com/yourname/shoppingapp-backend
go get github.com/ydb-platform/ydb-go-sdk/v3
```

Начнём с создания таблиц (`migrations/`) и базового подключения к YDB.
