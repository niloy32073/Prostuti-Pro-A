package com.prostuti.student.domain.graph

/** A comparison in an equation, inequality or piecewise condition. */
enum class Relation(val symbol: String, val isStrict: Boolean) {
    EQ("=", false), LT("<", true), LE("≤", false), GT(">", true), GE("≥", false);

    val isInequality: Boolean get() = this != EQ

    /** The same relation with its sides swapped: a < b ⇔ b > a. */
    fun flipped(): Relation = when (this) {
        EQ -> EQ
        LT -> GT
        LE -> GE
        GT -> LT
        GE -> LE
    }
}
