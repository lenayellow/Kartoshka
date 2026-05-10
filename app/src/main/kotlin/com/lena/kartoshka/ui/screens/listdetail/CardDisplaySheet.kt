package com.lena.kartoshka.ui.screens.listdetail

import android.app.Activity
import android.graphics.Bitmap
import android.view.WindowManager
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.lena.kartoshka.data.LoyaltyCard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun CardDisplaySheet(
    card: LoyaltyCard,
    onDelete: () -> Unit
) {
    val navBarPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    // Boost screen brightness while sheet is visible (helps barcode scanners)
    val window = (LocalContext.current as? Activity)?.window
    DisposableEffect(Unit) {
        val original = window?.attributes?.screenBrightness
            ?: WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
        window?.attributes = window?.attributes?.also { it.screenBrightness = 1f }
        onDispose {
            window?.attributes = window?.attributes?.also { it.screenBrightness = original }
        }
    }

    // Render barcode on a background thread
    var barcodeBitmap by remember(card.barcodeValue, card.barcodeFormat) {
        mutableStateOf<ImageBitmap?>(null)
    }
    LaunchedEffect(card.barcodeValue, card.barcodeFormat) {
        barcodeBitmap = withContext(Dispatchers.Default) {
            renderBarcode(card.barcodeValue, card.barcodeFormat)
        }
    }

    val isQr = card.barcodeFormat == BarcodeFormat.QR_CODE.ordinal

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(bottom = navBarPadding + 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header: card name + delete button
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = card.name,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }

        // Barcode / QR image
        if (barcodeBitmap != null) {
            Image(
                bitmap = barcodeBitmap!!,
                contentDescription = null,
                contentScale = ContentScale.FillWidth,
                modifier = Modifier
                    .fillMaxWidth(if (isQr) 0.65f else 1f)
                    .aspectRatio(if (isQr) 1f else 3f)
                    .clip(RoundedCornerShape(8.dp))
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(if (isQr) 1f else 3f),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }

        // Card number in monospace
        Text(
            text = card.barcodeValue,
            fontSize = 17.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            letterSpacing = 2.sp
        )
    }
}

internal fun renderBarcode(value: String, formatOrdinal: Int): ImageBitmap? =
    runCatching {
        val format = BarcodeFormat.values()[formatOrdinal]
        val isQr = format == BarcodeFormat.QR_CODE
        val width = if (isQr) 512 else 1024
        val height = if (isQr) 512 else 256
        val bitMatrix = MultiFormatWriter().encode(value, format, width, height)
        val pixels = IntArray(width * height) { i ->
            if (bitMatrix[i % width, i / width]) android.graphics.Color.BLACK
            else android.graphics.Color.WHITE
        }
        Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888).asImageBitmap()
    }.getOrNull()
