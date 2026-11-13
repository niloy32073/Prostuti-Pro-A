package com.prostuti.student.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.rememberViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.rememberViewModelStoreProvider
import androidx.navigation3.runtime.NavEntryDecorator
import androidx.navigation3.runtime.NavMetadataKey
import androidx.navigation3.runtime.get
import androidx.navigation3.runtime.metadata

/**
 * Lets the screens of one multi-step flow share a ViewModel (android-navigation-3 skill §12.3).
 * The flow's first screen carries [root] metadata and owns the shared store: it is cleared when
 * that screen leaves the back stack. Later screens carry [member] metadata. All read the store
 * from [LocalFlowViewModelStoreOwner].
 *
 * The store is in memory only (no saved state): after process death a member screen finds its
 * flow ViewModel empty and has to send the student back to a step that can rebuild it.
 */
object FlowViewModelStore {
    object RootKey : NavMetadataKey<String>
    object MemberKey : NavMetadataKey<String>

    fun root(flowId: String) = metadata { put(RootKey, flowId) }
    fun member(flowId: String) = metadata { put(MemberKey, flowId) }
}

val LocalFlowViewModelStoreOwner = staticCompositionLocalOf<ViewModelStoreOwner> {
    error("This entry is not part of a flow: add FlowViewModelStore.root()/member() metadata")
}

/** Install after the per-entry ViewModel decorator. */
@Composable
fun <T : Any> rememberFlowViewModelStoreNavEntryDecorator(
    owner: ViewModelStoreOwner = checkNotNull(LocalViewModelStoreOwner.current) { "No ViewModelStoreOwner" },
): NavEntryDecorator<T> {
    val provider = rememberViewModelStoreProvider(parent = owner)
    return remember(provider) {
        val flowOfRoot = mutableMapOf<Any, String>() // root entry's contentKey -> its flow's store key
        NavEntryDecorator(
            onPop = { contentKey -> flowOfRoot.remove(contentKey)?.let(provider::clearKey) },
            decorate = { entry ->
                val rootId = entry.metadata[FlowViewModelStore.RootKey]
                val flowId = rootId ?: entry.metadata[FlowViewModelStore.MemberKey]
                if (flowId == null) {
                    entry.Content()
                } else {
                    val storeKey = "flow:$flowId"
                    if (rootId != null) flowOfRoot[entry.contentKey] = storeKey
                    val flowOwner = rememberViewModelStoreOwner(storeKey, provider, savedStateRegistryOwner = null)
                    CompositionLocalProvider(LocalFlowViewModelStoreOwner provides flowOwner) { entry.Content() }
                }
            },
        )
    }
}
