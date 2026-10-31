package com.prostuti.student.domain.calculator

sealed interface LinearSolution {
    data class Unique(val values: List<Double>) : LinearSolution
    data object NoSolution : LinearSolution
    data object InfiniteSolutions : LinearSolution
}
