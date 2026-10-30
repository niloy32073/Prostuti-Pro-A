package com.prostuti.student.ui.screen.group

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GroupRoutineWindowUiStateTest {

    @Test fun totalMarksIsQuestionCountTimesMarkPerQuestion() {
        val state = GroupRoutineWindowUiState(questionCount = 25, markPerQuestion = 1f)
        assertEquals(25f, state.totalMarks)
    }

    @Test fun cannotSaveWithNoExamWeekDaysChosen() {
        val state = GroupRoutineWindowUiState(examWeekDays = emptySet())
        assertFalse(state.canSave)
    }

    @Test fun cannotSaveWhenWindowEndIsNotAfterStart() {
        val state = GroupRoutineWindowUiState(windowStartMinuteOfDay = 600, windowEndMinuteOfDay = 600)
        assertFalse(state.canSave)
    }

    @Test fun canSaveWithValidWeekDaysAndWindow() {
        val state = GroupRoutineWindowUiState(examWeekDays = setOf(2, 5), windowStartMinuteOfDay = 600, windowEndMinuteOfDay = 900)
        assertTrue(state.canSave)
    }
}
