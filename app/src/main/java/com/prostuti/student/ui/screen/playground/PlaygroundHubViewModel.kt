package com.prostuti.student.ui.screen.playground

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prostuti.student.data.service.local.SavedGraphEntity
import com.prostuti.student.domain.DeleteSavedGraphUseCase
import com.prostuti.student.domain.ObserveSavedGraphsUseCase
import com.prostuti.student.domain.RenameSavedGraphUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** The Playground hub: its tools and the student's saved graphs ("My graphs"). */
class PlaygroundHubViewModel(
    observeSavedGraphs: ObserveSavedGraphsUseCase,
    private val renameSavedGraph: RenameSavedGraphUseCase,
    private val deleteSavedGraph: DeleteSavedGraphUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(PlaygroundHubUiState())
    val state: StateFlow<PlaygroundHubUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            observeSavedGraphs().collect { graphs -> _state.update { it.copy(graphs = graphs, isLoading = false) } }
        }
    }

    fun startRename(graph: SavedGraphEntity) = _state.update { it.copy(renaming = graph) }
    fun startDelete(graph: SavedGraphEntity) = _state.update { it.copy(deleting = graph) }
    fun dismissDialogs() = _state.update { it.copy(renaming = null, deleting = null) }

    fun rename(name: String) {
        val target = _state.value.renaming ?: return
        _state.update { it.copy(renaming = null) }
        viewModelScope.launch { renameSavedGraph(target.id, name) }
    }

    fun confirmDelete() {
        val target = _state.value.deleting ?: return
        _state.update { it.copy(deleting = null) }
        viewModelScope.launch { deleteSavedGraph(target.id) }
    }
}
