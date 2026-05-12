package com.lena.kartoshka.ui.screens.listdetail

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items as lazyItems
import androidx.compose.ui.draw.drawBehind
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.ui.window.Dialog
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lena.kartoshka.R
import com.lena.kartoshka.data.AppRepository
import com.lena.kartoshka.data.Item
import com.lena.kartoshka.data.ItemCategory
import com.lena.kartoshka.data.ListMember
import com.lena.kartoshka.data.LoyaltyCard
import com.lena.kartoshka.data.ShoppingList
import com.lena.kartoshka.data.ItemTag
import com.lena.kartoshka.data.itemCategories
import com.lena.kartoshka.data.sort.SortRepository
import com.lena.kartoshka.ui.components.MemberAvatarRow
import com.lena.kartoshka.ui.screens.ideas.IdeasScreen
import com.lena.kartoshka.ui.screens.newlist.NewListScreen
import com.lena.kartoshka.ui.screens.profile.ImageCropScreen
import com.lena.kartoshka.ui.screens.profile.ProfileScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.material.icons.filled.Delete
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import java.io.File

private val ItemCardColor = Color(0xFFFF5449)
private val RecentlyUsedCardColor = Color(0xFF4D662A)
private val DeleteRed = Color(0xFFE53935)

private fun detectCategoryId(name: String): String? =
    itemCategories.firstOrNull { cat -> cat.items.any { it.equals(name, ignoreCase = true) } }?.id

