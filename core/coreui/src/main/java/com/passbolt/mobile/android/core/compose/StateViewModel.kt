package com.passbolt.mobile.android.core.compose

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

open class StateViewModel<ViewState>(
    initialState: ViewState,
) : ViewModel() {
    private val currentViewState = MutableStateFlow(initialState)
    val viewState: StateFlow<ViewState> = currentViewState.asStateFlow()

    protected fun updateViewState(block: ViewState.() -> ViewState) {
        currentViewState.update(block::invoke)
    }

    protected fun launch(block: suspend CoroutineScope.() -> Unit) = viewModelScope.launch(block = block)
}
