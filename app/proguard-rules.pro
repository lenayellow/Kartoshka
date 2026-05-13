# ── Retrofit ──────────────────────────────────────────────────────────────────
-keepattributes Signature, Exceptions, RuntimeVisibleAnnotations, AnnotationDefault
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response
# Required for suspend functions (coroutine continuation passing)
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation

# ── Gson — network models use plain field names, no @SerializedName ────────────
# Without this, R8 renames fields and Gson can no longer match them to JSON keys.
-keep class com.lena.kartoshka.network.** { *; }

# ── OkHttp / Okio ─────────────────────────────────────────────────────────────
# OkHttp 4.x ships with its own consumer ProGuard rules.
# Suppress warnings for optional TLS providers not present on Android.
-dontwarn org.bouncycastle.**
-dontwarn org.conscrypt.**
-dontwarn org.openjsse.**

# ── Kotlin Coroutines ─────────────────────────────────────────────────────────
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}

# ── ZXing (pure JAR — no consumer rules bundled) ──────────────────────────────
-keep class com.google.zxing.** { *; }

# ── ML Kit barcode-scanning ───────────────────────────────────────────────────
# Ships with its own consumer ProGuard rules; no additions needed.

# ── Coil 2.x ──────────────────────────────────────────────────────────────────
# Ships with its own consumer ProGuard rules; no additions needed.

# ── Room ──────────────────────────────────────────────────────────────────────
# Room's AAR includes consumer rules that keep @Entity / @Dao / @Database.
# No additions needed.
