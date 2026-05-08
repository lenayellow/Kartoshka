package com.lena.kartoshka.ui.screens.listdetail

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lena.kartoshka.R
import com.lena.kartoshka.data.ItemCategory
import kotlin.math.roundToInt

@Composable
fun SortScreen(
    categories: List<ItemCategory>,
    hiddenCategoryIds: Set<String>,
    onSave: (orderedIds: List<String>, hiddenIds: Set<String>) -> Unit,
    onBack: () -> Unit
) {
    val statusBarPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navBarPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    val sortedList = remember { mutableStateListOf(*categories.toTypedArray()) }
    val hidden = remember { mutableStateListOf(*hiddenCategoryIds.toTypedArray()) }

    var draggingIndex by remember { mutableStateOf<Int?>(null) }
    var cumulativeDrag by remember { mutableStateOf(0f) }

    // Fixed row height for drag position calculation
    val rowHeightPx = with(androidx.compose.ui.platform.LocalDensity.current) { 56.dp.toPx() }

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
                text = stringResource(R.string.sort_screen_title),
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            TextButton(
                onClick = {
                    onSave(
                        sortedList.map { it.id },
                        hidden.toSet()
                    )
                }
            ) {
                Text(
                    text = stringResource(R.string.sort_save),
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
            }
        }

        // Description
        Text(
            text = stringResource(R.string.sort_description),
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
                .padding(bottom = 8.dp)
        )

        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

        // Scrollable category list
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(bottom = navBarPadding)
        ) {
            sortedList.forEachIndexed { index, category ->
                val isDragging = draggingIndex == index
                val isHidden = hidden.contains(category.id)

                // rememberUpdatedState ensures the drag lambda always sees
                // the current index even after recomposition from a swap
                val currentIndex by rememberUpdatedState(index)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .background(
                            if (isDragging) MaterialTheme.colorScheme.surfaceVariant
                            else MaterialTheme.colorScheme.surface
                        )
                        .padding(start = 16.dp, end = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = category.name,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Normal,
                        color = if (isHidden)
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
                        else
                            MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )

                    IconButton(
                        onClick = {
                            if (isHidden) hidden.remove(category.id)
                            else hidden.add(category.id)
                        }
                    ) {
                        Icon(
                            imageVector = if (isHidden) Icons.Filled.VisibilityOff
                                          else Icons.Filled.Visibility,
                            contentDescription = null,
                            tint = if (isHidden)
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f)
                            else
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    IconButton(
                        onClick = {},
                        modifier = Modifier.pointerInput(Unit) {
                            detectDragGesturesAfterLongPress(
                                onDragStart = {
                                    draggingIndex = currentIndex
                                    cumulativeDrag = 0f
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    val idx = draggingIndex ?: return@detectDragGesturesAfterLongPress
                                    cumulativeDrag += dragAmount.y
                                    val steps = (cumulativeDrag / rowHeightPx).roundToInt()
                                    if (steps != 0) {
                                        val targetIdx = (idx + steps).coerceIn(0, sortedList.size - 1)
                                        if (targetIdx != idx) {
                                            val item = sortedList.removeAt(idx)
                                            sortedList.add(targetIdx, item)
                                            draggingIndex = targetIdx
                                            cumulativeDrag -= steps * rowHeightPx
                                        }
                                    }
                                },
                                onDragEnd = {
                                    draggingIndex = null
                                    cumulativeDrag = 0f
                                },
                                onDragCancel = {
                                    draggingIndex = null
                                    cumulativeDrag = 0f
                                }
                            )
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Filled.DragHandle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                    thickness = 0.5.dp
                )
            }
        }
    }
}
