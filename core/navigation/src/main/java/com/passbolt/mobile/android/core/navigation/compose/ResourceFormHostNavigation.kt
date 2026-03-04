package com.passbolt.mobile.android.core.navigation.compose

import androidx.compose.runtime.compositionLocalOf
import com.passbolt.mobile.android.ui.OtpParseResult

// TODO MOB-3690: Remove bridge interface after ResourceForm Compose navigation migration
interface ResourceFormHostNavigation {
    fun navigateBack()

    fun navigateBackWithCreateSuccess(
        name: String,
        resourceId: String,
    )

    fun navigateBackWithEditSuccess(name: String)

    fun navigateToScanOtp(resultCallback: (Boolean, OtpParseResult.OtpQr.TotpQr?) -> Unit)
}

val LocalResourceFormHostNavigation = compositionLocalOf<ResourceFormHostNavigation> { error("No ResourceFormHostNavigation provided") }
