package com.prostuti.student.domain.code.c.run

/** A pointer: a slot inside [obj]; [obj] null is NULL. Its element type is known statically. */
internal data class CPtr(val obj: MemObj?, val index: Int) {
    companion object {
        val NULL = CPtr(null, 0)
    }
}
