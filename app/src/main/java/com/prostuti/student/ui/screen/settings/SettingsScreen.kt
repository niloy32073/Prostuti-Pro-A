package com.prostuti.student.ui.screen.settings

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.dropUnlessResumed
import com.prostuti.student.BuildConfig
import com.prostuti.student.R
import com.prostuti.student.data.model.enums.ThemeMode
import com.prostuti.student.ui.component.FormFactorPreviews
import com.prostuti.student.ui.component.UserAvatar
import com.prostuti.student.ui.theme.ProstutiTheme
import com.prostuti.student.ui.theme.ProstutiTypography
import com.prostuti.student.util.LANGUAGE_BANGLA
import com.prostuti.student.util.LANGUAGE_ENGLISH
import com.prostuti.student.util.LocaleManager
import com.prostuti.student.util.toBengaliDigits
import androidx.compose.foundation.layout.fillMaxHeight
import com.prostuti.student.ui.component.ProstutiSpinner
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.window.core.layout.WindowSizeClass
import com.prostuti.student.data.model.response.MeData
import com.prostuti.student.ui.component.PullToRefreshErrorOverlayWrapper
import com.prostuti.student.ui.screen.profile.AccountSection
import com.prostuti.student.ui.screen.profile.ContactSection
import com.prostuti.student.ui.screen.profile.IdentityCard
import com.prostuti.student.ui.screen.profile.ProfileLoadFailed
import com.prostuti.student.ui.screen.profile.ProfileUiState
import com.prostuti.student.ui.screen.profile.ProfileViewModel
import org.koin.compose.viewmodel.koinViewModel

const val SETTINGS_PROFILE_TAG = "settings_profile"
const val SETTINGS_PLAYGROUND_TAG = "settings_playground"
const val SETTINGS_BOOKMARKS_TAG = "settings_bookmarks"
const val SETTINGS_EXAM_HISTORY_TAG = "settings_exam_history"
const val SETTINGS_REPORTS_TAG = "settings_reports"
const val SETTINGS_THEME_TAG = "settings_theme_"
const val SETTINGS_LANGUAGE_TAG = "settings_language_"
const val SETTINGS_LOGOUT_TAG = "settings_logout"
const val SETTINGS_PROFILE_PANE_TAG = "settings_profile_pane"

@Composable
fun SettingsScreen(
    onLoggedOut: () -> Unit,
    onOpenBookmarks: () -> Unit,
    onOpenExamHistory: () -> Unit,
    onOpenProfile: () -> Unit = {},
    onOpenPlayground: () -> Unit = {},
    viewModel: SettingsViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val activity = LocalActivity.current
    val panes = accountPaneCount()

    LaunchedEffect(state.isLoggedOut) {
        if (state.isLoggedOut) onLoggedOut()
    }

    // Medium and wider show the profile beside the settings, so it is only loaded then.
    val profileViewModel: ProfileViewModel? = if (panes > 1) koinViewModel() else null
    val profileState = profileViewModel?.state?.collectAsStateWithLifecycle()?.value ?: ProfileUiState()

    SettingsContent(
        state = state,
        language = LocalConfiguration.current.locales[0].language,
        panes = panes,
        profile = profileState,
        onProfileRefresh = { profileViewModel?.refresh() },
        onProfileClearError = { tag -> profileViewModel?.clearError(tag) },
        onOpenProfile = onOpenProfile,
        onOpenPlayground = onOpenPlayground,
        onOpenExamHistory = onOpenExamHistory,
        onOpenBookmarks = onOpenBookmarks,
        onLanguage = { code -> activity?.let { LocaleManager.changeLanguage(it, code) } },
        onThemeMode = viewModel::setThemeMode,
        onHaptics = viewModel::setHapticsEnabled,
        onSound = viewModel::setSoundEnabled,
        onLogout = viewModel::logout
    )
}

/** Account tab layout from the window width size class: compact 1 pane, medium 2, expanded and up 3. */
@Composable
private fun accountPaneCount(): Int {
    val windowSizeClass = currentWindowAdaptiveInfoV2().windowSizeClass
    return when {
        windowSizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND) -> 3
        windowSizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND) -> 2
        else -> 1
    }
}

/**
 * Account tab (design: "সেটিংস" artboard).
 * - 1 pane (compact): one centered column; the profile card opens the full Profile screen.
 * - 2 panes (medium): profile | all settings.
 * - 3 panes (expanded+): profile | activity | preferences.
 */
