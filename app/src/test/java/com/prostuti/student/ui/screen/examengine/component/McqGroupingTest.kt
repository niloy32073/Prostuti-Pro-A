package com.prostuti.student.ui.screen.examengine.component

import com.prostuti.student.data.model.response.FetchPassageData
import com.prostuti.student.data.model.response.Question
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class McqGroupingTest {

    private fun q(id: String, order: Int, passageId: String? = null, passageOrder: Int? = null) =
        Question(mcqId = id, options = emptyList(), order = order, passageId = passageId, passageOrder = passageOrder)

    /** (display number 1-based, mcqId) in the order the screen shows them. */
    private fun shown(items: List<McqGridItem>): List<Pair<Int, String>> = items.flatMap { item ->
        when (item) {
            is McqGridItem.Standalone -> listOf(item.index + 1 to item.question.mcqId)
            is McqGridItem.PassageGroup -> item.items.map { it.index + 1 to it.value.mcqId }
        }
    }

    @Test
    fun `passage questions are numbered where they appear on screen, not by API position`() {
        // API order: q1 q2 q3 p-a q5 q6 q7 p-b — the passage group shows up after q3.
        val questions = listOf(
            q("q1", 1), q("q2", 2), q("q3", 3),
            q("pa", 4, passageId = "p"),
            q("q5", 5), q("q6", 6), q("q7", 7),
            q("pb", 8, passageId = "p")
        )

        val result = shown(groupQuestionsByPassage(questions, emptyMap()))

        assertEquals(
            listOf(1 to "q1", 2 to "q2", 3 to "q3", 4 to "pa", 5 to "pb", 6 to "q5", 7 to "q6", 8 to "q7"),
            result
        )
    }

    @Test
    fun `questions inside a passage follow passage_order`() {
        val questions = listOf(
            q("q1", 1),
            q("second", 2, passageId = "p", passageOrder = 2),
            q("first", 3, passageId = "p", passageOrder = 1)
        )

        val result = shown(groupQuestionsByPassage(questions, emptyMap()))

        assertEquals(listOf(1 to "q1", 2 to "first", 3 to "second"), result)
    }

    @Test
    fun `passage data is attached to its group`() {
        val passage = FetchPassageData(id = "p", passageText = "text", passageImage = "https://cdn/x.png")
        val items = groupQuestionsByPassage(listOf(q("a", 1, "p"), q("b", 2, "p")), mapOf("p" to passage))

        assertEquals(1, items.size)
        val group = items.single()
        assertTrue(group is McqGridItem.PassageGroup)
        assertEquals(passage, (group as McqGridItem.PassageGroup).passage)
    }
}
