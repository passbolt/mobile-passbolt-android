package com.passbolt.mobile.android.feature.authentication.auth

import androidx.annotation.StringRes
import com.passbolt.mobile.android.core.mvp.BaseContract
import com.passbolt.mobile.android.core.navigation.ActivityIntents
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
interface AuthContract {

    @Suppress("TooManyFunctions")
    interface View : BaseContract.View {
        fun showWrongPassphrase()
        fun showError(message: String)
        fun showFingerprintChangedError()
        fun navigateBack()
        fun showGenericError()
        fun showProgress()
        fun hideProgress()
        fun showLabel(name: String)
        fun showEmail(email: String)
        fun showAvatar(url: String)
        fun showDomain(domain: String)
        fun showForgotPasswordDialog()
        fun showTitle()
        fun disableAuthButton()
        fun enableAuthButton()
        fun authSuccess()
        fun hideKeyboard()
        fun showLeaveConfirmationDialog()
        fun showBiometricPrompt(authReason: RefreshAuthReason?, fingeprintCipherCrypto: Cipher)
        fun setBiometricAuthButtonVisible()
        fun setBiometricAuthButtonGone()
        fun showAuthenticationError(@StringRes errorMessage: Int)
        fun clearPassphraseInput()
        fun showFeatureFlagsErrorDialog()
        fun closeFeatureFlagsFetchErrorDialog()
        fun showAuthenticationReason(reason: RefreshAuthReason)
        fun showAccountDoesNotExistDialog(label: String, email: String?, url: String)
        fun navigateToAccountList()
        fun showServerFingerprintChanged(newFingerprint: String)
        fun showDecryptionError(message: String?)
        fun showServerNotReachable(serverDomain: String)
        fun showTotpDialog(jwtToken: String, hasYubikeyProvider: Boolean)
        fun showYubikeyDialog(jwtToken: String, hasTotpProvider: Boolean)
        fun showUnknownProvider()
        fun showDeviceRootedDialog()
        fun showHelpMenu()
        fun showChallengeInvalidSignature()
        fun showChallengeTokenExpired()
        fun showChallengeVerificationFailure()

        enum class RefreshAuthReason {
            SESSION, PASSPHRASE
        }
    }

    interface Presenter : BaseContract.Presenter<View> {
        fun signInClick(passphrase: ByteArray)
        fun backClick(showConfirmationDialog: Boolean)
        fun argsRetrieved(authConfig: ActivityIntents.AuthConfig, userId: String)
        fun forgotPasswordClick()
        fun passphraseInputIsEmpty(isEmpty: Boolean)
        fun viewCreated()
        fun leaveConfirmationClick()
        fun biometricAuthSuccess(authenticatedCipher: Cipher?)
        fun biometricAuthError(messageResId: Int)
        fun biometricAuthClick()
        fun connectToExistingAccountClick()
        fun fingerprintServerConfirmationClick(fingerprint: String)
        fun totpSucceeded(mfaHeader: String?)
        fun yubikeySucceeded(mfaHeader: String?)
        fun onRootedDeviceAcknowledged()
        fun helpClick()
    }
}
