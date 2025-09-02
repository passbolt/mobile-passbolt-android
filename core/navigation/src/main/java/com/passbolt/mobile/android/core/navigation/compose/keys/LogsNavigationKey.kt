package com.passbolt.mobile.android.core.navigation.compose.keys

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

sealed interface LogsNavigationKey : NavKey {
    @Serializable
    object Logs : LogsNavigationKey
}
