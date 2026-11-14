package com.prostuti.student.domain.code.run

/** Reads the student's line number out of a Python error. */
object PythonTraceback {
    private val frame = Regex("""File "main\.py", line (\d+)""")

    /** The line the error happened on: the innermost `main.py` frame. Null when none is named. */
    fun errorLine(traceback: String): Int? = frame.findAll(traceback).lastOrNull()?.groupValues?.get(1)?.toIntOrNull()
}
