package com.prostuti.student.ui.screen.examengine

import com.prostuti.student.data.model.response.ExamAttemptData

object ExamAttemptStore {
    private var pending: ExamAttemptData? = null

    fun put(data: ExamAttemptData) { pending = data }
    fun consume(): ExamAttemptData? = pending.also { pending = null }
}
