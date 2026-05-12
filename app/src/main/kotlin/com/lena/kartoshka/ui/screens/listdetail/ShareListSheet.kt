package com.lena.kartoshka.ui.screens.listdetail

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lena.kartoshka.R
import com.lena.kartoshka.data.AppRepository
import com.lena.kartoshka.data.ListMember
import com.lena.kartoshka.ui.components.MemberAvatar
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareListSheet(
    listId: String,
    members: List<ListMember>,
    appRepository: AppRepository,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current
    val focusManager = LocalFocusManager.current

    var email by remember { mutableStateOf("") }
    var isSending by remember { mutableStateOf(false) }
    var isCopying by remember { mutableStateOf(false) }
    var feedback by remember { mutableStateOf("") }

    fun sendInvite() {
        if (email.isBlank()) return
        scope.launch {
            isSending = true
            focusManager.clearFocus()
            val result = appRepository.createInvite(listId, email.trim())
            when {
                result == null -> feedback = "error"
                result.emailSent -> {
                    email = ""
                    feedback = "sent"
                }
                else -> {
                    // Сервер создал инвайт, но письмо не ушло — кладём ссылку в буфер,
                    // чтобы пользователь мог отправить вручную.
                    clipboardManager.setText(AnnotatedString(result.webLink))
                    email = ""
                    feedback = "email_failed"
                }
            }
            isSending = false
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(R.string.share_sheet_title),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Current members
            if (members.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    members.forEach { member ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            MemberAvatar(member = member)
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = member.name,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = member.email,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                                )
                            }
                            if (member.role == "owner") {
                                Text(
                                    text = stringResource(R.string.share_role_owner),
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
            }

            // Email invite field
            OutlinedTextField(
                value = email,
                onValueChange = { email = it; feedback = "" },
                label = { Text(stringResource(R.string.share_email_hint)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Send
                ),
                keyboardActions = KeyboardActions(onSend = { sendInvite() })
            )

            Button(
                onClick = { sendInvite() },
                enabled = email.isNotBlank() && !isSending,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isSending) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text(stringResource(R.string.share_send_invite))
                }
            }

            AnimatedVisibility(visible = feedback.isNotEmpty()) {
                Text(
                    text = when (feedback) {
                        "sent" -> stringResource(R.string.share_invite_sent)
                        "copied" -> stringResource(R.string.share_link_copied)
                        "error" -> stringResource(R.string.share_error)
                        "email_failed" -> stringResource(R.string.share_email_failed_link_copied)
                        else -> ""
                    },
                    color = if (feedback == "error") MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.primary,
                    fontSize = 14.sp,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))

            // Copy link
            TextButton(
                onClick = {
                    scope.launch {
                        isCopying = true
                        feedback = ""
                        val result = appRepository.createInvite(listId)
                        val link = result?.webLink
                        if (!link.isNullOrEmpty()) {
                            clipboardManager.setText(AnnotatedString(link))
                            feedback = "copied"
                        } else {
                            feedback = "error"
                        }
                        isCopying = false
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isCopying) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                } else {
                    Icon(
                        imageVector = Icons.Filled.ContentCopy,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.share_copy_link))
                }
            }
        }
    }
}
