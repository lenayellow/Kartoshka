package com.lena.kartoshka.ui.screens.listdetail

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lena.kartoshka.R
import com.lena.kartoshka.data.AppRepository
import com.lena.kartoshka.data.ListMember
import com.lena.kartoshka.ui.components.MemberAvatar
import kotlinx.coroutines.launch

@Composable
fun ListMembersScreen(
    listId: String,
    appRepository: AppRepository,
    currentUserName: String? = null,
    currentUserEmail: String? = null,
    onBack: () -> Unit
) {
    val statusBarPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navBarPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val scope = rememberCoroutineScope()

    var members by remember { mutableStateOf<List<ListMember>>(emptyList()) }
    var showShareSheet by remember { mutableStateOf(false) }
    var memberToRemove by remember { mutableStateOf<ListMember?>(null) }

    suspend fun loadMembers() {
        // Immediately show current user from local prefs (works offline)
        val localUser = if (!currentUserName.isNullOrBlank()) {
            ListMember(
                id = "current_user",
                name = currentUserName,
                email = currentUserEmail ?: "",
                avatarUrl = null,
                role = "owner"
            )
        } else null

        if (localUser != null) members = listOf(localUser)

        // Try to load full members list from server
        val fetched = appRepository.getMembers(listId)
        val apiMe = appRepository.getCurrentUser()

        members = when {
            fetched.isNotEmpty() -> {
                // Server returned members — use them, add current user if missing
                if (apiMe != null && fetched.none { it.id == apiMe.user_id }) {
                    listOf(ListMember(apiMe.user_id, apiMe.name, apiMe.email, apiMe.avatar_url, "owner")) + fetched
                } else fetched
            }
            apiMe != null -> listOf(ListMember(apiMe.user_id, apiMe.name, apiMe.email, apiMe.avatar_url, "owner"))
            localUser != null -> listOf(localUser)
            else -> emptyList()
        }
    }

    BackHandler { onBack() }

    LaunchedEffect(listId) { loadMembers() }

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
                    text = stringResource(R.string.members_screen_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
                // Spacer to balance the back button
                Spacer(modifier = Modifier.size(48.dp))
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
            ) {
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = stringResource(R.string.members_count, members.size),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                if (members.isNotEmpty()) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column {
                            members.forEachIndexed { index, member ->
                                MemberRow(
                                    member = member,
                                    canRemove = member.role != "owner",
                                    onRemoveClick = { memberToRemove = member }
                                )
                                if (index < members.lastIndex) {
                                    HorizontalDivider(
                                        modifier = Modifier.padding(start = 72.dp),
                                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }

            // Invite button
            Button(
                onClick = { showShareSheet = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = navBarPadding + 16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(
                    text = stringResource(R.string.members_invite_more),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
        }
    }

    // Remove member confirmation
    memberToRemove?.let { member ->
        AlertDialog(
            onDismissRequest = { memberToRemove = null },
            title = { Text(stringResource(R.string.members_remove)) },
            text = { Text(member.name) },
            confirmButton = {
                TextButton(
                    onClick = {
                        val id = member.id
                        memberToRemove = null
                        scope.launch {
                            appRepository.removeMember(listId, id)
                            loadMembers()
                        }
                    }
                ) {
                    Text(stringResource(R.string.done), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { memberToRemove = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    // Share / invite sheet
    if (showShareSheet) {
        ShareListSheet(
            listId = listId,
            members = members,
            appRepository = appRepository,
            onDismiss = {
                showShareSheet = false
                scope.launch { loadMembers() }
            }
        )
    }
}

@Composable
private fun MemberRow(
    member: ListMember,
    canRemove: Boolean,
    onRemoveClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        MemberAvatar(member = member, modifier = Modifier.size(44.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = member.name,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = member.email,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
            )
        }

        if (canRemove) {
            IconButton(
                onClick = onRemoveClick,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.RemoveCircleOutline,
                    contentDescription = stringResource(R.string.members_remove),
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}
