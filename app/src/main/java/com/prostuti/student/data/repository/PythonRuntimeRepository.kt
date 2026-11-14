package com.prostuti.student.data.repository

import com.prostuti.student.domain.code.run.RuntimeState
import kotlinx.coroutines.flow.StateFlow

/** The Python runtime, an on-demand Play module. */
interface PythonRuntimeRepository {
    val state: StateFlow<RuntimeState>
    fun install()
}
