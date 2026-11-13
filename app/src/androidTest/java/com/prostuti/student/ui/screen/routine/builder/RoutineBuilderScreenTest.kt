package com.prostuti.student.ui.screen.routine.builder

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.prostuti.student.R
import com.prostuti.student.data.model.enums.GroupRole
import com.prostuti.student.data.model.enums.TopicGroupingLevel
import com.prostuti.student.data.model.response.ExamRoutine
import com.prostuti.student.data.model.response.PlannedSlot
import com.prostuti.student.data.model.response.RoutinePlan
import com.prostuti.student.data.model.response.StudyGroup
import com.prostuti.student.data.model.response.Subject
import com.prostuti.student.data.model.response.TopicGrouping
import com.prostuti.student.ui.screen.group.GroupDetailContent
import com.prostuti.student.ui.screen.group.GroupDetailUiState
import com.prostuti.student.ui.screen.group.TAG_ADMIN_SCHEDULE
import com.prostuti.student.ui.screen.group.TAG_CREATE_ROUTINE
import com.prostuti.student.ui.screen.group.TAG_SCHEDULE_ADHOC
import com.prostuti.student.ui.theme.ProstutiTheme
import org.junit.Assert.assertEquals
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

/** Step 3 differs by owner and mode, preview warnings always show, and GroupDetail's admin entry points. */
@RunWith(AndroidJUnit4::class)
class RoutineBuilderScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val today = LocalDate.of(2026, 11, 8)

    private fun state(owner: RoutineOwner, mode: RoutineBuilderMode = RoutineBuilderMode.ROUTINE, step: BuilderStep = BuilderStep.CONFIG) =
        RoutineBuilderUiState(
            owner = owner, mode = mode, step = step, isChecking = false, isLoadingSubjects = false,
            form = RoutineBuilderForm.initial(owner, today), today = today, nowMinute = 600,
        )

    private fun show(state: RoutineBuilderUiState, actions: RoutineBuilderActions = RoutineBuilderActions()) {
        composeTestRule.setContent { ProstutiTheme { RoutineBuilderContent(state = state, actions = actions) } }
    }

    @Test
    fun personalRoutineHidesTheWindow() {
        show(state(RoutineOwner.Personal))

        composeTestRule.onNodeWithTag(TAG_BUILDER_WINDOW).assertDoesNotExist()
        composeTestRule.onNodeWithTag(TAG_BUILDER_WEEKDAYS).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TAG_BUILDER_EXAM_DATE).assertDoesNotExist()
    }

    @Test
    fun groupRoutineShowsTheWindow() {
        show(state(RoutineOwner.Group("g1")))

        composeTestRule.onNodeWithTag(TAG_BUILDER_WINDOW).performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag(TAG_BUILDER_NEXT).assertIsEnabled()
    }

    @Test
    fun oneOffExamAsksForADateAndNameInsteadOfDays() {
        var next = 0
        show(state(RoutineOwner.Group("g1"), RoutineBuilderMode.ADHOC), RoutineBuilderActions(onNext = { next++ }))

        composeTestRule.onNodeWithTag(TAG_BUILDER_EXAM_DATE).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TAG_BUILDER_TITLE).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TAG_BUILDER_WEEKDAYS).assertDoesNotExist()
        composeTestRule.onNodeWithText(context.getString(R.string.routine_schedule_adhoc)).assertIsDisplayed().performClick()
        assertEquals(1, next)
    }

    @Test
    fun anInvalidFormBlocksNext() {
        val s = state(RoutineOwner.Group("g1"))
        show(s.copy(form = s.form.copy(weekdays = emptySet())))

        composeTestRule.onNodeWithTag(TAG_BUILDER_NEXT).assertIsNotEnabled()
        composeTestRule.onNodeWithText(context.getString(R.string.routine_problem_no_weekday)).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun previewShowsEveryWarningAndTheDays() {
        val plan = RoutinePlan(
            examDays = 1,
            slots = listOf(PlannedSlot("2026-11-11", "2026-11-11T13:00:00", "2026-11-11T17:00:00", listOf("t1"), TopicGrouping(TopicGroupingLevel.TOPICS))),
            warnings = listOf("Topics outnumber exam days", "Only 12 questions in topic X"),
        )
        show(state(RoutineOwner.Group("g1"), step = BuilderStep.PREVIEW).copy(plan = plan))

        composeTestRule.onNodeWithTag(TAG_BUILDER_WARNING).assertIsDisplayed()
        composeTestRule.onNodeWithText("Topics outnumber exam days").assertIsDisplayed()
        composeTestRule.onNodeWithText("Only 12 questions in topic X").assertIsDisplayed()
        composeTestRule.onNodeWithText(context.getString(R.string.routine_weekday_wed), substring = true).assertExists()
        composeTestRule.onNodeWithTag(TAG_BUILDER_NEXT).assertIsEnabled()
    }

    @Test
    fun aSubjectTapReachesTheViewModel() {
        var toggled: String? = null
        val subject = Subject(id = "phy", nameBn = "পদার্থবিজ্ঞান", nameEn = "Physics", createdAt = "", createdBy = "")
        show(state(RoutineOwner.Group("g1"), step = BuilderStep.SUBJECTS).copy(subjects = listOf(subject)), RoutineBuilderActions(onToggleSubject = { toggled = it }))

        composeTestRule.onNodeWithTag(TAG_BUILDER_NEXT).assertIsNotEnabled()
        composeTestRule.onNodeWithTag(TAG_BUILDER_SUBJECT_PREFIX + "phy").performClick()
        assertEquals("phy", toggled)
    }

    @Test
    fun groupDetailOffersRoutineAndOneOffExamToAdminsOnly() {
        var create = 0
        var adhoc = 0
        val group = StudyGroup(
            id = "g1", name = "HSC", description = null, isPublic = false, maxMembers = 500, memberCount = 3,
            createdAt = "2026-11-02T10:00:00", myRole = GroupRole.ADMIN, inviteCode = null,
        )
        var current by mutableStateOf(GroupDetailUiState(isLoading = false, group = group, routineKnown = true))
        composeTestRule.setContent {
            ProstutiTheme {
                GroupDetailContent(
                    state = current, onBack = {}, onSeeAllMembers = {}, onCopyCode = {}, onShareCode = { _, _ -> },
                    onCreateRoutine = { create++ }, onScheduleAdhoc = { adhoc++ }, onRefresh = {}, onClearError = {},
                )
            }
        }

        composeTestRule.onNodeWithTag(TAG_CREATE_ROUTINE).performScrollTo().performClick()
        composeTestRule.onNodeWithTag(TAG_SCHEDULE_ADHOC).performScrollTo().performClick()
        assertEquals(1 to 1, create to adhoc)

        current = current.copy(routine = ExamRoutine(
            id = "r1", groupId = "g1", status = "ACTIVE", startDate = "2026-11-08", durationDays = 30, windowOpenMinute = 1140,
            windowCloseMinute = 1380, questionsCount = 25, durationMinutes = 25, markPerQuestion = 1.0, negativeMarkValue = 0.25, createdAt = "",
        ))
        composeTestRule.onNodeWithTag(TAG_CREATE_ROUTINE).assertDoesNotExist()
        composeTestRule.onNodeWithTag(TAG_SCHEDULE_ADHOC).assertExists()

        current = current.copy(group = group.copy(myRole = GroupRole.MEMBER))
        composeTestRule.onNodeWithTag(TAG_ADMIN_SCHEDULE).assertDoesNotExist()
    }

    /** The real app module builds the builder's ViewModel, saved-state handle included. */
    @Test
    fun koinBuildsTheBuilderViewModel() {
        var built: RoutineBuilderViewModel? = null
        composeTestRule.setContent {
            built = koinViewModel(parameters = { parametersOf(RoutineOwner.Group("g1"), RoutineBuilderMode.ADHOC) })
        }
        composeTestRule.waitForIdle()
        assertEquals(RoutineOwner.Group("g1"), built?.state?.value?.owner)
        assertEquals(RoutineBuilderMode.ADHOC, built?.state?.value?.mode)
    }
}
