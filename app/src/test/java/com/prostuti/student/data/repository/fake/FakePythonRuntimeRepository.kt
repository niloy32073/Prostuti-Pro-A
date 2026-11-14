package com.prostuti.student.data.repository.fake

import com.prostuti.student.data.repository.PythonRuntimeRepository
import com.prostuti.student.domain.code.run.RuntimeState
import kotlinx.coroutines.flow.MutableStateFlow

class FakePythonRuntimeRepository(initial: RuntimeState = RuntimeState.NotInstalled) : PythonRuntimeRepository {
    override val state = MutableStateFlow(initial)
    var installs = 0
    override fun install() { installs++ }
}
