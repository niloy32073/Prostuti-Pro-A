package com.prostuti.student.domain.code.c.run

/** stdin: lines typed by the student, read character by character (each line ends with '\n'). */
internal class CInput(private val io: CIo) {
    private val buf = StringBuilder()
    private var pos = 0
    private var eof = false

    /** The next character without taking it; waits for a new line when needed. -1 at end of input. */
    fun peek(): Int {
        if (pos >= buf.length) {
            if (eof) return -1
            val line = io.readLine()
            if (line == null) { eof = true; return -1 }
            buf.setLength(0)
            pos = 0
            buf.append(line).append('\n')
        }
        return buf[pos].code
    }

    fun next(): Int {
        val c = peek()
        if (c >= 0) pos++
        return c
    }

    fun skipSpaces() {
        while (true) {
            val c = peek()
            if (c < 0 || !Character.isWhitespace(c)) return
            pos++
        }
    }
}
