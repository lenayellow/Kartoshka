import ydb
import ydb.iam
import os

ENDPOINT = "grpcs://ydb.serverless.yandexcloud.net:2135"
DATABASE = "/ru-central1/b1grgrpcdh6l9e7pvhjp/etnqka94oeqvjsndnno7"
SA_KEY   = "sa_key.json"

driver = ydb.Driver(
    endpoint=ENDPOINT,
    database=DATABASE,
    credentials=ydb.iam.ServiceAccountCredentials.from_file(SA_KEY),
)
driver.wait(fail_fast=True, timeout=10)

pool = ydb.SessionPool(driver)

TABLES = [
    """
    CREATE TABLE IF NOT EXISTS users (
        user_id    Utf8     NOT NULL,
        yandex_uid Utf8,
        email      Utf8     NOT NULL,
        name       Utf8     NOT NULL,
        avatar_url Utf8,
        created_at Datetime NOT NULL,
        updated_at Datetime NOT NULL,
        PRIMARY KEY (user_id)
    )
    """,
    """
    CREATE TABLE IF NOT EXISTS lists (
        list_id           Utf8     NOT NULL,
        owner_id          Utf8     NOT NULL,
        title             Utf8     NOT NULL,
        color_value       Int64    NOT NULL,
        position          Int32    NOT NULL,
        category_order    Utf8,
        hidden_categories Utf8,
        created_at        Datetime NOT NULL,
        updated_at        Datetime NOT NULL,
        PRIMARY KEY (list_id)
    )
    """,
    """
    CREATE TABLE IF NOT EXISTS list_members (
        list_id   Utf8     NOT NULL,
        user_id   Utf8     NOT NULL,
        role      Utf8     NOT NULL,
        joined_at Datetime NOT NULL,
        PRIMARY KEY (list_id, user_id)
    )
    """,
    """
    CREATE TABLE IF NOT EXISTS items (
        item_id     Utf8     NOT NULL,
        list_id     Utf8     NOT NULL,
        name        Utf8     NOT NULL,
        tags        Utf8,
        note        Utf8,
        category_id Utf8,
        photo_url   Utf8,
        is_deleted  Bool     NOT NULL,
        added_by    Utf8     NOT NULL,
        updated_by  Utf8     NOT NULL,
        updated_at  Datetime NOT NULL,
        sort_index  Int32,
        PRIMARY KEY (item_id)
    )
    """,
    """
    CREATE TABLE IF NOT EXISTS purchase_history (
        id           Utf8     NOT NULL,
        item_name    Utf8     NOT NULL,
        list_id      Utf8     NOT NULL,
        purchased_at Datetime NOT NULL,
        user_id      Utf8     NOT NULL,
        PRIMARY KEY (id)
    )
    """,
    """
    CREATE TABLE IF NOT EXISTS loyalty_cards (
        card_id        Utf8  NOT NULL,
        user_id        Utf8  NOT NULL,
        name           Utf8  NOT NULL,
        barcode_value  Utf8  NOT NULL,
        barcode_format Int32 NOT NULL,
        color          Int64 NOT NULL,
        PRIMARY KEY (card_id)
    )
    """,
    """
    CREATE TABLE IF NOT EXISTS invitations (
        invite_token  Utf8     NOT NULL,
        list_id       Utf8     NOT NULL,
        inviter_id    Utf8     NOT NULL,
        invitee_email Utf8,
        status        Utf8     NOT NULL,
        created_at    Datetime NOT NULL,
        expires_at    Datetime NOT NULL,
        PRIMARY KEY (invite_token)
    )
    """,
    """
    CREATE TABLE IF NOT EXISTS push_tokens (
        user_id       Utf8     NOT NULL,
        device_id     Utf8     NOT NULL,
        fcm_token     Utf8,
        rustore_token Utf8,
        updated_at    Datetime NOT NULL,
        PRIMARY KEY (user_id, device_id)
    )
    """,
    """
    CREATE TABLE IF NOT EXISTS refresh_tokens (
        token      Utf8     NOT NULL,
        user_id    Utf8     NOT NULL,
        expires_at Datetime NOT NULL,
        PRIMARY KEY (token)
    )
    """,
]

def create_table(session, ddl):
    session.execute_scheme(ddl)

for i, ddl in enumerate(TABLES):
    pool.retry_operation_sync(lambda s, d=ddl: create_table(s, d))
    print(f"OK Таблица {i+1}/{len(TABLES)} создана")

# ALTER TABLE — добавляем колонки для email-аутентификации
# Если колонки уже существуют — пропускаем без ошибки
ALTER_COLUMNS = [
    "ALTER TABLE users ADD COLUMN password_hash Utf8",
    "ALTER TABLE users ADD COLUMN is_verified   Bool",
]
for ddl in ALTER_COLUMNS:
    try:
        pool.retry_operation_sync(lambda s, d=ddl: s.execute_scheme(d))
        print(f"OK: {ddl}")
    except Exception as e:
        print(f"Пропущено (уже существует): {str(e)[:80]}")

print("\nМиграция выполнена успешно!")
driver.stop()