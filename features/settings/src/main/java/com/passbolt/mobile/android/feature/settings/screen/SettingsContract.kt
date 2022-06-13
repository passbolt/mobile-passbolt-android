package com.passbolt.mobile.android.feature.settings.screen

import com.passbolt.mobile.android.core.mvp.BaseContract
import javax.crypto.Cipher

/**
 * Passbolt - Open source password manager for teams
 * Copyright (c) 2021 Passbolt SA
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General
 * Public License (AGPL) as published by the Free Software Foundation version 3.
 *
 * The name "Passbolt" is a registered trademark of Passbolt SA, and Passbolt SA hereby declines to grant a trademark
 * license to "Passbolt" pursuant to the GNU Affero General Public License version 3 Section 7(e), without a separate
 * agreement with Passbolt SA.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program. If not,
 * see GNU Affero General Public License v3 (http://www.gnu.org/licenses/agpl-3.0.html).
 *
 * @copyright Copyright (c) Passbolt SA (https://www.passbolt.com)
 * @license https://opensource.org/licenses/AGPL-3.0 AGPL License
 * @link https://www.passbolt.com Passbolt (tm)
 * @since v1.0
 */
interface SettingsContract {

    @Suppress("TooManyFunctions")
    interface View : BaseContract.View {
        fun openUrl(url: String)
        fun navigateToManageAccounts()
        fun toggleFingerprintOn(silently: Boolean)
        fun toggleFingerprintOff(silently: Boolean)
        fun showAutofillSetting()
        fun navigateToAutofill()
        fun showAutofillEnabledDialog()
        fun showDisableFingerprintConfirmationDialog()
        fun navigateToAuthGetPassphrase()
        fun hideAutofillSetting()
        fun showLogoutDialog()
        fun showBiometricPrompt(fingerprintEncryptionCipher: Cipher)
        fun showAuthenticationError(errorMessage: Int)
        fun navigateToSignInWithLogout()
        fun showKeyChangesDetected()
        fun showGenericError()
        fun showConfigureFingerprintFirst()
        fun navigateToSystemSettings()
        fun hidePrivacyPolicyButton()
        fun hideTermsAndConditionsButton()
        fun showProgress()
        fun hideProgress()
        fun navigateToLicenses()
        fun navigateToLogs()
        fun setEnableLogsSwitchOn()
        fun setEnableLogsSwitchOff()
        fun enableAccessLogs()
        fun disableAccessLogs()
        fun showPrivacyPolicyButton()
        fun showTermsAndConditionsButton()
    }

    interface Presenter : BaseContract.Presenter<View> {
        fun privacyPolicyClick()
        fun termsClick()
        fun signOutClick()
        fun manageAccountsClick()
        fun autofillClick()
        fun fingerprintSettingChanged(isEnabled: Boolean)
        fun disableFingerprintConfirmed()
        fun disableFingerprintCanceled()
        fun logoutConfirmed()
        fun getPassphraseSucceeded()
        fun biometricAuthError(errorMessage: Int)
        fun biometricAuthSucceeded(authenticatedCipher: Cipher?)
        fun biometricAuthCanceled()
        fun keyChangesInfoConfirmClick()
        fun systemSettingsClick()
        fun licensesClick()
        fun logsClick()
        fun enableDebugLogsChanged(areLogsEnabled: Boolean)
        fun viewResumed()
    }
}
