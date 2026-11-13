package com.prostuti.student.ui.screen.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.prostuti.student.R
import com.prostuti.student.ui.component.AuthHeader
import com.prostuti.student.ui.component.BackButton
import com.prostuti.student.ui.component.IconBadge
import com.prostuti.student.ui.component.LockedIdentifierField
import com.prostuti.student.ui.component.PullToRefreshErrorOverlayWrapper
import com.prostuti.student.ui.component.OtpCodeField
import com.prostuti.student.ui.component.PrimaryButton
import com.prostuti.student.ui.component.ResendCodeRow
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * Forgot-password flow, step 1+2 combined on one screen: the identifier is already known (passed
 * in from AuthLogin, shown locked, never re-typed), and once "send code" succeeds the same screen
 * reveals the OTP field inline — no separate reset-OTP screen. (Signup's own OTP step is a
 * different flow and keeps its own screen, [AuthVerifyOtpScreen].)
 */
@Composable
fun AuthResetRequestScreen(
    identifier: String,
    onBack: () -> Unit,
    onCodeEntered: (code: String) -> Unit,
    viewModel: AuthResetRequestViewModel = koinViewModel { parametersOf(identifier) }
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(state.enteredCode) {
        val code = state.enteredCode
        if (code != null) {
            viewModel.onCodeConsumed() // so coming back from the next screen doesn't move on again
            onCodeEntered(code)
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        PullToRefreshErrorOverlayWrapper(
            errors = state.errors,
            errorCleaner = viewModel::clearError,
            overlayPadding = padding
        ) {
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
                        icon = if (state.isCodeSent) Icons.Filled.MarkEmailRead else Icons.Filled.LockReset,
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    AuthHeader(
                        title = stringResource(if (state.isCodeSent) R.string.auth_reset_otp_title else R.string.auth_forgot_password),
                        subtitle = if (state.isCodeSent) {
                            stringResource(R.string.auth_reset_otp_subtitle, identifier)
                        } else {
                            stringResource(R.string.auth_reset_request_subtitle)
                        },
                        compact = true
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        LockedIdentifierField(
                            label = stringResource(R.string.email_or_phone_label),
                            value = identifier,
                            onChangeClick = onBack
                        )

                        if (state.isCodeSent) {
                            OtpCodeField(
                                state = state.codeTextFieldState,
                                label = stringResource(R.string.otp_label),
                                isError = state.codeError != null,
                                errorText = state.codeError?.let { stringResource(it) }
                            )
                            ResendCodeRow(secondsLeft = state.secondsLeft, onResend = viewModel::resend)
                        }
                    }
                }

                Column(modifier = Modifier.padding(horizontal = 24.dp).padding(bottom = 28.dp, top = 12.dp)) {
                    PrimaryButton(
                        text = stringResource(if (state.isCodeSent) R.string.otp_verify_action else R.string.auth_send_code),
                        onClick = if (state.isCodeSent) viewModel::verify else viewModel::sendCode,
                        loading = state.isLoading
                    )
                }
            }
    
        }
    }
}
