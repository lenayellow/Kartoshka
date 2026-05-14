package com.lena.kartoshka.ui.screens.profile

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.widget.Toast
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.ManageAccounts
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lena.kartoshka.R
import com.lena.kartoshka.data.AppRepository
import com.lena.kartoshka.data.ShoppingList
import com.lena.kartoshka.data.sort.SortRepository
import com.lena.kartoshka.ui.screens.listdetail.ListSettingsScreen
import com.lena.kartoshka.ui.screens.newlist.NewListScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    allLists: List<ShoppingList>,
    currentListId: String,
    sortRepository: SortRepository,
    appRepository: AppRepository,
    isDarkTheme: Boolean,
    onThemeChange: (Boolean) -> Unit,
    avatarPath: String?,
    onAvatarChange: (String?) -> Unit,
    onClose: () -> Unit,
    onDeleteCurrentList: () -> Unit,
    onLogout: () -> Unit = {},
    userName: String? = null,
    userEmail: String? = null
) {
    BackHandler { onClose() }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val statusBarPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navBarPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    var selectedListForSettings by remember { mutableStateOf<ShoppingList?>(null) }
    var listToEdit by remember { mutableStateOf<ShoppingList?>(null) }
    var showListPicker by remember { mutableStateOf(false) }
    var showShareSheet by remember { mutableStateOf(false) }
    var showDeleteAccountConfirm by remember { mutableStateOf(false) }
    var showSupportSheet by remember { mutableStateOf(false) }
    var pendingCropUri by remember { mutableStateOf<Uri?>(null) }

    var avatarBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(avatarPath) {
        avatarBitmap = if (avatarPath != null) {
            val result = withContext(Dispatchers.IO) {
                runCatching { BitmapFactory.decodeFile(avatarPath)?.asImageBitmap() }
            }
            result.onFailure {
                Toast.makeText(context, context.getString(R.string.profile_avatar_error), Toast.LENGTH_SHORT).show()
            }
            result.getOrNull()
        } else null
    }
    val isAvatarLoading = avatarPath != null && avatarBitmap == null

    val imageLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) pendingCropUri = uri
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
        ) {
            // Top bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = statusBarPadding)
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onClose) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                Text(
                    text = stringResource(R.string.nav_profile),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.size(48.dp))
            }

            // Scrollable content
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(24.dp))

                // Avatar
                Box(contentAlignment = Alignment.BottomEnd) {
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                            .clickable { imageLauncher.launch("image/*") },
                        contentAlignment = Alignment.Center
                    ) {
                        if (avatarBitmap != null) {
                            Image(
                                bitmap = avatarBitmap!!,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Filled.Person,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(56.dp)
                            )
                        }
                        if (isAvatarLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(32.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                            .clickable { imageLauncher.launch("image/*") },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CameraAlt,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (userName != null) {
                    Text(
                        text = userName!!,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                if (userEmail != null) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = userEmail!!,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // ── Settings section ──────────────────────────────────
                ProfileSectionHeader(
                    icon = Icons.Filled.Settings,
                    title = stringResource(R.string.settings)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        // Theme toggle
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = if (isDarkTheme) Icons.Filled.DarkMode else Icons.Filled.LightMode,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                                Column {
                                    Text(
                                        text = stringResource(R.string.profile_theme),
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = if (isDarkTheme) stringResource(R.string.profile_theme_dark)
                                        else stringResource(R.string.profile_theme_light),
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Switch(
                                checked = isDarkTheme,
                                onCheckedChange = onThemeChange,
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                                    checkedTrackColor = MaterialTheme.colorScheme.primary,
                                    uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                                    uncheckedTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                                )
                            )
                        }

                        ProfileRowDivider()

                        ProfileSettingsRow(
                            icon = Icons.AutoMirrored.Filled.FormatListBulleted,
                            title = stringResource(R.string.list_menu_settings),
                            onClick = { showListPicker = true }
                        )

                        ProfileRowDivider()

                        ProfileSettingsRow(
                            icon = Icons.Filled.Favorite,
                            title = stringResource(R.string.list_menu_recommend),
                            onClick = { showShareSheet = true }
                        )

                        ProfileRowDivider()

                        ProfileSettingsRow(
                            icon = Icons.Filled.Star,
                            title = stringResource(R.string.profile_rate),
                            onClick = {
                                val intent = try {
                                    Intent(Intent.ACTION_VIEW, Uri.parse("rustore://apps/com.lena.kartoshka"))
                                } catch (_: Exception) {
                                    Intent(Intent.ACTION_VIEW, Uri.parse("https://apps.rustore.ru/app/com.lena.kartoshka"))
                                }
                                runCatching { context.startActivity(intent) }
                            }
                        )

                        ProfileRowDivider()

                        ProfileSettingsRow(
                            icon = Icons.Filled.Favorite,
                            title = stringResource(R.string.profile_support_dev),
                            onClick = { showSupportSheet = true }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // ── Profile settings section ──────────────────────────
                ProfileSectionHeader(
                    icon = Icons.Filled.ManageAccounts,
                    title = stringResource(R.string.profile_account)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        ProfileSettingsRow(
                            icon = Icons.Filled.Lock,
                            title = stringResource(R.string.profile_change_password),
                            onClick = {}
                        )
                        ProfileRowDivider()
                        ProfileSettingsRow(
                            icon = Icons.Filled.Email,
                            title = stringResource(R.string.profile_change_email),
                            onClick = {}
                        )
                        ProfileRowDivider()
                        ProfileSettingsRow(
                            icon = Icons.Filled.AccountCircle,
                            title = stringResource(R.string.profile_change_avatar),
                            onClick = { imageLauncher.launch("image/*") }
                        )
                        ProfileRowDivider()
                        ProfileSettingsRow(
                            icon = Icons.Filled.DeleteForever,
                            title = stringResource(R.string.profile_delete_account),
                            titleColor = MaterialTheme.colorScheme.error,
                            iconTint = MaterialTheme.colorScheme.error,
                            onClick = { showDeleteAccountConfirm = true }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Made in Russia badge
                Image(
                    painter = painterResource(R.drawable.made_in_russia),
                    contentDescription = null,
                    modifier = Modifier
                        .width(80.dp)
                        .align(Alignment.CenterHorizontally)
                        .alpha(0.6f),
                    contentScale = ContentScale.Fit,
                    colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(
                        MaterialTheme.colorScheme.onSurface
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Logout button
                OutlinedButton(
                    onClick = { onLogout() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = navBarPadding + 12.dp),
                    border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Logout,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.profile_logout),
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        modifier = Modifier.padding(vertical = 6.dp)
                    )
                }

            }
        }

        // Full-screen overlays
        selectedListForSettings?.let { settingsList ->
            ListSettingsScreen(
                list = settingsList,
                sortRepository = sortRepository,
                appRepository = appRepository,
                currentUserName = userName,
                currentUserEmail = userEmail,
                onBack = { selectedListForSettings = null },
                onDeleteList = {
                    scope.launch { appRepository.deleteList(settingsList.id) }
                    selectedListForSettings = null
                    if (settingsList.id == currentListId) onDeleteCurrentList()
                },
                onEditNameAndImage = {
                    selectedListForSettings = null
                    listToEdit = settingsList
                }
            )
        }

        listToEdit?.let { editList ->
            NewListScreen(
                initialName = editList.name,
                initialColor = editList.color,
                editingListId = editList.id,
                onSaved = { updatedList ->
                    scope.launch { appRepository.updateList(updatedList) }
                    listToEdit = null
                }
            )
        }

        // Delete account confirmation
        if (showDeleteAccountConfirm) {
            AlertDialog(
                onDismissRequest = { showDeleteAccountConfirm = false },
                title = {
                    Text(
                        text = stringResource(R.string.profile_delete_account),
                        fontWeight = FontWeight.Bold
                    )
                },
                text = { Text(text = stringResource(R.string.profile_delete_account_confirm)) },
                confirmButton = {
                    TextButton(onClick = { showDeleteAccountConfirm = false }) {
                        Text(
                            text = stringResource(R.string.cancel),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteAccountConfirm = false }) {
                        Text(
                            text = stringResource(R.string.profile_delete_account),
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            )
        }

        // Photo crop overlay
        pendingCropUri?.let { uri ->
            ImageCropScreen(
                imageUri = uri,
                onConfirm = { croppedBitmap ->
                    scope.launch(Dispatchers.IO) {
                        runCatching {
                            val file = File(context.filesDir, "avatar.jpg")
                            file.outputStream().use { out ->
                                croppedBitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
                            }
                            withContext(Dispatchers.Main) {
                                pendingCropUri = null
                                onAvatarChange(file.absolutePath)
                            }
                        }
                    }
                },
                onDismiss = { pendingCropUri = null }
            )
        }
    }

    // List picker bottom sheet
    if (showListPicker) {
        val sheetState = rememberModalBottomSheetState()
        ModalBottomSheet(
            onDismissRequest = { showListPicker = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ) {
            Column(modifier = Modifier.padding(bottom = navBarPadding + 16.dp)) {
                Text(
                    text = stringResource(R.string.profile_pick_list),
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
                )
                if (allLists.isEmpty()) {
                    Text(
                        text = stringResource(R.string.profile_no_lists),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)
                    )
                }
                allLists.forEach { list ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showListPicker = false
                                selectedListForSettings = list
                            }
                            .padding(horizontal = 20.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(list.color)
                        )
                        Text(
                            text = list.name,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }

    // Share / recommend sheet
    if (showShareSheet) {
        val sheetState = rememberModalBottomSheetState()
        val shareLink = "https://apps.rustore.ru/app/com.lena.kartoshka"
        ModalBottomSheet(
            onDismissRequest = { showShareSheet = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 20.dp)
                    .padding(bottom = navBarPadding + 16.dp)
            ) {
                Text(
                    text = stringResource(R.string.list_menu_recommend),
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(vertical = 12.dp)
                )

                ShareRow(
                    icon = Icons.Filled.NearMe,
                    label = "Telegram",
                    onClick = {
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, shareLink)
                            setPackage("org.telegram.messenger")
                        }
                        runCatching { context.startActivity(intent) }
                    }
                )
                ShareRow(
                    icon = Icons.AutoMirrored.Filled.Chat,
                    label = "WhatsApp",
                    onClick = {
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, shareLink)
                            setPackage("com.whatsapp")
                        }
                        runCatching { context.startActivity(intent) }
                    }
                )
                ShareRow(
                    icon = Icons.Filled.Group,
                    label = "ВКонтакте",
                    onClick = {
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, shareLink)
                            setPackage("com.vkontakte.android")
                        }
                        runCatching { context.startActivity(intent) }
                    }
                )
                ShareRow(
                    icon = Icons.Filled.ContentCopy,
                    label = stringResource(R.string.profile_share_copy),
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("app_link", shareLink))
                        showShareSheet = false
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }

    if (showSupportSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val phone = stringResource(R.string.profile_support_dev_phone)
        val cloudtipsUrl = stringResource(R.string.profile_support_cloudtips_url)

        var selectedTab by remember { mutableStateOf(0) }
        var sbpQrBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
        var cloudtipsQrBitmap by remember { mutableStateOf<ImageBitmap?>(null) }

        LaunchedEffect(Unit) {
            val sbpResult = withContext(Dispatchers.IO) {
                runCatching {
                    val size = 512
                    val bitMatrix = MultiFormatWriter().encode(phone, BarcodeFormat.QR_CODE, size, size)
                    val pixels = IntArray(size * size) { i ->
                        if (bitMatrix[i % size, i / size]) android.graphics.Color.BLACK
                        else android.graphics.Color.WHITE
                    }
                    Bitmap.createBitmap(pixels, size, size, Bitmap.Config.ARGB_8888).asImageBitmap()
                }
            }
            sbpResult.onFailure {
                Toast.makeText(context, context.getString(R.string.profile_qr_error), Toast.LENGTH_SHORT).show()
            }
            sbpQrBitmap = sbpResult.getOrNull()
            val cloudtipsResult = withContext(Dispatchers.IO) {
                runCatching {
                    val size = 512
                    val bitMatrix = MultiFormatWriter().encode(cloudtipsUrl, BarcodeFormat.QR_CODE, size, size)
                    val pixels = IntArray(size * size) { i ->
                        if (bitMatrix[i % size, i / size]) android.graphics.Color.BLACK
                        else android.graphics.Color.WHITE
                    }
                    Bitmap.createBitmap(pixels, size, size, Bitmap.Config.ARGB_8888).asImageBitmap()
                }
            }
            cloudtipsResult.onFailure {
                Toast.makeText(context, context.getString(R.string.profile_qr_error), Toast.LENGTH_SHORT).show()
            }
            cloudtipsQrBitmap = cloudtipsResult.getOrNull()
        }

        ModalBottomSheet(
            onDismissRequest = { showSupportSheet = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = navBarPadding + 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = stringResource(R.string.profile_support_dev),
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.fillMaxWidth()
                )

                // Tab switcher
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                ) {
                    listOf(
                        stringResource(R.string.profile_support_tab_sbp),
                        stringResource(R.string.profile_support_tab_cloudtips)
                    ).forEachIndexed { index, label ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (selectedTab == index) MaterialTheme.colorScheme.primary
                                    else Color.Transparent
                                )
                                .clickable { selectedTab = index }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (selectedTab == index) MaterialTheme.colorScheme.onPrimary
                                        else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                if (selectedTab == 0) {
                    Text(
                        text = stringResource(R.string.profile_support_dev_subtitle),
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    )

                    sbpQrBitmap?.let { bmp ->
                        Surface(
                            color = Color.White,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.size(220.dp)
                        ) {
                            Image(
                                bitmap = bmp,
                                contentDescription = null,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp)
                            )
                        }
                    }

                    Text(
                        text = phone,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 1.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    OutlinedButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("sbp_phone", phone))
                        },
                        modifier = Modifier.fillMaxWidth(),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ContentCopy,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.profile_support_dev_copy))
                    }
                } else {
                    Text(
                        text = stringResource(R.string.profile_support_cloudtips_subtitle),
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    )

                    cloudtipsQrBitmap?.let { bmp ->
                        Surface(
                            color = Color.White,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.size(220.dp)
                        ) {
                            Image(
                                bitmap = bmp,
                                contentDescription = null,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp)
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(cloudtipsUrl))
                            runCatching { context.startActivity(intent) }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(stringResource(R.string.profile_support_cloudtips_open))
                    }
                }

                Text(
                    text = stringResource(R.string.profile_support_dev_hint),
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun ProfileSectionHeader(icon: ImageVector, title: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp)
        )
        Text(
            text = title,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            letterSpacing = 0.5.sp
        )
    }
}

@Composable
private fun ProfileSettingsRow(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit,
    titleColor: Color = MaterialTheme.colorScheme.onSurface,
    iconTint: Color = MaterialTheme.colorScheme.primary
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(22.dp)
        )
        Text(
            text = title,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = titleColor,
            modifier = Modifier.weight(1f)
        )
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun ProfileRowDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 16.dp),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
    )
}

@Composable
private fun ShareRow(icon: ImageVector, label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(22.dp)
            )
        }
        Text(
            text = label,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
