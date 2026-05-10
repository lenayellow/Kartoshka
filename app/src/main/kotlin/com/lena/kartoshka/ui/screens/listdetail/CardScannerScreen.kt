package com.lena.kartoshka.ui.screens.listdetail

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.google.zxing.BarcodeFormat
import com.lena.kartoshka.R
import com.lena.kartoshka.data.LoyaltyCard
import com.lena.kartoshka.data.generateLoyaltyCardColor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.Executors
import androidx.compose.foundation.Image

private sealed class ScannerState {
    object Scanning : ScannerState()
    object ManualEntry : ScannerState()
    data class Confirming(val barcodeValue: String, val barcodeFormat: Int) : ScannerState()
}

@Composable
fun CardScannerFlow(
    onSaved: (LoyaltyCard) -> Unit,
    onDismiss: () -> Unit
) {
    var state by remember { mutableStateOf<ScannerState>(ScannerState.Scanning) }

    when (val s = state) {
        is ScannerState.Scanning -> CameraScanner(
            onDetected = { value, format ->
                state = ScannerState.Confirming(value, format)
            },
            onManualEntry = { state = ScannerState.ManualEntry },
            onDismiss = onDismiss
        )
        is ScannerState.ManualEntry -> ScanConfirmScreen(
            initialValue = "",
            barcodeFormat = BarcodeFormat.CODE_128.ordinal,
            isManual = true,
            onSave = { name, value ->
                val format = inferBarcodeFormat(value)
                onSaved(
                    LoyaltyCard(
                        id = UUID.randomUUID().toString(),
                        name = name,
                        barcodeValue = value,
                        barcodeFormat = format,
                        color = generateLoyaltyCardColor(name)
                    )
                )
            },
            onRetry = { state = ScannerState.Scanning },
            onDismiss = onDismiss
        )
        is ScannerState.Confirming -> ScanConfirmScreen(
            initialValue = s.barcodeValue,
            barcodeFormat = s.barcodeFormat,
            isManual = false,
            onSave = { name, value ->
                onSaved(
                    LoyaltyCard(
                        id = UUID.randomUUID().toString(),
                        name = name,
                        barcodeValue = value,
                        barcodeFormat = s.barcodeFormat,
                        color = generateLoyaltyCardColor(name)
                    )
                )
            },
            onRetry = { state = ScannerState.Scanning },
            onDismiss = onDismiss
        )
    }
}

