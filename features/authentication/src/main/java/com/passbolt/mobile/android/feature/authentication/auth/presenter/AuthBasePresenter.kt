package com.passbolt.mobile.android.feature.authentication.auth.presenter

import android.security.keystore.KeyPermanentlyInvalidatedException
import androidx.annotation.CallSuper
import com.passbolt.mobile.android.common.FingerprintInformationProvider
import com.passbolt.mobile.android.core.mvp.coroutinecontext.CoroutineLaunchContext
import com.passbolt.mobile.android.core.navigation.ActivityIntents
import com.passbolt.mobile.android.core.security.rootdetection.RootDetector
import com.passbolt.mobile.android.feature.authentication.auth.AuthContract
import com.passbolt.mobile.android.feature.setup.enterpassphrase.VerifyPassphraseUseCase
import com.passbolt.mobile.android.mappers.AccountModelMapper
import com.passbolt.mobile.android.storage.cache.passphrase.PassphraseMemoryCache
import com.passbolt.mobile.android.storage.cache.passphrase.PotentialPassphrase
import com.passbolt.mobile.android.storage.encrypted.biometric.BiometricCipher
import com.passbolt.mobile.android.storage.usecase.accountdata.GetAccountDataUseCase
import com.passbolt.mobile.android.storage.usecase.biometrickey.RemoveBiometricKeyUseCase
import com.passbolt.mobile.android.storage.usecase.input.UserIdInput
import com.passbolt.mobile.android.storage.usecase.passphrase.CheckIfPassphraseFileExistsUseCase
import com.passbolt.mobile.android.storage.usecase.passphrase.GetPassphraseUseCase
import com.passbolt.mobile.android.storage.usecase.passphrase.RemoveSelectedAccountPassphraseUseCase
import com.passbolt.mobile.android.storage.usecase.privatekey.GetPrivateKeyUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber
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

@Suppress("LongParameterList") // TODO extract interactors
// base presenter for auth view
// handles account details display, forgot password dialog, biometry
abstract class AuthBasePresenter(
    private val getAccountDataUseCase: GetAccountDataUseCase,
    private val checkIfPassphraseFileExistsUseCase: CheckIfPassphraseFileExistsUseCase,
    private val fingerprintInfoProvider: FingerprintInformationProvider,
    private val removeSelectedAccountPassphraseUseCase: RemoveSelectedAccountPassphraseUseCase,
    private val getPrivateKeyUseCase: GetPrivateKeyUseCase,
    private val verifyPassphraseUseCase: VerifyPassphraseUseCase,
    private val biometricCipher: BiometricCipher,
    private val getPassphraseUseCase: GetPassphraseUseCase,
    private val passphraseMemoryCache: PassphraseMemoryCache,
    private val removeBiometricKeyUseCase: RemoveBiometricKeyUseCase,
    private val authReasonMapper: AuthReasonMapper,
    private val rootDetector: RootDetector,
    coroutineLaunchContext: CoroutineLaunchContext
) : AuthContract.Presenter {

    override var view: AuthContract.View? = null

    private val job = SupervisorJob()
    protected val scope = CoroutineScope(job + coroutineLaunchContext.ui)

    protected lateinit var userId: String
    private lateinit var authConfig: ActivityIntents.AuthConfig

    private val authReason: AuthContract.View.RefreshAuthReason?
        get() = authReasonMapper.map(authConfig)

    override fun attach(view: AuthContract.View) {
        super.attach(view)
        view.showTitle()
        authReason?.let { view.showAuthenticationReason(it) }
        if (rootDetector.isDeviceRooted()) {
            view.showDeviceRootedDialog()
        } else {
            handleBiometry()
        }
    }

    override fun onRootedDeviceAcknowledged() {
        handleBiometry()
    }

    private fun handleBiometry() {
        if (checkIfPassphraseFileExistsUseCase.execute(UserIdInput(userId)).passphraseFileExists) {
            if (fingerprintInfoProvider.hasBiometricSetUp()) {
                view?.apply {
                    setBiometricAuthButtonVisible()
                    tryShowingBiometricPrompt()
                }
            } else {
                removeSelectedAccountPassphraseUseCase.execute(Unit)
            }
        }
    }

    override fun biometricAuthError(messageResId: Int) {
        view?.showAuthenticationError(messageResId)
    }

    override fun biometricAuthClick() {
        tryShowingBiometricPrompt()
    }

    private fun tryShowingBiometricPrompt() {
        try {
            view?.showBiometricPrompt(authReason, biometricCipher.getBiometricDecryptCipher(userId))
        } catch (exception: KeyPermanentlyInvalidatedException) {
            Timber.e(exception)
            removeSelectedAccountPassphraseUseCase.execute(Unit)
            removeBiometricKeyUseCase.execute(Unit)
            view?.setBiometricAuthButtonGone()
            view?.showFingerprintChangedError()
        } catch (exception: Exception) {
            Timber.e(exception)
            view?.showGenericError()
        }
    }

    override fun argsRetrieved(authConfig: ActivityIntents.AuthConfig, userId: String) {
        this.userId = userId
        this.authConfig = authConfig
    }

    override fun viewCreated() {
        getAccountData()
    }

    private fun getAccountData() {
        scope.launch {
            val accountData = getAccountDataUseCase.execute(UserIdInput(userId))

            view?.apply {
                showLabel(
                    accountData.label ?: AccountModelMapper.defaultLabel(
                        accountData.firstName,
                        accountData.lastName
                    )
                )
                showDomain(accountData.url)
            }
            accountData.email?.let { view?.showEmail(it) }
            accountData.avatarUrl?.let { view?.showAvatar(it) }
        }
    }

    override fun forgotPasswordClick() {
        view?.showForgotPasswordDialog()
    }

    override fun passphraseInputIsEmpty(isEmpty: Boolean) {
        if (isEmpty) {
            view?.disableAuthButton()
        } else {
            view?.enableAuthButton()
        }
    }

    override fun backClick(showConfirmationDialog: Boolean) {
        if (showConfirmationDialog) {
            view?.showLeaveConfirmationDialog()
        } else {
            view?.navigateBack()
        }
    }

    override fun leaveConfirmationClick() {
        view?.navigateBack()
    }

    @CallSuper
    override fun signInClick(passphrase: ByteArray) {
        view?.hideKeyboard()
        validatePassphrase(passphrase)
    }

    private fun validatePassphrase(passphrase: ByteArray) {
        scope.launch {
            val privateKey = requireNotNull(
                getPrivateKeyUseCase.execute(UserIdInput(userId)).privateKey
            )
            val isPassphraseCorrect =
                verifyPassphraseUseCase.execute(VerifyPassphraseUseCase.Input(privateKey, passphrase)).isCorrect
            if (isPassphraseCorrect) {
                onPassphraseVerified(passphrase)
            } else {
                view?.showWrongPassphrase()
            }
        }
    }

    abstract fun onPassphraseVerified(passphrase: ByteArray)

    @CallSuper
    override fun biometricAuthSuccess(authenticatedCipher: Cipher?) {
        authenticatedCipher?.let {
            val potentialPassphrase = getPassphraseUseCase.execute(
                GetPassphraseUseCase.Input(userId, authenticatedCipher)
            ).potentialPassphrase
            if (potentialPassphrase is PotentialPassphrase.Passphrase) {
                passphraseMemoryCache.set(potentialPassphrase.passphrase)
            } else {
                view?.showGenericError()
            }
        }
    }

    override fun connectToExistingAccountClick() {
        view?.navigateToAccountList()
    }

    override fun helpClick() {
        view?.showHelpMenu()
    }
}
