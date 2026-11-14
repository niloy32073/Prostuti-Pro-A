package com.prostuti.student.domain.code.c.run

/**
 * One piece of memory: a variable, an array, a struct, a string literal, a malloc() block or a
 * function (the target of function pointers). Each scalar takes one slot (Long for integers and
 * chars, Double for floating values, [CPtr] for pointers); null means "never given a value".
 * [elemSlots] is the size of one element and [elemType] its type (both for messages: `pts[2].y`).
 */
internal class MemObj(
    val name: String,
    var slots: Array<Any?>,
    var elemSlots: Int,
    val readOnly: Boolean = false,
    var elemType: CType? = null,
) {
    var freed = false
    /** A malloc() block not yet used through a typed pointer: its size in bytes; -1 once typed. */
    var untypedBytes = -1L
    var zeroWhenTyped = false
    val id = nextId++

    private companion object {
        var nextId = 1
    }
}
