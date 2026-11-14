package com.prostuti.student.domain.code.c.run

import com.prostuti.student.domain.code.run.TraceCommand
import com.prostuti.student.domain.code.run.TraceStep

/** Receives a traced C program's pauses; [pause] blocks until the student picks what happens next. */
fun interface CTracer {
    fun pause(step: TraceStep): TraceCommand
}