@Composable
fun SettingsContent(
    state: SettingsUiState,
    language: String,
    onOpenProfile: () -> Unit,
    onOpenPlayground: () -> Unit,
    onOpenExamHistory: () -> Unit,
    onOpenBookmarks: () -> Unit,
    onLanguage: (String) -> Unit,
    onThemeMode: (ThemeMode) -> Unit,
    onHaptics: (Boolean) -> Unit,
    onSound: (Boolean) -> Unit,
    onLogout: () -> Unit,
    panes: Int = 1,
    profile: ProfileUiState = ProfileUiState(),
    onProfileRefresh: () -> Unit = {},
    onProfileClearError: (String) -> Unit = {}
) {
    val playground: @Composable () -> Unit = {
        FeatureCard(
            title = stringResource(R.string.playground_title),
            subtitle = stringResource(R.string.settings_playground_subtitle),
            icon = Icons.Outlined.SportsEsports,
            onClick = dropUnlessResumed { onOpenPlayground() },
            modifier = Modifier.testTag(SETTINGS_PLAYGROUND_TAG)
        )
    }
    val activitySection: @Composable () -> Unit = { ActivitySection(onOpenExamHistory, onOpenBookmarks) }
    val preferences: @Composable () -> Unit = {
        PreferencesSection(state, language, onLanguage, onThemeMode, onHaptics, onSound)
        OtherSection(onLogout)
        VersionText(language)
    }

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
        if (panes == 1) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()),
                contentAlignment = Alignment.TopCenter
            ) {
                Column(
                    modifier = Modifier
                        .widthIn(max = 560.dp)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    Text(
                        text = stringResource(R.string.settings_title),
                        style = ProstutiTypography.bnHeadlineMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    ProfileCard(name = state.userName, imageUrl = state.profileImage, onClick = dropUnlessResumed { onOpenProfile() })
                    PlanCard()
                    playground()
                    activitySection()
                    preferences()
                }
            }
        } else {
            Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                Text(
                    text = stringResource(R.string.nav_account),
                    style = ProstutiTypography.bnHeadlineMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 12.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth().weight(1f).padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    ProfilePane(
                        profile = profile,
                        onRefresh = onProfileRefresh,
                        onClearError = onProfileClearError,
                        modifier = Modifier.weight(if (panes == 3) 0.32f else 0.4f).fillMaxHeight()
                    )
                    VerticalDivider()
                    if (panes == 3) {
                        PaneColumn(Modifier.weight(0.34f)) { playground(); activitySection() }
                        VerticalDivider()
                        PaneColumn(Modifier.weight(0.34f)) { preferences() }
                    } else {
                        PaneColumn(Modifier.weight(0.6f)) { playground(); activitySection(); preferences() }
                    }
                }
            }
        }
    }
}

/** One scrolling column of a multi-pane layout. */
@Composable
private fun PaneColumn(modifier: Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = modifier.fillMaxHeight().verticalScroll(rememberScrollState()).padding(bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
        content = content
    )
}

/** The profile beside the settings on wide windows: same cards as the Profile screen, plus the plan. */
@Composable
private fun ProfilePane(
    profile: ProfileUiState,
    onRefresh: () -> Unit,
    onClearError: (String) -> Unit,
    modifier: Modifier
) {
    val me = profile.profile
    Box(modifier = modifier.testTag(SETTINGS_PROFILE_PANE_TAG)) {
        PullToRefreshErrorOverlayWrapper(
            errors = profile.errors,
            errorCleaner = onClearError,
            errorTopPadding = 8.dp,
            pullToRefreshEnabled = true,
            isRefreshing = profile.isLoading && me != null,
            onRefresh = onRefresh
        ) {
            when {
                me != null -> Column(
                    modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    IdentityCard(me, modifier = Modifier.fillMaxWidth())
                    PlanCard()
                    ContactSection(me)
                    AccountSection(me)
                }
                profile.isLoading -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    ProstutiSpinner()
                }
                else -> ProfileLoadFailed(onRetry = onRefresh)
            }
        }
    }
}