private fun ItemTag.toIcon() = when (this) {
    ItemTag.URGENT      -> Icons.Filled.PriorityHigh
    ItemTag.ON_SALE     -> Icons.Filled.LocalOffer
    ItemTag.IF_CONVENIENT -> Icons.Filled.AccessTime
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListDetailScreen(
    list: ShoppingList,
    items: List<Item>,
    allLists: List<ShoppingList>,
    sortRepository: SortRepository,
    appRepository: AppRepository,
    onBack: () -> Unit,
    isDarkTheme: Boolean = true,
    onThemeChange: (Boolean) -> Unit = {},
    avatarPath: String? = null,
    onAvatarChange: (String?) -> Unit = {},
    userName: String? = null,
    userEmail: String? = null,
    onLogout: () -> Unit = {}
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val statusBarPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val scope = rememberCoroutineScope()

    val activeItems = remember { mutableStateListOf(*items.toTypedArray()) }
    val recentlyUsed = remember { mutableStateListOf<Item>() }

    // Sync items added or updated from outside (e.g., from IdeasScreen via Room)
    LaunchedEffect(items) {
        val existingIds = (activeItems + recentlyUsed).map { it.id }.toSet()
        val newItems = items.filter { it.id !in existingIds }
        if (newItems.isNotEmpty()) activeItems.addAll(0, newItems)
        items.forEach { roomItem ->
            val idx = activeItems.indexOfFirst { it.id == roomItem.id }
            if (idx >= 0 && activeItems[idx] != roomItem) activeItems[idx] = roomItem
        }
    }
    val expandedCategories = remember { mutableStateListOf<String>() }

    var selectedItem by remember { mutableStateOf<Item?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedLoyaltyCard by remember { mutableStateOf<LoyaltyCard?>(null) }
    val cardSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showCardScanner by remember { mutableStateOf(false) }
    var showSortScreen by remember { mutableStateOf(false) }
    var justAddedItem by remember { mutableStateOf<Item?>(null) }
    val addInfoSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showListMenu by remember { mutableStateOf(false) }
    val listMenuSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showListSettings by remember { mutableStateOf(false) }
    var listToEdit by remember { mutableStateOf<ShoppingList?>(null) }
    var showIdeasScreen by remember { mutableStateOf(false) }
    var showProfileScreen by remember { mutableStateOf(false) }
    var showShareSheet by remember { mutableStateOf(false) }
    var members by remember { mutableStateOf<List<ListMember>>(emptyList()) }

    LaunchedEffect(list.id) {
        members = appRepository.getMembers(list.id)
    }
    var showCategoryPicker by remember { mutableStateOf(false) }
    var showMoveItemPicker by remember { mutableStateOf(false) }
    var photoTargetItem by remember { mutableStateOf<Item?>(null) }
    var pendingItemCropUri by remember { mutableStateOf<Uri?>(null) }

    val itemPhotoLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) pendingItemCropUri = uri
    }

    val loyaltyCards by appRepository.observeLoyaltyCards().collectAsState(initial = emptyList())

    val categoryOrderIds by sortRepository.observeCategoryOrder().collectAsState(initial = emptyList())
    val hiddenCategoryIds by sortRepository.observeHiddenCategories().collectAsState(initial = emptySet())

    // Apply saved order and filter out hidden categories
    val sortedCategories = remember(categoryOrderIds, hiddenCategoryIds) {
        val ordered = if (categoryOrderIds.isEmpty()) {
            itemCategories
        } else {
            val positionById = categoryOrderIds.mapIndexed { pos, id -> id to pos }.toMap()
            itemCategories.sortedBy { positionById[it.id] ?: Int.MAX_VALUE }
        }
        ordered.filter { it.id !in hiddenCategoryIds }
    }

    // Full-screen categories list in saved order (without hidden filter) for the sort screen
    val categoriesForSort = remember(categoryOrderIds) {
        if (categoryOrderIds.isEmpty()) {
            itemCategories
        } else {
            val positionById = categoryOrderIds.mapIndexed { pos, id -> id to pos }.toMap()
            itemCategories.sortedBy { positionById[it.id] ?: Int.MAX_VALUE }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
      Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
                .imePadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = statusBarPadding)
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back_to_lists),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Text(
                        text = stringResource(R.string.back_to_lists),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        fontSize = 16.sp
                    )
                }
                Text(
                    text = list.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MemberAvatarRow(
                        members = members,
                        onAddClick = { showShareSheet = true },
                        iconTint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    IconButton(onClick = { showListMenu = true }) {
                        Icon(
                            imageVector = Icons.Filled.MoreVert,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // Sort button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.End
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.clickable { showSortScreen = true }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.SwapVert,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = stringResource(R.string.sort_list_button),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 12.dp)
            ) {
                items(activeItems, key = { it.id }) { item ->
                    ItemCard(
                        item = item,
                        onLongClick = { selectedItem = item },
                        onClick = {
                            activeItems.remove(item)
                            recentlyUsed.add(0, item)
                            scope.launch {
                                appRepository.deleteItem(item.id)
                                appRepository.recordPurchase(item, list.id)
                            }
                        }
                    )
                }
                item(span = { GridItemSpan(3) }) {
                    LoyaltyCardsSection(
                        cards = loyaltyCards,
                        onCardClick = { selectedLoyaltyCard = it },
                        onAddClick = { showCardScanner = true }
                    )
                }
                if (recentlyUsed.isNotEmpty()) {
                    item(span = { GridItemSpan(3) }) {
                        RecentlyUsedSection(
                            items = recentlyUsed,
                            onItemClick = { item ->
                                recentlyUsed.remove(item)
                                activeItems.add(0, item)
                                scope.launch { appRepository.insertItem(item, list.id) }
                            }
                        )
                    }
                }
                item(span = { GridItemSpan(3) }) {
                    CategoriesSection(
                        categories = sortedCategories,
                        expandedIds = expandedCategories,
                        onToggle = { id ->
                            if (expandedCategories.contains(id)) expandedCategories.remove(id)
                            else expandedCategories.add(id)
                        },
                        onItemAdd = { name ->
                            val newItem = Item(
                                id = "added_${name}_${System.currentTimeMillis()}",
                                name = name
                            )
                            if (activeItems.none { it.name == name }) {
                                activeItems.add(0, newItem)
                                scope.launch { appRepository.insertItem(newItem, list.id) }
                            }
                        }
                    )
                }
            }

            AddItemRow(onAdd = { name ->
                val newItem = Item(
                    id = "added_${name}_${System.currentTimeMillis()}",
                    name = name
                )
                if (activeItems.none { it.name == name }) {
                    activeItems.add(0, newItem)
                    scope.launch { appRepository.insertItem(newItem, list.id) }
                }
                justAddedItem = newItem
            })
        }

        if (showSortScreen) {
            SortScreen(
                categories = categoriesForSort,
                hiddenCategoryIds = hiddenCategoryIds,
                onSave = { orderedIds, hiddenIds ->
                    scope.launch {
                        sortRepository.saveCategoryOrder(orderedIds)
                        sortRepository.saveHiddenCategories(hiddenIds)
                    }
                    showSortScreen = false
                },
                onBack = { showSortScreen = false }
            )
        }

        if (showListSettings) {
            ListSettingsScreen(
                list = list,
                sortRepository = sortRepository,
                onBack = { showListSettings = false },
                onDeleteList = {
                    scope.launch { appRepository.deleteList(list.id) }
                    onBack()
                },
                onEditNameAndImage = {
                    showListSettings = false
                    listToEdit = list
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

        if (showIdeasScreen) {
            IdeasScreen(
                initialListId = list.id,
                lists = allLists,
                onAddIngredients = { listId, newItems ->
                    scope.launch { appRepository.insertItems(listId, newItems) }
                },
                onClose = { showIdeasScreen = false }
            )
        }

        if (showProfileScreen) {
            ProfileScreen(
                allLists = allLists,
                currentListId = list.id,
                sortRepository = sortRepository,
                appRepository = appRepository,
                isDarkTheme = isDarkTheme,
                onThemeChange = onThemeChange,
                avatarPath = avatarPath,
                onAvatarChange = onAvatarChange,
                onClose = { showProfileScreen = false },
                onDeleteCurrentList = {
                    scope.launch { appRepository.deleteList(list.id) }
                    onBack()
                },
                onLogout = onLogout,
                userName = userName,
                userEmail = userEmail
            )
        }
      }

      BottomNavBar(
          activeTab = when {
              showIdeasScreen -> 1
              showProfileScreen -> 2
              else -> 0
          },
          onIdeasClick = { showIdeasScreen = true; showProfileScreen = false },
          onListsClick = { showIdeasScreen = false; showProfileScreen = false },
          onProfileClick = { showProfileScreen = true; showIdeasScreen = false }
      )
    }

    if (selectedItem != null) {
        ModalBottomSheet(
            onDismissRequest = { selectedItem = null },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ) {
            ItemDetailSheet(
                item = selectedItem!!,
                onChangeCategoryClick = { showCategoryPicker = true },
                onMoveItemClick = { showMoveItemPicker = true },
                onAddPhotoClick = {
                    photoTargetItem = selectedItem
                    selectedItem = null
                    itemPhotoLauncher.launch("image/*")
                },
                onDeletePhoto = {
                    val current = selectedItem
                    if (current != null) {
                        current.imagePath?.let { File(it).delete() }
                        val updated = current.copy(imagePath = null)
                        val aIdx = activeItems.indexOfFirst { it.id == current.id }
                        if (aIdx >= 0) activeItems[aIdx] = updated
                        val rIdx = recentlyUsed.indexOfFirst { it.id == current.id }
                        if (rIdx >= 0) recentlyUsed[rIdx] = updated
                        selectedItem = updated
                        scope.launch { appRepository.updateItem(updated, list.id) }
                    }
                },
                onDeleteItem = {
                    val current = selectedItem
                    if (current != null) {
                        activeItems.removeIf { it.id == current.id }
                        recentlyUsed.removeIf { it.id == current.id }
                        scope.launch { appRepository.deleteItem(current.id) }
                    }
                    selectedItem = null
                },
                onTagToggle = { tag ->
                    val current = selectedItem
                    if (current != null) {
                        val newTags = if (tag in current.tags) current.tags - tag
                                      else current.tags + tag
                        val updated = current.copy(tags = newTags)
                        val aIdx = activeItems.indexOfFirst { it.id == current.id }
                        if (aIdx >= 0) activeItems[aIdx] = updated
                        val rIdx = recentlyUsed.indexOfFirst { it.id == current.id }
                        if (rIdx >= 0) recentlyUsed[rIdx] = updated
                        selectedItem = updated
                        scope.launch { appRepository.updateItem(updated, list.id) }
                    }
                },
                onDone = { note ->
                    val current = selectedItem
                    if (current != null) {
                        val updated = current.copy(note = note)
                        val aIdx = activeItems.indexOfFirst { it.id == current.id }
                        if (aIdx >= 0) activeItems[aIdx] = updated
                        val rIdx = recentlyUsed.indexOfFirst { it.id == current.id }
                        if (rIdx >= 0) recentlyUsed[rIdx] = updated
                        scope.launch { appRepository.updateItem(updated, list.id) }
                    }
                    selectedItem = null
                }
            )
        }
    }

    if (showCategoryPicker && selectedItem != null) {
        CategoryPickerSheet(
            itemName = selectedItem!!.name,
            currentCategoryId = selectedItem!!.categoryId ?: detectCategoryId(selectedItem!!.name),
            onCategorySelected = { categoryId ->
                val current = selectedItem
                if (current != null) {
                    val updated = current.copy(categoryId = categoryId)
                    val aIdx = activeItems.indexOfFirst { it.id == current.id }
                    if (aIdx >= 0) activeItems[aIdx] = updated
                    val rIdx = recentlyUsed.indexOfFirst { it.id == current.id }
                    if (rIdx >= 0) recentlyUsed[rIdx] = updated
                    selectedItem = updated
                    scope.launch { appRepository.updateItem(updated, list.id) }
                }
                showCategoryPicker = false
            },
            onDismiss = { showCategoryPicker = false }
        )
    }

    if (showMoveItemPicker && selectedItem != null) {
        MoveItemSheet(
            currentListId = list.id,
            allLists = allLists,
            onListSelected = { targetList ->
                val current = selectedItem
                if (current != null && targetList.id != list.id) {
                    activeItems.removeIf { it.id == current.id }
                    recentlyUsed.removeIf { it.id == current.id }
                    scope.launch { appRepository.moveItem(current, targetList.id) }
                }
                showMoveItemPicker = false
                selectedItem = null
            },
            onDismiss = { showMoveItemPicker = false }
        )
    }

    pendingItemCropUri?.let { uri ->
        ImageCropScreen(
            imageUri = uri,
            aspectRatio = 4f / 3f,
            onConfirm = { croppedBitmap ->
                scope.launch(Dispatchers.IO) {
                    photoTargetItem?.let { item ->
                        val file = File(context.filesDir, "item_${item.id}.jpg")
                        file.outputStream().use { out ->
                            croppedBitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
                        }
                        val updated = item.copy(imagePath = file.absolutePath)
                        val aIdx = activeItems.indexOfFirst { it.id == item.id }
                        withContext(Dispatchers.Main) {
                            if (aIdx >= 0) activeItems[aIdx] = updated
                            val rIdx = recentlyUsed.indexOfFirst { it.id == item.id }
                            if (rIdx >= 0) recentlyUsed[rIdx] = updated
                            pendingItemCropUri = null
                            photoTargetItem = null
                        }
                        appRepository.updateItem(updated, list.id)
                    }
                }
            },
            onDismiss = {
                pendingItemCropUri = null
                photoTargetItem = null
            }
        )
    }

    if (justAddedItem != null) {
        ModalBottomSheet(
            onDismissRequest = { justAddedItem = null },
            sheetState = addInfoSheetState,
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ) {
            AddItemInfoSheet(
                item = justAddedItem!!,
                onTagToggle = { tag ->
                    val current = justAddedItem
                    if (current != null) {
                        val idx = activeItems.indexOfFirst { it.id == current.id }
                        if (idx >= 0) {
                            val updated = activeItems[idx].let { existing ->
                                existing.copy(
                                    tags = if (tag in existing.tags) existing.tags - tag
                                           else existing.tags + tag
                                )
                            }
                            activeItems[idx] = updated
                            justAddedItem = updated
                            scope.launch { appRepository.updateItem(updated, list.id) }
                        }
                    }
                },
                onNotesClick = {
                    val item = justAddedItem
                    justAddedItem = null
                    selectedItem = item
                },
                onNextItemAdd = { name ->
                    val newItem = Item(
                        id = "added_${name}_${System.currentTimeMillis()}",
                        name = name
                    )
                    if (activeItems.none { it.name == name }) {
                        activeItems.add(0, newItem)
                        scope.launch { appRepository.insertItem(newItem, list.id) }
                    }
                    justAddedItem = newItem
                },
                onCancel = { justAddedItem = null }
            )
        }
    }

    if (showListMenu) {
        ModalBottomSheet(
            onDismissRequest = { showListMenu = false },
            sheetState = listMenuSheetState,
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ) {
            ListMenuSheet(
                onSendClick = {
                    showListMenu = false
                    showShareSheet = true
                },
                onPrintClick = {
                    showListMenu = false
                    printList(context, list.name, activeItems.toList())
                },
                onRecommendClick = {
                    showListMenu = false
                    recommendApp(context)
                },
                onCardsClick = {
                    showListMenu = false
                    showCardScanner = true
                },
                onSettingsClick = {
                    showListMenu = false
                    showListSettings = true
                }
            )
        }
    }

    if (showCardScanner) {
        CardScannerFlow(
            onSaved = { card ->
                scope.launch { appRepository.insertLoyaltyCard(card) }
                showCardScanner = false
            },
            onDismiss = { showCardScanner = false }
        )
    }

    selectedLoyaltyCard?.let { card ->
        ModalBottomSheet(
            onDismissRequest = { selectedLoyaltyCard = null },
            sheetState = cardSheetState,
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ) {
            CardDisplaySheet(
                card = card,
                onDelete = {
                    scope.launch { appRepository.deleteLoyaltyCard(card.id) }
                    selectedLoyaltyCard = null
                }
            )
        }
    }

    if (showShareSheet) {
        ShareListSheet(
            listId = list.id,
            members = members,
            appRepository = appRepository,
            onDismiss = {
                showShareSheet = false
                scope.launch { members = appRepository.getMembers(list.id) }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ItemCard(
    item: Item,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    onLongClick: () -> Unit = {},
    cardColor: Color = ItemCardColor
) {
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(16.dp))
            .background(cardColor)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(10.dp)
    ) {
        Text(
            text = item.name.first().uppercaseChar().toString(),
            fontSize = 52.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White.copy(alpha = 0.35f),
            modifier = Modifier.align(Alignment.Center)
        )
        if (item.tags.isNotEmpty()) {
            Row(
                modifier = Modifier.align(Alignment.TopCenter),
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                item.tags.sortedBy { it.ordinal }.forEach { tag ->
                    Icon(
                        imageVector = tag.toIcon(),
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.9f),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
        if (item.imagePath != null) {
            Icon(
                imageVector = Icons.Filled.CameraAlt,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.85f),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(14.dp)
            )
        }
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = item.name,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                lineHeight = 16.sp,
                maxLines = 2,
                textAlign = TextAlign.Center
            )
            if (item.note.isNotEmpty()) {
                Text(
                    text = item.note,
                    fontSize = 10.sp,
                    color = Color.White.copy(alpha = 0.75f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ItemDetailSheet(item: Item, onTagToggle: (ItemTag) -> Unit = {}, onChangeCategoryClick: () -> Unit = {}, onMoveItemClick: () -> Unit = {}, onAddPhotoClick: () -> Unit = {}, onDeletePhoto: () -> Unit = {}, onDeleteItem: () -> Unit = {}, onDone: (note: String) -> Unit) {
    var note by remember(item.id) { mutableStateOf(item.note) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    val noteHint = stringResource(R.string.item_note_hint)

    var itemPhotoBitmap by remember(item.imagePath) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(item.imagePath) {
        itemPhotoBitmap = withContext(Dispatchers.IO) {
            item.imagePath?.let { path ->
                runCatching { BitmapFactory.decodeFile(path)?.asImageBitmap() }.getOrNull()
            }
        }
    }

    Box(modifier = Modifier.fillMaxWidth()) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = item.name,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = { onDone(note) }) {
                Text(
                    text = stringResource(R.string.done),
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Medium,
                    fontSize = 16.sp
                )
            }
        }

        Surface(
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            BasicTextField(
                value = note,
                onValueChange = { note = it },
                textStyle = TextStyle(
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 16.sp
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.onSurface),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                decorationBox = { inner ->
                    if (note.isEmpty()) {
                        Text(
                            text = noteHint,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                            fontSize = 16.sp
                        )
                    }
                    inner()
                }
            )
        }

        // Photo section
        itemPhotoBitmap?.let { bmp ->
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.item_photo_section),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                ) {
                    Image(
                        bitmap = bmp,
                        contentDescription = null,
                        modifier = Modifier.fillMaxWidth(),
                        contentScale = ContentScale.FillWidth
                    )
                    IconButton(
                        onClick = onDeletePhoto,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .size(36.dp)
                            .background(Color.Black.copy(alpha = 0.45f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        Text(
            text = stringResource(R.string.item_details_about, item.name),
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("1", "2", "3", "4", "5", "8", "10", "1 кг").forEach { label ->
                QuickChip(label = label, onClick = { note = label })
            }
        }

        // Tag chips with icons (separate from quantity chips)
        val tagInfos = listOf(
            Triple(ItemTag.URGENT,         Icons.Filled.PriorityHigh, stringResource(R.string.tag_urgent)),
            Triple(ItemTag.ON_SALE,        Icons.Filled.LocalOffer,   stringResource(R.string.tag_sale)),
            Triple(ItemTag.IF_CONVENIENT,  Icons.Filled.AccessTime,   stringResource(R.string.tag_if_convenient))
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            tagInfos.forEach { (tag, icon, label) ->
                val isSelected = tag in item.tags
                Surface(
                    color = if (isSelected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.clickable { onTagToggle(tag) }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                   else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = label,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                    else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.settings),
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SettingsButton(
                    icon = Icons.Filled.Edit,
                    label = stringResource(R.string.change_icon),
                    modifier = Modifier.weight(1f)
                )
                SettingsButton(
                    icon = Icons.Filled.CameraAlt,
                    label = stringResource(R.string.add_photo),
                    modifier = Modifier.weight(1f),
                    onClick = onAddPhotoClick
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SettingsButton(
                    icon = Icons.Filled.Category,
                    label = stringResource(R.string.change_category),
                    modifier = Modifier.weight(1f),
                    onClick = onChangeCategoryClick
                )
                SettingsButton(
                    icon = Icons.Filled.SwapHoriz,
                    label = stringResource(R.string.move_item),
                    modifier = Modifier.weight(1f),
                    onClick = onMoveItemClick
                )
            }
        }

        Button(
            onClick = { showDeleteConfirm = true },
            colors = ButtonDefaults.buttonColors(containerColor = DeleteRed),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = stringResource(R.string.delete_item),
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp
            )
        }
    }

    if (showDeleteConfirm) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(Color.Black.copy(alpha = 0.5f))
                .clickable { showDeleteConfirm = false },
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .clickable { }
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    Text(
                        text = stringResource(R.string.delete_item_confirm),
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TextButton(onClick = { showDeleteConfirm = false }) {
                            Text(
                                text = stringResource(R.string.delete_item_no),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        TextButton(onClick = {
                            showDeleteConfirm = false
                            onDeleteItem()
                        }) {
                            Text(
                                text = stringResource(R.string.delete_item_yes),
                                color = DeleteRed
                            )
                        }
                    }
                }
            }
        }
    }
    } // Box
}

@Composable
private fun QuickChip(label: String, onClick: () -> Unit = {}) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
        )
    }
}

@Composable
private fun SettingsButton(icon: ImageVector, label: String, modifier: Modifier = Modifier, onClick: () -> Unit = {}) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(14.dp),
        modifier = modifier.height(90.dp).clickable(onClick = onClick)
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
                fontWeight = FontWeight.Medium
            )
        }
    }
}

private val CategoryItemColor = Color(0xFF5B7178)

@Composable
private fun CategoriesSection(
    categories: List<ItemCategory>,
    expandedIds: List<String>,
    onToggle: (String) -> Unit,
    onItemAdd: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 4.dp)
    ) {
        categories.forEach { category ->
            val isExpanded = expandedIds.contains(category.id)
            CategoryRow(
                category = category,
                isExpanded = isExpanded,
                onToggle = { onToggle(category.id) },
                onItemAdd = onItemAdd
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CategoryRow(
    category: ItemCategory,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    onItemAdd: (String) -> Unit
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                .combinedClickable(onClick = onToggle)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = category.name,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = if (isExpanded) Icons.Filled.KeyboardArrowDown
                              else Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
        }
        if (isExpanded) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    horizontal = 4.dp, vertical = 10.dp
                )
            ) {
                lazyItems(category.items) { itemName ->
                    CategoryItemCard(name = itemName, onAdd = { onItemAdd(itemName) })
                }
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CategoryItemCard(name: String, onAdd: () -> Unit) {
    Box(
        modifier = Modifier
            .size(width = 100.dp, height = 80.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(CategoryItemColor)
            .combinedClickable(onClick = onAdd)
            .padding(10.dp)
    ) {
        Text(
            text = name.first().uppercaseChar().toString(),
            fontSize = 36.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White.copy(alpha = 0.3f),
            modifier = Modifier.align(Alignment.Center)
        )
        Text(
            text = name,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
            lineHeight = 14.sp,
            maxLines = 2,
            modifier = Modifier.align(Alignment.BottomStart)
        )
    }
}

@Composable
private fun RecentlyUsedSection(items: List<Item>, onItemClick: (Item) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 4.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.recently_used),
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = Icons.Filled.MoreVert,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                modifier = Modifier.size(20.dp)
            )
        }
        items.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { item ->
                    ItemCard(
                        item = item,
                        cardColor = RecentlyUsedCardColor,
                        modifier = Modifier.weight(1f),
                        onClick = { onItemClick(item) }
                    )
                }
                repeat(3 - row.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun LoyaltyCardsSection(
    cards: List<LoyaltyCard>,
    onCardClick: (LoyaltyCard) -> Unit = {},
    onAddClick: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 4.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.your_cards),
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = Icons.Filled.MoreVert,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                modifier = Modifier.size(20.dp)
            )
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            lazyItems(cards, key = { it.id }) { card ->
                LoyaltyCardItem(card = card, onClick = { onCardClick(card) })
            }
            item {
                AddLoyaltyCard(onClick = onAddClick)
            }
        }
    }
}

