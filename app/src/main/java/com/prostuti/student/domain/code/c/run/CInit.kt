package com.prostuti.student.domain.code.c.run

/** A declaration's initializer, already laid out by [CInitLayout]. */
sealed interface CInit {
    /** One expression converted to the declared type (a scalar, or a whole struct: `struct P q = p;`). */
    class Single(val expr: CExpr) : CInit

    /**
     * `{ … }` or a string filling a `char` array: [slots] are memory slots of the object and
     * [values] what goes there (a [CExpr] to evaluate, or a character code as [Long]), in order.
     * Slots not listed are zero, as C requires.
     */
    class Placed(val slots: IntArray, val values: List<Any>) : CInit
}
