package com.passbolt.mobile.android.feature.authentication.auth

// remember to regenerate hashCode and equals
data class AuthState(
    val accountData: AccountData = AccountData(),
    val authReason: RefreshAuthReason? = null,
    val showBiometricButton: Boolean = false,
    val passphrase: ByteArray = ByteArray(0),
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
    @Suppress("CyclomaticComplexMethod")
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AuthState) return false
        return accountData == other.accountData &&
            authReason == other.authReason &&
            showBiometricButton == other.showBiometricButton &&
            passphrase.contentEquals(other.passphrase) &&
            isAuthButtonEnabled == other.isAuthButtonEnabled &&
            showProgress == other.showProgress &&
            showForgotPasswordDialog == other.showForgotPasswordDialog &&
            canShowLeaveConfirmation == other.canShowLeaveConfirmation &&
            showLeaveConfirmationDialog == other.showLeaveConfirmationDialog &&
            showServerNotReachable == other.showServerNotReachable &&
            serverNotReachableDomain == other.serverNotReachableDomain &&
            showDeviceRooted == other.showDeviceRooted &&
            showFetchFeatureFlagsError == other.showFetchFeatureFlagsError &&
            showHelpMenu == other.showHelpMenu &&
            showAccountDoesNotExist == other.showAccountDoesNotExist &&
            accountDoesNotExistLabel == other.accountDoesNotExistLabel &&
            accountDoesNotExistEmail == other.accountDoesNotExistEmail &&
            accountDoesNotExistUrl == other.accountDoesNotExistUrl &&
            showServerFingerprintChanged == other.showServerFingerprintChanged &&
            serverFingerprintChangedFingerprint == other.serverFingerprintChangedFingerprint
    }

    override fun hashCode(): Int {
        var result = accountData.hashCode()
        result = 31 * result + (authReason?.hashCode() ?: 0)
        result = 31 * result + showBiometricButton.hashCode()
        result = 31 * result + passphrase.contentHashCode()
        result = 31 * result + isAuthButtonEnabled.hashCode()
        result = 31 * result + showProgress.hashCode()
        result = 31 * result + showForgotPasswordDialog.hashCode()
        result = 31 * result + canShowLeaveConfirmation.hashCode()
        result = 31 * result + showLeaveConfirmationDialog.hashCode()
        result = 31 * result + showServerNotReachable.hashCode()
        result = 31 * result + serverNotReachableDomain.hashCode()
        result = 31 * result + showDeviceRooted.hashCode()
        result = 31 * result + showFetchFeatureFlagsError.hashCode()
        result = 31 * result + showHelpMenu.hashCode()
        result = 31 * result + showAccountDoesNotExist.hashCode()
        result = 31 * result + accountDoesNotExistLabel.hashCode()
        result = 31 * result + (accountDoesNotExistEmail?.hashCode() ?: 0)
        result = 31 * result + accountDoesNotExistUrl.hashCode()
        result = 31 * result + showServerFingerprintChanged.hashCode()
        result = 31 * result + serverFingerprintChangedFingerprint.hashCode()
        return result
    }

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
