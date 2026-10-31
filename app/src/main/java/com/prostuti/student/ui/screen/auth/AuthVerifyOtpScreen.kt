package com.prostuti.student.ui.screen.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
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
import com.prostuti.student.ui.component.InfoNote
import com.prostuti.student.ui.component.PullToRefreshErrorOverlayWrapper
import com.prostuti.student.ui.component.OtpCodeField
import com.prostuti.student.ui.component.PrimaryButton
import com.prostuti.student.ui.component.ResendCodeRow
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun AuthVerifyOtpScreen(
    identifier: String,
    onBack: () -> Unit,
    onVerified: () -> Unit,
    viewModel: AuthVerifyOtpViewModel = koinViewModel { parametersOf(identifier) }
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(state.isVerified) {
        if (state.isVerified) onVerified()
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
                        icon = Icons.Filled.MarkEmailRead,
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    AuthHeader(
                        title = stringResource(R.string.auth_verify_otp_title),
                        subtitle = stringResource(R.string.auth_verify_otp_subtitle, identifier),
                        compact = true
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                        OtpCodeField(
                            value = state.code,
                            onValueChange = viewModel::updateCode,
                            label = stringResource(R.string.otp_label),
                            isError = state.codeError != null,
                            errorText = state.codeError?.let { stringResource(it) }
                        )
                        ResendCodeRow(secondsLeft = state.secondsLeft, onResend = viewModel::resend)
                        InfoNote(text = stringResource(R.string.auth_verify_otp_info_note))
                    }
                }

                Column(modifier = Modifier.padding(horizontal = 24.dp).padding(bottom = 28.dp, top = 12.dp)) {
                    PrimaryButton(text = stringResource(R.string.otp_verify_action), onClick = viewModel::verify, loading = state.isLoading)
                }
            }
    
        }
    }
}
