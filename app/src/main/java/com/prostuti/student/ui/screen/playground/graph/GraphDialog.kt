package com.prostuti.student.ui.screen.playground.graph

/** Dialogs of the graph builder that must survive rotation. */
sealed interface GraphDialog {
    /** Ask for a name; [exitAfter] closes the screen once saved. */
    data class SaveName(val exitAfter: Boolean) : GraphDialog

    /** Leaving with unsaved changes: save, discard or stay. */
    data object UnsavedChanges : GraphDialog
}
