package com.prostuti.student.domain.code.flow

import com.prostuti.student.domain.code.CodeScan

/** Steps that call one of [names] (and are not input/output) become call symbols. */
internal fun markCalls(blocks: List<FlowBlock>, names: Set<String>): List<FlowBlock> = blocks.map { block ->
    when (block) {
        is FlowBlock.Step -> if (block.kind == StepKind.PROCESS) {
            CodeScan.calledNames(block.text).firstOrNull { it in names }
                ?.let { block.copy(kind = StepKind.CALL, callee = it) } ?: block
        } else block
        is FlowBlock.Jump -> CodeScan.calledNames(block.text).firstOrNull { it in names }?.let { block.copy(callee = it) } ?: block
        is FlowBlock.Branch -> block.copy(yes = markCalls(block.yes, names), no = markCalls(block.no, names))
        is FlowBlock.Loop -> block.copy(
            body = markCalls(block.body, names),
            orElse = markCalls(block.orElse, names),
            init = block.init?.let { markCalls(listOf(it), names).single() as FlowBlock.Step },
            update = block.update?.let { markCalls(listOf(it), names).single() as FlowBlock.Step },
        )
        is FlowBlock.Switch -> block.copy(cases = block.cases.map { it.copy(body = markCalls(it.body, names)) })
        is FlowBlock.Connector -> block
    }
}