@Composable
private fun ActivitySection(onOpenExamHistory: () -> Unit, onOpenBookmarks: () -> Unit) {
    SettingsSection(title = stringResource(R.string.settings_section_activity)) {
        LinkRow(
            title = stringResource(R.string.settings_exam_history),
            subtitle = stringResource(R.string.settings_exam_history_subtitle),
            icon = Icons.Outlined.History,
            onClick = dropUnlessResumed { onOpenExamHistory() },
            modifier = Modifier.testTag(SETTINGS_EXAM_HISTORY_TAG)
        )
        RowDivider()
        LinkRow(
            title = stringResource(R.string.settings_bookmarks),
            subtitle = stringResource(R.string.settings_bookmarks_subtitle),
            icon = Icons.Filled.Favorite,
            onClick = dropUnlessResumed { onOpenBookmarks() },
            modifier = Modifier.testTag(SETTINGS_BOOKMARKS_TAG)
        )
        RowDivider()
        // No reports screen yet: shown dimmed and not clickable.
        LinkRow(
            title = stringResource(R.string.settings_reports),
            subtitle = stringResource(R.string.settings_reports_subtitle),
            icon = Icons.Outlined.BarChart,
            onClick = null,
            modifier = Modifier.testTag(SETTINGS_REPORTS_TAG)
        )
    }
}

@Composable
private fun PreferencesSection(
    state: SettingsUiState,
    language: String,
    onLanguage: (String) -> Unit,
    onThemeMode: (ThemeMode) -> Unit,
    onHaptics: (Boolean) -> Unit,
    onSound: (Boolean) -> Unit
) {
    SettingsSection(title = stringResource(R.string.settings_section_preferences)) {
        ValueRow(label = stringResource(R.string.settings_language)) {
            Segmented(
                options = listOf(
                    LANGUAGE_BANGLA to stringResource(R.string.settings_language_bangla),
                    LANGUAGE_ENGLISH to stringResource(R.string.settings_language_english)
                ),
                selected = if (language == LANGUAGE_ENGLISH) LANGUAGE_ENGLISH else LANGUAGE_BANGLA,
                onSelect = { if (it != language) onLanguage(it) },
                tagPrefix = SETTINGS_LANGUAGE_TAG
            )
        }
        RowDivider()
        ValueRow(label = stringResource(R.string.settings_theme)) {
            Segmented(
                options = listOf(
                    ThemeMode.LIGHT to stringResource(R.string.settings_theme_light),
                    ThemeMode.DARK to stringResource(R.string.settings_theme_dark),
                    ThemeMode.SYSTEM to stringResource(R.string.settings_theme_system)
                ),
                selected = state.themeMode,
                onSelect = onThemeMode,
                tagPrefix = SETTINGS_THEME_TAG
            )
        }
        RowDivider()
        ValueRow(label = stringResource(R.string.settings_haptics)) {
            Switch(checked = state.hapticsEnabled, onCheckedChange = onHaptics)
        }
        RowDivider()
        ValueRow(label = stringResource(R.string.settings_sound)) {
            Switch(checked = state.soundEnabled, onCheckedChange = onSound)
        }
    }
}

@Composable
private fun OtherSection(onLogout: () -> Unit) {
    SettingsSection(title = stringResource(R.string.settings_section_other)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp)
                .clickable(role = Role.Button, onClick = onLogout)
                .padding(horizontal = 14.dp)
                .testTag(SETTINGS_LOGOUT_TAG),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.settings_logout),
                style = ProstutiTypography.bnLabelLarge,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
