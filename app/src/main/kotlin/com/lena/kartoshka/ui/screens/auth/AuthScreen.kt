package com.lena.kartoshka.ui.screens.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import android.content.Intent
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.width
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.ui.platform.LocalContext
import com.lena.kartoshka.YandexAuthBus
import kotlinx.coroutines.flow.filterNotNull
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lena.kartoshka.R

@Composable
fun AuthScreen(
    vm: AuthViewModel,
    onSuccess: (name: String, email: String) -> Unit
) {
    val state by vm.state.collectAsState()
    val focusManager = LocalFocusManager.current
    var passwordVisible by remember { mutableStateOf(false) }
    var showForgotDialog by remember { mutableStateOf(false) }
    var forgotEmail by remember { mutableStateOf("") }
    val context = LocalContext.current
    val yandexErrorNoConnection = stringResource(R.string.auth_error_no_connection)

    // Получаем код от Яндекс OAuth когда браузер редиректит обратно в приложение
    LaunchedEffect(Unit) {
        YandexAuthBus.code.filterNotNull().collect { code ->
            YandexAuthBus.code.value = null
            vm.loginWithYandex(code, yandexErrorNoConnection)
        }
    }

    val errorNoConnection = stringResource(R.string.auth_error_no_connection)
    val errorWrongCredentials = stringResource(R.string.auth_error_wrong_credentials)
    val errorEmailTaken = stringResource(R.string.auth_error_email_taken)
    val errorNotVerified = stringResource(R.string.auth_error_not_verified)
    val errorShortPassword = stringResource(R.string.auth_error_short_password)
    val errorGeneric = stringResource(R.string.auth_error_generic)

    LaunchedEffect(state) {
        val s = state
        if (s is AuthViewModel.UiState.LoginSuccess) onSuccess(s.name, s.email)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(Modifier.height(32.dp))

        Image(
            painter = painterResource(R.drawable.ic_app_logo),
            contentDescription = null,
            modifier = Modifier.size(96.dp),
            contentScale = ContentScale.Fit
        )

        Spacer(Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.app_name),
            fontSize = 36.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(Modifier.height(40.dp))

        // Login / Register toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            TextButton(onClick = { vm.switchMode(AuthViewModel.Mode.LOGIN) }) {
                Text(
                    text = stringResource(R.string.auth_login_tab),
                    color = if (vm.mode == AuthViewModel.Mode.LOGIN)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = if (vm.mode == AuthViewModel.Mode.LOGIN)
                        FontWeight.Bold else FontWeight.Normal
                )
            }
            TextButton(onClick = { vm.switchMode(AuthViewModel.Mode.REGISTER) }) {
                Text(
                    text = stringResource(R.string.auth_register_tab),
                    color = if (vm.mode == AuthViewModel.Mode.REGISTER)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = if (vm.mode == AuthViewModel.Mode.REGISTER)
                        FontWeight.Bold else FontWeight.Normal
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // Name field — only for register
        AnimatedVisibility(visible = vm.mode == AuthViewModel.Mode.REGISTER) {
            Column {
                OutlinedTextField(
                    value = vm.name,
                    onValueChange = { vm.name = it; vm.clearError() },
                    label = { Text(stringResource(R.string.auth_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Down) }
                    )
                )
                Spacer(Modifier.height(12.dp))
            }
        }

        OutlinedTextField(
            value = vm.email,
            onValueChange = { vm.email = it; vm.clearError() },
            label = { Text(stringResource(R.string.auth_email)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next
            ),
            keyboardActions = KeyboardActions(
                onNext = { focusManager.moveFocus(FocusDirection.Down) }
            )
        )

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = vm.password,
            onValueChange = { vm.password = it; vm.clearError() },
            label = { Text(stringResource(R.string.auth_password)) },
            supportingText = if (vm.mode == AuthViewModel.Mode.REGISTER) {
                { Text(stringResource(R.string.auth_password_hint)) }
            } else null,
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = if (passwordVisible)
                VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        imageVector = if (passwordVisible)
                            Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                        contentDescription = null
                    )
                }
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    focusManager.clearFocus()
                    vm.submit(errorNoConnection, errorWrongCredentials, errorEmailTaken,
                        errorNotVerified, errorShortPassword, errorGeneric)
                }
            )
        )

        // Forgot password — только в режиме Login
        if (vm.mode == AuthViewModel.Mode.LOGIN) {
            TextButton(
                onClick = {
                    forgotEmail = vm.email
                    showForgotDialog = true
                },
                modifier = Modifier.align(Alignment.End)
            ) {
                Text(
                    text = stringResource(R.string.auth_forgot_password),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            Spacer(Modifier.height(16.dp))
        }

        // Error message
        val error = state as? AuthViewModel.UiState.Error
        AnimatedVisibility(visible = error != null) {
            Text(
                text = error?.message ?: "",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        val isLoading = state is AuthViewModel.UiState.Loading

        Button(
            onClick = {
                focusManager.clearFocus()
                vm.submit(errorNoConnection, errorWrongCredentials, errorEmailTaken,
                    errorNotVerified, errorShortPassword, errorGeneric)
            },
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text(
                    text = if (vm.mode == AuthViewModel.Mode.LOGIN)
                        stringResource(R.string.auth_login_button)
                    else
                        stringResource(R.string.auth_register_button)
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // Разделитель "или"
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HorizontalDivider(modifier = Modifier.weight(1f))
            Text(
                text = stringResource(R.string.auth_or),
                modifier = Modifier.padding(horizontal = 12.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            HorizontalDivider(modifier = Modifier.weight(1f))
        }

        Spacer(Modifier.height(12.dp))

        // Кнопка Яндекс
        val yandexRed = Color(0xFFFC3F1D)
        val yandexUrl = "https://oauth.yandex.ru/authorize?response_type=code" +
                "&client_id=bd70d33bc21c4f98b0414701a5cfa5d8" +
                "&redirect_uri=kartoshka%3A%2F%2Fauth%2Fyandex%2Fcallback"
        OutlinedButton(
            onClick = {
                context.startActivity(
                    Intent(Intent.ACTION_VIEW, android.net.Uri.parse(yandexUrl))
                )
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading,
            border = BorderStroke(1.dp, yandexRed),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = yandexRed)
        ) {
            Text(
                text = "Я",
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                fontSize = 18.sp
            )
            Spacer(Modifier.width(8.dp))
            Text(text = stringResource(R.string.auth_yandex_button))
        }

        Spacer(Modifier.height(32.dp))
    }

    // Диалог "Забыл пароль?"
    if (showForgotDialog) {
        val isSent = state is AuthViewModel.UiState.ForgotPasswordSent
        val isLoading = state is AuthViewModel.UiState.Loading
        val forgotErrorNotFound = stringResource(R.string.auth_error_email_not_found)
        val forgotErrorNoConnection = stringResource(R.string.auth_error_no_connection)

        AlertDialog(
            onDismissRequest = {
                if (!isLoading) {
                    showForgotDialog = false
                    vm.clearError()
                }
            },
            title = { Text(stringResource(R.string.auth_forgot_dialog_title)) },
            text = {
                if (isSent) {
                    Text(stringResource(R.string.auth_forgot_sent))
                } else {
                    val dialogError = (state as? AuthViewModel.UiState.Error)?.message
                    Column {
                        Text(
                            text = stringResource(R.string.auth_forgot_dialog_text),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(Modifier.height(12.dp))
                        OutlinedTextField(
                            value = forgotEmail,
                            onValueChange = { forgotEmail = it; vm.clearError() },
                            label = { Text(stringResource(R.string.auth_email)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            enabled = !isLoading,
                            isError = dialogError != null,
                            supportingText = if (dialogError != null) {
                                { Text(dialogError, color = MaterialTheme.colorScheme.error) }
                            } else null
                        )
                    }
                }
            },
            confirmButton = {
                if (isSent) {
                    TextButton(onClick = {
                        showForgotDialog = false
                        vm.clearError()
                    }) { Text(stringResource(R.string.done)) }
                } else {
                    Button(
                        onClick = {
                            vm.forgotPassword(forgotEmail, forgotErrorNotFound, forgotErrorNoConnection)
                        },
                        enabled = !isLoading && forgotEmail.isNotBlank()
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Text(stringResource(R.string.auth_forgot_send))
                        }
                    }
                }
            },
            dismissButton = {
                if (!isSent) {
                    TextButton(
                        onClick = {
                            if (!isLoading) {
                                showForgotDialog = false
                                vm.clearError()
                            }
                        }
                    ) { Text(stringResource(R.string.cancel)) }
                }
            }
        )
    }
}
