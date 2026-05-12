# Промт для Claude Code — Внедрение новой цветовой системы MD3

---

## Задача

Мне нужно полностью заменить цветовую систему Android-приложения (Kotlin / Jetpack Compose) на новую палитру в стиле Material Design 3. Приложение — совместные списки покупок.

Проверь текущую структуру проекта командами `find . -name "*.kt" | grep -i theme` и `find . -name "*.kt" | grep -i color`, чтобы найти все файлы, связанные с темой.

---

## Новая цветовая палитра (Material Design 3)

Ниже — полный набор MD3 токенов. Реализуй их через `MaterialTheme.colorScheme` в Jetpack Compose.

### Primary (тёмно-зелёный)
```
primary               = #1C3A2F   // Кнопки, FAB, активные элементы
onPrimary             = #FFFFFF   // Текст/иконки поверх primary
primaryContainer      = #C8E6D4   // Карточки, чипы, фоны
onPrimaryContainer    = #0A1F18   // Текст поверх primaryContainer
```

### Secondary (приглушённый зелёный)
```
secondary             = #3D6B56   // Вторичные кнопки, табы
onSecondary           = #FFFFFF
secondaryContainer    = #D6EDE2   // Фильтры, теги
onSecondaryContainer  = #0F2B1F
```

### Tertiary (золотисто-бежевый акцент)
```
tertiary              = #7A6545   // Специальные акценты
onTertiary            = #FFFFFF
tertiaryContainer     = #F0E0C4   // Декоративные блоки
onTertiaryContainer   = #2C2010
```

### Error
```
error                 = #BA1A1A
onError               = #FFFFFF
errorContainer        = #FFDAD6
onErrorContainer      = #410002
```

### Surface (кремово-бежевые фоны)
```
surface               = #F5F0E8   // Основной фон приложения
surfaceDim            = #DDD8CF   // Приглушённый фон
surfaceBright         = #FAF8F3   // Яркий фон, карточки
surfaceContainerLowest  = #FFFFFF
surfaceContainerLow     = #F0EBE2
surfaceContainer        = #EAE5DC
surfaceContainerHigh    = #E4DED5
surfaceContainerHighest = #DED8CE
```

### On Surface (текст и иконки)
```
onSurface             = #1A1C19   // Основной текст
onSurfaceVariant      = #42493F   // Вторичный текст, подсказки
outline               = #72796E   // Разделители, рамки полей
outlineVariant        = #C2C9BD   // Лёгкие разделители
```

### Inverse / Scrim
```
inverseSurface        = #2E312C   // Snackbar, тултипы
inverseOnSurface      = #F0F1EB
inversePrimary        = #9DD4B5   // Ссылки на тёмном фоне
scrim                 = #000000
shadow                = #000000
```

---

## Как реализовать

### 1. Создай файл `ui/theme/Color.kt`

Объяви все цвета как `val` через `Color(0xFF??????)`:

```kotlin
// ui/theme/Color.kt
import androidx.compose.ui.graphics.Color

val Primary = Color(0xFF1C3A2F)
val OnPrimary = Color(0xFFFFFFFF)
val PrimaryContainer = Color(0xFFC8E6D4)
val OnPrimaryContainer = Color(0xFF0A1F18)
// ... и так далее для всех токенов
```

### 2. Обнови `ui/theme/Theme.kt`

Используй `lightColorScheme()` из Material3 и заполни все параметры. Пример структуры:

```kotlin
private val LightColorScheme = lightColorScheme(
    primary = Primary,
    onPrimary = OnPrimary,
    primaryContainer = PrimaryContainer,
    onPrimaryContainer = OnPrimaryContainer,
    secondary = Secondary,
    // ... все токены
    surface = Surface,
    onSurface = OnSurface,
    // ...
)
```

### 3. Удали тёмную тему (если есть)

Приложение пока поддерживает только светлую тему. Убери `darkColorScheme` или сделай `darkColorScheme = LightColorScheme` до отдельной задачи.

### 4. Проверь кастомные цвета

Найди в коде все хардкодированные цвета вида `Color(0xFF...)`, `Color.Red`, `colorResource(R.color....)` и замени на соответствующие токены из новой схемы. Логика замены:
- Акцентные/брендовые цвета → `MaterialTheme.colorScheme.primary`
- Фоны карточек → `MaterialTheme.colorScheme.surfaceContainerLow` или `primaryContainer`
- Основной текст → `MaterialTheme.colorScheme.onSurface`
- Вторичный текст → `MaterialTheme.colorScheme.onSurfaceVariant`
- Разделители → `MaterialTheme.colorScheme.outlineVariant`
- Кнопка «Удалить» (красная) → `MaterialTheme.colorScheme.error`

