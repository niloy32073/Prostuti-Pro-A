package com.prostuti.student.data.repository.fake

import com.prostuti.student.data.model.enums.TimedExamKind
import com.prostuti.student.data.model.response.ApiResponse
import com.prostuti.student.data.model.response.TimedExam
import com.prostuti.student.data.repository.LiveExamRepository
import com.prostuti.student.data.repository.TimedExamRepository

class FakeLiveExamRepository(
    private val timedExamRepository: TimedExamRepository
) : LiveExamRepository {

    override suspend fun liveExamList(): ApiResponse<List<TimedExam>> =
        ApiResponse.Success(ExamDummyData.liveExams.map { buildTimedExam(it) })

    override suspend fun liveExamDetail(examId: String): ApiResponse<TimedExam> {
        val seed = ExamDummyData.liveExams.find { it.examId == examId }
            ?: return ApiResponse.Failure("এক্সাম পাওয়া যায়নি", "NOT_FOUND")
        return ApiResponse.Success(buildTimedExam(seed))
    }

    private suspend fun buildTimedExam(seed: ExamDummyData.LiveExamSeed): TimedExam {
        val (start, end) = timedExamRepository.resolveWindow(seed.examId)
        return TimedExam(
            examId = seed.examId, kind = TimedExamKind.LIVE, titleBn = seed.titleBn,
            subtitleBn = "লাইভ এক্সাম · সম্পূর্ণ স্ট্রিম একসাথে",
            windowStartEpochMillis = start, windowEndEpochMillis = end,
            examDurationMinutes = seed.examDurationMinutes, questionCount = seed.questionCount,
            markPerQuestion = seed.markPerQuestion, negativeMark = seed.negativeMark,
            totalParticipants = seed.totalParticipants
        )
    }
}
