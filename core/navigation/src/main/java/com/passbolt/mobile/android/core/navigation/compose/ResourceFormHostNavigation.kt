package com.passbolt.mobile.android.core.navigation.compose

import androidx.compose.runtime.compositionLocalOf

interface ResourceFormHostNavigation {
    fun navigateBack()

    fun navigateBackWithCreateSuccess(
        name: String,
        resourceId: String,
    )

    fun navigateBackWithEditSuccess(name: String)

    fun navigateToScanOtp()
}

val LocalResourceFormHostNavigation = compositionLocalOf<ResourceFormHostNavigation> { error("No ResourceFormHostNavigation provided") }
