package com.prostuti.student.ui.screen.home

import com.prostuti.student.data.model.enums.AttemptMode
import com.prostuti.student.data.model.response.ApiResponse
import com.prostuti.student.data.model.response.ExamAttemptResultResponse
import com.prostuti.student.data.model.response.ExamSummary
import com.prostuti.student.data.model.response.MetaData
import com.prostuti.student.data.repository.fake.FakeExamAttemptRepositoryForEngineTest
import com.prostuti.student.data.repository.fake.FakeExamAttemptRepositoryForEngineTest.Companion.DEFAULT_START_AT
import com.prostuti.student.data.model.response.ExamResponseAckResponse
import com.prostuti.student.data.repository.fake.FakeAttemptProgressRepository
import com.prostuti.student.data.repository.fake.FakeUserRepository
import com.prostuti.student.domain.GetMeUseCase
import com.prostuti.student.domain.GetResumablePracticeAttemptUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private val repo = FakeExamAttemptRepositoryForEngineTest()
    private val progressRepo = FakeAttemptProgressRepository()
    private val userRepo = FakeUserRepository()

    @Before fun setup() { Dispatchers.setMain(UnconfinedTestDispatcher()) }
    @After fun teardown() { Dispatchers.resetMain() }

    private fun buildViewModel() = HomeViewModel(GetResumablePracticeAttemptUseCase(repo, progressRepo), GetMeUseCase(userRepo))

    private fun unfinished(
        id: String = "attempt1",
        examId: String = "exam1",
        mode: AttemptMode = AttemptMode.PRACTICE
    ) = ExamAttemptResultResponse(
        id = id, examId = examId, studentId = "s1", mode = mode, startAt = DEFAULT_START_AT,
        endAt = null, rightAnswerCount = 0, wrongAnswerCount = 0, skipCount = 0, totalScore = 0.0
    )

    private fun exam(id: String = "exam1", durationMinutes: Int = 60) = ExamSummary(
        id = id, titleBn = "রসায়ন মডেল টেস্ট", titleEn = "Chemistry model test", duration = durationMinutes
    )

    // ── Demo sections (still static) ───────────────────────────────────────────────────────

    @Test fun initialStateHasTwoUpcomingExamCardsWithOnePrimary() {
        val state = buildViewModel().state.value
        assertEquals(2, state.upcomingExams.size)
        assertTrue(state.upcomingExams.count { it.isPrimary } == 1)
    }

    @Test fun initialStateHasThreeLeaderboardEntriesRankedFirstSecondThird() {
        val state = buildViewModel().state.value
        assertEquals(3, state.leaderboardTop3.size)
        assertEquals(listOf(2, 1, 3), state.leaderboardTop3.map { it.rank })
        assertTrue(state.showMyRankCard)
    }

    // ── Profile (real data, GET auth/me) ───────────────────────────────────────────────────

    @Test fun profileIsFetchedFreshOnOpenAndShown() {
        userRepo.results = listOf(ApiResponse.Success(FakeUserRepository.me()))

        val state = buildViewModel().state.value

        assertEquals(listOf(true), userRepo.refreshArgs)
        assertEquals("Muwaz Ahmed", state.profile?.fullName)
        assertEquals("muwy", state.profile?.username)
    }

    @Test fun profileFailureWithNoCacheLeavesNameEmpty() {
        userRepo.results = listOf(ApiResponse.Failure(message = "offline", errorCode = "NETWORK_ERROR"))

        assertNull(buildViewModel().state.value.profile)
    }

    @Test fun profileFailureStillShowsCachedProfile() {
        userRepo.results = listOf(
            ApiResponse.Failure(message = "offline", errorCode = "NETWORK_ERROR"),
            ApiResponse.Success(FakeUserRepository.me(firstName = "Cached"))
        )

        assertEquals("Cached", buildViewModel().state.value.profile?.firstName)
    }

    // ── Continue attempt (real data) ───────────────────────────────────────────────────────

    @Test fun refreshShowsNewestPracticeAttemptWithTitleAndRemainingTime() = runTest {
        repo.listMyAttemptsResult = ApiResponse.Success(
            listOf(unfinished(), unfinished(id = "attempt2"), unfinished(id = "attempt3")),
            metaData = MetaData(currentPage = 1, pageLimit = 20, totalItems = 3, totalPages = 1)
        )
        repo.getExamResult = ApiResponse.Success(exam(durationMinutes = 60))
        repo.attemptResponsesResult = ApiResponse.Success(
            listOf(ExamResponseAckResponse(mcqId = "m1", selectedOptionId = "o1", responseTimeMs = 15 * 60_000))
        )

        val viewModel = buildViewModel()
        viewModel.refreshContinueAttempt()

        val state = viewModel.state.value
        assertEquals("attempt1", state.continueAttempt?.attemptId)
        assertEquals("রসায়ন মডেল টেস্ট", state.continueAttempt?.titleBn)
        assertEquals(2, state.continueAttempt?.otherUnfinishedCount)
        assertEquals(45 * 60, state.continueRemainingSeconds) // 60 min − 15 min used
        assertEquals(null to true, repo.lastListArgs) // all exams, unfinished only
    }

    @Test fun remainingTimeUsesLocalTimeWhenAheadOfServer() = runTest {
        repo.listMyAttemptsResult = ApiResponse.Success(listOf(unfinished()))
        repo.getExamResult = ApiResponse.Success(exam(durationMinutes = 30))
        progressRepo.saved["attempt1"] = 20 * 60_000L

        val viewModel = buildViewModel()
        viewModel.refreshContinueAttempt()

        assertEquals(10 * 60, viewModel.state.value.continueRemainingSeconds)
    }

    @Test fun leftoverLiveAttemptsAreSubmittedAndNeverShown() = runTest {
        repo.listMyAttemptsResult = ApiResponse.Success(
            listOf(unfinished(id = "live1", mode = AttemptMode.LIVE), unfinished(id = "practice1"))
        )

        val viewModel = buildViewModel()
        viewModel.refreshContinueAttempt()

        assertEquals(listOf("live1"), repo.submittedAttemptIds)
        assertEquals("practice1", viewModel.state.value.continueAttempt?.attemptId)
        assertEquals(0, viewModel.state.value.continueAttempt?.otherUnfinishedCount)
    }

    @Test fun onlyLiveAttemptsLeftMeansNoCard() = runTest {
        repo.listMyAttemptsResult = ApiResponse.Success(listOf(unfinished(id = "live1", mode = AttemptMode.LIVE)))

        val viewModel = buildViewModel()
        viewModel.refreshContinueAttempt()

        assertNull(viewModel.state.value.continueAttempt)
        assertEquals(listOf("live1"), repo.submittedAttemptIds)
    }

    @Test fun refreshWithNoUnfinishedAttemptHidesTheCard() = runTest {
        val viewModel = buildViewModel()
        viewModel.refreshContinueAttempt()

        assertNull(viewModel.state.value.continueAttempt)
        assertNull(viewModel.state.value.continueRemainingSeconds)
    }

    @Test fun examLookupFailureStillShowsTheCardWithoutTitleOrTime() = runTest {
        repo.listMyAttemptsResult = ApiResponse.Success(listOf(unfinished()))
        repo.getExamResult = ApiResponse.Failure(message = "forbidden", errorCode = "FORBIDDEN")

        val viewModel = buildViewModel()
        viewModel.refreshContinueAttempt()

        val state = viewModel.state.value
        assertEquals("attempt1", state.continueAttempt?.attemptId)
        assertNull(state.continueAttempt?.titleBn)
        assertNull(state.continueRemainingSeconds)
    }

    @Test fun listFailureKeepsWhatWasShownBefore() = runTest {
        repo.listMyAttemptsResult = ApiResponse.Success(listOf(unfinished()))
        val viewModel = buildViewModel()
        viewModel.refreshContinueAttempt()

        repo.listMyAttemptsResult = ApiResponse.Failure(message = "offline", errorCode = "NETWORK_ERROR")
        viewModel.refreshContinueAttempt()

        assertEquals("attempt1", viewModel.state.value.continueAttempt?.attemptId)
    }

    @Test fun refreshDropsTheCardOnceTheAttemptIsSubmitted() = runTest {
        repo.listMyAttemptsResult = ApiResponse.Success(listOf(unfinished()))
        val viewModel = buildViewModel()
        viewModel.refreshContinueAttempt()

        repo.listMyAttemptsResult = ApiResponse.Success(emptyList())
        viewModel.refreshContinueAttempt()

        assertNull(viewModel.state.value.continueAttempt)
    }
}
