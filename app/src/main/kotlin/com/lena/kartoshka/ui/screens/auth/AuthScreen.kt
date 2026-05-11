package com.lena.kartoshka.ui.screens.auth

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
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
    onSuccess: () -> Unit
) {
    val state by vm.state.collectAsState()
    val focusManager = LocalFocusManager.current
    var passwordVisible by remember { mutableStateOf(false) }

    val errorNoConnection = stringResource(R.string.auth_error_no_connection)
    val errorWrongCredentials = stringResource(R.string.auth_error_wrong_credentials)
    val errorEmailTaken = stringResource(R.string.auth_error_email_taken)
    val errorNotVerified = stringResource(R.string.auth_error_not_verified)
    val errorShortPassword = stringResource(R.string.auth_error_short_password)
    val errorGeneric = stringResource(R.string.auth_error_generic)

    LaunchedEffect(state) {
        if (state is AuthViewModel.UiState.LoginSuccess) onSuccess()
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
        Spacer(Modifier.height(48.dp))

        Text(
            text = stringResource(R.string.app_name),
            fontSize = 40.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(Modifier.height(48.dp))

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

        Spacer(Modifier.height(16.dp))

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

        Spacer(Modifier.height(48.dp))
    }
}
