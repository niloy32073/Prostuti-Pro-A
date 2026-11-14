package com.prostuti.student.domain.code.c.run

import com.prostuti.student.domain.code.CodeErrorKind
import com.prostuti.student.domain.code.CodeParseException
import com.prostuti.student.domain.code.c.CLexer

/** Source text → checked [CProgram]: lexing, `#define` expansion, parsing and type checking. */
object CCompiler {
    /** Throws [CProgramException] (compile errors) with the line. */
    fun compile(source: String): CProgram {
        val tokens = try {
            CLexer(source.replace("\r\n", "\n").replace('\r', '\n'), keepDirectives = true).tokens()
        } catch (e: CodeParseException) {
            val reason = when (e.error.kind) {
                CodeErrorKind.UNTERMINATED_COMMENT -> "a /* comment is never closed with */"
                else -> "a string or character is missing its closing quote"
            }
            throw CProgramException(e.error.line, reason, isCompile = true)
        }
        return CProgramParser(CPreprocessor().process(tokens)).parse()
    }
}
