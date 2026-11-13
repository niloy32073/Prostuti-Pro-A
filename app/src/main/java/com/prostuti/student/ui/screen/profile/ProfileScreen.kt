package com.prostuti.student.ui.screen.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.outlined.ErrorOutline
import com.prostuti.student.ui.component.ProstutiSpinner
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.window.core.layout.WindowSizeClass
import com.prostuti.student.R
import com.prostuti.student.data.model.response.MeData
import com.prostuti.student.ui.component.BackButton
import com.prostuti.student.ui.component.PullToRefreshErrorOverlayWrapper
import com.prostuti.student.ui.component.UserAvatar
import com.prostuti.student.ui.theme.MonoFontFamily
import com.prostuti.student.ui.theme.ProstutiTheme
import com.prostuti.student.ui.theme.ProstutiTypography
import com.prostuti.student.util.formatApiDateTime
import org.koin.compose.viewmodel.koinViewModel

const val PROFILE_NAME_TAG = "profile_name"
const val PROFILE_USERNAME_TAG = "profile_username"
const val PROFILE_EMAIL_TAG = "profile_email"
const val PROFILE_EMAIL_BADGE_TAG = "profile_email_badge"
const val PROFILE_PHONE_TAG = "profile_phone"
const val PROFILE_PHONE_BADGE_TAG = "profile_phone_badge"
const val PROFILE_ROLE_TAG = "profile_role"
const val PROFILE_PROVIDER_TAG = "profile_provider"
const val PROFILE_CREATED_TAG = "profile_created_at"
const val PROFILE_LAST_LOGIN_TAG = "profile_last_login"
const val PROFILE_RETRY_TAG = "profile_retry"

@Composable
fun ProfileScreen(
    onBack: () -> Unit,
    viewModel: ProfileViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ProfileContent(
        state = state,
        onBack = onBack,
        onRefresh = viewModel::refresh,
        onClearError = viewModel::clearError
    )
}

/** Stateless profile UI: avatar + name card, then contact and account details. */
@Composable
fun ProfileContent(
    state: ProfileUiState,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onClearError: (String) -> Unit
) {
    Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 18.dp, start = 16.dp, end = 16.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BackButton(onClick = onBack)
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = stringResource(R.string.profile),
                    style = ProstutiTypography.bnTitleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            val profile = state.profile
            // Below the header, so the error stack only needs a small top gap.
            PullToRefreshErrorOverlayWrapper(
                errors = state.errors,
                errorCleaner = onClearError,
                errorTopPadding = 8.dp,
                pullToRefreshEnabled = true,
                isRefreshing = state.isLoading && profile != null,
                onRefresh = onRefresh
            ) {
                when {
                    profile != null -> ProfileBody(profile)
                    state.isLoading -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        ProstutiSpinner()
                    }
                    else -> ProfileLoadFailed(onRetry = onRefresh)
                }
            }
        }
    }
}

@Composable
private fun ProfileBody(profile: MeData) {
    // Medium and wider: identity card on the left, details on the right.
    val windowSizeClass = currentWindowAdaptiveInfoV2().windowSizeClass
    val twoPane = windowSizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND)

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (twoPane) {
            Row(
                modifier = Modifier.widthIn(max = 960.dp).fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                IdentityCard(profile, modifier = Modifier.weight(0.4f))
                Column(modifier = Modifier.weight(0.6f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    ContactSection(profile)
                    AccountSection(profile)
                }
            }
        } else {
            Column(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                IdentityCard(profile, modifier = Modifier.fillMaxWidth())
                ContactSection(profile)
                AccountSection(profile)
            }
        }
    }
}

@Composable
internal fun IdentityCard(profile: MeData, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surfaceContainerLow, RoundedCornerShape(16.dp))
            .padding(horizontal = 16.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        UserAvatar(
            imageUrl = profile.profileImage,
            name = profile.firstName,
            contentDescription = stringResource(R.string.profile_avatar),
            size = 96.dp,
            initialStyle = ProstutiTypography.bnHeadlineMedium
        )
        Text(
            text = profile.fullName,
            style = ProstutiTypography.bnTitleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 14.dp).testTag(PROFILE_NAME_TAG)
        )
        Text(
            text = stringResource(R.string.username_handle, profile.username),
            style = ProstutiTypography.enBodyMedium.copy(fontFamily = MonoFontFamily),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp).testTag(PROFILE_USERNAME_TAG)
        )
        Text(
            text = roleLabelRes(profile.role)?.let { stringResource(it) } ?: profile.role,
            style = ProstutiTypography.bnLabelLarge,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier
                .padding(top = 12.dp)
                .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(50))
                .padding(horizontal = 12.dp, vertical = 4.dp)
                .testTag(PROFILE_ROLE_TAG)
        )
    }
}

