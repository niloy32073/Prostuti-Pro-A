package com.prostuti.student.data.model.response

import kotlinx.serialization.Serializable

/** The score-spread histogram + rank list shown once an exam's window has closed. */
@Serializable
data class WindowRanking(
    val submittedCount: Int,
    val totalParticipants: Int,
    val averagePercent: Int,
    val myRank: Int?,
    /** Relative bar heights, 0..100 — a coarse histogram, not exact buckets. */
    val histogramBars: List<Int>,
    val myHistogramBarIndex: Int,
    val rows: List<RankRow>
)

@Serializable
data class RankRow(
    val rank: Int,
    val name: String,
    val initial: String,
    val scoreLabel: String,
    val isMe: Boolean
)
