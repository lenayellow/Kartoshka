# Kartoshka — Super Списки

Локальное Android-приложение для совместных списков покупок.

## Сборка release-версии

### 1. Получи API-ключ AppMetrica

Зарегистрируйся на [appmetrica.yandex.ru](https://appmetrica.yandex.ru), создай приложение и скопируй API-ключ.

Пропиши в `local.properties` (файл в .gitignore, не коммитится):

```
APPMETRICA_API_KEY=xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
```

Без ключа крэш-репортинг просто не включится — приложение продолжит работать.

### 2. Подготовь keystore

Скопируй шаблон и заполни значения:

```
copy keystore.properties.example keystore.properties
```

Отредактируй `keystore.properties`:

```
storeFile=C:/путь/к/kartoshka-release.jks
storePassword=...
keyAlias=kartoshka
keyPassword=...
```

Сгенерировать keystore (один раз):

```
keytool -genkey -v -keystore kartoshka-release.jks -alias kartoshka -keyalg RSA -keysize 2048 -validity 10000
```

> keystore.properties и .jks-файл в git не попадают (уже в .gitignore). Храни их локально + зашифрованный бэкап.

### 2. Поставь версию

**Вариант А — через git tag (приоритетный):**

```
git tag v1.0.0
git push origin v1.0.0
git push github v1.0.0
```

Формат тега: `vX.Y.Z`. При сборке `versionName` станет `1.0.0`.

**Вариант Б — через файл VERSION (fallback):**

Отредактируй файл `VERSION` в корне репозитория. Используется только если git-тег `v*` не найден.

### 3. Собери AAB

```
scripts\build_release.bat
```

Скрипт:
- проверит наличие `keystore.properties`
- предупредит, если есть незакоммиченные изменения или нет тега на HEAD
- запустит `gradlew.bat clean :app:bundleRelease --no-daemon`
- покажет путь к AAB, размер, SHA-256 и версию

### 4. Результат

```
app\build\outputs\bundle\release\app-release.aab
```

Этот AAB загружается в RuStore через веб-консоль или CLI.
