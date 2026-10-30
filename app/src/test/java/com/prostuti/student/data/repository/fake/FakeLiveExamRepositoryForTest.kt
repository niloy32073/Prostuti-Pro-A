package com.prostuti.student.data.repository.fake

import com.prostuti.student.data.model.response.ApiResponse
import com.prostuti.student.data.model.response.TimedExam
import com.prostuti.student.data.repository.LiveExamRepository

fun fakeTimedExam(
    examId: String = "live_1",
    titleBn: String = "পদার্থবিজ্ঞান — মডেল টেস্ট",
    windowStartEpochMillis: Long = 0L,
    windowEndEpochMillis: Long = 1_000L,
    totalParticipants: Int = 100
) = TimedExam(
    examId = examId,
    kind = com.prostuti.student.data.model.enums.TimedExamKind.LIVE,
    titleBn = titleBn,
    subtitleBn = "লাইভ এক্সাম",
    windowStartEpochMillis = windowStartEpochMillis,
    windowEndEpochMillis = windowEndEpochMillis,
    examDurationMinutes = 60,
    questionCount = 25,
    markPerQuestion = 1f,
    negativeMark = 0.25f,
    totalParticipants = totalParticipants
)

/** Configurable fake of [LiveExamRepository] for ViewModel-layer unit tests (distinct name from
 * the production FakeLiveExamRepository in app/src/main, which this test source set also sees on
 * its classpath — a same-named class here would collide with it). */
class FakeLiveExamRepositoryForTest(
    var liveExamListResult: ApiResponse<List<TimedExam>> = ApiResponse.Success(listOf(fakeTimedExam())),
    var liveExamDetailResult: ApiResponse<TimedExam> = ApiResponse.Success(fakeTimedExam())
) : LiveExamRepository {

    var liveExamDetailCallCount = 0
    var lastDetailExamId: String? = null

    override suspend fun liveExamList(): ApiResponse<List<TimedExam>> = liveExamListResult

    override suspend fun liveExamDetail(examId: String): ApiResponse<TimedExam> {
        lastDetailExamId = examId
        liveExamDetailCallCount++
        return liveExamDetailResult
    }
}
