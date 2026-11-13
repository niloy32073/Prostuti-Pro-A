package com.prostuti.student.ui.screen.group

import com.prostuti.student.ui.screen.routine.slot.TAG_PREP_CARD
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
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
import com.prostuti.student.data.model.response.RoutineSlot
import com.prostuti.student.data.model.response.StudyGroup
import com.prostuti.student.data.model.response.TopicGrouping
import com.prostuti.student.domain.routine.TopicLabel
import com.prostuti.student.ui.screen.routine.slot.TAG_SLOT_CARD_PREFIX
import com.prostuti.student.ui.screen.routine.slot.TAG_SLOT_DELETE
import com.prostuti.student.ui.screen.routine.slot.TAG_SLOT_RESCHEDULE
import com.prostuti.student.ui.screen.routine.slot.TAG_SLOT_SHEET
import com.prostuti.student.ui.theme.ProstutiTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import java.time.LocalDate
import com.prostuti.student.data.model.response.CurriculumName

/** GroupDetail's exam cards and slot sheet, the routine calendar's days, and GroupList's badge. "Now" is 10 Nov, 18:00 Dhaka. */
@RunWith(AndroidJUnit4::class)
class GroupScheduleScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val vectorName = CurriculumName("ভেক্টর", "Vector")
    private val now = Instant.parse("2026-11-10T12:00:00Z")

    private fun group(role: GroupRole) = StudyGroup(
        id = "g1", name = "HSC", description = null, isPublic = false, maxMembers = 500, memberCount = 3,
        createdAt = "2026-11-02T10:00:00", myRole = role, inviteCode = null,
    )

    /** Window 19:00–23:00 Dhaka. */
    private fun slot(id: String, date: String = "2026-11-10", kind: SlotKind = SlotKind.ADHOC, status: SlotMyStatus = SlotMyStatus.UPCOMING, title: String? = null) =
        RoutineSlot(
            id = id, kind = kind, groupId = "g1", title = title, slotDate = date, opensAt = "${date}T13:00:00", closesAt = "${date}T17:00:00",
            topicIds = listOf("t1"), topicGrouping = TopicGrouping(TopicGroupingLevel.TOPICS), questionsCount = 25, durationMinutes = 25,
            markPerQuestion = 1.0, negativeMarkValue = 0.25, myStatus = status,
        )

    private fun showDetail(state: GroupDetailUiState, onOpenSlot: (String) -> Unit = {}) {
        composeTestRule.setContent {
            ProstutiTheme {
                GroupDetailContent(
                    state = state, onBack = {}, onSeeAllMembers = {}, onCopyCode = {}, onShareCode = { _, _ -> },
                    onOpenSlot = onOpenSlot, onRefresh = {}, onClearError = {}, clock = now,
                )
            }
        }
    }

    @Test
    fun memberSeesTodaysExamCardAndCanOpenIt() {
        var opened: String? = null
        showDetail(
            GroupDetailUiState(
                isLoading = false, group = group(GroupRole.MEMBER), routineKnown = true,
                todaySlots = listOf(slot("s1", title = "শুক্রবারের মডেল টেস্ট")), labels = mapOf("s1" to TopicLabel.Topics(listOf(vectorName), 0)),
            ),
            onOpenSlot = { opened = it },
        )

        composeTestRule.onNodeWithText("শুক্রবারের মডেল টেস্ট").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText(vectorName.inLanguage(context.resources.configuration.locales[0].language)).assertIsDisplayed()
        composeTestRule.onNodeWithText(context.getString(R.string.slot_adhoc_chip)).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TAG_ADMIN_SCHEDULE).assertDoesNotExist()
        composeTestRule.onNodeWithTag(TAG_SLOT_CARD_PREFIX + "s1").performClick()
        assertEquals("s1", opened)
    }

    @Test
    fun noExamTodayPointsToTheNextOneAndTheCalendar() {
        showDetail(GroupDetailUiState(isLoading = false, group = group(GroupRole.MEMBER), routineKnown = true, nextSlot = slot("n", date = "2026-11-12")))

        composeTestRule.onNodeWithTag(TAG_NO_EXAM_TODAY).performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag(TAG_OPEN_CALENDAR).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun memberWithoutAnyScheduleIsToldTheAdminHasntSetOne() {
        showDetail(GroupDetailUiState(isLoading = false, group = group(GroupRole.MEMBER), routineKnown = true))

        composeTestRule.onNodeWithText(context.getString(R.string.group_member_no_routine)).performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag(TAG_OPEN_CALENDAR).assertDoesNotExist()
    }

    @Test
    fun onlyAnAdminGetsMoveAndDeleteInTheSheet() {
        val base = GroupDetailUiState(isLoading = false, routineKnown = true, todaySlots = listOf(slot("s1")), openSlotId = "s1")
        showDetail(base.copy(group = group(GroupRole.ADMIN)))
        composeTestRule.onNodeWithTag(TAG_SLOT_SHEET).assertExists()
        composeTestRule.onNodeWithTag(TAG_SLOT_RESCHEDULE).assertExists()
        composeTestRule.onNodeWithTag(TAG_SLOT_DELETE).assertExists()
    }

    @Test
    fun memberSheetHasNoActions() {
        showDetail(GroupDetailUiState(isLoading = false, group = group(GroupRole.MEMBER), routineKnown = true, todaySlots = listOf(slot("s1")), openSlotId = "s1"))
        composeTestRule.onNodeWithTag(TAG_SLOT_SHEET).assertExists()
        composeTestRule.onNodeWithTag(TAG_SLOT_RESCHEDULE).assertDoesNotExist()
        composeTestRule.onNodeWithTag(TAG_SLOT_DELETE).assertDoesNotExist()
    }

    @Test
    fun calendarShowsTheWeekAndTheNextExamOnAQuietDay() {
        var picked: LocalDate? = null
        val today = LocalDate.of(2026, 11, 10)
        val state = GroupCalendarUiState(
            isLoading = false, group = group(GroupRole.MEMBER), today = today, weekStart = LocalDate.of(2026, 11, 9),
            selectedDate = LocalDate.of(2026, 11, 11), weekSlots = listOf(slot("thu", date = "2026-11-12", kind = SlotKind.ROUTINE, title = "সাপ্তাহিক")),
        )
        composeTestRule.setContent {
            ProstutiTheme { GroupRoutineCalendarContent(state = state, onBack = {}, onSelectDate = { picked = it }, clock = now) }
        }

        composeTestRule.onNodeWithTag(TAG_CAL_DAY_PREFIX + "2026-11-11").assertIsSelected()
        composeTestRule.onNodeWithTag(TAG_CAL_DAY_TEXT).assertIsDisplayed()
        // The next exam's card (a wide calendar also lists it in the week's exams).
        composeTestRule.onNode(hasText("সাপ্তাহিক", substring = true) and hasAnyAncestor(hasTestTag(TAG_PREP_CARD))).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TAG_CAL_MENU).assertDoesNotExist()
        composeTestRule.onNodeWithTag(TAG_CAL_DAY_PREFIX + "2026-11-12").performClick()
        assertEquals(LocalDate.of(2026, 11, 12), picked)
    }

    @Test
    fun groupListCardShowsTodaysBadge() {
        val open = slot("s1", status = SlotMyStatus.OPEN)
        composeTestRule.setContent {
            ProstutiTheme {
                GroupListContent(
                    state = GroupListUiState(isLoading = false, groups = listOf(group(GroupRole.MEMBER)), todaySlots = mapOf("g1" to listOf(open))),
                    onBack = {}, onAdd = {}, onOpenGroup = {}, onRefresh = {}, onClearError = {},
                    clock = Instant.parse("2026-11-10T14:00:00Z"),
                )
            }
        }
        val prefix = context.getString(R.string.group_badge_open_untitled, "").substringBefore("%").trim().take(8)
        composeTestRule.onNodeWithTag(TAG_GROUP_BADGE_PREFIX + "g1", useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithText(prefix, substring = true).assertIsDisplayed()
    }
}
