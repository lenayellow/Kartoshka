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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lena.kartoshka.R
import com.lena.kartoshka.data.Item
import com.lena.kartoshka.data.ItemCategory
import com.lena.kartoshka.data.LoyaltyCard
import com.lena.kartoshka.data.ShoppingList
import com.lena.kartoshka.data.itemCategories
import com.lena.kartoshka.data.sampleLoyaltyCards
import com.lena.kartoshka.data.sort.SortRepository
import kotlinx.coroutines.launch

private val ItemCardColor = Color(0xFFE07870)
private val RecentlyUsedCardColor = Color(0xFF4A8579)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListDetailScreen(
    list: ShoppingList,
    items: List<Item>,
    sortRepository: SortRepository,
    onBack: () -> Unit
) {
    val statusBarPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navBarPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val scope = rememberCoroutineScope()

    val activeItems = remember { mutableStateListOf(*items.toTypedArray()) }
    val recentlyUsed = remember { mutableStateListOf<Item>() }
    val expandedCategories = remember { mutableStateListOf<String>() }

    var selectedItem by remember { mutableStateOf<Item?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showSortScreen by remember { mutableStateOf(false) }

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

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
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
                Spacer(modifier = Modifier.weight(1f))
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
                        }
                    )
                }
                item(span = { GridItemSpan(3) }) {
                    LoyaltyCardsSection(cards = sampleLoyaltyCards)
                }
                if (recentlyUsed.isNotEmpty()) {
                    item(span = { GridItemSpan(3) }) {
                        RecentlyUsedSection(items = recentlyUsed)
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
                            }
                        }
                    )
                }
            }

            AddItemRow(modifier = Modifier.padding(bottom = navBarPadding))
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
    }

    if (selectedItem != null) {
        ModalBottomSheet(
            onDismissRequest = { selectedItem = null },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ) {
            ItemDetailSheet(
                item = selectedItem!!,
                onDone = { selectedItem = null }
            )
        }
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ItemDetailSheet(item: Item, onDone: () -> Unit) {
    var note by remember { mutableStateOf("") }
    val noteHint = stringResource(R.string.item_note_hint)

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
            TextButton(onClick = onDone) {
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
                QuickChip(label = label)
            }
            QuickChip(label = stringResource(R.string.tag_urgent))
            QuickChip(label = stringResource(R.string.tag_sale))
            QuickChip(label = stringResource(R.string.tag_if_convenient))
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
                    modifier = Modifier.weight(1f)
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SettingsButton(
                    icon = Icons.Filled.Category,
                    label = stringResource(R.string.change_category),
                    modifier = Modifier.weight(1f)
                )
                SettingsButton(
                    icon = Icons.Filled.SwapHoriz,
                    label = stringResource(R.string.move_item),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun QuickChip(label: String) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(10.dp)
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
private fun SettingsButton(icon: ImageVector, label: String, modifier: Modifier = Modifier) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(14.dp),
        modifier = modifier.height(90.dp)
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
private fun RecentlyUsedSection(items: List<Item>) {
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
                        modifier = Modifier.weight(1f)
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
private fun LoyaltyCardsSection(cards: List<LoyaltyCard>) {
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
                LoyaltyCardItem(card = card)
            }
            item {
                AddLoyaltyCard()
            }
        }
    }
}

@Composable
private fun LoyaltyCardItem(card: LoyaltyCard) {
    Box(
        modifier = Modifier
            .width(160.dp)
            .height(100.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(card.color),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = card.storeName,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}

@Composable
private fun AddLoyaltyCard() {
    val borderColor = Color.White.copy(alpha = 0.25f)
    Box(
        modifier = Modifier
            .width(160.dp)
            .height(100.dp)
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
            color = Color.White.copy(alpha = 0.5f)
        )
    }
}

@Composable
private fun AddItemRow(modifier: Modifier = Modifier) {
    var text by remember { mutableStateOf("") }
    val hint = stringResource(R.string.add_item_hint)

    Row(
        modifier = modifier
            .fillMaxWidth()
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
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                decorationBox = { inner ->
                    if (text.isEmpty()) {
                        Text(
                            text = hint,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                            fontSize = 16.sp
                        )
                    }
                    inner()
                }
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Box(
            modifier = Modifier
                .size(52.dp)
                .background(ItemCardColor, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}
