package com.prostuti.student.ui.screen.playground.code

enum class RunStatus { IDLE, PREPARING, STARTING, RUNNING, WAITING_INPUT, PAUSED, DONE, ERROR, STOPPED, TIME_LIMIT, OUTPUT_LIMIT, CRASHED;

    val isActive: Boolean get() = this == PREPARING || this == STARTING || this == RUNNING || this == WAITING_INPUT || this == PAUSED
}
