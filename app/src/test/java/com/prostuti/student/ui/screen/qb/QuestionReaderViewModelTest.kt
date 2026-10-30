package com.prostuti.student.ui.screen.qb

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class QuestionReaderViewModelTest {

    @Test
    fun `initial state loads all demo exam questions with nothing revealed`() {
        val viewModel = QuestionReaderViewModel()
        val state = viewModel.state.value

        assertEquals(10, state.questions.size)
        assertTrue(state.revealedAnswers.isEmpty())
        assertFalse(state.showAllAnswers)
    }

    @Test
    fun `toggleAllAnswers reveals every question then clears them all`() {
        val viewModel = QuestionReaderViewModel()

        viewModel.toggleAllAnswers()
        var state = viewModel.state.value
        assertTrue(state.showAllAnswers)
        assertEquals(state.questions.map { it.id }.toSet(), state.revealedAnswers)

        viewModel.toggleAllAnswers()
        state = viewModel.state.value
        assertFalse(state.showAllAnswers)
        assertTrue(state.revealedAnswers.isEmpty())
    }

    @Test
    fun `toggling one answer turns off the showAllAnswers flag`() {
        val viewModel = QuestionReaderViewModel()
        viewModel.toggleAllAnswers()

        val firstId = viewModel.state.value.questions.first().id
        viewModel.toggleAnswer(firstId)

        assertFalse(viewModel.state.value.showAllAnswers)
        assertTrue(firstId !in viewModel.state.value.revealedAnswers)
    }

    @Test
    fun `toggleFavorite is a set toggle`() {
        val viewModel = QuestionReaderViewModel()
        val qid = viewModel.state.value.questions.first().id

        viewModel.toggleFavorite(qid)
        assertTrue(qid in viewModel.state.value.favoriteIds)
        viewModel.toggleFavorite(qid)
        assertTrue(qid !in viewModel.state.value.favoriteIds)
    }
}
