package com.prostuti.student.ui.screen.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prostuti.student.domain.widget.AddHomeWidgetUseCase
import com.prostuti.student.domain.widget.DismissWidgetPromoUseCase
import com.prostuti.student.domain.widget.ShouldShowWidgetPromoUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant

data class HomeWidgetPromoUiState(val visible: Boolean = false)

/**
 * The small "add the widget" card on Home. Checked every time Home comes back (the launcher's
 * add dialog sits on top of the app, so returning from it hides the card once the widget is in).
 */
class HomeWidgetPromoViewModel(
    private val shouldShow: ShouldShowWidgetPromoUseCase,
    private val dismissPromo: DismissWidgetPromoUseCase,
    private val addWidget: AddHomeWidgetUseCase,
    private val now: () -> Instant = Instant::now,
) : ViewModel() {
    private val _state = MutableStateFlow(HomeWidgetPromoUiState())
    val state: StateFlow<HomeWidgetPromoUiState> = _state.asStateFlow()

    private var checkJob: Job? = null

    fun refresh() {
        checkJob?.cancel()
        checkJob = viewModelScope.launch {
            val visible = shouldShow(now())
            _state.update { it.copy(visible = visible) }
        }
    }

    fun dismiss() {
        _state.update { it.copy(visible = false) }
        viewModelScope.launch { dismissPromo(now()) }
    }

    /** The launcher couldn't take the request: nothing to offer, hide the card. */
    fun add() {
        viewModelScope.launch {
            if (!addWidget()) _state.update { it.copy(visible = false) }
        }
    }
}
