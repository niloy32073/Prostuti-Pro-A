package com.prostuti.student.navigation

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.material3.SheetState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavMetadataKey
import androidx.navigation3.runtime.get
import androidx.navigation3.runtime.metadata
import androidx.navigation3.scene.OverlayScene
import androidx.navigation3.scene.Scene
import androidx.navigation3.scene.SceneStrategy
import androidx.navigation3.scene.SceneStrategyScope

/**
 * A destination shown as a modal bottom sheet over the screens below it (android-navigation-3 skill
 * §8.2 — the fixed version: no callback in equals, no lateinit sheet state). The sheet's own Back /
 * scrim tap pops it; popping it from code animates it away ([onRemove]).
 */
@OptIn(ExperimentalMaterial3Api::class)
internal class BottomSheetScene<T : Any>(
    override val key: Any,
    override val previousEntries: List<NavEntry<T>>,
    override val overlaidEntries: List<NavEntry<T>>,
    private val entry: NavEntry<T>,
    private val config: BottomSheetSceneStrategy.Config,
    private val onBack: () -> Unit,
) : OverlayScene<T> {
    override val entries: List<NavEntry<T>> = listOf(entry)
    private var sheetState: SheetState? = null

    override val content: @Composable () -> Unit = {
        val state = rememberModalBottomSheetState(skipPartiallyExpanded = config.skipPartiallyExpanded)
        sheetState = state
        ModalBottomSheet(onDismissRequest = onBack, sheetState = state, properties = config.properties) {
            entry.Content()
        }
    }

    override suspend fun onRemove() {
        sheetState?.hide()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is BottomSheetScene<*>) return false
        return key == other.key && previousEntries == other.previousEntries &&
            overlaidEntries == other.overlaidEntries && entry == other.entry && config == other.config
    }

    override fun hashCode(): Int {
        var result = key.hashCode()
        result = 31 * result + previousEntries.hashCode()
        result = 31 * result + overlaidEntries.hashCode()
        result = 31 * result + entry.hashCode()
        result = 31 * result + config.hashCode()
        return result
    }
}

/** Put BEFORE every non-overlay strategy. */
@OptIn(ExperimentalMaterial3Api::class)
class BottomSheetSceneStrategy<T : Any> : SceneStrategy<T> {
    data class Config(
        val properties: ModalBottomSheetProperties = ModalBottomSheetProperties(),
        val skipPartiallyExpanded: Boolean = true,
    )

    override fun SceneStrategyScope<T>.calculateScene(entries: List<NavEntry<T>>): Scene<T>? {
        val last = entries.lastOrNull() ?: return null
        val config = last.metadata[BottomSheetKey] ?: return null
        val below = entries.dropLast(1)
        if (below.isEmpty()) return null // overlaidEntries must not be empty
        return BottomSheetScene(last.contentKey, below, below, last, config, onBack)
    }

    companion object {
        object BottomSheetKey : NavMetadataKey<Config>

        fun bottomSheet(config: Config = Config()) = metadata { put(BottomSheetKey, config) }
    }
}
