package com.lena.kartoshka.ui.screens.ideas

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LocalDining
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.content.Intent
import androidx.compose.ui.platform.LocalContext
import com.lena.kartoshka.R
import com.lena.kartoshka.data.Ingredient
import com.lena.kartoshka.data.Item
import com.lena.kartoshka.data.Recipe
import com.lena.kartoshka.data.ShoppingList
import com.lena.kartoshka.data.sampleRecipes
import java.util.UUID

private enum class IdeasFilter { ALL, POPULAR, MY }

@Composable
fun IdeasScreen(
    initialListId: String = "",
    lists: List<ShoppingList> = emptyList(),
    onAddIngredients: (listId: String, items: List<Item>) -> Unit = { _, _ -> },
    onClose: (() -> Unit)? = null
) {
    if (onClose != null) BackHandler { onClose() }
    var selectedRecipe by remember { mutableStateOf<Recipe?>(null) }
    var quickAddRecipe by remember { mutableStateOf<Recipe?>(null) }
    var activeFilter by remember { mutableStateOf(IdeasFilter.ALL) }
    var showSuccessDialog by remember { mutableStateOf(false) }
    var showCreateScreen by remember { mutableStateOf(false) }

    val filteredRecipes = when (activeFilter) {
        IdeasFilter.ALL -> sampleRecipes.toList()
        IdeasFilter.POPULAR -> sampleRecipes.sortedByDescending { it.likes }
        IdeasFilter.MY -> sampleRecipes.filter { it.isOwn || it.isSaved }
    }
    val emptyTitle = if (activeFilter == IdeasFilter.MY)
        stringResource(R.string.ideas_empty_my_title)
    else
        stringResource(R.string.ideas_empty_title)
    val emptySubtitle = if (activeFilter == IdeasFilter.MY)
        stringResource(R.string.ideas_empty_my_subtitle)
    else
        stringResource(R.string.ideas_empty_subtitle)

    val toggleSave: (Recipe) -> Unit = { recipe ->
        val index = sampleRecipes.indexOfFirst { it.id == recipe.id }
        if (index >= 0) {
            val updated = recipe.copy(
                isSaved = !recipe.isSaved,
                likes = if (!recipe.isSaved) recipe.likes + 1 else recipe.likes - 1
            )
            sampleRecipes[index] = updated
            if (selectedRecipe?.id == updated.id) selectedRecipe = updated
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Text(
                text = stringResource(R.string.ideas_title),
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 4.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IdeasFilter.entries.forEach { filter ->
                    val label = when (filter) {
                        IdeasFilter.ALL -> stringResource(R.string.ideas_filter_all)
                        IdeasFilter.POPULAR -> stringResource(R.string.ideas_filter_popular)
                        IdeasFilter.MY -> stringResource(R.string.ideas_filter_my)
                    }
                    FilterChip(
                        selected = activeFilter == filter,
                        onClick = { activeFilter = filter },
                        label = { Text(label, fontSize = 13.sp) }
                    )
                }
            }

            LazyColumn(
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                items(filteredRecipes, key = { it.id }) { recipe ->
                    RecipeCard(
                        recipe = recipe,
                        onOpenRecipe = { selectedRecipe = recipe },
                        onToggleSave = { toggleSave(recipe) },
                        onQuickAdd = { quickAddRecipe = recipe }
                    )
                }
                if (filteredRecipes.isEmpty()) {
                    item {
                        Box(
                            Modifier.fillMaxWidth().padding(vertical = 64.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Filled.SearchOff,
                                    contentDescription = null,
                                    modifier = Modifier.size(64.dp),
                                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                                )
                                Spacer(Modifier.height(16.dp))
                                Text(
                                    text = emptyTitle,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = emptySubtitle,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                )
                            }
                        }
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = { showCreateScreen = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(16.dp),
            containerColor = MaterialTheme.colorScheme.primary
        ) {
            Icon(imageVector = Icons.Filled.Add, contentDescription = null, tint = Color.White)
        }

        if (showCreateScreen) {
            CreateRecipeScreen(onClose = { showCreateScreen = false })
        }

        selectedRecipe?.let { recipe ->
            RecipeDetailOverlay(
                recipe = recipe,
                initialListId = initialListId.ifEmpty { lists.firstOrNull()?.id ?: "" },
                lists = lists,
                onBack = { selectedRecipe = null },
                onToggleSave = { toggleSave(recipe) },
                onAddIngredients = { listId, ingredients ->
                    val newItems = ingredients.map { ingredient ->
                        Item(id = "idea_${UUID.randomUUID()}", name = ingredient.name, note = ingredient.amount)
                    }
                    onAddIngredients(listId, newItems)
                    selectedRecipe = null
                    showSuccessDialog = true
                }
            )
        }

        quickAddRecipe?.let { recipe ->
            QuickAddToListDialog(
                recipe = recipe,
                lists = lists,
                initialListId = initialListId.ifEmpty { lists.firstOrNull()?.id ?: "" },
                onDismiss = { quickAddRecipe = null },
                onAdd = { listId, items ->
                    onAddIngredients(listId, items)
                    quickAddRecipe = null
                    showSuccessDialog = true
                }
            )
        }

        if (showSuccessDialog) {
            LaunchedEffect(Unit) {
                delay(2000)
                showSuccessDialog = false
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.padding(horizontal = 48.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 36.dp, vertical = 28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .background(Color(0xFF4F8579), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                        Text(
                            text = stringResource(R.string.ideas_success),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RecipeCard(
    recipe: Recipe,
    onOpenRecipe: () -> Unit,
    onToggleSave: () -> Unit,
    onQuickAdd: () -> Unit = {}
) {
    val context = LocalContext.current
    val shareRecipe = {
        val text = buildString {
            appendLine(recipe.title)
            appendLine("${recipe.author} — ${recipe.tagline}")
            appendLine()
            appendLine("${context.getString(R.string.ideas_ingredients)}:")
            recipe.ingredients.forEach { appendLine("• ${it.name} — ${it.amount}") }
        }
        context.startActivity(
            Intent.createChooser(
                Intent(Intent.ACTION_SEND).apply {
                    putExtra(Intent.EXTRA_TEXT, text)
                    type = "text/plain"
                },
                null
            )
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(text = recipe.author, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
        Text(text = recipe.tagline, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = recipe.title, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        Spacer(modifier = Modifier.height(10.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(recipe.coverColor)
                .clickable(onClick = onOpenRecipe)
        ) {
            Surface(
                shape = RoundedCornerShape(50),
                color = Color.Black.copy(alpha = 0.25f),
                modifier = Modifier.padding(12.dp).align(Alignment.TopStart)
            ) {
                Text(
                    text = stringResource(R.string.ideas_recipe_badge),
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp)
                    .clickable(onClick = onToggleSave),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = if (recipe.isSaved) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    contentDescription = null,
                    tint = if (recipe.isSaved) Color(0xFFE57373) else Color.White,
                    modifier = Modifier.size(18.dp)
                )
                Text(text = recipe.likes.toString(), color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            }
        }

        Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
            CardAction(icon = Icons.Filled.Link, label = stringResource(R.string.ideas_view), modifier = Modifier.weight(1f), onClick = {})
            CardAction(icon = Icons.Filled.Share, label = stringResource(R.string.ideas_share), modifier = Modifier.weight(1f), onClick = shareRecipe)
            CardAction(icon = Icons.AutoMirrored.Filled.PlaylistAdd, label = stringResource(R.string.ideas_add_to_list), modifier = Modifier.weight(1f), onClick = onQuickAdd)
        }
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
}

@Composable
private fun CardAction(icon: ImageVector, label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Column(
        modifier = modifier.clickable(onClick = onClick).padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f), modifier = Modifier.size(18.dp))
        Text(text = label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f), textAlign = TextAlign.Center, maxLines = 1)
    }
}

@Composable
private fun RecipeDetailOverlay(
    recipe: Recipe,
    initialListId: String = "",
    lists: List<ShoppingList> = emptyList(),
    onBack: () -> Unit,
    onToggleSave: () -> Unit,
    onAddIngredients: (listId: String, List<Ingredient>) -> Unit
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    val shareRecipe = {
        val text = buildString {
            appendLine(recipe.title)
            appendLine("${recipe.author} — ${recipe.tagline}")
            appendLine()
            appendLine("${context.getString(R.string.ideas_ingredients)}:")
            recipe.ingredients.forEach { appendLine("• ${it.name} — ${it.amount}") }
        }
        context.startActivity(
            Intent.createChooser(
                Intent(Intent.ACTION_SEND).apply {
                    putExtra(Intent.EXTRA_TEXT, text)
                    type = "text/plain"
                },
                null
            )
        )
    }

    var portions by remember { mutableIntStateOf(4) }
    var selectedListId by remember { mutableStateOf(initialListId) }
    var showListDropdown by remember { mutableStateOf(false) }

    val regularIngredients = recipe.ingredients.filter { !it.isProbablyOwned }
    val ownedIngredients = recipe.ingredients.filter { it.isProbablyOwned }
    val selectedList = lists.find { it.id == selectedListId } ?: lists.firstOrNull()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .verticalScroll(rememberScrollState())
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
                .background(recipe.coverColor)
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(4.dp)
                    .background(Color.Black.copy(alpha = 0.3f), CircleShape)
                    .align(Alignment.TopStart)
            ) {
                Icon(imageVector = Icons.Filled.Close, contentDescription = null, tint = Color.White)
            }
        }

        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Text(text = recipe.author, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            Text(text = recipe.title, fontSize = 26.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        }

        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CardAction(icon = Icons.Filled.Link, label = stringResource(R.string.ideas_view), onClick = {})
            CardAction(icon = Icons.Filled.Share, label = stringResource(R.string.ideas_share), onClick = shareRecipe)
            Row(
                modifier = Modifier.clickable(onClick = onToggleSave),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = if (recipe.isSaved) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    contentDescription = null,
                    tint = if (recipe.isSaved) Color(0xFFE57373)
                           else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                    modifier = Modifier.size(18.dp)
                )
                Text(text = recipe.likes.toString(), fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f))
            }
        }

        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = portions.toString(), fontSize = 22.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(
                    onClick = { if (portions > 1) portions-- },
                    modifier = Modifier.size(36.dp).background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                ) {
                    Icon(imageVector = Icons.Filled.Remove, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(20.dp))
                }
                IconButton(
                    onClick = { portions++ },
                    modifier = Modifier.size(36.dp).background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                ) {
                    Icon(imageVector = Icons.Filled.Add, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(20.dp))
                }
            }
        }

        Text(
            text = stringResource(R.string.ideas_ingredients),
            fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        IngredientGrid(ingredients = regularIngredients, modifier = Modifier.padding(horizontal = 16.dp))

        if (ownedIngredients.isNotEmpty()) {
            Text(
                text = stringResource(R.string.ideas_probably_have),
                fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp)
            )
            IngredientGrid(ingredients = ownedIngredients, cardColor = Color(0xFF4A5060), modifier = Modifier.padding(horizontal = 16.dp))
        }

        Spacer(modifier = Modifier.height(20.dp))

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.ideas_active_list),
                fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                modifier = Modifier.weight(1f)
            )
            Box {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.clickable { showListDropdown = true }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(text = selectedList?.name ?: "", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                        Icon(imageVector = Icons.Filled.KeyboardArrowDown, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(16.dp))
                    }
                }
                DropdownMenu(expanded = showListDropdown, onDismissRequest = { showListDropdown = false }) {
                    lists.forEach { list ->
                        DropdownMenuItem(
                            text = { Text(list.name) },
                            onClick = { selectedListId = list.id; showListDropdown = false }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = { onAddIngredients(selectedListId, regularIngredients) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .navigationBarsPadding()
                .padding(bottom = 16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F8579))
        ) {
            Text(
                text = stringResource(R.string.ideas_add_button, regularIngredients.size),
                fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.White,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }
    }
}

@Composable
private fun IngredientGrid(ingredients: List<Ingredient>, cardColor: Color = Color(0xFFFF5449), modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ingredients.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { ingredient ->
                    IngredientCard(ingredient = ingredient, color = cardColor, modifier = Modifier.weight(1f))
                }
                repeat(3 - row.size) { Spacer(modifier = Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun IngredientCard(ingredient: Ingredient, color: Color, modifier: Modifier = Modifier) {
    Surface(color = color, shape = RoundedCornerShape(12.dp), modifier = modifier.aspectRatio(1f)) {
        Column(
            modifier = Modifier.fillMaxSize().padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(imageVector = Icons.Filled.LocalDining, contentDescription = null, tint = Color.White.copy(alpha = 0.35f), modifier = Modifier.size(26.dp))
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = ingredient.name, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color.White, textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(text = ingredient.amount, fontSize = 10.sp, color = Color.White.copy(alpha = 0.7f), textAlign = TextAlign.Center, maxLines = 1)
        }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun QuickAddToListDialog(
    recipe: Recipe,
    lists: List<ShoppingList>,
    initialListId: String,
    onDismiss: () -> Unit,
    onAdd: (listId: String, items: List<Item>) -> Unit
) {
    var selectedListId by remember { mutableStateOf(initialListId.ifEmpty { lists.firstOrNull()?.id ?: "" }) }
    var expanded by remember { mutableStateOf(false) }
    val selectedList = lists.find { it.id == selectedListId } ?: lists.firstOrNull()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = recipe.title,
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "${recipe.ingredients.size} ${ingredientWord(recipe.ingredients.size)}",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                if (lists.isNotEmpty()) {
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = !expanded }
                    ) {
                        OutlinedTextField(
                            value = selectedList?.name ?: "",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(stringResource(R.string.profile_pick_list)) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth(),
                            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                        )
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            lists.forEach { list ->
                                DropdownMenuItem(
                                    text = { Text(list.name) },
                                    onClick = {
                                        selectedListId = list.id
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val items = recipe.ingredients.map { ing ->
                        Item(id = "idea_${java.util.UUID.randomUUID()}", name = ing.name, note = ing.amount)
                    }
                    onAdd(selectedListId, items)
                },
                enabled = selectedList != null
            ) {
                Text(stringResource(R.string.ideas_add_to_list), fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

private fun ingredientWord(count: Int): String = when {
    count % 100 in 11..19 -> "ингредиентов"
    count % 10 == 1 -> "ингредиент"
    count % 10 in 2..4 -> "ингредиента"
    else -> "ингредиентов"
}
