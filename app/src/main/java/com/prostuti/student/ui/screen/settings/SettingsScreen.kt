package com.prostuti.student.ui.screen.settings

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.prostuti.student.R
import com.prostuti.student.ui.component.PrimaryButton
import com.prostuti.student.ui.theme.ProstutiTypography
import com.prostuti.student.util.LANGUAGE_BANGLA
import com.prostuti.student.util.LANGUAGE_ENGLISH
import com.prostuti.student.util.LocaleManager
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun SettingsScreen(
    onLoggedOut: () -> Unit,
    viewModel: SettingsViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsState()
    val activity = LocalActivity.current

    LaunchedEffect(state.isLoggedOut) {
        if (state.isLoggedOut) onLoggedOut()
    }

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(28.dp)
        ) {
            Text(
                text = stringResource(R.string.settings_title),
                style = ProstutiTypography.bnHeadlineMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = stringResource(R.string.settings_language),
                    style = ProstutiTypography.bnLabelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(onClick = { activity?.let { LocaleManager.changeLanguage(it, LANGUAGE_BANGLA) } }) {
                        Text(stringResource(R.string.settings_language_bangla))
                    }
                    TextButton(onClick = { activity?.let { LocaleManager.changeLanguage(it, LANGUAGE_ENGLISH) } }) {
                        Text(stringResource(R.string.settings_language_english))
                    }
                }
            }

            PrimaryButton(text = stringResource(R.string.settings_logout), onClick = viewModel::logout)
        }
    }
}
