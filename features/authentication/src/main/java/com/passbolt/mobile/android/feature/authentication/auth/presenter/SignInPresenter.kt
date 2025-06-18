package com.passbolt.mobile.android.feature.authentication.auth.presenter

import com.passbolt.mobile.android.common.usecase.UserIdInput
import com.passbolt.mobile.android.core.accounts.usecase.accountdata.GetAccountDataUseCase
import com.passbolt.mobile.android.core.accounts.usecase.accountdata.SaveServerFingerprintUseCase
import com.passbolt.mobile.android.core.accounts.usecase.privatekey.GetPrivateKeyUseCase
import com.passbolt.mobile.android.core.accounts.usecase.selectedaccount.SaveSelectedAccountUseCase
import com.passbolt.mobile.android.core.authenticationcore.passphrase.GetPassphraseUseCase
import com.passbolt.mobile.android.core.authenticationcore.session.SaveSessionUseCase
import com.passbolt.mobile.android.core.idlingresource.SignInIdlingResource
import com.passbolt.mobile.android.core.inappreview.InAppReviewInteractor
import com.passbolt.mobile.android.core.mvp.coroutinecontext.CoroutineLaunchContext
import com.passbolt.mobile.android.core.passphrasememorycache.PassphraseMemoryCache
import com.passbolt.mobile.android.core.passphrasememorycache.PotentialPassphrase
import com.passbolt.mobile.android.core.preferences.usecase.GetGlobalPreferencesUseCase
import com.passbolt.mobile.android.core.security.rootdetection.RootDetector
import com.passbolt.mobile.android.core.security.runtimeauth.RuntimeAuthenticatedFlag
import com.passbolt.mobile.android.encryptedstorage.biometric.BiometricCipher
import com.passbolt.mobile.android.feature.authentication.auth.challenge.MfaStatus
import com.passbolt.mobile.android.feature.authentication.auth.challenge.MfaStatusProvider
import com.passbolt.mobile.android.feature.authentication.auth.usecase.BiometryInteractor
import com.passbolt.mobile.android.feature.authentication.auth.usecase.ChallengeVerificationErrorType
import com.passbolt.mobile.android.feature.authentication.auth.usecase.GetAndVerifyServerKeysAndTimeInteractor
import com.passbolt.mobile.android.feature.authentication.auth.usecase.PostSignInActionsInteractor
import com.passbolt.mobile.android.feature.authentication.auth.usecase.SignInVerifyInteractor
import com.passbolt.mobile.android.feature.authentication.auth.usecase.SignOutUseCase
import com.passbolt.mobile.android.feature.authentication.auth.usecase.VerifyPassphraseUseCase
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
// presenter for sign in view used for performing full sign in
open class SignInPresenter(
    private val saveSessionUseCase: SaveSessionUseCase,
    private val saveSelectedAccountUseCase: SaveSelectedAccountUseCase,
    private val passphraseMemoryCache: PassphraseMemoryCache,
    private val signOutUseCase: SignOutUseCase,
    private val saveServerFingerprintUseCase: SaveServerFingerprintUseCase,
    private val mfaStatusProvider: MfaStatusProvider,
    private val getAndVerifyServerKeysInteractor: GetAndVerifyServerKeysAndTimeInteractor,
    private val signInVerifyInteractor: SignInVerifyInteractor,
    private val inAppReviewInteractor: InAppReviewInteractor,
    private val signInIdlingResource: SignInIdlingResource,
    private val postSignInActionsInteractor: PostSignInActionsInteractor,
    biometryInteractor: BiometryInteractor,
    getAccountDataUseCase: GetAccountDataUseCase,
    biometricCipher: BiometricCipher,
    getPassphraseUseCase: GetPassphraseUseCase,
    getPrivateKeyUseCase: GetPrivateKeyUseCase,
    verifyPassphraseUseCase: VerifyPassphraseUseCase,
    coroutineLaunchContext: CoroutineLaunchContext,
    authReasonMapper: AuthReasonMapper,
    rootDetector: RootDetector,
    runtimeAuthenticatedFlag: RuntimeAuthenticatedFlag,
    getGlobalPreferencesUseCase: GetGlobalPreferencesUseCase
) : AuthBasePresenter(
    getAccountDataUseCase,
    getPrivateKeyUseCase,
    verifyPassphraseUseCase,
    biometricCipher,
    getPassphraseUseCase,
    passphraseMemoryCache,
    authReasonMapper,
    rootDetector,
    biometryInteractor,
    getGlobalPreferencesUseCase,
    runtimeAuthenticatedFlag,
    coroutineLaunchContext
) {

    private var loginState: LoginState? = null

    override fun onPassphraseVerified(passphrase: ByteArray) {
        performSignIn(passphrase)
    }

    override fun biometricAuthSuccess(authenticatedCipher: Cipher?) {
        super.biometricAuthSuccess(authenticatedCipher)
        when (val potentialPassphrase = passphraseMemoryCache.get()) {
            is PotentialPassphrase.Passphrase -> {
                performSignIn(potentialPassphrase.passphrase)
            }
            else -> {
                view?.showGenericError()
                Timber.e("Passphrase not found in cache")
            }
        }
    }

    protected open fun performSignIn(passphrase: ByteArray) {
        signInIdlingResource.setIdle(false)
        view?.showProgress()
        scope.launch {
            getAndVerifyServerKeysInteractor.getAndVerifyServerKeys(userId,
                onError = {
                    view?.hideProgress()
                    when (it) {
                        is GetAndVerifyServerKeysAndTimeInteractor.Error.Generic -> {
                            view?.showGenericError()
                        }
                        is GetAndVerifyServerKeysAndTimeInteractor.Error.IncorrectServerFingerprint -> {
                            view?.showServerFingerprintChanged(it.fingerprint)
                        }
                        is GetAndVerifyServerKeysAndTimeInteractor.Error.ServerNotReachable -> {
                            view?.showServerNotReachable(it.serverUrl)
                        }
                        is GetAndVerifyServerKeysAndTimeInteractor.Error.TimeIsOutOfSync -> {
                            view?.showTimeIsOutOfSync()
                        }
                    }
                }
            ) {
                signIn(passphrase.copyOf(), it.pgpKey, it.rsaKey, it.pgpKeyFingerprint)
            }
        }
    }

    private suspend fun signIn(
        passphrase: ByteArray,
        serverPublicKey: String,
        rsaKey: String,
        fingerprint: String
    ) {
        signInVerifyInteractor.signInVerify(serverPublicKey, passphrase, userId, rsaKey,
            onError = {
                view?.hideProgress()
                signInIdlingResource.setIdle(true)
                when (it) {
                    is SignInVerifyInteractor.Error.AccountDoesNotExist -> {
                        view?.showAccountDoesNotExistDialog(
                            it.label,
                            it.email,
                            it.serverUrl
                        )
                    }
                    is SignInVerifyInteractor.Error.ChallengeDecryptionError -> {
                        view?.showDecryptionError(it.message)
                    }
                    is SignInVerifyInteractor.Error.ChallengeVerificationError -> {
                        when (it.type) {
                            ChallengeVerificationErrorType.TOKEN_EXPIRED -> view?.showChallengeTokenExpired()
                            ChallengeVerificationErrorType.INVALID_SIGNATURE -> view?.showChallengeInvalidSignature()
                            ChallengeVerificationErrorType.FAILURE -> view?.showChallengeVerificationFailure()
                        }
                    }
                    is SignInVerifyInteractor.Error.IncorrectPassphrase -> {
                        view?.showWrongPassphrase()
                    }
                    is SignInVerifyInteractor.Error.SignInFailure -> {
                        view?.showError(it.message)
                    }
                }
            }) {
            loginState = LoginState(
                accessToken = it.accessToken,
                refreshToken = it.refreshToken,
                fingerprint = fingerprint,
                mfaToken = it.mfaToken
            )
            Timber.d("Checking MFA status")
            mfaStatusProvider.setState(
                MfaStatusProvider.MfaState(
                    challengeResponseDto = it.challengeResponseDto,
                    newMfaToken = it.mfaToken,
                    currentMfaToken = it.currentMfaToken
                )
            )
            when (mfaStatusProvider.provideMfaStatus()) {
                MfaStatus.NOT_REQUIRED -> {
                    Timber.d("MFA not required")
                    signInSuccess()
                }
                MfaStatus.REQUIRED -> {
                    Timber.d("MFA required")
                    mfaRequired(it.accessToken, it.challengeResponseDto.mfaProviders)
                }
            }
        }
    }

    override fun fingerprintServerConfirmationClick(fingerprint: String) {
        saveServerFingerprintUseCase.execute(SaveServerFingerprintUseCase.Input(userId, fingerprint))
    }

    override fun mfaSucceeded(mfaHeader: String?) {
        Timber.d("MFA succeeded")
        mfaHeader?.let {
            loginState?.mfaToken = it
        }
        signInSuccess(mfaHeader != null)
    }

    private fun signInSuccess(updateSession: Boolean = true) {
        Timber.d("Authentication success")
        runtimeAuthenticatedFlag.isAuthenticated = true
        val currentLoginState = requireNotNull(loginState)
        if (updateSession) {
            saveSessionUseCase.execute(
                SaveSessionUseCase.Input(
                    userId = userId,
                    accessToken = currentLoginState.accessToken,
                    refreshToken = currentLoginState.refreshToken,
                    mfaToken = loginState?.mfaToken
                )
            )
        }
        saveServerFingerprintUseCase.execute(
            SaveServerFingerprintUseCase.Input(
                userId,
                currentLoginState.fingerprint
            )
        )
        saveSelectedAccountUseCase.execute(UserIdInput(userId))
        Timber.d("Increasing sign in count")
        inAppReviewInteractor.processSuccessfulSignIn()
        loginState = null
        launchPostSignInActions()
    }

    private fun launchPostSignInActions() {
        scope.launch {
            postSignInActionsInteractor.launchPostSignInActions(
                onError = {
                    view?.hideProgress()
                    signInIdlingResource.setIdle(true)
                    when (it) {
                        PostSignInActionsInteractor.Error.ConfigurationFetchError -> view?.showGenericError()
                        PostSignInActionsInteractor.Error.UserProfileFetchError -> view?.showGenericError()
                    }
                }
            ) {
                view?.apply {
                    hideProgress()
                    clearPassphraseInput()
                    authSuccess()
                }
                signInIdlingResource.setIdle(true)
            }
        }
    }

    fun signOutClick() {
        view?.apply {
            closeFeatureFlagsFetchErrorDialog()
            showProgress()
        }
        scope.launch {
            signOutUseCase.execute(Unit)
            view?.apply {
                hideProgress()
                navigateBack()
            }
        }
    }

    fun refreshClick() {
        when (val potentialPassphrase = passphraseMemoryCache.get()) {
            is PotentialPassphrase.Passphrase -> performSignIn(potentialPassphrase.passphrase)
            is PotentialPassphrase.PassphraseNotPresent -> view?.closeFeatureFlagsFetchErrorDialog()
        }
    }

    private class LoginState(
        val accessToken: String,
        val refreshToken: String,
        val fingerprint: String,
        var mfaToken: String? = null
    )
}
