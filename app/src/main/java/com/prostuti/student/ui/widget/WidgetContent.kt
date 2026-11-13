package com.prostuti.student.ui.widget

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.action.Action
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.prostuti.student.MainActivity
import com.prostuti.student.R
import com.prostuti.student.data.model.widget.WidgetExamItem
import com.prostuti.student.data.model.widget.WidgetExamKind
import com.prostuti.student.data.model.widget.WidgetSnapshot
import com.prostuti.student.navigation.AppLink
import java.time.Instant

/** Demo values for the streak tab until the backend has streak, XP and rank. */
internal object WidgetDemoProgress {
    const val STREAK_DAYS = 7
    const val XP = 1240
    const val RANK = 12
}

// Sizes (not breakpoints): a widget has no window size class; lists scroll when it's short.
private val Gap = 8.dp
private val CardRadius = 16.dp
private val BadgeSize = 34.dp

/**
 * The whole widget, stateless: brand header with refresh, an icon tab bar, then the chosen tab.
 * [text] carries the app language; [snapshot] null = nothing loaded yet.
 */
@Composable
fun WidgetContent(snapshot: WidgetSnapshot?, tab: WidgetTab, text: Context, now: Instant) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .appWidgetBackground()
            .background(GlanceTheme.colors.widgetBackground)
            .cornerRadius(24.dp)
            .padding(12.dp)
    ) {
        Header(snapshot, text)
        Spacer(GlanceModifier.height(10.dp))
        TabBar(tab, text)
        Spacer(GlanceModifier.height(10.dp))
        Box(modifier = GlanceModifier.fillMaxWidth().defaultWeight()) {
            when {
                snapshot == null -> Message(R.drawable.ic_widget_refresh, text.getString(R.string.widget_loading))
                !snapshot.signedIn -> Message(
                    R.drawable.ic_widget_lock,
                    text.getString(R.string.widget_sign_in_body),
                    button = text.getString(R.string.widget_sign_in) to openApp(AppLink.Home),
                )
                tab == WidgetTab.EXAMS -> ExamsTab(snapshot, text, now)
                tab == WidgetTab.ROUTINE -> RoutineTab(snapshot, text, now)
                else -> ProgressTab(text)
            }
        }
    }
}

/** App mark and name (tap opens the app), when it was updated, and refresh. */
@Composable
private fun Header(snapshot: WidgetSnapshot?, text: Context) {
    Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Row(modifier = GlanceModifier.defaultWeight().clickable(openApp(AppLink.Home)), verticalAlignment = Alignment.CenterVertically) {
            Image(
                provider = ImageProvider(R.mipmap.ic_launcher_round),
                contentDescription = null,
                modifier = GlanceModifier.size(22.dp),
            )
            Spacer(GlanceModifier.width(8.dp))
            Text(
                text = text.getString(R.string.widget_name),
                style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 15.sp, fontWeight = FontWeight.Bold),
                maxLines = 1,
            )
        }
        if (snapshot?.signedIn == true && snapshot.updatedAtMillis > 0) {
            Text(
                text = text.getString(R.string.widget_updated_at, text.widgetClock(snapshot.updatedAtMillis)),
                style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 10.sp),
                maxLines = 1,
            )
            Spacer(GlanceModifier.width(6.dp))
        }
        Box(
            modifier = GlanceModifier
                .size(28.dp)
                .cornerRadius(14.dp)
                .background(GlanceTheme.colors.surface)
                .clickable(actionRunCallback<RefreshWidgetAction>()),
            contentAlignment = Alignment.Center,
        ) {
            Icon(R.drawable.ic_widget_refresh, GlanceTheme.colors.primary, 16.dp, text.getString(R.string.widget_refresh))
        }
    }
}

