package com.prostuti.student.ui.screen.auth

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.prostuti.student.R
import com.prostuti.student.ui.component.AuthHeader
import com.prostuti.student.ui.component.BackButton
import com.prostuti.student.ui.component.LockedIdentifierField
import com.prostuti.student.ui.component.ObserveErrors
import com.prostuti.student.ui.component.PasswordField
import com.prostuti.student.ui.component.PrimaryButton
import com.prostuti.student.ui.theme.ProstutiTypography
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun AuthLoginScreen(
    identifier: String,
    firstName: String?,
    onBack: () -> Unit,
    onForgotPassword: (identifier: String) -> Unit,
    onSignedIn: () -> Unit,
    viewModel: AuthLoginViewModel = koinViewModel { parametersOf(identifier, firstName) }
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    ObserveErrors(state.errors, snackbarHostState, viewModel::clearError)

    LaunchedEffect(state.isSignedIn) {
        if (state.isSignedIn) onSignedIn()
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
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(26.dp)
            ) {
                AuthHeader(
                    title = if (firstName != null) {
                        stringResource(R.string.auth_login_welcome_named, firstName)
                    } else {
                        stringResource(R.string.auth_login_welcome)
                    },
                    subtitle = stringResource(R.string.auth_login_subtitle)
                )

                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    LockedIdentifierField(
                        label = stringResource(R.string.email_or_phone_label),
                        value = identifier,
                        onChangeClick = onBack
                    )
                    PasswordField(
                        value = state.password,
                        onValueChange = viewModel::updatePassword,
                        label = stringResource(R.string.password_label),
                        placeholder = stringResource(R.string.password_placeholder),
                        isError = state.passwordError != null,
                        errorText = state.passwordError?.let { stringResource(it) }
                    )
                    Text(
                        text = stringResource(R.string.auth_forgot_password),
                        style = ProstutiTypography.bnLabelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onForgotPassword(identifier) },
                        textAlign = TextAlign.End
                    )
                }
            }

            Column(modifier = Modifier.padding(horizontal = 24.dp).padding(bottom = 28.dp, top = 12.dp)) {
                PrimaryButton(text = stringResource(R.string.continue_action), onClick = viewModel::signIn, loading = state.isLoading)
            }
        }
    }
}
