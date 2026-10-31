package com.prostuti.student.domain.calculator

import kotlin.math.PI

enum class AngleUnit(val indicator: String, private val perHalfTurn: Double) {
    DEG("D", 180.0),
    RAD("R", PI),
    GRAD("G", 200.0);

    fun toRadians(value: Double): Double = value * PI / perHalfTurn
    fun fromRadians(radians: Double): Double = radians * perHalfTurn / PI

    /** Converts [value] given in this unit into [target]. */
    fun convert(value: Double, target: AngleUnit): Double =
        if (this == target) value else value / perHalfTurn * target.perHalfTurn

    /** How many units make a quarter turn — used to return exact zeros for sin/cos/tan. */
    val quarterTurn: Double get() = perHalfTurn / 2
}
