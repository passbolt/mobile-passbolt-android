package com.passbolt.mobile.android.core.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.Lifecycle.State.STARTED
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

@Composable
fun <UiAction> SideEffectDispatcher(
    eventsFlow: Flow<UiAction>,
    onEvent: suspend (UiAction) -> Unit,
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(eventsFlow, lifecycleOwner.lifecycle) {
        lifecycleOwner.repeatOnLifecycle(STARTED) {
            withContext(Main.immediate) {
                eventsFlow.collect(onEvent)
            }
        }
    }
}
