package com.lena.kartoshka.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import com.lena.kartoshka.data.ListMember
import kotlin.math.abs

private val memberColors = listOf(
    Color(0xFF5B7178), Color(0xFFDDA68B), Color(0xFF4F8579),
    Color(0xFF7B6B8A), Color(0xFF3D5A4E), Color(0xFF9D8060)
)

private fun memberColor(id: String): Color =
    memberColors[abs(id.hashCode()) % memberColors.size]

@Composable
fun MemberAvatarRow(
    members: List<ListMember>,
    onAddClick: () -> Unit,
    modifier: Modifier = Modifier,
    borderColor: Color = Color.Transparent,
    iconTint: Color = Color.White
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy((-8).dp)) {
            val visible = members.take(3)
            visible.forEachIndexed { index, member ->
                MemberAvatar(
                    member = member,
                    borderColor = borderColor,
                    modifier = Modifier.zIndex((visible.size - index).toFloat())
                )
            }
            if (members.size > 3) {
                Box(
                    modifier = Modifier
                        .zIndex(0f)
                        .size(28.dp)
                        .background(Color(0xFF555555), CircleShape)
                        .border(1.5.dp, borderColor, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "+${members.size - 3}",
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        IconButton(
            onClick = onAddClick,
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.PersonAdd,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
fun MemberAvatar(
    member: ListMember,
    modifier: Modifier = Modifier,
    borderColor: Color = Color.Transparent
) {
    Box(
        modifier = modifier
            .size(28.dp)
            .clip(CircleShape)
            .background(memberColor(member.id))
            .border(1.5.dp, borderColor, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        if (member.avatarUrl != null) {
            AsyncImage(
                model = member.avatarUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
            )
        } else {
            Text(
                text = member.name.firstOrNull()?.uppercase() ?: "?",
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
