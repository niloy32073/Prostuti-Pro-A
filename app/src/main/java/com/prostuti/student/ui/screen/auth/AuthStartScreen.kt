package com.prostuti.student.ui.screen.auth

import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.prostuti.student.R
import com.prostuti.student.ui.component.AuthHeader
import com.prostuti.student.ui.component.ObserveErrors
import com.prostuti.student.ui.component.OrDivider
import com.prostuti.student.ui.component.OutlinedPillButton
import com.prostuti.student.ui.component.PrimaryButton
import com.prostuti.student.ui.component.ProstutiTextField
import com.prostuti.student.ui.theme.ProstutiTypography
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

/**
 * The entry point of the auth flow — reached either from Intro (first launch) or, after a
 * logout, as the root of a fresh backstack. There is no screen to go back to from here, so it
 * has no back button; the system back gesture instead asks whether to exit the app.
 */
@Composable
fun AuthStartScreen(
    onNavigateToLogin: (identifier: String, firstName: String?) -> Unit,
    onNavigateToSignup: (identifier: String) -> Unit,
    viewModel: AuthStartViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var showExitDialog by rememberSaveable { mutableStateOf(false) }
    val activity = LocalActivity.current

    BackHandler { showExitDialog = true }

    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = {
                Text(text = stringResource(R.string.auth_exit_title), style = ProstutiTypography.bnTitleLarge)
            },
            confirmButton = {
                TextButton(onClick = { activity?.finish() }) {
                    Text(text = stringResource(R.string.auth_exit_yes), style = ProstutiTypography.bnLabelLarge)
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitDialog = false }) {
                    Text(text = stringResource(R.string.auth_exit_no), style = ProstutiTypography.bnLabelLarge)
                }
            }
        )
    }

    ObserveErrors(state.errors, snackbarHostState, viewModel::clearError)

    LaunchedEffect(state.accountExists) {
        when (state.accountExists) {
            true -> {
                onNavigateToLogin(state.identifier.trim(), state.firstName)
                viewModel.consumeNavigation()
            }
            false -> {
                onNavigateToSignup(state.identifier.trim())
                viewModel.consumeNavigation()
            }
            null -> Unit
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 24.dp)
                    .padding(top = 44.dp),
                verticalArrangement = Arrangement.spacedBy(28.dp)
            ) {
                AuthHeader(
                    title = stringResource(R.string.auth_start_title),
                    subtitle = stringResource(R.string.auth_start_subtitle)
                )

                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    ProstutiTextField(
                        value = state.identifier,
                        onValueChange = viewModel::updateIdentifier,
                        label = stringResource(R.string.email_or_phone_label),
                        placeholder = stringResource(R.string.email_or_phone_placeholder),
                        isError = state.identifierError != null,
                        errorText = state.identifierError?.let { stringResource(it) },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Done
                        )
                    )
                    PrimaryButton(
                        text = stringResource(R.string.continue_action),
                        onClick = viewModel::continueClicked,
                        loading = state.isLoading
                    )
                }

                OrDivider()

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    val googleComingSoon = stringResource(R.string.auth_social_coming_soon_google)
                    OutlinedPillButton(
                        text = stringResource(R.string.auth_continue_with_google),
                        onClick = {
                            scope.launch { snackbarHostState.showSnackbar(googleComingSoon) }
                        }
                    )
                    val facebookComingSoon = stringResource(R.string.auth_social_coming_soon_facebook)
                    OutlinedPillButton(
                        text = stringResource(R.string.auth_continue_with_facebook),
                        onClick = {
                            scope.launch { snackbarHostState.showSnackbar(facebookComingSoon) }
                        }
                    )
                }

                Text(
                    text = stringResource(R.string.auth_terms_notice),
                    style = ProstutiTypography.bnLabelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
