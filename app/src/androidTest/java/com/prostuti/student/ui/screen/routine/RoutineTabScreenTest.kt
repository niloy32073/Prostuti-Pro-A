package com.prostuti.student.ui.screen.routine

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.prostuti.student.R
import com.prostuti.student.data.model.enums.GroupRole
import com.prostuti.student.data.model.enums.SlotKind
import com.prostuti.student.data.model.enums.SlotMyStatus
import com.prostuti.student.data.model.enums.TopicGroupingLevel
import com.prostuti.student.data.model.response.ExamRoutine
import com.prostuti.student.data.model.response.RoutineSlot
import com.prostuti.student.data.model.response.StudyGroup
import com.prostuti.student.data.model.response.TopicGrouping
import com.prostuti.student.ui.screen.routine.slot.TAG_SLOT_CARD_PREFIX
import com.prostuti.student.ui.theme.ProstutiTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import java.time.LocalDate

/** The Routine tab: empty personal state, a day's exam, the mode toggle, and group mode. "Now" is Tue 10 Nov. */
@RunWith(AndroidJUnit4::class)
class RoutineTabScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val today = LocalDate.of(2026, 11, 10)
    private val now = Instant.parse("2026-11-10T12:00:00Z")

    private fun state() = RoutineTabUiState(isLoading = false, today = today, weekStart = LocalDate.of(2026, 11, 9), selectedDate = today)

    private fun slot(id: String, groupId: String?, title: String) = RoutineSlot(
        id = id, kind = SlotKind.ROUTINE, groupId = groupId, title = title, slotDate = "2026-11-10", opensAt = "2026-11-10T13:00:00",
        closesAt = "2026-11-10T17:00:00", topicIds = listOf("t1"), topicGrouping = TopicGrouping(TopicGroupingLevel.TOPICS),
        questionsCount = 25, durationMinutes = 25, markPerQuestion = 1.0, negativeMarkValue = 0.25, myStatus = SlotMyStatus.UPCOMING,
    )

    private fun show(state: RoutineTabUiState, actions: RoutineTabActions = RoutineTabActions()) {
        composeTestRule.setContent { ProstutiTheme { RoutineTabContent(state = state, actions = actions, clock = now) } }
    }

    @Test
    fun noPersonalRoutineOffersToCreateOne() {
        var created = 0
        show(state().copy(routineKnown = true), RoutineTabActions(onCreateRoutine = { created++ }))

        composeTestRule.onNodeWithText(context.getString(R.string.routine_tab_empty)).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TAG_ROUTINE_CREATE).performClick()
        assertEquals(1, created)
    }

    @Test
    fun personalRoutineShowsTodaysExamAndTheToggleSwitchesMode() {
        var mode: RoutineTabMode? = null
        val routine = ExamRoutine(
            id = "r1", status = "ACTIVE", startDate = "2026-11-08", durationDays = 30, windowOpenMinute = 0, windowCloseMinute = 1440,
            questionsCount = 25, durationMinutes = 25, markPerQuestion = 1.0, negativeMarkValue = 0.25, createdAt = "",
        )
        show(
            state().copy(routineKnown = true, routine = routine, routineSlots = listOf(slot("s1", null, "মঙ্গলবারের টেস্ট"))),
            RoutineTabActions(onSelectMode = { mode = it }),
        )

        composeTestRule.onNodeWithTag(TAG_SLOT_CARD_PREFIX + "s1").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag(TAG_ROUTINE_MENU).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TAG_ROUTINE_MODE_GROUP).performClick()
        assertEquals(RoutineTabMode.GROUP, mode)
    }

    @Test
    fun groupModeWithoutGroupsPointsToGroups() {
        var opened = 0
        show(state().copy(mode = RoutineTabMode.GROUP, groupsKnown = true), RoutineTabActions(onOpenGroups = { opened++ }))

        composeTestRule.onNodeWithTag(TAG_ROUTINE_NO_GROUPS).assertIsDisplayed()
        composeTestRule.onNodeWithText(context.getString(R.string.routine_tab_go_groups)).performClick()
        assertEquals(1, opened)
    }

    @Test
    fun groupModeCardsNameTheirGroup() {
        val groups = listOf(
            StudyGroup(id = "g1", name = "পদার্থ ক্লাব", description = null, isPublic = false, maxMembers = 500, memberCount = 3,
                createdAt = "", myRole = GroupRole.MEMBER, inviteCode = null),
        )
        show(state().copy(mode = RoutineTabMode.GROUP, groupsKnown = true, groups = groups, groupSlots = listOf(slot("s1", "g1", "সাপ্তাহিক"))))

        composeTestRule.onNodeWithText("পদার্থ ক্লাব").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText(context.getString(R.string.calendar_note)).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun withoutTheRoutineScopeTheTabSaysSo() {
        composeTestRule.setContent { ProstutiTheme { RoutineFeatureOff() } }
        composeTestRule.onNodeWithTag(TAG_ROUTINE_FEATURE_OFF).assertIsDisplayed()
        composeTestRule.onNodeWithText(context.getString(R.string.routine_feature_off)).assertIsDisplayed()
    }
}
