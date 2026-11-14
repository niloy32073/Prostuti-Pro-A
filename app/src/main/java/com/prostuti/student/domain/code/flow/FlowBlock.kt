package com.prostuti.student.domain.code.flow

/**
 * One structured piece of a program, as a flowchart draws it. Both the C and the Python reader
 * produce these, so the layout only knows about flowchart shapes. [line] is the 1-based source line.
 */
sealed interface FlowBlock {
    val line: Int

    /** [callee] is set when the step calls a function of the same program. */
    data class Step(val kind: StepKind, val text: String, override val line: Int, val callee: String? = null) : FlowBlock

    /** `if` / `else`; `else if` / `elif` is a [Branch] inside [no]. [no] is empty when there is no else. */
    data class Branch(val condition: String, val yes: List<FlowBlock>, val no: List<FlowBlock>, override val line: Int) : FlowBlock

    /**
     * A loop. C `for` keeps its [init] and [update] as steps around the condition;
     * [orElse] is Python's `while … else` / `for … else` block (skipped by `break`).
     */
    data class Loop(
        val kind: LoopKind,
        val condition: String,
        val body: List<FlowBlock>,
        override val line: Int,
        val init: Step? = null,
        val update: Step? = null,
        val orElse: List<FlowBlock> = emptyList(),
    ) : FlowBlock

    /** C `switch` ([fallsThrough], `break` leaves it) or Python `match` (no fall-through, `break` belongs to a loop). */
    data class Switch(val subject: String, val cases: List<SwitchCase>, val fallsThrough: Boolean, override val line: Int) : FlowBlock

    /**
     * A C label or `goto`, drawn as a connector circle named [label]: a goto's flow ends in its
     * circle ([isGoto]); a label's circle sits in the flow where the goto continues.
     */
    data class Connector(val label: String, val isGoto: Boolean, override val line: Int) : FlowBlock

    /** `break`, `continue`, `return …` or `raise …`. */
    data class Jump(val kind: JumpKind, val text: String, override val line: Int, val callee: String? = null) : FlowBlock
}
