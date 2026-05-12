# Claude Code Prompt — Loyalty Card Feature (Kotlin/Android)

## Context

This is an existing Android shopping list app (Kotlin). The app already has:
- A home screen showing lists with cover images (`1home_1_lists.jpg`, `1home_2_lists_scroll.jpg`)
- A "Ваши карты" (Your Cards) section visible inside a list screen, currently showing store loyalty program tiles (Coop, Migros, IKEA) as colored rectangles — see `7list_3scroll.jpg` and `7list_4scroll.jpg`
- Dark theme UI throughout
- Bottom navigation: Списки / Идеи / Предложения / Профиль

**The loyalty card scanner is a new small section to be added inside the existing "Ваши карты" area on the list/profile screen.** It is NOT a standalone screen — it's a collapsible section within that page.

---

## Feature to implement: Loyalty Card Scanner & Wallet

### User flow

1. **Photograph** — User taps "+ Add card" inside the "Ваши карты" section → camera opens → user photographs their physical loyalty card
2. **Scan & Save** — ML Kit reads the barcode/QR from the photo, extracts the raw value + format (EAN-13, Code 128, QR, etc.) → user sees a preview with the decoded number → optionally types the card name and confirms → card is saved to Room DB (no photo stored, only the decoded string + format + card name + optional store logo)
3. **Use** — The "Ваши карты" section shows saved cards as tiles with the store logo (or auto-detected brand icon, or initials fallback). Tapping a tile opens a bottom sheet showing:
   - The barcode/QR rendered cleanly at full width via ZXing (not the original photo)
   - The card number in large, easy-to-read text below it
   - Card name at the top

### Technical requirements

**Dependencies to add to `build.gradle`:**
```
// ML Kit barcode scanning
implementation 'com.google.mlkit:barcode-scanning:17.2.0'

// ZXing for rendering barcodes
implementation 'com.journeyapps:zxing-android-embedded:4.3.0'
// or just the core if you render manually:
implementation 'com.google.zxing:core:3.5.3'

// CameraX for capture
implementation 'androidx.camera:camera-camera2:1.3.x'
implementation 'androidx.camera:camera-lifecycle:1.3.x'
implementation 'androidx.camera:camera-view:1.3.x'

// Room for persistence
implementation 'androidx.room:room-runtime:2.6.x'
kapt 'androidx.room:room-compiler:2.6.x'
```

**Room entity:**
```kotlin
@Entity(tableName = "loyalty_cards")
data class LoyaltyCard(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,           // e.g. "Coop Supercard"
    val barcodeValue: String,   // raw decoded string
    val barcodeFormat: Int,     // BarcodeFormat ordinal from ZXing
    val logoUrl: String? = null // optional, for known chains
)
```

**Scan flow (in fragment/activity):**
```kotlin
// After capturing image with CameraX:
val image = InputImage.fromBitmap(bitmap, 0)
val scanner = BarcodeScanning.getClient()
scanner.process(image)
    .addOnSuccessListener { barcodes ->
        val barcode = barcodes.firstOrNull() ?: return@addOnSuccessListener
        val value = barcode.rawValue ?: return@addOnSuccessListener
        val format = barcode.format // maps to ZXing BarcodeFormat
        // Show confirmation UI with value, then save to Room
    }
```

**Barcode rendering (ZXing):**
```kotlin
fun renderBarcode(value: String, format: BarcodeFormat, width: Int, height: Int): Bitmap {
    val writer = MultiFormatWriter()
    val bitMatrix = writer.encode(value, format, width, height)
    return BarcodeEncoder().createBitmap(bitMatrix)
}
// Set as ImageView source — sharp vector-like render, not a photo
```

### UI requirements

- Match the existing dark theme (`#1E2530` background, teal/green accent `#3DDC84` or whatever the app uses)
- Card tiles in "Ваши карты" section: same size/style as the existing Coop/Migros tiles but with the actual store logo or colored initial badge
- The "Add card" button: a `+` tile at the end of the horizontal card list (same pattern as the `+` FAB already in the app)
- Bottom sheet for card display: full-width barcode centered, card number in `24sp` monospace below, brightness boosted to max while sheet is open (helps scanners read it), auto-restore brightness on dismiss
- No photo is stored anywhere — only the decoded string

### Known edge cases to handle

- Barcode not detected: show "Couldn't read barcode — try better lighting" with retry + manual entry fallback
- QR codes: render with `BarcodeFormat.QR_CODE` at square aspect ratio
- Linear barcodes (EAN, Code128, etc.): render at wide aspect ratio (approx 3:1)
- ML Kit format → ZXing format mapping needed (they use different enums)

### What NOT to implement

- NFC emulation (not feasible without device support)
- Payment card reading
- Cloud sync of card data
- OCR of the card number from photo (manual entry is the fallback)

---

## Files to create / modify

1. `LoyaltyCard.kt` — Room entity (above)
2. `LoyaltyCardDao.kt` — Room DAO with insert/getAll/delete
3. `LoyaltyCardRepository.kt` — wraps DAO
4. `LoyaltyCardViewModel.kt` — exposes StateFlow<List<LoyaltyCard>>
5. `CardScanFragment.kt` — CameraX capture + ML Kit scan + confirmation dialog
6. `LoyaltyCardsSection.kt` (or XML + adapter) — the horizontal tile list + "+" button, to be embedded in the existing list/profile screen
7. `CardDisplayBottomSheet.kt` — ZXing barcode render + card number + brightness control
8. Modify the existing screen that shows "Ваши карты" to include this section

---

## Design reference

See attached screenshots from the project:
- `7list_3scroll.jpg` — shows the current "Ваши карты" horizontal strip with Coop/Migros/IKEA tiles. New saved loyalty cards go in this same strip.
- `7list_4scroll.jpg` — shows "Recently Used" grid below. Similar tile style.
- `8_edit_item.jpg` — shows the dark bottom sheet pattern used elsewhere in the app (use same style for card display sheet).
- `4new_empty_list.jpg` — shows the "Мне нужно..." search bar + `+` FAB at bottom — same `+` pattern for "add card".
