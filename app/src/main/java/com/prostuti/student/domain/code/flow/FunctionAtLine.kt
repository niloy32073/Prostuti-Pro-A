package com.prostuti.student.domain.code.flow

/**
 * The function whose code holds [line]: the innermost function defined around it, else the entry
 * (`main` / the main program). Used to show the chart a traced program is in.
 */
fun FlowProgram.functionAt(line: Int): FlowFunction? =
    functions.filter { !it.isEntry && line in it.line..lastLine(it) }.maxByOrNull { it.line }
        ?: functions.firstOrNull { it.isEntry }

private fun lastLine(f: FlowFunction): Int = maxOf(f.line, lastLine(f.body))

private fun lastLine(blocks: List<FlowBlock>): Int = blocks.maxOfOrNull { b ->
    when (b) {
        is FlowBlock.Step -> b.line
        is FlowBlock.Jump -> b.line
        is FlowBlock.Connector -> b.line
        is FlowBlock.Branch -> maxOf(b.line, lastLine(b.yes), lastLine(b.no))
        is FlowBlock.Loop -> maxOf(b.line, lastLine(b.body), lastLine(b.orElse), b.update?.line ?: 0)
        is FlowBlock.Switch -> maxOf(b.line, b.cases.maxOfOrNull { maxOf(it.line, lastLine(it.body)) } ?: 0)
    }
} ?: 0
