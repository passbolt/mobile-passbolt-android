package com.passbolt.mobile.android.core.navigation.compose.results

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateMapOf
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.BUFFERED
import kotlinx.coroutines.flow.receiveAsFlow

object NavigationResultEventBus {
    private val LocalResultEventBus: ProvidableCompositionLocal<ResultEventBus?> =
        compositionLocalOf { null }

    val current: ResultEventBus
        @Composable
        get() = LocalResultEventBus.current ?: error("No ResultEventBus has been provided")

    infix fun provides(bus: ResultEventBus) = LocalResultEventBus.provides(bus)
}

class ResultEventBus {
    val channelMap = mutableStateMapOf<String, Channel<Any?>>()

    inline fun <reified T> getResultFlow(resultKey: String = T::class.toString()) = channelMap[resultKey]?.receiveAsFlow()

    inline fun <reified T> sendResult(
        resultKey: String = T::class.toString(),
        result: T,
    ) {
        if (!channelMap.contains(resultKey)) {
            channelMap[resultKey] =
                Channel(capacity = BUFFERED, onBufferOverflow = BufferOverflow.SUSPEND)
        }
        channelMap[resultKey]?.trySend(result)
    }
}
