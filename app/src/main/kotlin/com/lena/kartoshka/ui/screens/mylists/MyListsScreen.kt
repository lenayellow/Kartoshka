package com.lena.kartoshka.ui.screens.mylists

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Style
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lena.kartoshka.R
import com.lena.kartoshka.data.ShoppingList
import com.lena.kartoshka.data.Suggestion
import com.lena.kartoshka.data.sampleLists
import com.lena.kartoshka.data.sampleSuggestions
import com.lena.kartoshka.ui.theme.KartoshkaTheme

@Composable
fun MyListsScreen(modifier: Modifier = Modifier, onListClick: (String) -> Unit = {}) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        Header()
        LazyColumn(
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 8.dp,
                bottom = 8.dp
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(sampleLists, key = { it.id }) { list ->
                MyListCard(list = list, onClick = { onListClick(list.id) })
            }
            item {
                NewListCard(onClick = { /* TODO: open New List screen */ })
            }
            item {
                SuggestionsSection(suggestions = sampleSuggestions)
            }
        }
        BottomNavBar()
    }
}

@Composable
private fun Header() {
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
        TextButton(onClick = { /* TODO: edit mode */ }) {
            Text(
                text = stringResource(R.string.edit),
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun BottomNavBar() {
    val navBarPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    Column {
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(bottom = navBarPadding)
                .padding(vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            NavItem(
                icon = Icons.Filled.Checklist,
                label = stringResource(R.string.nav_lists),
                selected = true
            )
            NavItem(
                icon = Icons.Filled.Style,
                label = stringResource(R.string.nav_ideas),
                selected = false
            )
            NavItem(
                icon = Icons.Filled.LocalOffer,
                label = stringResource(R.string.nav_offers),
                selected = false
            )
            NavItem(
                icon = Icons.Filled.Person,
                label = stringResource(R.string.nav_profile),
                selected = false
            )
        }
    }
}

@Composable
private fun NavItem(
    icon: ImageVector,
    label: String,
    selected: Boolean
) {
    val tint = if (selected) MaterialTheme.colorScheme.onSurface
               else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)

    Column(
        modifier = Modifier
            .clickable(enabled = !selected, onClick = {})
            .padding(horizontal = 16.dp, vertical = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(24.dp)
        )
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = tint,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun MyListCard(list: ShoppingList, onClick: () -> Unit = {}) {
    Surface(
        color = list.color,
        shape = RoundedCornerShape(20.dp),
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 140.dp)
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
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
private fun SuggestionsSection(suggestions: List<Suggestion>) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = stringResource(R.string.suggestions_title),
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = Color.White.copy(alpha = 0.5f)
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(suggestions, key = { it.id }) { suggestion ->
                SuggestionCard(suggestion = suggestion)
            }
        }
    }
}

@Composable
private fun SuggestionCard(suggestion: Suggestion) {
    Surface(
        color = suggestion.color,
        shape = RoundedCornerShape(16.dp),
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
    val borderColor = Color.White.copy(alpha = 0.25f)
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
            color = Color.White.copy(alpha = 0.5f)
        )
    }
}

private val BadgeRed = Color(0xFFE55353)

@Preview(showBackground = true, backgroundColor = 0xFF121412, widthDp = 360, heightDp = 720)
@Composable
private fun MyListsScreenPreview() {
    KartoshkaTheme {
        MyListsScreen()
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF121412, widthDp = 360)
@Composable
private fun MyListCardPreview() {
    KartoshkaTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            MyListCard(
                list = ShoppingList("preview", "Holiday Home", 7, Color(0xFF4F8579))
            )
        }
    }
}
