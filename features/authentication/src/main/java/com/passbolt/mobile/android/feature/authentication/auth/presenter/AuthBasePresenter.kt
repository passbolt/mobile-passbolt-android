package com.passbolt.mobile.android.feature.authentication.auth.presenter

import android.security.keystore.KeyPermanentlyInvalidatedException
import androidx.annotation.CallSuper
import com.passbolt.mobile.android.common.usecase.UserIdInput
import com.passbolt.mobile.android.core.accounts.usecase.accountdata.GetAccountDataUseCase
import com.passbolt.mobile.android.core.accounts.usecase.privatekey.GetPrivateKeyUseCase
import com.passbolt.mobile.android.core.authenticationcore.passphrase.GetPassphraseUseCase
import com.passbolt.mobile.android.core.mvp.authentication.AuthenticationState
import com.passbolt.mobile.android.core.mvp.authentication.AuthenticationState.Unauthenticated.Reason.Mfa.MfaProvider.DUO
import com.passbolt.mobile.android.core.mvp.authentication.AuthenticationState.Unauthenticated.Reason.Mfa.MfaProvider.TOTP
import com.passbolt.mobile.android.core.mvp.authentication.AuthenticationState.Unauthenticated.Reason.Mfa.MfaProvider.YUBIKEY
import com.passbolt.mobile.android.core.mvp.authentication.MfaProvidersHandler
import com.passbolt.mobile.android.core.mvp.coroutinecontext.CoroutineLaunchContext
import com.passbolt.mobile.android.core.navigation.ActivityIntents
import com.passbolt.mobile.android.core.passphrasememorycache.PassphraseMemoryCache
import com.passbolt.mobile.android.core.passphrasememorycache.PotentialPassphrase
import com.passbolt.mobile.android.core.preferences.usecase.GetGlobalPreferencesUseCase
import com.passbolt.mobile.android.core.security.rootdetection.RootDetector
import com.passbolt.mobile.android.core.security.runtimeauth.RuntimeAuthenticatedFlag
import com.passbolt.mobile.android.feature.authentication.auth.AuthContract
import com.passbolt.mobile.android.feature.authentication.auth.usecase.BiometryInteractor
import com.passbolt.mobile.android.feature.authentication.auth.usecase.VerifyPassphraseUseCase
import com.passbolt.mobile.android.mappers.AccountModelMapper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
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

// base presenter for auth view
// handles account details display, forgot password dialog, biometry
abstract class AuthBasePresenter(
    private val getAccountDataUseCase: GetAccountDataUseCase,
    private val getPrivateKeyUseCase: GetPrivateKeyUseCase,
    private val verifyPassphraseUseCase: VerifyPassphraseUseCase,
    private val biometricCipher: com.passbolt.mobile.android.encryptedstorage.biometric.BiometricCipher,
    private val getPassphraseUseCase: GetPassphraseUseCase,
    private val passphraseMemoryCache: PassphraseMemoryCache,
    private val authReasonMapper: AuthReasonMapper,
    private val rootDetector: RootDetector,
    private val biometryInteractor: BiometryInteractor,
    private val getGlobalPreferencesUseCase: GetGlobalPreferencesUseCase,
    protected val runtimeAuthenticatedFlag: RuntimeAuthenticatedFlag,
    coroutineLaunchContext: CoroutineLaunchContext
) : AuthContract.Presenter, KoinComponent {

    override var view: AuthContract.View? = null

    private val job = SupervisorJob()
    protected val scope = CoroutineScope(job + coroutineLaunchContext.ui)
    protected lateinit var userId: String
    private lateinit var authConfig: ActivityIntents.AuthConfig

    private val authReason: AuthContract.View.RefreshAuthReason?
        get() = authReasonMapper.map(authConfig)

    private val mfaProvidersHandler: MfaProvidersHandler by inject()

    override fun attach(view: AuthContract.View) {
        super.attach(view)
        view.showTitle()
        authReason?.let { view.showAuthenticationReason(it) }
        if (!getGlobalPreferencesUseCase.execute(Unit).isHideRootDialogEnabled && rootDetector.isDeviceRooted()) {
            view.showDeviceRootedDialog()
        } else {
            handleBiometry()
        }
    }

    override fun onRootedDeviceAcknowledged() {
        handleBiometry()
    }

    private fun handleBiometry() {
        biometryInteractor.onBiometryReady(userId) {
            view?.apply {
                setBiometricAuthButtonVisible()
                tryShowingBiometricPrompt()
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
            Timber.e(exception, "Biometric key has been invalidated")
            biometryInteractor.disableBiometry()
            view?.apply {
                setBiometricAuthButtonGone()
                showFingerprintChangedError()
            }
        } catch (exception: Exception) {
            Timber.e(exception, "Exception during getting biometric cipher")
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
            getAccountDataUseCase.execute(UserIdInput(userId)).let { accountData ->
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
                passphraseMemoryCache.set(passphrase)
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

    protected fun mfaRequired(jwtToken: String, mfaProviders: List<String>?) {
        mfaProvidersHandler.setProviders(
            mfaProviders.orEmpty().map { AuthenticationState.Unauthenticated.Reason.Mfa.MfaProvider.parse(it) })
        when (val provider = mfaProvidersHandler.firstMfaProvider()) {
            YUBIKEY -> view?.showYubikeyDialog(
                jwtToken,
                mfaProvidersHandler.hasMultipleProviders()
            )
            TOTP -> view?.showTotpDialog(
                jwtToken,
                mfaProvidersHandler.hasMultipleProviders()
            )
            DUO -> view?.showDuoDialog(
                jwtToken,
                mfaProvidersHandler.hasMultipleProviders()
            )
            else -> {
                view?.showUnknownProvider()
                Timber.e("Unknown provider: $provider")
            }
        }
        view?.hideProgress()
    }

    override fun otherProviderClick(
        bearer: String?,
        currentProvider: AuthenticationState.Unauthenticated.Reason.Mfa.MfaProvider
    ) {
        when (mfaProvidersHandler.nextMfaProvider(currentProvider)) {
            YUBIKEY -> view?.showYubikeyDialog(bearer, mfaProvidersHandler.hasMultipleProviders())
            TOTP -> view?.showTotpDialog(bearer, mfaProvidersHandler.hasMultipleProviders())
            DUO -> view?.showDuoDialog(bearer, mfaProvidersHandler.hasMultipleProviders())
            null -> view?.showUnknownProvider()
        }
    }
}
