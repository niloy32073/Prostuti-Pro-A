package com.prostuti.student.domain

import com.prostuti.student.data.model.response.AnswerSheetData
import com.prostuti.student.data.model.response.AnswerSheetQuestion
import com.prostuti.student.data.model.response.ExamQuestionWithOptions
import com.prostuti.student.data.model.response.FetchPassageData
import com.prostuti.student.data.model.response.Option
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MergeAnswerSheetTest {

    private fun sheetQ(id: String, order: Int, result: String, selected: String?, correct: String?, topic: String? = "t1") =
        AnswerSheetQuestion(
            mcqId = id, order = order, subjectId = "s1", chapterId = "c1", topicId = topic,
            selectedOptionId = selected, correctOptionId = correct, result = result, isCorrect = result == "CORRECT",
            explanationText = "exp $id", explanationImageLink = "https://cdn/e_$id.png"
        )

    private val content = listOf(
        ExamQuestionWithOptions(
            mcqId = "m1", order = 1, questionText = "Q1", questionImageLink = "https://cdn/q1.png",
            options = listOf(
                Option(id = "b", optionText = "B", order = 2, optionImageLink = "https://cdn/b.png"),
                Option(id = "a", optionText = "A", order = 1)
            )
        ),
        ExamQuestionWithOptions(mcqId = "m2", order = 2, questionText = "Q2", options = listOf(Option(id = "c", order = 1)))
    )

    @Test
    fun `joins text, images and marks the correct option from the answer sheet`() {
        val sheet = AnswerSheetData("at", "ex", "SNAPSHOT", listOf(sheetQ("m1", 1, "WRONG", selected = "b", correct = "a")))

        val q = mergeAnswerSheet(sheet, content).single()

        assertEquals("Q1", q.review.questionText)
        assertEquals("https://cdn/q1.png", q.review.questionImageLink)
        assertEquals("https://cdn/e_m1.png", q.review.explanationImageLink)
        assertEquals("exp m1", q.review.explanationText)
        assertEquals(listOf("a", "b"), q.review.options.map { it.id })   // sorted by option order
        assertTrue(q.review.options.first { it.id == "a" }.isCorrect)
        assertFalse(q.review.options.first { it.id == "b" }.isCorrect)
        assertEquals("https://cdn/b.png", q.review.options.first { it.id == "b" }.optionImageLink)
        assertEquals(false, q.review.isCorrect)
        assertEquals("b", q.review.selectedOptionId)
        assertEquals("t1", q.topicId)
        assertEquals("c1", q.chapterId)
    }

    @Test
    fun `result maps to isCorrect true, false, null and order follows the answer sheet`() {
        val sheet = AnswerSheetData(
            "at", "ex", null,
            listOf(
                sheetQ("m2", 3, "SKIPPED", selected = null, correct = "c"),
                sheetQ("m1", 1, "CORRECT", selected = "a", correct = "a")
            )
        )

        val merged = mergeAnswerSheet(sheet, content)

        assertEquals(listOf("m1", "m2"), merged.map { it.review.mcqId })
        assertEquals(true, merged[0].review.isCorrect)
        assertNull(merged[1].review.isCorrect)
    }

    @Test
    fun `a question missing from the exam keeps its result without text or options`() {
        val sheet = AnswerSheetData("at", "ex", null, listOf(sheetQ("gone", 1, "CORRECT", selected = "x", correct = "x")))

        val q = mergeAnswerSheet(sheet, content).single()

        assertNull(q.review.questionText)
        assertTrue(q.review.options.isEmpty())
        assertEquals(true, q.review.isCorrect)
    }

    @Test
    fun `passage comes from the question content, or from the fetched map when only its ID is given`() {
        val withPassage = content[0].copy(passageId = "p1", passage = FetchPassageData("p1", passageText = "উদ্দীপক ১", passageImage = "https://cdn/p1.png"))
        val idOnly = content[1].copy(passageId = "p2", passage = null)
        val sheet = AnswerSheetData(
            "at", "ex", null,
            listOf(sheetQ("m1", 1, "CORRECT", "a", "a"), sheetQ("m2", 2, "WRONG", "c", "x"))
        )

        val merged = mergeAnswerSheet(sheet, listOf(withPassage, idOnly), mapOf("p2" to FetchPassageData("p2", passageText = "উদ্দীপক ২")))

        assertEquals("উদ্দীপক ১", merged[0].review.passage?.passageText)
        assertEquals("https://cdn/p1.png", merged[0].review.passage?.passageImage)
        assertEquals("উদ্দীপক ২", merged[1].review.passage?.passageText)
    }

    @Test
    fun `question without a passage has none`() {
        val sheet = AnswerSheetData("at", "ex", null, listOf(sheetQ("m1", 1, "CORRECT", "a", "a")))
        assertNull(mergeAnswerSheet(sheet, content).single().review.passage)
    }
}