/** A segmented control: the chosen tab is a filled pill with its icon. */
@Composable
private fun TabBar(selected: WidgetTab, text: Context) {
    Row(
        modifier = GlanceModifier.fillMaxWidth().cornerRadius(20.dp).background(GlanceTheme.colors.surface).padding(3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        WidgetTab.entries.forEach { tab ->
            val isSelected = tab == selected
            val content = if (isSelected) GlanceTheme.colors.onPrimary else GlanceTheme.colors.onSurfaceVariant
            Row(
                modifier = GlanceModifier
                    .defaultWeight()
                    .cornerRadius(17.dp)
                    .background(if (isSelected) GlanceTheme.colors.primary else GlanceTheme.colors.surface)
                    .clickable(actionRunCallback<SelectWidgetTabAction>(actionParametersOf(WIDGET_TAB_PARAM to tab.name)))
                    .padding(vertical = 6.dp, horizontal = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(tab.iconRes(), content, 15.dp)
                Spacer(GlanceModifier.width(4.dp))
                Text(
                    text = text.getString(tab.labelRes()),
                    style = TextStyle(color = content, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium),
                    maxLines = 1,
                )
            }
        }
    }
}

private fun WidgetTab.labelRes(): Int = when (this) {
    WidgetTab.EXAMS -> R.string.widget_tab_exams
    WidgetTab.ROUTINE -> R.string.widget_tab_routine
    WidgetTab.PROGRESS -> R.string.widget_tab_progress
}

@DrawableRes
private fun WidgetTab.iconRes(): Int = when (this) {
    WidgetTab.EXAMS -> R.drawable.ic_widget_timer
    WidgetTab.ROUTINE -> R.drawable.ic_widget_calendar
    WidgetTab.PROGRESS -> R.drawable.ic_widget_fire
}

@Composable
private fun ExamsTab(snapshot: WidgetSnapshot, text: Context, now: Instant) {
    // Time has passed since the snapshot: an exam that ended since then isn't "upcoming".
    val exams = snapshot.exams?.filter { it.endsAtMillis > now.toEpochMilli() }
    when {
        exams == null -> Message(
            R.drawable.ic_widget_cloud_off,
            text.getString(R.string.widget_exams_failed),
            button = text.getString(R.string.widget_see_live_exams) to openApp(AppLink.LiveExams),
        )
        exams.isEmpty() -> Message(
            R.drawable.ic_widget_timer,
            text.getString(R.string.widget_no_exams),
            button = text.getString(R.string.widget_see_live_exams) to openApp(AppLink.LiveExams),
        )
        else -> ExamList(exams, text, now)
    }
}

@Composable
private fun RoutineTab(snapshot: WidgetSnapshot, text: Context, now: Instant) {
    val today = snapshot.today
    when {
        snapshot.routineOff -> Message(R.drawable.ic_widget_calendar, text.getString(R.string.widget_routine_off))
        today == null -> Message(
            R.drawable.ic_widget_cloud_off,
            text.getString(R.string.widget_routine_failed),
            button = text.getString(R.string.widget_open_routine) to openApp(AppLink.RoutineTab),
        )
        today.isEmpty() && snapshot.hasPersonalRoutine -> Message(
            R.drawable.ic_widget_check,
            text.getString(R.string.widget_no_exam_today),
            button = text.getString(R.string.widget_open_routine) to openApp(AppLink.RoutineTab),
        )
        today.isEmpty() -> Message(
            R.drawable.ic_widget_calendar,
            text.getString(R.string.widget_no_routine),
            button = text.getString(R.string.widget_create_routine) to openApp(AppLink.CreateRoutine),
        )
        else -> ExamList(today, text, now)
    }
}

@Composable
private fun ProgressTab(text: Context) {
    Column(modifier = GlanceModifier.fillMaxSize()) {
        Row(modifier = GlanceModifier.fillMaxWidth()) {
            StatTile(
                R.drawable.ic_widget_fire,
                text.getString(R.string.widget_streak_days, text.widgetNumber(WidgetDemoProgress.STREAK_DAYS)),
                text.getString(R.string.widget_streak_label),
                highlight = true,
                modifier = GlanceModifier.defaultWeight(),
            )
            Spacer(GlanceModifier.width(6.dp))
            StatTile(R.drawable.ic_widget_bolt, text.widgetNumber(WidgetDemoProgress.XP), text.getString(R.string.widget_xp_label), modifier = GlanceModifier.defaultWeight())
            Spacer(GlanceModifier.width(6.dp))
            StatTile(R.drawable.ic_widget_trophy, "#" + text.widgetNumber(WidgetDemoProgress.RANK), text.getString(R.string.widget_rank_label), modifier = GlanceModifier.defaultWeight())
        }
        Spacer(GlanceModifier.height(Gap))
        Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            // Not real numbers yet: say so, so nobody takes them as their own.
            Text(
                text = text.getString(R.string.widget_demo),
                style = TextStyle(color = GlanceTheme.colors.onTertiaryContainer, fontSize = 10.sp, fontWeight = FontWeight.Bold),
                modifier = GlanceModifier.cornerRadius(8.dp).background(GlanceTheme.colors.tertiaryContainer).padding(horizontal = 8.dp, vertical = 3.dp),
                maxLines = 1,
            )
            Spacer(GlanceModifier.defaultWeight())
            Row(modifier = GlanceModifier.clickable(openApp(AppLink.LeaderboardTab)).padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = text.getString(R.string.widget_see_leaderboard),
                    style = TextStyle(color = GlanceTheme.colors.primary, fontSize = 12.sp, fontWeight = FontWeight.Bold),
                    maxLines = 1,
                )
                Icon(R.drawable.ic_widget_chevron, GlanceTheme.colors.primary, 16.dp)
            }
        }
    }
}

@Composable
private fun StatTile(@DrawableRes icon: Int, value: String, label: String, modifier: GlanceModifier, highlight: Boolean = false) {
    Column(
        modifier = modifier
            .cornerRadius(CardRadius)
            .background(if (highlight) GlanceTheme.colors.primary else GlanceTheme.colors.surface)
            .padding(vertical = 10.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icon, if (highlight) GlanceTheme.colors.onPrimary else GlanceTheme.colors.primary, 20.dp)
        Spacer(GlanceModifier.height(4.dp))
        Text(
            text = value,
            style = TextStyle(
                color = if (highlight) GlanceTheme.colors.onPrimary else GlanceTheme.colors.onSurface,
                fontSize = 17.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center,
            ),
            maxLines = 1,
        )
        Text(
            text = label,
            style = TextStyle(
                color = if (highlight) GlanceTheme.colors.onPrimary else GlanceTheme.colors.onSurfaceVariant,
                fontSize = 10.sp, textAlign = TextAlign.Center,
            ),
            maxLines = 1,
        )
    }
}

