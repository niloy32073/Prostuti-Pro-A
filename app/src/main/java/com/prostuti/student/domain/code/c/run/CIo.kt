package com.prostuti.student.domain.code.c.run

/** The running program's console. Called on the interpreter's own thread. */
interface CIo {
    fun write(text: String)

    /** Blocks until the student types a line; null = no more input. */
    fun readLine(): String?

    /** True once the run should stop (stop button, limits). */
    val stopped: Boolean
}
