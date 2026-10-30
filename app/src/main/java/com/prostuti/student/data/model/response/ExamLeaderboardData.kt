package com.prostuti.student.data.model.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ExamLeaderboardData(
    @SerialName("exam_id")
    val examId: String,
    @SerialName("page_number")
    val pageNumber: Int,
    @SerialName("page_size")
    val pageSize: Int,
    @SerialName("total_participants")
    val totalParticipants: Int,
    @SerialName("computed_at")
    val computedAt: String,
    @SerialName("entries")
    val entries: List<LeaderboardEntry>
)

@Serializable
data class LeaderboardEntry(
    @SerialName("rank")
    val rank: Int,
    @SerialName("user_id")
    val userId: String,
    @SerialName("username")
    val username: String,
    @SerialName("score")
    val score: Double,
    @SerialName("time_taken")
    val timeTakenSec: Int
)