/** Scrolls when the widget is too short for every item. */
@Composable
private fun ExamList(items: List<WidgetExamItem>, text: Context, now: Instant) {
    LazyColumn(modifier = GlanceModifier.fillMaxSize()) {
        items(items, itemId = { it.id.hashCode().toLong() }) { item ->
            Column(modifier = GlanceModifier.fillMaxWidth().padding(bottom = 6.dp)) {
                ExamCard(item, text, now)
            }
        }
    }
}

/** One exam: a type badge, title and "where · when", and a live pill, a tick or a chevron. */
@Composable
private fun ExamCard(item: WidgetExamItem, text: Context, now: Instant) {
    val nowMillis = now.toEpochMilli()
    val running = !item.done && item.startsAtMillis <= nowMillis && nowMillis < item.endsAtMillis
    val time = when {
        item.done -> text.getString(R.string.widget_done)
        item.endsAtMillis <= nowMillis -> text.getString(R.string.widget_ended)
        else -> text.widgetExamTime(item, now)
    }
    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .cornerRadius(CardRadius)
            .background(GlanceTheme.colors.surface)
            .clickable(openApp(AppLink.parse(Uri.parse(item.link)) ?: AppLink.Home))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = GlanceModifier
                .size(BadgeSize)
                .cornerRadius(BadgeSize / 2)
                .background(if (running) GlanceTheme.colors.primary else GlanceTheme.colors.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(item.kind.iconRes(), if (running) GlanceTheme.colors.onPrimary else GlanceTheme.colors.onPrimaryContainer, 18.dp)
        }
        Spacer(GlanceModifier.width(10.dp))
        Column(modifier = GlanceModifier.defaultWeight()) {
            Text(
                text = item.title,
                style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 13.sp, fontWeight = FontWeight.Medium),
                maxLines = 1,
            )
            Text(
                text = "${item.source} · $time",
                style = TextStyle(color = if (running) GlanceTheme.colors.primary else GlanceTheme.colors.onSurfaceVariant, fontSize = 11.sp),
                maxLines = 1,
            )
        }
        Spacer(GlanceModifier.width(6.dp))
        when {
            running -> Text(
                text = "● " + text.getString(R.string.widget_live_badge),
                style = TextStyle(color = GlanceTheme.colors.onPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold),
                modifier = GlanceModifier.cornerRadius(10.dp).background(GlanceTheme.colors.primary).padding(horizontal = 8.dp, vertical = 3.dp),
                maxLines = 1,
            )
            item.done -> Icon(R.drawable.ic_widget_check, GlanceTheme.colors.primary, 18.dp)
            else -> Icon(R.drawable.ic_widget_chevron, GlanceTheme.colors.onSurfaceVariant, 18.dp)
        }
    }
}

@DrawableRes
private fun WidgetExamKind.iconRes(): Int = when (this) {
    WidgetExamKind.LIVE -> R.drawable.ic_widget_timer
    WidgetExamKind.GROUP -> R.drawable.ic_widget_groups
    WidgetExamKind.PERSONAL -> R.drawable.ic_widget_person
}

/** An empty or error state: an icon in a circle, a line of text, an optional pill button. */
@Composable
private fun Message(@DrawableRes icon: Int, message: String, button: Pair<String, Action>? = null) {
    Column(
        modifier = GlanceModifier.fillMaxSize(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = GlanceModifier.size(40.dp).cornerRadius(20.dp).background(GlanceTheme.colors.surface),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, GlanceTheme.colors.primary, 20.dp)
        }
        Spacer(GlanceModifier.height(Gap))
        Text(
            text = message,
            style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 13.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center),
            maxLines = 2,
        )
        if (button != null) {
            Spacer(GlanceModifier.height(Gap))
            Box(
                modifier = GlanceModifier
                    .cornerRadius(18.dp)
                    .background(GlanceTheme.colors.primary)
                    .clickable(button.second)
                    .padding(horizontal = 18.dp, vertical = 7.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = button.first,
                    style = TextStyle(color = GlanceTheme.colors.onPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold),
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun Icon(@DrawableRes res: Int, tint: ColorProvider, size: Dp, description: String? = null) {
    Image(
        provider = ImageProvider(res),
        contentDescription = description,
        colorFilter = ColorFilter.tint(tint),
        modifier = GlanceModifier.size(size),
    )
}

/**
 * Opens [link] in the app: the running activity gets it in `onNewIntent` (no second copy of the
 * app), a stopped one starts with it.
 */
@Composable
private fun openApp(link: AppLink): Action {
    val context = LocalContext.current
    val intent = Intent(context, MainActivity::class.java)
        .setData(Uri.parse(link.uri))
        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
    return actionStartActivity(intent)
}