### 5. Цвет статус-бара и навигационной панели

В `MainActivity.kt` или `Theme.kt` обнови:
```kotlin
WindowCompat.getInsetsController(window, view).apply {
    isAppearanceLightStatusBars = true  // тёмные иконки статусбара на светлом фоне
}
```
И установи цвет статус-бара = `#F5F0E8` (совпадает с `surface`).

---

## Специфика экранов

После замены цветов проверь визуально каждый экран. Особое внимание:

- **Карточки списков на главном экране** — должны использовать `surfaceContainerLow` (#F0EBE2) как фон, а не белый
- **Метки NEW на товарах** — `primaryContainer` (#C8E6D4) фон + `onPrimaryContainer` (#0A1F18) текст
- **FAB «+»** — `primary` (#1C3A2F) фон + `onPrimary` (#FFFFFF) иконка
- **Bottom navigation** — активный таб: `primary`, неактивный: `onSurfaceVariant`
- **Кнопка «Удалить этот список»** — `error` (#BA1A1A) фон + `onError` (#FFFFFF) текст
- **Карты магазинов (Coop, Migros, IKEA)** — оставить оригинальные брендовые цвета, они не заменяются

---

## Проверка после изменений

1. Запусти `./gradlew build` — убедись, что нет ошибок компиляции
2. Запусти на эмуляторе
3. Пройди по всем экранам из списка выше
4. Если где-то текст не читается — скажи мне, я пришлю скриншот

---

## Дополнительно (по желанию)

Если в проекте есть кастомные компоненты с явными цветами (например, цвет текста `color = Color.Gray`), — замени на семантические токены. Не меняй размеры, отступы, формы — только цвета.

---

---

# Тёмная тема (Dark Color Scheme)

Тёмная тема построена по правилам MD3: акцентные цвета становятся светлее (чтобы читаться на тёмном фоне), поверхности — тёмными, роли on* инвертируются.

---

## Тёмная палитра — все токены

### Primary
```
primary               = #9DD4B5   // Светло-зелёный — читается на тёмном фоне
onPrimary             = #003821   // Тёмный текст поверх primary
primaryContainer      = #1A5240   // Тёмно-зелёный контейнер
onPrimaryContainer    = #B8EDD0   // Светлый текст поверх контейнера
```

### Secondary
```
secondary             = #A3CCBA   // Приглушённый мятный
onSecondary           = #0A3526
secondaryContainer    = #224D3B   // Тёмный вторичный контейнер
onSecondaryContainer  = #BEE8D5
```

### Tertiary
```
tertiary              = #D4BC8E   // Светло-золотой акцент
onTertiary            = #3E2D0A
tertiaryContainer     = #574320   // Тёмный золотистый контейнер
onTertiaryContainer   = #F0D8A8
```

### Error
```
error                 = #FFB4AB
onError               = #690005
errorContainer        = #93000A
onErrorContainer      = #FFDAD6
```

### Surface (тёмные фоны)
```
surface               = #111510   // Основной фон — почти чёрный с зелёным подтоном
surfaceDim            = #111510   // Приглушённый = совпадает с surface
surfaceBright         = #363A34   // Яркий фон для приподнятых элементов
surfaceContainerLowest  = #0C0F0B
surfaceContainerLow     = #1A1C19
surfaceContainer        = #1E211D
surfaceContainerHigh    = #282B27
surfaceContainerHighest = #333631
```

### On Surface (текст и иконки)
```
onSurface             = #E2E3DC   // Основной текст — почти белый с тёплым тоном
onSurfaceVariant      = #C2C9BD   // Вторичный текст, подсказки
outline               = #8C9388   // Разделители, рамки полей
outlineVariant        = #42493F   // Лёгкие разделители
```

### Inverse / Scrim
```
inverseSurface        = #E2E3DC   // Snackbar на тёмной теме — светлый
inverseOnSurface      = #2E312C
inversePrimary        = #1C3A2F   // Ссылки на светлом snackbar
scrim                 = #000000
shadow                = #000000
```

---

## Как добавить тёмную тему в код

### 1. Дополни `ui/theme/Color.kt` тёмными токенами

```kotlin
// === DARK THEME COLORS ===
val DarkPrimary = Color(0xFF9DD4B5)
val DarkOnPrimary = Color(0xFF003821)
val DarkPrimaryContainer = Color(0xFF1A5240)
val DarkOnPrimaryContainer = Color(0xFFB8EDD0)

val DarkSecondary = Color(0xFFA3CCBA)
val DarkOnSecondary = Color(0xFF0A3526)
val DarkSecondaryContainer = Color(0xFF224D3B)
val DarkOnSecondaryContainer = Color(0xFFBEE8D5)

val DarkTertiary = Color(0xFFD4BC8E)
val DarkOnTertiary = Color(0xFF3E2D0A)
val DarkTertiaryContainer = Color(0xFF574320)
val DarkOnTertiaryContainer = Color(0xFFF0D8A8)

val DarkError = Color(0xFFFFB4AB)
val DarkOnError = Color(0xFF690005)
val DarkErrorContainer = Color(0xFF93000A)
val DarkOnErrorContainer = Color(0xFFFFDAD6)

val DarkSurface = Color(0xFF111510)
val DarkSurfaceDim = Color(0xFF111510)
val DarkSurfaceBright = Color(0xFF363A34)
val DarkSurfaceContainerLowest = Color(0xFF0C0F0B)
val DarkSurfaceContainerLow = Color(0xFF1A1C19)
val DarkSurfaceContainer = Color(0xFF1E211D)
val DarkSurfaceContainerHigh = Color(0xFF282B27)
val DarkSurfaceContainerHighest = Color(0xFF333631)

val DarkOnSurface = Color(0xFFE2E3DC)
val DarkOnSurfaceVariant = Color(0xFFC2C9BD)
val DarkOutline = Color(0xFF8C9388)
val DarkOutlineVariant = Color(0xFF42493F)

val DarkInverseSurface = Color(0xFFE2E3DC)
val DarkInverseOnSurface = Color(0xFF2E312C)
val DarkInversePrimary = Color(0xFF1C3A2F)
```

### 2. Создай `DarkColorScheme` в `ui/theme/Theme.kt`

```kotlin
private val DarkColorScheme = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = DarkOnPrimary,
    primaryContainer = DarkPrimaryContainer,
    onPrimaryContainer = DarkOnPrimaryContainer,
    secondary = DarkSecondary,
    onSecondary = DarkOnSecondary,
    secondaryContainer = DarkSecondaryContainer,
    onSecondaryContainer = DarkOnSecondaryContainer,
    tertiary = DarkTertiary,
    onTertiary = DarkOnTertiary,
    tertiaryContainer = DarkTertiaryContainer,
    onTertiaryContainer = DarkOnTertiaryContainer,
    error = DarkError,
    onError = DarkOnError,
    errorContainer = DarkErrorContainer,
    onErrorContainer = DarkOnErrorContainer,
    surface = DarkSurface,
    surfaceDim = DarkSurfaceDim,
    surfaceBright = DarkSurfaceBright,
    surfaceContainerLowest = DarkSurfaceContainerLowest,
    surfaceContainerLow = DarkSurfaceContainerLow,
    surfaceContainer = DarkSurfaceContainer,
    surfaceContainerHigh = DarkSurfaceContainerHigh,
    surfaceContainerHighest = DarkSurfaceContainerHighest,
    onSurface = DarkOnSurface,
    onSurfaceVariant = DarkOnSurfaceVariant,
    outline = DarkOutline,
    outlineVariant = DarkOutlineVariant,
    inverseSurface = DarkInverseSurface,
    inverseOnSurface = DarkInverseOnSurface,
    inversePrimary = DarkInversePrimary,
    scrim = Color(0xFF000000),
)
```

### 3. Подключи обе схемы в `AppTheme`

```kotlin
@Composable
fun AppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).apply {
                // Светлые иконки статусбара в тёмной теме, тёмные — в светлой
                isAppearanceLightStatusBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
```

---

## Специфика тёмной темы для ваших экранов

- **Карточки списков** — фон `surfaceContainerLow` (#1A1C19), текст `onSurface` (#E2E3DC)
- **Метки NEW** — фон `primaryContainer` (#1A5240), текст `onPrimaryContainer` (#B8EDD0)
- **FAB «+»** — фон `primary` (#9DD4B5), иконка `onPrimary` (#003821)
- **Bottom navigation** — активный таб `primary` (#9DD4B5), неактивный `onSurfaceVariant` (#C2C9BD)
- **Кнопка «Удалить»** — фон `error` (#FFB4AB), текст `onError` (#690005)
- **Статус-бар** — `isAppearanceLightStatusBars = false` (светлые иконки на тёмном фоне)

---

## Проверка тёмной темы

1. В эмуляторе: **Settings → Display → Dark theme** — включить
2. Пройти все экраны: главный, детальный список, редактирование товара, настройки списка
3. Проверить читаемость текста на всех фонах
4. Убедиться, что иконки статус-бара светлые (не сливаются с тёмным фоном)
