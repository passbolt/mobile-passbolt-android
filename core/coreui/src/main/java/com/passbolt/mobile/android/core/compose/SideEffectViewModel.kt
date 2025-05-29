package com.passbolt.mobile.android.core.compose

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow

open class SideEffectViewModel<ViewState, SideEffect>(
    initialState: ViewState,
) : StateViewModel<ViewState>(initialState) {
    private val sideEffectChannel = Channel<SideEffect>()
    val sideEffect: Flow<SideEffect> = sideEffectChannel.receiveAsFlow()

    protected fun emitSideEffect(event: SideEffect) {
        launch {
            sideEffectChannel.send(event)
        }
    }
}
