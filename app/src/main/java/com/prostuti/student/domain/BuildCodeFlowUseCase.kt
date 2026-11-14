package com.prostuti.student.domain

import com.prostuti.student.domain.code.CodeFlowResult
import com.prostuti.student.domain.code.CodeLanguage
import com.prostuti.student.domain.code.CodeParseException
import com.prostuti.student.domain.code.c.CFlowParser
import com.prostuti.student.domain.code.python.PyFlowParser

/** Reads [code] in [language] into flowchart blocks. Never throws: a syntax problem comes back as an error. */
class BuildCodeFlowUseCase {
    operator fun invoke(language: CodeLanguage, code: String): CodeFlowResult = try {
        val program = when (language) {
            CodeLanguage.C -> CFlowParser().parse(code)
            CodeLanguage.PYTHON -> PyFlowParser().parse(code)
        }
        CodeFlowResult(program, null)
    } catch (e: CodeParseException) {
        CodeFlowResult(null, e.error)
    }
}