@Composable
private fun LoyaltyCardItem(card: LoyaltyCard, onClick: () -> Unit = {}) {
    Box(
        modifier = Modifier
            .width(160.dp)
            .height(100.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .background(Color(card.color)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = card.name,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}

@Composable
private fun AddLoyaltyCard(onClick: () -> Unit = {}) {
    val borderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f)
    val textColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
    Box(
        modifier = Modifier
            .width(160.dp)
            .height(100.dp)
            .clickable { onClick() }
            .drawBehind {
                drawRoundRect(
                    color = borderColor,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(
                        width = 2.dp.toPx(),
                        pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(
                            floatArrayOf(14f, 10f), 0f
                        )
                    ),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(16.dp.toPx())
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = stringResource(R.string.add_card),
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = textColor
        )
    }
}

@Composable
private fun AddItemRow(modifier: Modifier = Modifier, onAdd: (String) -> Unit = {}) {
    var text by remember { mutableStateOf("") }
    val hint = stringResource(R.string.add_item_hint)

    fun submit() {
        val name = text.trim()
        if (name.isNotEmpty()) {
            onAdd(name)
            text = ""
        }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.weight(1f)
        ) {
            BasicTextField(
                value = text,
                onValueChange = { text = it },
                textStyle = TextStyle(
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 16.sp
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.onSurface),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { submit() }),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                decorationBox = { inner ->
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.weight(1f)) {
                            if (text.isEmpty()) {
                                Text(
                                    text = hint,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                    fontSize = 16.sp
                                )
                            }
                            inner()
                        }
                        if (text.isNotEmpty()) {
                            Icon(
                                imageVector = Icons.Filled.Clear,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                modifier = Modifier
                                    .padding(start = 8.dp)
                                    .size(18.dp)
                                    .clickable { text = "" }
                            )
                        }
                    }
                }
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Box(
            modifier = Modifier
                .size(52.dp)
                .background(MaterialTheme.colorScheme.primary, CircleShape)
                .clickable { submit() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

@Composable
private fun BottomNavBar(
    activeTab: Int = 0,
    onIdeasClick: () -> Unit = {},
    onListsClick: () -> Unit = {},
    onProfileClick: () -> Unit = {}
) {
    val navBarPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    Column {
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceContainerLow)
                .padding(bottom = navBarPadding)
                .padding(vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            NavItem(icon = Icons.Filled.Checklist, label = stringResource(R.string.nav_lists),   selected = activeTab == 0, onClick = onListsClick)
            NavItem(icon = Icons.Filled.Style,     label = stringResource(R.string.nav_ideas),   selected = activeTab == 1, onClick = onIdeasClick)
            NavItem(icon = Icons.Filled.Person,    label = stringResource(R.string.nav_profile), selected = activeTab == 2, onClick = onProfileClick)
        }
    }
}

@Composable
private fun NavItem(icon: ImageVector, label: String, selected: Boolean, onClick: () -> Unit = {}) {
    val tint = if (selected) MaterialTheme.colorScheme.onSurface
               else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
    Column(
        modifier = Modifier
            .clickable(enabled = !selected, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Icon(imageVector = icon, contentDescription = label, tint = tint, modifier = Modifier.size(24.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = tint,
            textAlign = TextAlign.Center
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AddItemInfoSheet(
    item: Item,
    onTagToggle: (ItemTag) -> Unit,
    onNotesClick: () -> Unit,
    onNextItemAdd: (String) -> Unit,
    onCancel: () -> Unit
) {
    var nextText by remember { mutableStateOf("") }
    val nextHint = stringResource(R.string.next_item_hint)

    fun submitNext() {
        val name = nextText.trim()
        if (name.isNotEmpty()) {
            onNextItemAdd(name)
            nextText = ""
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .imePadding()
            .padding(horizontal = 16.dp)
            .padding(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Item card centred
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(ItemCardColor)
                    .padding(12.dp)
            ) {
                Text(
                    text = item.name.first().uppercaseChar().toString(),
                    fontSize = 52.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.35f),
                    modifier = Modifier.align(Alignment.Center)
                )
                Text(
                    text = item.name,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    lineHeight = 16.sp,
                    maxLines = 2,
                    modifier = Modifier.align(Alignment.BottomStart)
                )
            }
        }

        Text(
            text = stringResource(R.string.item_details_about, item.name),
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )

        // Tag chips with icons
        val tagInfos = listOf(
            Triple(ItemTag.URGENT,         Icons.Filled.PriorityHigh, stringResource(R.string.tag_urgent)),
            Triple(ItemTag.ON_SALE,        Icons.Filled.LocalOffer,   stringResource(R.string.tag_sale)),
            Triple(ItemTag.IF_CONVENIENT,  Icons.Filled.AccessTime,   stringResource(R.string.tag_if_convenient))
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            tagInfos.forEach { (tag, icon, label) ->
                val isSelected = tag in item.tags
                Surface(
                    color = if (isSelected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.clickable { onTagToggle(tag) }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                   else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = label,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                    else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        // Notes button
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onNotesClick() }
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.Edit,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = stringResource(R.string.notes),
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // "Next item" input + Cancel
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.weight(1f)
            ) {
                BasicTextField(
                    value = nextText,
                    onValueChange = { nextText = it },
                    textStyle = TextStyle(
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 16.sp
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.onSurface),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { submitNext() }),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    decorationBox = { inner ->
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(modifier = Modifier.weight(1f)) {
                                if (nextText.isEmpty()) {
                                    Text(
                                        text = nextHint,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                        fontSize = 16.sp
                                    )
                                }
                                inner()
                            }
                            if (nextText.isNotEmpty()) {
                                Icon(
                                    imageVector = Icons.Filled.Clear,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                    modifier = Modifier
                                        .padding(start = 8.dp)
                                        .size(18.dp)
                                        .clickable { nextText = "" }
                                )
                            }
                        }
                    }
                )
            }
            TextButton(onClick = onCancel) {
                Text(
                    text = stringResource(R.string.cancel),
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Medium,
                    fontSize = 16.sp
                )
            }
        }
    }
}

private fun printList(context: Context, listName: String, items: List<Item>) {
    val html = buildString {
        append("<html><body style='font-family:sans-serif;padding:24px'>")
        append("<h2 style='margin-bottom:16px'>$listName</h2><ul style='list-style:none;padding:0'>")
        items.forEach {
            append("<li style='font-size:16px;padding:6px 0;border-bottom:1px solid #eee'>${it.name}")
            if (it.note.isNotBlank()) append(" <span style='color:#888;font-size:13px'>${it.note}</span>")
            append("</li>")
        }
        append("</ul></body></html>")
    }
    val webView = WebView(context)
    webView.webViewClient = object : WebViewClient() {
        override fun onPageFinished(view: WebView, url: String) {
            val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
            val adapter = view.createPrintDocumentAdapter(listName)
            printManager.print(listName, adapter, PrintAttributes.Builder().build())
        }
    }
    webView.loadDataWithBaseURL(null, html, "text/HTML", "UTF-8", null)
}

private fun recommendApp(context: Context) {
    val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(
            android.content.Intent.EXTRA_TEXT,
            "Попробуй Супер списки — удобное приложение для списков покупок 🛒\nhttps://apps.rustore.ru/app/com.lena.kartoshka"
        )
    }
    context.startActivity(android.content.Intent.createChooser(intent, null))
}

@Composable
private fun ListMenuSheet(
    onSendClick: () -> Unit = {},
    onPrintClick: () -> Unit = {},
    onRecommendClick: () -> Unit = {},
    onCardsClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {}
) {
    val navBarPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp + navBarPadding)
    ) {
        Text(
            text = stringResource(R.string.list_menu_title),
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)
        )

        ListMenuItem(icon = Icons.Filled.Share,      label = stringResource(R.string.list_menu_send),      onClick = onSendClick)
        ListMenuItem(icon = Icons.Filled.Print,       label = stringResource(R.string.list_menu_print),     onClick = onPrintClick)

        HorizontalDivider(
            modifier = Modifier.padding(vertical = 8.dp),
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
        )

        ListMenuItem(icon = Icons.Filled.Favorite, label = stringResource(R.string.list_menu_recommend), onClick = onRecommendClick)
        ListMenuItem(icon = Icons.Filled.Settings, label = stringResource(R.string.list_menu_settings),  onClick = onSettingsClick)

        HorizontalDivider(
            modifier = Modifier.padding(vertical = 8.dp),
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
        )

        ListMenuItem(icon = Icons.Filled.CreditCard, label = stringResource(R.string.list_menu_cards), onClick = onCardsClick)
    }
}

@Composable
private fun ListMenuItem(icon: ImageVector, label: String, onClick: () -> Unit = {}) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(22.dp)
        )
        Text(
            text = label,
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryPickerSheet(
    itemName: String,
    currentCategoryId: String?,
    onCategorySelected: (String?) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Filled.Clear,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                Text(
                    text = itemName,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
            }
            Text(
                text = stringResource(R.string.category_picker_description),
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 32.dp)
            ) {
                itemCategories.forEach { category ->
                    val isSelected = category.id == currentCategoryId
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onCategorySelected(category.id) }
                            .padding(horizontal = 20.dp, vertical = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = category.name,
                            fontSize = 16.sp,
                            color = if (isSelected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurface
                        )
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Filled.Checklist,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onCategorySelected(null) }
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.category_own_items),
                        fontSize = 16.sp,
                        color = if (currentCategoryId == null) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                    )
                    if (currentCategoryId == null) {
                        Icon(
                            imageVector = Icons.Filled.Checklist,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MoveItemSheet(
    currentListId: String,
    allLists: List<ShoppingList>,
    onListSelected: (ShoppingList) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Filled.Clear,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                Text(
                    text = stringResource(R.string.move_item_title),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
            }
            Text(
                text = stringResource(R.string.move_item_description),
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 32.dp)
            ) {
                allLists.forEach { shoppingList ->
                    val isCurrent = shoppingList.id == currentListId
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onListSelected(shoppingList) }
                            .padding(horizontal = 20.dp, vertical = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = shoppingList.name,
                            fontSize = 16.sp,
                            color = if (isCurrent) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurface
                        )
                        if (isCurrent) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                }
            }
        }
    }
}
