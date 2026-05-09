package com.lena.kartoshka.ui.screens.ideas

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lena.kartoshka.R
import com.lena.kartoshka.data.Recipe
import com.lena.kartoshka.data.sampleRecipes
import java.util.UUID

private val recipeCoverColors = listOf(
    Color(0xFF4F8579),
    Color(0xFFDDA68B),
    Color(0xFF7B6B8A),
    Color(0xFF4A5060),
    Color(0xFF5B7178),
    Color(0xFF8AAECC),
    Color(0xFF9B8EC4),
    Color(0xFF3D5A4E),
    Color(0xFF6B9B6A),
    Color(0xFFC4B882)
)

@Composable
fun CreateRecipeScreen(onClose: () -> Unit) {
    BackHandler { onClose() }

    val myCollectionLabel = stringResource(R.string.ideas_filter_my)
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var link by remember { mutableStateOf("") }
    var photoSelected by remember { mutableStateOf(false) }
    var showLinkDialog by remember { mutableStateOf(false) }
    var showPhotoDialog by remember { mutableStateOf(false) }
    val coverColor = remember { recipeCoverColors.random() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onClose) {
                Icon(imageVector = Icons.Filled.Close, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface)
            }
            Spacer(modifier = Modifier.weight(1f))
            TextButton(
                onClick = {
                    sampleRecipes.add(
                        0,
                        Recipe(
                            id = "user_${UUID.randomUUID()}",
                            title = name.trim(),
                            author = myCollectionLabel,
                            tagline = description.trim(),
                            coverColor = coverColor,
                            likes = 0,
                            ingredients = emptyList(),
                            isOwn = true
                        )
                    )
                    onClose()
                },
                enabled = name.isNotBlank()
            ) {
                Text(
                    text = stringResource(R.string.create_recipe_save),
                    fontWeight = FontWeight.Bold,
                    color = if (name.isNotBlank()) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
                )
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
        ) {
            // Photo area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .background(
                        if (photoSelected) coverColor
                        else MaterialTheme.colorScheme.surfaceVariant
                    )
                    .clickable { showPhotoDialog = true },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Image,
                    contentDescription = null,
                    tint = if (photoSelected) Color.White.copy(alpha = 0.5f)
                           else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                    modifier = Modifier.size(56.dp)
                )
            }

            // Name field
            TextField(
                value = name,
                onValueChange = { name = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                textStyle = MaterialTheme.typography.headlineSmall,
                placeholder = {
                    Text(
                        text = stringResource(R.string.create_recipe_name_hint),
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
                    )
                },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                singleLine = true
            )

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)
            )

            // Description + link icon
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 8.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextField(
                    value = description,
                    onValueChange = { description = it },
                    modifier = Modifier.weight(1f),
                    placeholder = {
                        Text(
                            text = stringResource(R.string.create_recipe_description_hint),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
                        )
                    },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    singleLine = true
                )
                IconButton(onClick = { showLinkDialog = true }) {
                    Icon(
                        imageVector = Icons.Filled.Link,
                        contentDescription = null,
                        tint = if (link.isNotEmpty()) MaterialTheme.colorScheme.primary
                               else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f))
            Spacer(modifier = Modifier.height(24.dp))

            // Items section
            Text(
                text = stringResource(R.string.create_recipe_items),
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            val solidBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .drawBehind {
                        drawRoundRect(
                            color = solidBorderColor,
                            style = Stroke(width = 1.5.dp.toPx()),
                            cornerRadius = CornerRadius(16.dp.toPx())
                        )
                    }
                    .clickable { }
                    .padding(vertical = 20.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.create_recipe_add_items),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    fontSize = 15.sp
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Optional items section
            Text(
                text = stringResource(R.string.create_recipe_optional_items),
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
            Text(
                text = stringResource(R.string.create_recipe_optional_desc),
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
            val dashedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .drawBehind {
                        drawRoundRect(
                            color = dashedBorderColor,
                            style = Stroke(
                                width = 1.5.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
                            ),
                            cornerRadius = CornerRadius(16.dp.toPx())
                        )
                    }
                    .clickable { }
                    .padding(vertical = 20.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.create_recipe_add_optional),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    fontSize = 15.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // Link dialog
    if (showLinkDialog) {
        var linkInput by remember { mutableStateOf(link) }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
                .clickable { showLinkDialog = false },
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                    .padding(horizontal = 32.dp)
                    .clickable { }
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = stringResource(R.string.create_recipe_add_link),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    TextField(
                        value = linkInput,
                        onValueChange = { linkInput = it },
                        placeholder = { Text(stringResource(R.string.create_recipe_link_hint), color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)) },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                            unfocusedIndicatorColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showLinkDialog = false }) {
                            Text(stringResource(R.string.cancel).uppercase(), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        }
                        TextButton(onClick = { link = linkInput; showLinkDialog = false }) {
                            Text("OK", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // Photo dialog
    if (showPhotoDialog) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
                .clickable { showPhotoDialog = false },
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                    .padding(horizontal = 32.dp)
                    .clickable { }
            ) {
                Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        text = stringResource(R.string.add_photo_title),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = stringResource(R.string.add_photo_description),
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TextButton(onClick = { photoSelected = true; showPhotoDialog = false }) {
                            Text(stringResource(R.string.add_photo_from_album), color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Medium)
                        }
                        TextButton(onClick = { photoSelected = true; showPhotoDialog = false }) {
                            Text(stringResource(R.string.add_photo_take), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
        }
    }
}
