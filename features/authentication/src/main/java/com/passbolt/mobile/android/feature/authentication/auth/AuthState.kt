package com.passbolt.mobile.android.feature.authentication.auth

data class AuthState(
    val accountData: AccountData = AccountData(),
    val authReason: RefreshAuthReason? = null,
    val showBiometricButton: Boolean = false,
    val passphrase: String = "",
    val isAuthButtonEnabled: Boolean = false,
    val showProgress: Boolean = false,
    val showForgotPasswordDialog: Boolean = false,
    val canShowLeaveConfirmation: Boolean = false,
    val showLeaveConfirmationDialog: Boolean = false,
    val showServerNotReachable: Boolean = false,
    val serverNotReachableDomain: String = "",
    val showDeviceRooted: Boolean = false,
    val showFetchFeatureFlagsError: Boolean = false,
    val showHelpMenu: Boolean = false,
    val showAccountDoesNotExist: Boolean = false,
    val accountDoesNotExistLabel: String = "",
    val accountDoesNotExistEmail: String? = null,
    val accountDoesNotExistUrl: String = "",
    val showServerFingerprintChanged: Boolean = false,
    val serverFingerprintChangedFingerprint: String = "",
) {
    data class AccountData(
        val label: String = "",
        val email: String? = null,
        val domain: String = "",
        val avatarUrl: String? = null,
    )

    enum class RefreshAuthReason {
        SESSION,
        PASSPHRASE,
    }
}
