package com.passbolt.mobile.android.feature.authentication.mfa.yubikey

data class ScanYubikeyState(
    val showProgress: Boolean = false,
    val hasOtherProvider: Boolean = false,
    val rememberMe: Boolean = true,
    val showScanCancelledDialog: Boolean = false,
    val showNotFromCurrentUserDialog: Boolean = false,
)