@Composable
internal fun ContactSection(profile: MeData) {
    val notSet = stringResource(R.string.profile_not_set)
    DetailSection(title = stringResource(R.string.profile_section_contact)) {
        DetailRow(
            icon = Icons.Filled.Email,
            label = stringResource(R.string.profile_email),
            value = profile.email ?: notSet,
            valueTag = PROFILE_EMAIL_TAG,
            verified = profile.isEmailVerified.takeIf { profile.email != null },
            badgeTag = PROFILE_EMAIL_BADGE_TAG
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        DetailRow(
            icon = Icons.Filled.Phone,
            label = stringResource(R.string.profile_phone),
            value = profile.phone ?: notSet,
            valueTag = PROFILE_PHONE_TAG,
            verified = profile.isPhoneVerified.takeIf { profile.phone != null },
            badgeTag = PROFILE_PHONE_BADGE_TAG
        )
    }
}

@Composable
internal fun AccountSection(profile: MeData) {
    val locale = LocalConfiguration.current.locales[0]
    DetailSection(title = stringResource(R.string.profile_section_account)) {
        DetailRow(
            icon = Icons.Filled.Badge,
            label = stringResource(R.string.profile_role),
            value = roleLabelRes(profile.role)?.let { stringResource(it) } ?: profile.role
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        DetailRow(
            icon = Icons.AutoMirrored.Filled.Login,
            label = stringResource(R.string.profile_provider),
            value = providerLabelRes(profile.provider)?.let { stringResource(it) } ?: profile.provider,
            valueTag = PROFILE_PROVIDER_TAG
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        DetailRow(
            icon = Icons.Filled.CalendarMonth,
            label = stringResource(R.string.profile_created_at),
            value = formatApiDateTime(profile.createdAt, locale) ?: profile.createdAt,
            valueTag = PROFILE_CREATED_TAG
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        DetailRow(
            icon = Icons.Filled.History,
            label = stringResource(R.string.profile_last_login),
            value = profile.lastLoginAt?.let { formatApiDateTime(it, locale, withTime = true) ?: it }
                ?: stringResource(R.string.profile_not_set),
            valueTag = PROFILE_LAST_LOGIN_TAG
        )
    }
}

@Composable
private fun DetailSection(title: String, content: @Composable () -> Unit) {
    Column {
        Text(
            text = title,
            style = ProstutiTypography.bnLabelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceContainerLow, RoundedCornerShape(16.dp))
                .padding(horizontal = 16.dp, vertical = 4.dp)
        ) { content() }
    }
}

/** One label/value line; [verified] null = no badge (e.g. the value isn't set). */
@Composable
private fun DetailRow(
    icon: ImageVector,
    label: String,
    value: String,
    valueTag: String? = null,
    verified: Boolean? = null,
    badgeTag: String? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = label, style = ProstutiTypography.bnLabelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                text = value,
                style = ProstutiTypography.bnBodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp).then(if (valueTag != null) Modifier.testTag(valueTag) else Modifier)
            )
        }
        if (verified != null) {
            VerifiedBadge(verified, modifier = if (badgeTag != null) Modifier.testTag(badgeTag) else Modifier)
        }
    }
}

@Composable
private fun VerifiedBadge(verified: Boolean, modifier: Modifier = Modifier) {
    val container = if (verified) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer
    val content = if (verified) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer
    Row(
        modifier = modifier
            .background(container, RoundedCornerShape(50))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = if (verified) Icons.Filled.Verified else Icons.Outlined.ErrorOutline,
            contentDescription = null,
            tint = content,
            modifier = Modifier.size(14.dp)
        )
        Text(
            text = stringResource(if (verified) R.string.profile_verified else R.string.profile_not_verified),
            style = ProstutiTypography.bnLabelMedium,
            color = content
        )
    }
}

@Composable
internal fun ProfileLoadFailed(onRetry: () -> Unit) {
    // Scrollable so pull-to-refresh works here too.
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(modifier = Modifier.padding(top = 80.dp))
        Text(
            text = stringResource(R.string.profile_load_failed),
            style = ProstutiTypography.bnBodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        TextButton(onClick = onRetry, modifier = Modifier.padding(top = 8.dp).testTag(PROFILE_RETRY_TAG)) {
            Text(stringResource(R.string.profile_retry))
        }
    }
}

private val previewProfile = MeData(
    userId = "018f3c7e-007b-7000-8000-000000000001",
    firstName = "Muwaz", lastName = "Ahmed", username = "muwy",
    email = "muwy@proton.me", phone = "+8801712345678", profileImage = null, gender = "MALE",
    role = "STUDENT", status = "ACTIVE", provider = "EMAIL",
    isEmailVerified = true, isPhoneVerified = false,
    lastLoginAt = "2026-06-20T10:30:00Z", createdAt = "2025-01-15T08:00:00Z",
    scopes = listOf("exam:read")
)

@Preview(name = "Phone", device = Devices.PHONE, showBackground = true)
@Preview(name = "Foldable", device = Devices.FOLDABLE, showBackground = true)
@Preview(name = "Tablet", device = Devices.TABLET, showBackground = true)
@Preview(name = "Desktop", device = Devices.DESKTOP, showBackground = true)
@Composable
private fun ProfileContentPreview() {
    ProstutiTheme {
        ProfileContent(state = ProfileUiState(profile = previewProfile), onBack = {}, onRefresh = {}, onClearError = {})
    }
}
