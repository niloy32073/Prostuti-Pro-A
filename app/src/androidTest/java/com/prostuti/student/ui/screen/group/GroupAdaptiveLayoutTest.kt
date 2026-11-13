package com.prostuti.student.ui.screen.group

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.window.core.layout.WindowSizeClass
import com.prostuti.student.data.model.enums.GroupRole
import com.prostuti.student.data.model.enums.SlotKind
import com.prostuti.student.data.model.enums.SlotMyStatus
import com.prostuti.student.data.model.enums.TopicGroupingLevel
import com.prostuti.student.data.model.response.LeaderboardEntry
import com.prostuti.student.data.model.response.RoutineSlot
import com.prostuti.student.data.model.response.StudyGroup
import com.prostuti.student.data.model.response.TopicGrouping
import com.prostuti.student.data.repository.GroupExamStats
import com.prostuti.student.ui.component.LocalPaneSizeClass
import com.prostuti.student.ui.component.PaneSized
import com.prostuti.student.ui.screen.routine.builder.BuilderStep
import com.prostuti.student.ui.screen.routine.builder.RoutineBuilderContent
import com.prostuti.student.ui.screen.routine.builder.RoutineBuilderForm
import com.prostuti.student.ui.screen.routine.builder.RoutineBuilderUiState
import com.prostuti.student.ui.screen.routine.builder.RoutineOwner
import com.prostuti.student.ui.screen.routine.builder.RoutineBuilderMode
import com.prostuti.student.ui.screen.routine.builder.RoutineBuilderActions
import com.prostuti.student.ui.screen.routine.builder.TAG_BUILDER_RULES_PANE
import com.prostuti.student.ui.screen.routine.builder.TAG_BUILDER_STEP_RAIL
import com.prostuti.student.ui.theme.ProstutiTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import java.time.LocalDate

/**
 * Group and routine screens lay out by the width they get (GROUP_ROUTINE_PLAN.md §10): one column
 * when narrow, panes side by side when wide. The width is set through [LocalPaneSizeClass], so this
 * runs the same on a phone or a tablet.
 */
@RunWith(AndroidJUnit4::class)
class GroupAdaptiveLayoutTest {
    @get:Rule
    val composeTestRule = createComposeRule()
    private val now = Instant.parse("2026-11-10T12:00:00Z")
    private val narrow = WindowSizeClass.compute(400f, 800f)
    private val medium = WindowSizeClass.compute(700f, 900f)
    private val expanded = WindowSizeClass.compute(1100f, 800f)

    private fun show(sizeClass: WindowSizeClass, content: @Composable () -> Unit) {
        composeTestRule.setContent { ProstutiTheme { CompositionLocalProvider(LocalPaneSizeClass provides sizeClass) { content() } } }
    }

    private val group = StudyGroup(
        id = "g1", name = "HSC", description = null, isPublic = false, maxMembers = 500, memberCount = 3,
        createdAt = "2026-11-02T10:00:00", myRole = GroupRole.MEMBER, inviteCode = null,
    )
    private fun slot(id: String, date: String) = RoutineSlot(
        id = id, kind = SlotKind.ROUTINE, groupId = "g1", title = "সাপ্তাহিক", slotDate = date, opensAt = "${date}T13:00:00", closesAt = "${date}T17:00:00",
        topicIds = listOf("t1"), topicGrouping = TopicGrouping(TopicGroupingLevel.TOPICS), questionsCount = 25, durationMinutes = 25,
        markPerQuestion = 1.0, negativeMarkValue = 0.25, myStatus = SlotMyStatus.UPCOMING,
    )

    @Composable
    private fun Detail() = GroupDetailContent(
        state = GroupDetailUiState(isLoading = false, group = group, routineKnown = true),
        onBack = {}, onSeeAllMembers = {}, onCopyCode = {}, onShareCode = { _, _ -> }, onRefresh = {}, onClearError = {}, clock = now,
    )

    @Test
    fun groupDetailPutsMembersBesideTheExamsWhenWide() {
        show(medium) { Detail() }
        composeTestRule.onNodeWithTag(TAG_DETAIL_SIDE_PANE).assertExists()
        composeTestRule.onNodeWithTag(TAG_EXAM_SECTION).assertExists()
    }