@Composable
private fun CameraScanner(
    onDetected: (value: String, format: Int) -> Unit,
    onManualEntry: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasPermission = granted }

    LaunchedEffect(Unit) {
        if (!hasPermission) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (hasPermission) {
            val previewView = remember { PreviewView(context) }
            val analysisExecutor = remember { Executors.newSingleThreadExecutor() }
            val detected = remember { AtomicBoolean(false) }

            DisposableEffect(Unit) {
                val future = ProcessCameraProvider.getInstance(context)
                var cameraProvider: ProcessCameraProvider? = null

                future.addListener({
                    try {
                        cameraProvider = future.get()

                        val preview = Preview.Builder().build().also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }

                        val barcodeScanner = BarcodeScanning.getClient()
                        val imageAnalysis = ImageAnalysis.Builder()
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build()

                        imageAnalysis.setAnalyzer(analysisExecutor) { imageProxy ->
                            if (detected.get()) { imageProxy.close(); return@setAnalyzer }
                            val mediaImage = imageProxy.image
                            if (mediaImage != null) {
                                val image = InputImage.fromMediaImage(
                                    mediaImage, imageProxy.imageInfo.rotationDegrees
                                )
                                barcodeScanner.process(image)
                                    .addOnSuccessListener { barcodes ->
                                        val barcode = barcodes.firstOrNull { it.rawValue != null }
                                        if (barcode != null && detected.compareAndSet(false, true)) {
                                            val zxingFormat = mlKitFormatToZxing(barcode.format)
                                            onDetected(barcode.rawValue!!, zxingFormat)
                                        }
                                    }
                                    .addOnCompleteListener { imageProxy.close() }
                            } else {
                                imageProxy.close()
                            }
                        }

                        cameraProvider!!.unbindAll()
                        cameraProvider!!.bindToLifecycle(
                            lifecycleOwner,
                            CameraSelector.DEFAULT_BACK_CAMERA,
                            preview,
                            imageAnalysis
                        )
                    } catch (_: Exception) { }
                }, ContextCompat.getMainExecutor(context))

                onDispose {
                    cameraProvider?.unbindAll()
                    analysisExecutor.shutdown()
                }
            }

            AndroidView(
                factory = { previewView },
                modifier = Modifier.fillMaxSize()
            )

            // Viewfinder overlay
            Canvas(modifier = Modifier.fillMaxSize()) {
                val sw = size.width
                val sh = size.height
                val frameW = sw * 0.75f
                val frameH = frameW * 0.55f
                val frameL = (sw - frameW) / 2f
                val frameT = (sh - frameH) / 2f - sh * 0.05f

                // Dark overlay
                drawRect(Color(0x99000000))
                // Clear the viewfinder rectangle
                drawRoundRect(
                    color = Color.Transparent,
                    topLeft = Offset(frameL, frameT),
                    size = Size(frameW, frameH),
                    cornerRadius = CornerRadius(16.dp.toPx()),
                    blendMode = BlendMode.Clear
                )
                // Frame border
                drawRoundRect(
                    color = Color.White,
                    topLeft = Offset(frameL, frameT),
                    size = Size(frameW, frameH),
                    cornerRadius = CornerRadius(16.dp.toPx()),
                    style = Stroke(2.dp.toPx())
                )
            }

            // "Scanning…" hint inside viewfinder area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.Center)
                    .padding(horizontal = 64.dp)
                    .padding(top = 80.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.scanner_hint),
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            // Permission denied state
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.scanner_no_permission),
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }) {
                    Text(stringResource(R.string.scanner_grant_permission))
                }
            }
        }

        // Top bar: back + manual entry
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onDismiss) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = null,
                    tint = Color.White
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            TextButton(onClick = onManualEntry) {
                Text(
                    text = stringResource(R.string.scanner_manual_entry),
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun ScanConfirmScreen(
    initialValue: String,
    barcodeFormat: Int,
    isManual: Boolean,
    onSave: (name: String, barcodeValue: String) -> Unit,
    onRetry: () -> Unit,
    onDismiss: () -> Unit
) {
    var cardName by remember { mutableStateOf("") }
    var barcodeValue by remember { mutableStateOf(initialValue) }

    var barcodeBitmap by remember(barcodeValue, barcodeFormat) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(barcodeValue, barcodeFormat) {
        if (barcodeValue.isNotBlank()) {
            barcodeBitmap = withContext(Dispatchers.Default) {
                renderBarcode(barcodeValue, if (isManual) inferBarcodeFormat(barcodeValue) else barcodeFormat)
            }
        }
    }

    val isQr = barcodeFormat == BarcodeFormat.QR_CODE.ordinal && !isManual
    val canSave = cardName.isNotBlank() && barcodeValue.isNotBlank()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp)
            .padding(top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top bar
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onRetry) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
            }
            Text(
                text = stringResource(R.string.scanner_confirm_title),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
        }

        // Barcode preview
        if (barcodeBitmap != null) {
            Surface(
                color = Color.White,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Image(
                    bitmap = barcodeBitmap!!,
                    contentDescription = null,
                    contentScale = ContentScale.FillWidth,
                    modifier = Modifier
                        .fillMaxWidth(if (isQr) 0.6f else 1f)
                        .aspectRatio(if (isQr) 1f else 3f)
                        .padding(16.dp)
                        .align(Alignment.CenterHorizontally)
                )
            }
        }

        // Barcode value (editable if manual)
        if (isManual) {
            OutlinedTextField(
                value = barcodeValue,
                onValueChange = { barcodeValue = it },
                label = { Text(stringResource(R.string.scanner_barcode_number)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        } else {
            Text(
                text = barcodeValue,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Card name
        OutlinedTextField(
            value = cardName,
            onValueChange = { cardName = it },
            label = { Text(stringResource(R.string.scanner_card_name)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words)
        )

        Spacer(modifier = Modifier.weight(1f))

        // Save button
        Button(
            onClick = { onSave(cardName.trim(), barcodeValue.trim()) },
            enabled = canSave,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text(
                text = stringResource(R.string.scanner_save),
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }
    }
}

private fun mlKitFormatToZxing(mlKitFormat: Int): Int = when (mlKitFormat) {
    Barcode.FORMAT_CODE_128   -> BarcodeFormat.CODE_128.ordinal
    Barcode.FORMAT_CODE_39    -> BarcodeFormat.CODE_39.ordinal
    Barcode.FORMAT_CODE_93    -> BarcodeFormat.CODE_93.ordinal
    Barcode.FORMAT_EAN_13     -> BarcodeFormat.EAN_13.ordinal
    Barcode.FORMAT_EAN_8      -> BarcodeFormat.EAN_8.ordinal
    Barcode.FORMAT_UPC_A      -> BarcodeFormat.UPC_A.ordinal
    Barcode.FORMAT_UPC_E      -> BarcodeFormat.UPC_E.ordinal
    Barcode.FORMAT_QR_CODE    -> BarcodeFormat.QR_CODE.ordinal
    Barcode.FORMAT_DATA_MATRIX -> BarcodeFormat.DATA_MATRIX.ordinal
    Barcode.FORMAT_PDF417     -> BarcodeFormat.PDF_417.ordinal
    Barcode.FORMAT_AZTEC      -> BarcodeFormat.AZTEC.ordinal
    else                      -> BarcodeFormat.CODE_128.ordinal
}

private fun inferBarcodeFormat(value: String): Int {
    val onlyDigits = value.all { it.isDigit() }
    return when {
        onlyDigits && value.length == 13 -> BarcodeFormat.EAN_13.ordinal
        onlyDigits && value.length == 8  -> BarcodeFormat.EAN_8.ordinal
        else                              -> BarcodeFormat.CODE_128.ordinal
    }
}
