package com.prostuti.student.ui.screen.qb

import com.prostuti.student.data.model.response.ApiResponse
import com.prostuti.student.data.model.response.ExamQuestionWithOptions
import com.prostuti.student.data.model.response.FetchPassageData
import com.prostuti.student.data.model.response.Option
import com.prostuti.student.data.repository.fake.FakeBookmarkRepository
import com.prostuti.student.data.repository.fake.FakeReportRepository
import com.prostuti.student.domain.ObserveReportedIdsUseCase
import com.prostuti.student.domain.SubmitMcqReportUseCase
import com.prostuti.student.data.repository.fake.FakeQbMcqExamRepository
import com.prostuti.student.domain.AddBookmarkUseCase
import com.prostuti.student.domain.GetExamQuestionsWithOptionsUseCase
import com.prostuti.student.domain.ObserveBookmarkedIdsUseCase
import com.prostuti.student.domain.RemoveBookmarkUseCase
import com.prostuti.student.ui.screen.examengine.component.McqGridItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class QuestionReaderViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val bookmarkRepo = FakeBookmarkRepository()
    private val reportRepo = FakeReportRepository()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
    }

    private val passage = FetchPassageData(id = "p1", passageText = "অনুচ্ছেদ", passageImage = "https://cdn.prostuti.com/p1.png")

    private fun mcq(id: String, order: Int, passageId: String? = null, passageOrder: Int? = null) = ExamQuestionWithOptions(
        mcqId = id,
        order = order,
        questionText = "প্রশ্ন $id",
        passageId = passageId,
        passageOrder = passageOrder,
        passage = if (passageId != null) passage else null,
        options = listOf(Option(id = "$id-o1", optionText = "ক", order = 1, isCorrect = true), Option(id = "$id-o2", optionText = "খ", order = 2, isCorrect = false))
    )

    private fun buildViewModel(result: ApiResponse<List<ExamQuestionWithOptions>>): Pair<QuestionReaderViewModel, FakeQbMcqExamRepository> {
        val repo = FakeQbMcqExamRepository(questionsWithOptionsResult = result)
        val vm = QuestionReaderViewModel(
            examId = "exam-1",
            getExamQuestionsWithOptions = GetExamQuestionsWithOptionsUseCase(repo),
            observeBookmarkedIdsUseCase = ObserveBookmarkedIdsUseCase(bookmarkRepo),
            addBookmarkUseCase = AddBookmarkUseCase(bookmarkRepo),
            removeBookmarkUseCase = RemoveBookmarkUseCase(bookmarkRepo),
            submitMcqReportUseCase = SubmitMcqReportUseCase(reportRepo),
            observeReportedIdsUseCase = ObserveReportedIdsUseCase(reportRepo)
        )
        return vm to repo
    }

    @Test
    fun `loads questions sorted by order and groups the passage after question 3`() {
        // Sent out of order on purpose; the passage questions are 4 and 8 in the API.
        val (vm, _) = buildViewModel(
            ApiResponse.Success(
                listOf(
                    mcq("q8", 8, "p1", passageOrder = 2), mcq("q1", 1), mcq("q2", 2), mcq("q3", 3),
                    mcq("q4", 4, "p1", passageOrder = 1), mcq("q5", 5), mcq("q6", 6), mcq("q7", 7)
                )
            )
        )

        val state = vm.state.value
        assertFalse(state.isLoading)
        assertEquals(8, state.questionCount)

        val shown = state.items.flatMap { item ->
            when (item) {
                is McqGridItem.Standalone -> listOf(item.index + 1 to item.question.mcqId)
                is McqGridItem.PassageGroup -> item.items.map { it.index + 1 to it.value.mcqId }
            }
        }
        assertEquals(
            listOf(1 to "q1", 2 to "q2", 3 to "q3", 4 to "q4", 5 to "q8", 6 to "q5", 7 to "q6", 8 to "q7"),
            shown
        )

        val group = state.items[3] as McqGridItem.PassageGroup
        assertEquals("https://cdn.prostuti.com/p1.png", group.passage?.passageImage)
    }

    @Test
    fun `failure shows an error and refresh retries`() {
        val (vm, repo) = buildViewModel(ApiResponse.Failure("Exam not found.", "NOT_FOUND"))

        assertEquals("Exam not found.", vm.state.value.errors.values.single())
        assertTrue(vm.state.value.items.isEmpty())

        vm.refresh()
        assertEquals(2, repo.questionsWithOptionsCalls)
    }

    @Test
    fun `clearError removes the error`() {
        val (vm, _) = buildViewModel(ApiResponse.Failure("boom", "INTERNAL_ERROR"))
        val tag = vm.state.value.errors.keys.single()

        vm.clearError(tag)

        assertTrue(vm.state.value.errors.isEmpty())
    }

    @Test
    fun `toggleFavorite is a set toggle`() {
        val (vm, _) = buildViewModel(ApiResponse.Success(listOf(mcq("q1", 1))))

        vm.toggleFavorite("q1")
        assertTrue("q1" in vm.state.value.favoriteIds)
        vm.toggleFavorite("q1")
        assertTrue("q1" !in vm.state.value.favoriteIds)
    }

    @Test
    fun `submitReport sends once, marks the question reported and raises the sent notice`() {
        val (vm, _) = buildViewModel(ApiResponse.Success(listOf(mcq("q1", 1))))

        vm.submitReport("q1", "উত্তর ভুল আছে", "")
        vm.submitReport("q1", "অন্যান্য সমস্যা", "")

        assertEquals(listOf(Triple("q1", "উত্তর ভুল আছে", null as String?)), reportRepo.reportCalls)
        assertTrue("q1" in vm.state.value.reportedIds)
        assertTrue(vm.state.value.isReportSent)
        vm.onReportSentConsumed()
        assertFalse(vm.state.value.isReportSent)
    }

    @Test
    fun `a failed report shows the error and leaves the question reportable`() {
        reportRepo.reportResult = ApiResponse.Failure("No internet connection", "NETWORK_ERROR")
        val (vm, _) = buildViewModel(ApiResponse.Success(listOf(mcq("q1", 1))))

        vm.submitReport("q1", "উত্তর ভুল আছে", "")

        assertTrue("q1" !in vm.state.value.reportedIds)
        assertEquals("No internet connection", vm.state.value.errors["REPORT"])
    }

    @Test
    fun `toggleAllAnswers reveals every question then clears them all`() {
        val (vm, _) = buildViewModel(ApiResponse.Success(listOf(mcq("q1", 1), mcq("q2", 2))))

        vm.toggleAllAnswers()
        assertTrue(vm.state.value.showAllAnswers)
        assertEquals(setOf("q1", "q2"), vm.state.value.revealedAnswers)

        vm.toggleAllAnswers()
        assertFalse(vm.state.value.showAllAnswers)
        assertTrue(vm.state.value.revealedAnswers.isEmpty())
    }

    @Test
    fun `hiding one answer turns off show-all, revealing the last one turns it on`() {
        val (vm, _) = buildViewModel(ApiResponse.Success(listOf(mcq("q1", 1), mcq("q2", 2))))
        vm.toggleAllAnswers()

        vm.toggleAnswer("q1")
        assertFalse(vm.state.value.showAllAnswers)
        assertEquals(setOf("q2"), vm.state.value.revealedAnswers)

        vm.toggleAnswer("q1")
        assertTrue(vm.state.value.showAllAnswers)
    }

    @Test
    fun `is_correct is kept on options`() {
        val (vm, _) = buildViewModel(ApiResponse.Success(listOf(mcq("q1", 1))))

        val question = (vm.state.value.items.single() as McqGridItem.Standalone).question
        assertEquals(listOf(true, false), question.options.map { it.isCorrect })
    }
}
