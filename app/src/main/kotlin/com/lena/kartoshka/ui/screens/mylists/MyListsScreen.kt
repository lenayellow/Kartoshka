package com.lena.kartoshka.ui.screens.mylists

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lena.kartoshka.R
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import com.lena.kartoshka.data.AppRepository
import com.lena.kartoshka.data.ListMember
import com.lena.kartoshka.data.ShoppingList
import com.lena.kartoshka.data.Suggestion
import com.lena.kartoshka.data.sampleSuggestions
import com.lena.kartoshka.data.sort.SortRepository
import com.lena.kartoshka.ui.components.MemberAvatarRow
import com.lena.kartoshka.ui.screens.listdetail.ShareListSheet
import kotlinx.coroutines.launch
import com.lena.kartoshka.ui.screens.listdetail.ListSettingsScreen
import com.lena.kartoshka.ui.screens.newlist.NewListScreen
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@Composable
fun MyListsScreen(
    modifier: Modifier = Modifier,
    lists: List<ShoppingList>,
    sortRepository: SortRepository,
    appRepository: AppRepository,
    onListClick: (String) -> Unit = {},
    onNewListClick: () -> Unit = {},
    onSuggestionClick: (String) -> Unit = {}
) {
    val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val scope = rememberCoroutineScope()
    var isEditMode by remember { mutableStateOf(false) }
    var listForSettings by remember { mutableStateOf<ShoppingList?>(null) }
    var listToEdit by remember { mutableStateOf<ShoppingList?>(null) }
    var membersMap by remember { mutableStateOf(mapOf<String, List<ListMember>>()) }
    var shareListId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(lists) {
        val map = mutableMapOf<String, List<ListMember>>()
        lists.forEach { list -> map[list.id] = appRepository.getMembers(list.id) }
        membersMap = map
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        Header(isEditMode = isEditMode, onEditToggle = { isEditMode = !isEditMode })

        if (isEditMode) {
            EditableList(
                lists = lists,
                onSettingsClick = { list -> listForSettings = list },
                navBarBottom = navBarBottom
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 8.dp,
                    bottom = 8.dp + navBarBottom
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(lists, key = { it.id }) { list ->
                    MyListCard(
                        list = list,
                        members = membersMap[list.id] ?: emptyList(),
                        onClick = { onListClick(list.id) },
                        onShareClick = { shareListId = list.id }
                    )
                }
                item {
                    NewListCard(onClick = onNewListClick)
                }
                item {
                    SuggestionsSection(suggestions = sampleSuggestions, onSuggestionClick = onSuggestionClick)
                }
            }
        }
    }

    listForSettings?.let { list ->
        ListSettingsScreen(
            list = list,
            sortRepository = sortRepository,
            appRepository = appRepository,
            onBack = { listForSettings = null },
            onDeleteList = {
                scope.launch { appRepository.deleteList(list.id) }
                listForSettings = null
            },
            onEditNameAndImage = {
                listToEdit = list
                listForSettings = null
            }
        )
    }

    listToEdit?.let { list ->
        NewListScreen(
            initialName = list.name,
            initialColor = list.color,
            editingListId = list.id,
            onSaved = { updatedList ->
                scope.launch { appRepository.updateList(updatedList) }
                listToEdit = null
            }
        )
    }

    shareListId?.let { listId ->
        ShareListSheet(
            listId = listId,
            members = membersMap[listId] ?: emptyList(),
            appRepository = appRepository,
            onDismiss = { shareListId = null }
        )
    }
}

@Composable
private fun Header(isEditMode: Boolean, onEditToggle: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(R.string.my_lists_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        TextButton(onClick = onEditToggle) {
            Text(
                text = if (isEditMode) stringResource(R.string.done) else stringResource(R.string.edit),
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun EditableList(
    lists: List<ShoppingList>,
    onSettingsClick: (ShoppingList) -> Unit,
    navBarBottom: Dp
) {
    val localLists = remember(lists) { lists.toMutableList() }
    val lazyListState = rememberLazyListState()
    val reorderState = rememberReorderableLazyListState(
        lazyListState = lazyListState,
        onMove = { from, to ->
            localLists.apply { add(to.index, removeAt(from.index)) }
        }
    )

    LazyColumn(
        state = lazyListState,
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 8.dp,
            bottom = 8.dp + navBarBottom
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(localLists, key = { it.id }) { list ->
            ReorderableItem(reorderState, key = list.id) { isDragging ->
                val elevation by animateDpAsState(
                    targetValue = if (isDragging) 8.dp else 0.dp,
                    label = "drag_elevation"
                )
                Surface(
                    color = list.color,
                    shape = RoundedCornerShape(20.dp),
                    shadowElevation = elevation,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 120.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = list.name,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                IconButton(
                                    onClick = { onSettingsClick(list) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Settings,
                                        contentDescription = null,
                                        tint = Color.White.copy(alpha = 0.8f),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            ItemCountBadge(count = list.itemCount)
                        }
                        Icon(
                            imageVector = Icons.Filled.DragHandle,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.5f),
                            modifier = Modifier
                                .size(32.dp)
                                .draggableHandle()
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MyListCard(
    list: ShoppingList,
    members: List<ListMember> = emptyList(),
    onClick: () -> Unit = {},
    onShareClick: () -> Unit = {}
) {
    Surface(
        color = list.color,
        shape = RoundedCornerShape(20.dp),
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 140.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = list.name,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    ItemCountBadge(count = list.itemCount)
                }
                Spacer(modifier = Modifier.width(16.dp))
                ArrowCircle()
            }
            Spacer(modifier = Modifier.height(12.dp))
            MemberAvatarRow(
                members = members,
                onAddClick = onShareClick,
                borderColor = list.color,
                iconTint = Color.White.copy(alpha = 0.85f)
            )
        }
    }
}

@Composable
private fun ItemCountBadge(count: Int) {
    Surface(
        color = BadgeRed,
        shape = RoundedCornerShape(50)
    ) {
        Text(
            text = pluralStringResource(R.plurals.items, count, count),
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun ArrowCircle() {
    Box(
        modifier = Modifier
            .size(64.dp)
            .background(Color.White.copy(alpha = 0.18f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(36.dp)
        )
    }
}

@Composable
private fun SuggestionsSection(suggestions: List<Suggestion>, onSuggestionClick: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = stringResource(R.string.suggestions_title),
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(suggestions, key = { it.id }) { suggestion ->
                SuggestionCard(suggestion = suggestion, onClick = { onSuggestionClick(suggestion.name) })
            }
        }
    }
}

@Composable
private fun SuggestionCard(suggestion: Suggestion, onClick: () -> Unit = {}) {
    Surface(
        color = suggestion.color,
        shape = RoundedCornerShape(16.dp),
        onClick = onClick,
        modifier = Modifier.size(width = 120.dp, height = 90.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = suggestion.name,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                lineHeight = 17.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.weight(1f))
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun NewListCard(onClick: () -> Unit) {
    val borderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f)
    val textColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 80.dp)
            .drawBehind {
                drawRoundRect(
                    color = borderColor,
                    style = Stroke(
                        width = 2.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f), 0f)
                    ),
                    cornerRadius = CornerRadius(20.dp.toPx())
                )
            }
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = stringResource(R.string.new_list),
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            color = textColor
        )
    }
}

private val BadgeRed = Color(0xFFE55353)
