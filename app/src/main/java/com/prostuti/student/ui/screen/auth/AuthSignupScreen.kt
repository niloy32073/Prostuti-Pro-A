package com.prostuti.student.ui.screen.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.prostuti.student.R
import com.prostuti.student.ui.component.AuthHeader
import com.prostuti.student.ui.component.BackButton
import com.prostuti.student.ui.component.LockedIdentifierField
import com.prostuti.student.ui.component.ObserveErrors
import com.prostuti.student.ui.component.PasswordField
import com.prostuti.student.ui.component.PrimaryButton
import com.prostuti.student.ui.component.ProstutiTextField
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun AuthSignupScreen(
    identifier: String,
    onBack: () -> Unit,
    onSignedUp: (identifier: String) -> Unit,
    viewModel: AuthSignupViewModel = koinViewModel { parametersOf(identifier) }
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    ObserveErrors(state.errors, snackbarHostState, viewModel::clearError)

    LaunchedEffect(state.isSignedUp) {
        if (state.isSignedUp) onSignedUp(identifier)
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
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(22.dp)
            ) {
                AuthHeader(
                    title = stringResource(R.string.auth_signup_title),
                    subtitle = stringResource(R.string.auth_signup_subtitle)
                )

                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    LockedIdentifierField(
                        label = stringResource(R.string.email_or_phone_label),
                        value = identifier,
                        onChangeClick = onBack
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        ProstutiTextField(
                            value = state.firstName,
                            onValueChange = viewModel::updateFirstName,
                            label = stringResource(R.string.first_name_label),
                            placeholder = stringResource(R.string.first_name_placeholder),
                            isError = state.firstNameError != null,
                            errorText = state.firstNameError?.let { stringResource(it) },
                            modifier = Modifier.weight(1f)
                        )
                        ProstutiTextField(
                            value = state.lastName,
                            onValueChange = viewModel::updateLastName,
                            label = stringResource(R.string.last_name_label),
                            placeholder = stringResource(R.string.last_name_placeholder),
                            isError = state.lastNameError != null,
                            errorText = state.lastNameError?.let { stringResource(it) },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    PasswordField(
                        value = state.password,
                        onValueChange = viewModel::updatePassword,
                        label = stringResource(R.string.password_label),
                        placeholder = stringResource(R.string.new_password_placeholder),
                        isError = state.passwordError != null,
                        errorText = state.passwordError?.let { stringResource(it) },
                        supportingText = stringResource(R.string.password_rules_hint)
                    )
                }
            }

            Column(modifier = Modifier.padding(horizontal = 24.dp).padding(bottom = 28.dp, top = 12.dp)) {
                PrimaryButton(text = stringResource(R.string.auth_signup_action), onClick = viewModel::signUp, loading = state.isLoading)
            }
        }
    }
}
