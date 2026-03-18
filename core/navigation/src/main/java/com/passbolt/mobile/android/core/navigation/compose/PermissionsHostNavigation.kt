package com.passbolt.mobile.android.core.navigation.compose

import androidx.compose.runtime.compositionLocalOf
import com.passbolt.mobile.android.ui.PermissionsMode

// TODO remove after navigation migration
interface PermissionsHostNavigation {
    fun navigateBack()

    fun navigateToSelfWithMode(
        id: String,
        mode: PermissionsMode,
    )

    fun closeWithShareSuccessResult()

    fun navigateToHome()
}

val LocalPermissionsHostNavigation =
    compositionLocalOf<PermissionsHostNavigation> { error("No PermissionsHostNavigation provided") }
