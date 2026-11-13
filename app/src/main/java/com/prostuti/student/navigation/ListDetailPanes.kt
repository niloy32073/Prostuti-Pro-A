package com.prostuti.student.navigation

import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.layout.PaneAdaptedValue
import androidx.compose.material3.adaptive.layout.SupportingPaneScaffoldRole
import androidx.compose.material3.adaptive.navigation3.LocalListDetailSceneScope
import androidx.compose.material3.adaptive.navigation3.LocalSupportingPaneSceneScope
import androidx.compose.runtime.Composable

/**
 * True while the calling entry is a detail pane with the list pane shown beside it (medium and wider).
 * Such a pane hides its back arrow: the list it would go back to is already on screen.
 * False on a single pane, where the screen is all there is and needs its back arrow.
 */
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun isShownBesideList(): Boolean {
    val scope = LocalListDetailSceneScope.current ?: return false
    val value = scope.scaffoldTransitionScope.scaffoldStateTransition.targetState
    return value[ListDetailPaneScaffoldRole.List] == PaneAdaptedValue.Expanded
}

/** The same for a supporting pane: true while the main pane it belongs to is shown beside it. */
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun isShownBesideMainPane(): Boolean {
    val scope = LocalSupportingPaneSceneScope.current ?: return false
    val value = scope.scaffoldTransitionScope.scaffoldStateTransition.targetState
    return value[SupportingPaneScaffoldRole.Main] == PaneAdaptedValue.Expanded
}
