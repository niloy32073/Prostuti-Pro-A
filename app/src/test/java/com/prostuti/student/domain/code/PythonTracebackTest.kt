package com.prostuti.student.domain.code

import com.prostuti.student.domain.code.run.PythonTraceback
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PythonTracebackTest {
    @Test fun innermostStudentFrameWins() {
        val tb = "Traceback (most recent call last):\n  File \"main.py\", line 4, in <module>\n  File \"main.py\", line 2, in f\nZeroDivisionError: division by zero\n"
        assertEquals(2, PythonTraceback.errorLine(tb))
    }

    @Test fun syntaxErrorAndNoLine() {
        assertEquals(1, PythonTraceback.errorLine("  File \"main.py\", line 1\n    print(\"hi\"\nSyntaxError: '(' was never closed\n"))
        assertNull(PythonTraceback.errorLine("SystemExit: 3\n"))
    }
}