private fun VersionText(language: String) {
    val version = stringResource(R.string.settings_version, BuildConfig.VERSION_NAME)
    Text(
        text = if (language == LANGUAGE_BANGLA) version.toBengaliDigits() else version,
        style = ProstutiTypography.enLabelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun ProfileCard(name: String?, imageUrl: String?, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .clickable(onClick = onClick)
            .padding(14.dp)
            .testTag(SETTINGS_PROFILE_TAG),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        UserAvatar(
            imageUrl = imageUrl,
            name = name,
            contentDescription = null,
            size = 46.dp,
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
        Column(modifier = Modifier.weight(1f)) {
            if (!name.isNullOrBlank()) {
                Text(name, style = ProstutiTypography.bnTitleMedium, color = MaterialTheme.colorScheme.onSurface)
            }
            Text(
                stringResource(R.string.settings_profile_subtitle),
                style = ProstutiTypography.bnLabelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Chevron(MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Subscription status. Placeholder until subscriptions are built — always "free plan", not clickable. */
@Composable
private fun PlanCard() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.primary)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        IconCircle(Icons.Filled.Bolt, background = MaterialTheme.colorScheme.onPrimary, tint = MaterialTheme.colorScheme.primary)
        Column(modifier = Modifier.weight(1f)) {
            Text(stringResource(R.string.settings_plan_free), style = ProstutiTypography.bnLabelLarge, color = MaterialTheme.colorScheme.onPrimary)
            Text(stringResource(R.string.settings_plan_free_subtitle), style = ProstutiTypography.bnLabelMedium, color = MaterialTheme.colorScheme.onPrimary)
        }
    }
}

@Composable
private fun FeatureCard(title: String, subtitle: String, icon: ImageVector, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        IconCircle(icon)
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = ProstutiTypography.bnLabelLarge, color = MaterialTheme.colorScheme.onSurface)
            Text(subtitle, style = ProstutiTypography.bnLabelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Chevron(MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** A titled group: one light card whose rows are split by thin dividers. */
@Composable
private fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column {
        Text(
            text = title,
            style = ProstutiTypography.bnLabelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 2.dp, bottom = 8.dp)
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerLow),
            content = content
        )
    }
}

@Composable
private fun RowDivider() {
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
}

/** A row that opens another screen. [onClick] null = not available yet (dimmed, not clickable). */
@Composable
private fun LinkRow(title: String, subtitle: String, icon: ImageVector, onClick: (() -> Unit)?, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = onClick != null, onClick = { onClick?.invoke() })
            .alpha(if (onClick != null) 1f else 0.6f)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        IconCircle(icon, size = 34)
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = ProstutiTypography.bnBodyMedium, color = MaterialTheme.colorScheme.onSurface)
            Text(subtitle, style = ProstutiTypography.bnLabelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (onClick != null) Chevron(MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ValueRow(label: String, control: @Composable () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp).padding(horizontal = 14.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = ProstutiTypography.bnBodyMedium, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
        control()
    }
}

/** A small pill toggle: the selected option is filled with the brand color. */
@Composable
private fun <T> Segmented(options: List<Pair<T, String>>, selected: T, onSelect: (T) -> Unit, tagPrefix: String) {
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surface)
            .padding(3.dp)
            .selectableGroup()
    ) {
        options.forEach { (value, label) ->
            val active = value == selected
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(if (active) MaterialTheme.colorScheme.primary else Color.Transparent)
                    .selectable(selected = active, role = Role.RadioButton, onClick = { onSelect(value) })
                    .heightIn(min = 36.dp)
                    .padding(horizontal = 12.dp)
                    .testTag(tagPrefix + value.toString()),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    label,
                    style = ProstutiTypography.bnLabelMedium,
                    color = if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun IconCircle(
    icon: ImageVector,
    size: Int = 38,
    background: Color = MaterialTheme.colorScheme.secondaryContainer,
    tint: Color = MaterialTheme.colorScheme.onSecondaryContainer
) {
    Box(
        modifier = Modifier.size(size.dp).clip(CircleShape).background(background),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size((size / 2).dp))
    }
}

@Composable
private fun Chevron(tint: Color) {
    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = tint)
}

@FormFactorPreviews
@Preview(name = "Phone dark", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SettingsContentPreview() {
    ProstutiTheme {
        SettingsContent(
            state = SettingsUiState(userName = "নিফা রহমান"),
            language = LANGUAGE_BANGLA,
            onOpenProfile = {}, onOpenPlayground = {}, onOpenExamHistory = {}, onOpenBookmarks = {},
            onLanguage = {}, onThemeMode = {}, onHaptics = {}, onSound = {}, onLogout = {},
            panes = accountPaneCount(),
            profile = ProfileUiState(profile = previewProfile)
        )
    }
}

private val previewProfile = MeData(
    userId = "018f3c7e-007b-7000-8000-000000000001",
    firstName = "নিফা", lastName = "রহমান", username = "nifa",
    email = "nifa@example.com", phone = null, profileImage = null, gender = "FEMALE",
    role = "STUDENT", status = "ACTIVE", provider = "EMAIL",
    isEmailVerified = true, isPhoneVerified = false,
    lastLoginAt = null, createdAt = "2025-01-15T08:00:00Z",
    scopes = emptyList()
)
