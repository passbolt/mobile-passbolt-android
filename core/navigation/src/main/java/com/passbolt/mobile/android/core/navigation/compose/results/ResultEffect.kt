package com.passbolt.mobile.android.core.navigation.compose.results

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect

@Composable
inline fun <reified T> ResultEffect(
    resultEventBus: ResultEventBus = NavigationResultEventBus.current,
    resultKey: String = T::class.toString(),
    crossinline onResult: suspend (T) -> Unit,
) {
    LaunchedEffect(resultKey, resultEventBus.channelMap[resultKey]) {
        resultEventBus.getResultFlow<T>(resultKey)?.collect { result ->
            @Suppress("UNCHECKED_CAST")
            onResult.invoke(result as T)
        }
    }
}
