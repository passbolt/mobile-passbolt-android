package com.passbolt.mobile.android.core.navigation.compose.keys

import androidx.navigation3.runtime.NavKey
import com.passbolt.mobile.android.ui.TransferAccountStatusType
import kotlinx.serialization.Serializable

sealed interface TransferAccountToAnotherDeviceKey : NavKey {
    @Serializable
    object Onboarding : TransferAccountToAnotherDeviceKey

    @Serializable
    object Transfer : TransferAccountToAnotherDeviceKey

    @Serializable
    data class TransferStatus(
        val statusType: TransferAccountStatusType,
    ) : TransferAccountToAnotherDeviceKey
}
