package com.prostuti.student.domain

import com.prostuti.student.data.repository.CodeRunRepository

/** Answers the running program's `input()`. */
class SendRunInputUseCase(private val repo: CodeRunRepository) {
    operator fun invoke(line: String) = repo.sendInput(line)
}