    @Test
    fun groupDetailIsOneColumnWhenNarrow() {
        show(narrow) { Detail() }
        composeTestRule.onNodeWithTag(TAG_DETAIL_SIDE_PANE).assertDoesNotExist()
        composeTestRule.onNodeWithTag(TAG_EXAM_SECTION).assertExists()
    }

    @Test
    fun aNarrowPaneGetsTheNarrowLayoutEvenOnAWideWindow() {
        // A detail pane beside the group list is narrower than the window: PaneSized measures it.
        composeTestRule.setContent { ProstutiTheme { Box(Modifier.width(360.dp)) { PaneSized { Detail() } } } }
        composeTestRule.onNodeWithTag(TAG_DETAIL_SIDE_PANE).assertDoesNotExist()
    }

    @Test
    fun calendarAddsTheWeeksExamsWhenExpanded() {
        val state = GroupCalendarUiState(
            isLoading = false, group = group, today = LocalDate.of(2026, 11, 10), weekStart = LocalDate.of(2026, 11, 9),
            selectedDate = LocalDate.of(2026, 11, 10), weekSlots = listOf(slot("s1", "2026-11-12")),
        )
        show(expanded) { GroupRoutineCalendarContent(state = state, onBack = {}, clock = now) }
        composeTestRule.onNodeWithTag(TAG_CAL_WEEK_EXAMS).assertExists()
        composeTestRule.onNodeWithTag(TAG_CAL_DAY_TEXT).assertExists()
    }

    @Test
    fun calendarHasNoThirdPaneWhenMedium() {
        val state = GroupCalendarUiState(
            isLoading = false, group = group, today = LocalDate.of(2026, 11, 10), weekStart = LocalDate.of(2026, 11, 9),
            selectedDate = LocalDate.of(2026, 11, 10),
        )
        show(medium) { GroupRoutineCalendarContent(state = state, onBack = {}, clock = now) }
        composeTestRule.onNodeWithTag(TAG_CAL_WEEK_EXAMS).assertDoesNotExist()
        composeTestRule.onNodeWithTag(TAG_CAL_DAY_TEXT).assertExists()
    }

    @Test
    fun rankingKeepsTheSummaryBesideTheListWhenWide() {
        val state = GroupRankingUiState(
            isLoading = false, groupName = "HSC", memberCount = 40, totalParticipants = 1, page = 1, myUserId = "me",
            entries = listOf(LeaderboardEntry(1, "me", "muwy", 18.0, 610)), stats = GroupExamStats(12, 70, List(10) { 30 }),
        )
        show(medium) { GroupRankingContent(state, "সাপ্তাহিক", onBack = {}) }
        composeTestRule.onNodeWithTag(TAG_RANK_SUMMARY).assertExists()
        composeTestRule.onNodeWithTag(TAG_RANK_ROW_PREFIX + "me").assertExists()
    }

    @Test
    fun builderShowsTheStepRailAndRulesPaneWhenExpanded() {
        val today = LocalDate.of(2026, 11, 8)
        val owner = RoutineOwner.Group("g1")
        val state = RoutineBuilderUiState(
            owner = owner, mode = RoutineBuilderMode.ROUTINE, step = BuilderStep.CONFIG, isChecking = false, isLoadingSubjects = false,
            form = RoutineBuilderForm.initial(owner, today), today = today, nowMinute = 600,
        )
        show(expanded) { RoutineBuilderContent(state = state, actions = RoutineBuilderActions()) }
        composeTestRule.onNodeWithTag(TAG_BUILDER_STEP_RAIL).assertExists()
        composeTestRule.onNodeWithTag(TAG_BUILDER_RULES_PANE).assertExists()
    }

    @Test
    fun builderIsOneColumnWhenNarrow() {
        val today = LocalDate.of(2026, 11, 8)
        val owner = RoutineOwner.Group("g1")
        val state = RoutineBuilderUiState(
            owner = owner, mode = RoutineBuilderMode.ROUTINE, step = BuilderStep.CONFIG, isChecking = false, isLoadingSubjects = false,
            form = RoutineBuilderForm.initial(owner, today), today = today, nowMinute = 600,
        )
        show(narrow) { RoutineBuilderContent(state = state, actions = RoutineBuilderActions()) }
        composeTestRule.onNodeWithTag(TAG_BUILDER_STEP_RAIL).assertDoesNotExist()
        composeTestRule.onNodeWithTag(TAG_BUILDER_RULES_PANE).assertDoesNotExist()
    }
}
