package com.passbolt.mobile.android.feature.authentication.auth.presenter

import com.passbolt.mobile.android.core.idlingresource.SignInIdlingResource
import com.passbolt.mobile.android.core.inappreview.InAppReviewInteractor
import com.passbolt.mobile.android.core.mvp.coroutinecontext.CoroutineLaunchContext
import com.passbolt.mobile.android.core.security.rootdetection.RootDetector
import com.passbolt.mobile.android.core.security.runtimeauth.RuntimeAuthenticatedFlag
import com.passbolt.mobile.android.core.users.profile.UserProfileInteractor
import com.passbolt.mobile.android.feature.authentication.auth.challenge.MfaStatus
import com.passbolt.mobile.android.feature.authentication.auth.challenge.MfaStatusProvider
import com.passbolt.mobile.android.feature.authentication.auth.challenge.MfaStatusProvider.Companion.MFA_PROVIDER_TOTP
import com.passbolt.mobile.android.feature.authentication.auth.challenge.MfaStatusProvider.Companion.MFA_PROVIDER_YUBIKEY
import com.passbolt.mobile.android.feature.authentication.auth.usecase.BiometryInteractor
import com.passbolt.mobile.android.feature.authentication.auth.usecase.ChallengeVerificationErrorType
import com.passbolt.mobile.android.feature.authentication.auth.usecase.GetAndVerifyServerKeysInteractor
import com.passbolt.mobile.android.feature.authentication.auth.usecase.SignInVerifyInteractor
import com.passbolt.mobile.android.feature.authentication.auth.usecase.SignOutUseCase
import com.passbolt.mobile.android.feature.setup.enterpassphrase.VerifyPassphraseUseCase
import com.passbolt.mobile.android.featureflags.usecase.FeatureFlagsInteractor
import com.passbolt.mobile.android.storage.cache.passphrase.PassphraseMemoryCache
import com.passbolt.mobile.android.storage.cache.passphrase.PotentialPassphrase
import com.passbolt.mobile.android.storage.encrypted.biometric.BiometricCipher
import com.passbolt.mobile.android.storage.usecase.accountdata.GetAccountDataUseCase
import com.passbolt.mobile.android.storage.usecase.accountdata.SaveServerFingerprintUseCase
import com.passbolt.mobile.android.storage.usecase.input.UserIdInput
import com.passbolt.mobile.android.storage.usecase.passphrase.GetPassphraseUseCase
import com.passbolt.mobile.android.storage.usecase.privatekey.GetPrivateKeyUseCase
import com.passbolt.mobile.android.storage.usecase.selectedaccount.SaveSelectedAccountUseCase
import com.passbolt.mobile.android.storage.usecase.session.SaveSessionUseCase
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
    private val featureFlagsInteractor: FeatureFlagsInteractor,
    private val getAndVerifyServerKeysInteractor: GetAndVerifyServerKeysInteractor,
    private val signInVerifyInteractor: SignInVerifyInteractor,
    private val userProfileInteractor: UserProfileInteractor,
    private val inAppReviewInteractor: InAppReviewInteractor,
    private val signInIdlingResource: SignInIdlingResource,
    biometryInteractor: BiometryInteractor,
    getAccountDataUseCase: GetAccountDataUseCase,
    biometricCipher: BiometricCipher,
    getPassphraseUseCase: GetPassphraseUseCase,
    getPrivateKeyUseCase: GetPrivateKeyUseCase,
    verifyPassphraseUseCase: VerifyPassphraseUseCase,
    coroutineLaunchContext: CoroutineLaunchContext,
    authReasonMapper: AuthReasonMapper,
    rootDetector: RootDetector,
    runtimeAuthenticatedFlag: RuntimeAuthenticatedFlag
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
                        is GetAndVerifyServerKeysInteractor.Error.Generic -> {
                            view?.showGenericError()
                        }
                        is GetAndVerifyServerKeysInteractor.Error.IncorrectServerFingerprint -> {
                            view?.showServerFingerprintChanged(it.fingerprint)
                        }
                        is GetAndVerifyServerKeysInteractor.Error.ServerNotReachable -> {
                            view?.showServerNotReachable(it.serverUrl)
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
                        view?.showGenericError()
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
            when (val mfaStatus = mfaStatusProvider.provideMfaStatus(
                it.challengeResponseDto,
                it.mfaToken,
                it.currentMfaToken
            )) {
                MfaStatus.NotRequired -> {
                    Timber.d("MFA not required")
                    signInSuccess()
                }
                is MfaStatus.Required -> {
                    Timber.d("MFA required")
                    mfaRequired(mfaStatus.mfaProviders, it.accessToken)
                }
            }
        }
    }

    override fun fingerprintServerConfirmationClick(fingerprint: String) {
        saveServerFingerprintUseCase.execute(SaveServerFingerprintUseCase.Input(userId, fingerprint))
    }

    private fun mfaRequired(mfaProviders: List<String>, jwtToken: String) {
        when (val provider = mfaProviders.first()) {
            MFA_PROVIDER_TOTP -> view?.showTotpDialog(jwtToken, mfaProviders.contains(MFA_PROVIDER_YUBIKEY))
            MFA_PROVIDER_YUBIKEY -> view?.showYubikeyDialog(jwtToken, mfaProviders.contains(MFA_PROVIDER_TOTP))
            else -> {
                view?.showUnknownProvider()
                Timber.e("Unknown provider: $provider")
            }
        }
        view?.hideProgress()
    }

    override fun totpSucceeded(mfaHeader: String?) {
        Timber.d("TOTP succeeded")
        mfaHeader?.let {
            loginState?.mfaToken = it
        }
        signInSuccess(mfaHeader != null)
    }

    override fun yubikeySucceeded(mfaHeader: String?) {
        Timber.d("Yubikey succeeded")
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
        fetchFeatureFlags()
    }

    private fun fetchFeatureFlags() {
        Timber.d("Fetching feature flags")
        scope.launch {
            when (featureFlagsInteractor.fetchAndSaveFeatureFlags()) {
                is FeatureFlagsInteractor.Output.Success -> {
                    Timber.d("Feature flags fetched")
                    fetchUserAvatar()
                }
                is FeatureFlagsInteractor.Output.Failure -> {
                    view?.showFeatureFlagsErrorDialog()
                }
            }
        }
    }

    private suspend fun fetchUserAvatar() {
        Timber.d("Fetching user profile")
        when (val result = userProfileInteractor.fetchAndUpdateUserProfile()) {
            is UserProfileInteractor.Output.Failure -> {
                Timber.e("Failed to update user profile: ${result.message}")
                view?.showFailedToFetchUserProfile(result.message)
            }
            is UserProfileInteractor.Output.Success -> {
                Timber.d("User profile updated successfully")
            }
        }
        view?.apply {
            hideProgress()
            clearPassphraseInput()
            authSuccess()
        }
        signInIdlingResource.setIdle(true)
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
