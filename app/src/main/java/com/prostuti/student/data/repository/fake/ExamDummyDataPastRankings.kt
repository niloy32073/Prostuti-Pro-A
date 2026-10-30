package com.prostuti.student.data.repository.fake

/** Ranking numbers for every entry in [ExamDummyData.pastExamsByGroupId] — split out from
 * [ExamDummyData] only to keep that file from growing even longer. */
internal object ExamDummyDataPastRankings {
    private val histogram = listOf(30, 50, 65, 42, 28, 58, 46, 34)

    fun build(): Map<String, ExamDummyData.RankingSeed> {
        val hsc27Past = ExamDummyData.pastExamsByGroupId[ExamDummyData.GROUP_HSC27].orEmpty()
        val dhakaPast = ExamDummyData.pastExamsByGroupId[ExamDummyData.GROUP_DHAKA].orEmpty()
        val entries = (hsc27Past + dhakaPast).mapIndexed { i, past ->
            past.examId to ExamDummyData.RankingSeed(
                submittedCount = past.submittedCount,
                totalParticipants = past.totalCount,
                averagePercent = past.averagePercent,
                myRank = if (past.myScoreLabel != null) (i % 7) + 1 else null,
                histogramBars = histogram.shuffled(kotlin.random.Random(i)),
                myHistogramBarIndex = i % histogram.size,
                extraRows = emptyList()
            )
        }
        return entries.toMap()
    }
}
