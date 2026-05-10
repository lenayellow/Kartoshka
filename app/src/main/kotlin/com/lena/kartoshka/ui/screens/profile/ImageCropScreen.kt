package com.lena.kartoshka.ui.screens.profile

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.lena.kartoshka.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

/**
 * Full-screen image cropper. Shows the photo with a square crop frame; user drags to position.
 * [aspectRatio] controls crop width/height ratio (1f = square, default).
 */
@Composable
fun ImageCropScreen(
    imageUri: Uri,
    aspectRatio: Float = 1f,
    onConfirm: (Bitmap) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(imageUri) {
        bitmap = withContext(Dispatchers.IO) {
            runCatching {
                context.contentResolver.openInputStream(imageUri)?.use {
                    BitmapFactory.decodeStream(it)
                }
            }.getOrNull()
        }
    }

    var dragX by remember { mutableFloatStateOf(0f) }
    var dragY by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        val bmp = bitmap
        if (bmp != null) {
            BoxWithConstraints(Modifier.fillMaxSize()) {
                val sw = constraints.maxWidth.toFloat()
                val sh = constraints.maxHeight.toFloat()

                // Crop frame: square (or given ratio), centered, 82% of screen width
                val cropW = sw * 0.82f
                val cropH = cropW / aspectRatio
                val cropL = (sw - cropW) / 2f
                val cropT = (sh - cropH) / 2f

                // Scale to ensure the image fully covers the crop frame
                val scale = maxOf(sw / bmp.width, cropH / bmp.height)
                val scaledW = bmp.width * scale
                val scaledH = bmp.height * scale
                val natL = (sw - scaledW) / 2f
                val natT = (sh - scaledH) / 2f

                // Clamp drag so crop frame is always covered by the image
                val minDX = cropL + cropW - natL - scaledW
                val maxDX = cropL - natL
                val minDY = cropT + cropH - natT - scaledH
                val maxDY = cropT - natT

                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectDragGestures { _, delta ->
                                dragX = (dragX + delta.x).coerceIn(minDX, maxDX)
                                dragY = (dragY + delta.y).coerceIn(minDY, maxDY)
                            }
                        }
                ) {
                    val imgL = natL + dragX
                    val imgT = natT + dragY

                    // Draw scaled image
                    drawImage(
                        image = bmp.asImageBitmap(),
                        srcOffset = IntOffset.Zero,
                        srcSize = IntSize(bmp.width, bmp.height),
                        dstOffset = IntOffset(imgL.roundToInt(), imgT.roundToInt()),
                        dstSize = IntSize(scaledW.roundToInt(), scaledH.roundToInt())
                    )

                    // Dark overlay — 4 rects around the crop frame
                    val overlay = Color(0x99000000)
                    drawRect(overlay, Offset.Zero, Size(sw, cropT))
                    drawRect(overlay, Offset(0f, cropT), Size(cropL, cropH))
                    drawRect(overlay, Offset(cropL + cropW, cropT), Size(sw - cropL - cropW, cropH))
                    drawRect(overlay, Offset(0f, cropT + cropH), Size(sw, sh - cropT - cropH))

                    // Thin frame border
                    drawRect(
                        color = Color.White.copy(alpha = 0.35f),
                        topLeft = Offset(cropL, cropT),
                        size = Size(cropW, cropH),
                        style = Stroke(1.dp.toPx())
                    )

                    // L-shaped corner handles
                    val cLen = 22.dp.toPx()
                    val cW = 3.dp.toPx()
                    val white = Color.White

                    // top-left
                    drawLine(white, Offset(cropL, cropT + cLen), Offset(cropL, cropT), cW, StrokeCap.Square)
                    drawLine(white, Offset(cropL, cropT), Offset(cropL + cLen, cropT), cW, StrokeCap.Square)
                    // top-right
                    drawLine(white, Offset(cropL + cropW - cLen, cropT), Offset(cropL + cropW, cropT), cW, StrokeCap.Square)
                    drawLine(white, Offset(cropL + cropW, cropT), Offset(cropL + cropW, cropT + cLen), cW, StrokeCap.Square)
                    // bottom-left
                    drawLine(white, Offset(cropL, cropT + cropH - cLen), Offset(cropL, cropT + cropH), cW, StrokeCap.Square)
                    drawLine(white, Offset(cropL, cropT + cropH), Offset(cropL + cLen, cropT + cropH), cW, StrokeCap.Square)
                    // bottom-right
                    drawLine(white, Offset(cropL + cropW - cLen, cropT + cropH), Offset(cropL + cropW, cropT + cropH), cW, StrokeCap.Square)
                    drawLine(white, Offset(cropL + cropW, cropT + cropH - cLen), Offset(cropL + cropW, cropT + cropH), cW, StrokeCap.Square)
                }

                // OK button
                Button(
                    onClick = {
                        val imgL = natL + dragX
                        val imgT = natT + dragY
                        val bitmapCropL = ((cropL - imgL) / scale).roundToInt().coerceIn(0, bmp.width - 1)
                        val bitmapCropT = ((cropT - imgT) / scale).roundToInt().coerceIn(0, bmp.height - 1)
                        val bitmapCropW = (cropW / scale).roundToInt().coerceAtLeast(1)
                        val bitmapCropH = (cropH / scale).roundToInt().coerceAtLeast(1)
                        val safeW = minOf(bitmapCropW, bmp.width - bitmapCropL).coerceAtLeast(1)
                        val safeH = minOf(bitmapCropH, bmp.height - bitmapCropT).coerceAtLeast(1)
                        val cropped = Bitmap.createBitmap(bmp, bitmapCropL, bitmapCropT, safeW, safeH)
                        onConfirm(cropped)
                    },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 20.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4CAF50)
                    )
                ) {
                    Text(
                        text = stringResource(R.string.crop_ok),
                        color = Color.White
                    )
                }
            }
        } else {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = Color.White
            )
        }
    }
}
