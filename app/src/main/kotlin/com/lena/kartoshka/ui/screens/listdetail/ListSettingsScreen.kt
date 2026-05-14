package com.lena.kartoshka.ui.screens.listdetail

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lena.kartoshka.R
import androidx.activity.compose.BackHandler
import com.lena.kartoshka.data.AppRepository
import com.lena.kartoshka.data.ShoppingList
import com.lena.kartoshka.data.itemCategories
import com.lena.kartoshka.data.sort.SortRepository
import kotlinx.coroutines.launch

@Composable
fun ListSettingsScreen(
    list: ShoppingList,
    sortRepository: SortRepository,
    appRepository: AppRepository,
    currentUserName: String? = null,
    currentUserEmail: String? = null,
    onBack: () -> Unit,
    onDeleteList: () -> Unit,
    onEditNameAndImage: () -> Unit = {}
) {
    val statusBarPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navBarPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val scope = rememberCoroutineScope()

    val categoryOrderIds by sortRepository.observeCategoryOrder().collectAsState(initial = emptyList())
    val hiddenCategoryIds by sortRepository.observeHiddenCategories().collectAsState(initial = emptySet())

    val categoriesForSort = remember(categoryOrderIds) {
        if (categoryOrderIds.isEmpty()) itemCategories
        else {
            val positionById = categoryOrderIds.mapIndexed { pos, id -> id to pos }.toMap()
            itemCategories.sortedBy { positionById[it.id] ?: Int.MAX_VALUE }
        }
    }

    BackHandler { onBack() }

    var showSort by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showMembers by remember { mutableStateOf(false) }

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
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                Text(
                    text = stringResource(R.string.list_menu_settings),
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
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // List color banner
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(list.color)
                        .padding(20.dp)
                ) {
                    Text(
                        text = list.name,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.align(Alignment.BottomStart)
                    )
                }

                // Configure section
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Settings,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = stringResource(R.string.list_settings_configure),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            SettingsActionTile(
                                icon = Icons.Filled.SwapVert,
                                label = stringResource(R.string.sort_screen_title),
                                modifier = Modifier.weight(1f),
                                onClick = { showSort = true }
                            )
                            SettingsActionTile(
                                icon = Icons.Filled.Language,
                                label = stringResource(R.string.list_settings_language),
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            SettingsActionTile(
                                icon = Icons.Filled.Group,
                                label = stringResource(R.string.list_settings_members),
                                modifier = Modifier.weight(1f),
                                onClick = { showMembers = true }
                            )
                            SettingsActionTile(
                                icon = Icons.Filled.Edit,
                                label = stringResource(R.string.list_settings_name_image),
                                modifier = Modifier.weight(1f),
                                onClick = onEditNameAndImage
                            )
                        }
                    }
                }
            }

            // Delete button
            Button(
                onClick = { showDeleteConfirm = true },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .padding(bottom = navBarPadding)
            ) {
                Text(
                    text = stringResource(R.string.list_settings_delete),
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp
                )
            }
        }

        if (showDeleteConfirm) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .clickable { showDeleteConfirm = false },
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .padding(horizontal = 32.dp)
                        .clickable { } // поглощаем клик чтобы не закрыть скримом
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.delete_confirm_message),
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            TextButton(onClick = { showDeleteConfirm = false }) {
                                Text(
                                    text = stringResource(R.string.delete_confirm_no),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            TextButton(onClick = {
                                showDeleteConfirm = false
                                onDeleteList()
                            }) {
                                Text(
                                    text = stringResource(R.string.delete_confirm_yes),
                                    color = MaterialTheme.colorScheme.error,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        if (showMembers) {
            ListMembersScreen(
                listId = list.id,
                appRepository = appRepository,
                currentUserName = currentUserName,
                currentUserEmail = currentUserEmail,
                onBack = { showMembers = false }
            )
        }

        if (showSort) {
            SortScreen(
                categories = categoriesForSort,
                hiddenCategoryIds = hiddenCategoryIds,
                onSave = { orderedIds, hiddenIds ->
                    scope.launch {
                        sortRepository.saveCategoryOrder(orderedIds)
                        sortRepository.saveHiddenCategories(hiddenIds)
                    }
                    showSort = false
                },
                onBack = { showSort = false }
            )
        }
    }
}

@Composable
private fun SettingsActionTile(
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
            .height(90.dp)
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(26.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = label,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                lineHeight = 16.sp
            )
        }
    }
}
