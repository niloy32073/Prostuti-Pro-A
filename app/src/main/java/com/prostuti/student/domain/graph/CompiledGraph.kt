package com.prostuti.student.domain.graph

import com.prostuti.student.domain.calculator.AngleUnit

class CompiledGraph(
    val lines: List<CompiledLine>,
    val parameters: Map<String, Double>,
    val angleUnit: AngleUnit,
) {
    fun line(id: Long): CompiledLine? = lines.firstOrNull { it.id == id }

    companion object {
        val EMPTY = CompiledGraph(emptyList(), emptyMap(), AngleUnit.RAD)
    }
}
