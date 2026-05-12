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
