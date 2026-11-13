package com.prostuti.student.ui.screen.playground

import com.prostuti.student.data.service.local.SavedGraphEntity

data class PlaygroundHubUiState(
    val graphs: List<SavedGraphEntity> = emptyList(),
    val isLoading: Boolean = true,
    /** Saved graph whose rename dialog is open. */
    val renaming: SavedGraphEntity? = null,
    /** Saved graph waiting for delete confirmation. */
    val deleting: SavedGraphEntity? = null,
)
