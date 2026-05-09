package com.lena.kartoshka.ui.screens.ideas

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lena.kartoshka.R
import com.lena.kartoshka.data.itemCategories

@Composable
fun AddItemsScreen(
    initialSelection: List<String> = emptyList(),
    onDone: (List<String>) -> Unit,
    onClose: () -> Unit
) {
    BackHandler { onClose() }

    var searchQuery by remember { mutableStateOf("") }
    val expandedCategories = remember { mutableStateListOf<String>() }
    val selectedItems = remember { mutableStateListOf<String>().apply { addAll(initialSelection) } }

    val displayCategories = if (searchQuery.isBlank()) {
        itemCategories
    } else {
        itemCategories.mapNotNull { cat ->
            val matching = cat.items.filter { it.contains(searchQuery, ignoreCase = true) }
            if (matching.isNotEmpty() || cat.name.contains(searchQuery, ignoreCase = true)) {
                cat.copy(items = if (matching.isNotEmpty()) matching else cat.items)
            } else null
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onClose) {
                Icon(Icons.Filled.Close, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface)
            }
            Text(
                text = stringResource(R.string.add_items_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center
            )
            TextButton(onClick = { onDone(selectedItems.toList()) }) {
                Text(
                    text = stringResource(R.string.done).uppercase(),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        TextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            placeholder = {
                Text(
                    stringResource(R.string.add_items_search_hint),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                )
            },
            leadingIcon = {
                Icon(
                    Icons.Filled.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                )
            },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            ),
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
        ) {
            if (selectedItems.isEmpty() && searchQuery.isBlank()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(R.string.add_items_empty_title),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.add_items_empty_desc),
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }

            displayCategories.forEach { category ->
                val isExpanded = searchQuery.isNotBlank() || category.id in expandedCategories

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            if (category.id in expandedCategories) expandedCategories.remove(category.id)
                            else expandedCategories.add(category.id)
                        }
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = category.name,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Icon(
                        imageVector = if (isExpanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))

                if (isExpanded) {
                    category.items.forEach { itemName ->
                        val isSelected = itemName in selectedItems
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (isSelected) selectedItems.remove(itemName)
                                    else selectedItems.add(itemName)
                                }
                                .padding(horizontal = 24.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = itemName,
                                fontSize = 15.sp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
                            )
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Filled.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        HorizontalDivider(
                            modifier = Modifier.padding(start = 24.dp),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)
                        )
                    }
                }
            }
        }
    }
}
