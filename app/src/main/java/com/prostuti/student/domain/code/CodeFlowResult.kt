package com.prostuti.student.domain.code

import com.prostuti.student.domain.code.flow.FlowProgram

/** What reading a program gave: its flowcharts, or the first [error] (then [program] is null). */
data class CodeFlowResult(val program: FlowProgram?, val error: CodeError?)
