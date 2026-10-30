package com.prostuti.student.ui.screen.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.prostuti.student.R
import com.prostuti.student.ui.component.AuthHeader
import com.prostuti.student.ui.component.BackButton
import com.prostuti.student.ui.component.IconBadge
import com.prostuti.student.ui.component.ObserveErrors
import com.prostuti.student.ui.component.PasswordField
import com.prostuti.student.ui.component.PrimaryButton
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun AuthResetNewPasswordScreen(
    resetToken: String,
    onBack: () -> Unit,
    onPasswordReset: () -> Unit,
    viewModel: AuthResetNewPasswordViewModel = koinViewModel { parametersOf(resetToken) }
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    ObserveErrors(state.errors, snackbarHostState, viewModel::clearError)

    LaunchedEffect(state.isReset) {
        if (state.isReset) onPasswordReset()
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            Column(modifier = Modifier.padding(top = 20.dp, start = 16.dp, end = 16.dp)) {
                BackButton(onClick = onBack)
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(26.dp)
            ) {
                IconBadge(
                    icon = Icons.Filled.LockReset,
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
                AuthHeader(
                    title = stringResource(R.string.auth_reset_new_password_title),
                    subtitle = stringResource(R.string.auth_reset_new_password_subtitle),
                    compact = true
                )
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    PasswordField(
                        value = state.password,
                        onValueChange = viewModel::updatePassword,
                        label = stringResource(R.string.new_password_label),
                        placeholder = stringResource(R.string.new_password_placeholder),
                        isError = state.passwordError != null,
                        errorText = state.passwordError?.let { stringResource(it) },
                        supportingText = stringResource(R.string.password_rules_hint)
                    )
                    PasswordField(
                        value = state.confirmPassword,
                        onValueChange = viewModel::updateConfirmPassword,
                        label = stringResource(R.string.confirm_password_label),
                        placeholder = stringResource(R.string.confirm_password_placeholder),
                        isError = state.confirmPasswordError != null,
                        errorText = state.confirmPasswordError?.let { stringResource(it) }
                    )
                }
            }

            Column(modifier = Modifier.padding(horizontal = 24.dp).padding(bottom = 28.dp, top = 12.dp)) {
                PrimaryButton(text = stringResource(R.string.auth_reset_password_action), onClick = viewModel::resetPassword, loading = state.isLoading)
            }
        }
    }
}
