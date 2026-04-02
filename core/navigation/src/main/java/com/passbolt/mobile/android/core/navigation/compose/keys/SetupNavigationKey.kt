package com.passbolt.mobile.android.core.navigation.compose.keys

import androidx.navigation3.runtime.NavKey
import com.passbolt.mobile.android.ui.ResultStatus
import kotlinx.serialization.Serializable

sealed interface SetupNavigationKey : NavKey {
    @Serializable
    object Welcome : SetupNavigationKey

    @Serializable
    object TransferDetails : SetupNavigationKey

    @Serializable
    object ScanQrCodes : SetupNavigationKey

    @Serializable
    object ImportProfile : SetupNavigationKey

    @Serializable
    object BiometricSetup : SetupNavigationKey

    @Serializable
    data class Summary(
        val status: ResultStatus,
    ) : SetupNavigationKey
}
