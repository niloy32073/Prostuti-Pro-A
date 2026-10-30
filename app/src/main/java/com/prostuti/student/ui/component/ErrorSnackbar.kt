package com.prostuti.student.ui.component

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect

/**
 * Surfaces the first entry of a ViewModel's `errors: Map<tag, message>` as a snackbar, then
 * tells the caller to drop that tag (`onConsumed`) so the same failure doesn't reappear on
 * recomposition. Every screen in the auth flow uses this instead of inventing its own error UI.
 */
@Composable
fun ObserveErrors(
    errors: Map<String, String>,
    snackbarHostState: SnackbarHostState,
    onConsumed: (String) -> Unit
) {
    val first = errors.entries.firstOrNull()
    LaunchedEffect(first) {
        if (first != null) {
            snackbarHostState.showSnackbar(first.value)
            onConsumed(first.key)
        }
    }
}
