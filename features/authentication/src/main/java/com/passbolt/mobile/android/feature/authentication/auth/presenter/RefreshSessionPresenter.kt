package com.passbolt.mobile.android.feature.authentication.auth.presenter

import com.passbolt.mobile.android.common.FingerprintInformationProvider
import com.passbolt.mobile.android.core.mvp.coroutinecontext.CoroutineLaunchContext
import com.passbolt.mobile.android.core.security.rootdetection.RootDetector
import com.passbolt.mobile.android.feature.authentication.auth.challenge.ChallengeDecryptor
import com.passbolt.mobile.android.feature.authentication.auth.challenge.ChallengeProvider
import com.passbolt.mobile.android.feature.authentication.auth.challenge.ChallengeVerifier
import com.passbolt.mobile.android.feature.authentication.auth.challenge.MfaStatusProvider
import com.passbolt.mobile.android.feature.authentication.auth.usecase.GetServerPublicPgpKeyUseCase
import com.passbolt.mobile.android.feature.authentication.auth.usecase.GetServerPublicRsaKeyUseCase
import com.passbolt.mobile.android.feature.authentication.auth.usecase.RefreshSessionUseCase
import com.passbolt.mobile.android.feature.authentication.auth.usecase.SignInUseCase
import com.passbolt.mobile.android.feature.authentication.auth.usecase.SignOutUseCase
import com.passbolt.mobile.android.feature.setup.enterpassphrase.VerifyPassphraseUseCase
import com.passbolt.mobile.android.featureflags.usecase.FeatureFlagsInteractor
import com.passbolt.mobile.android.storage.cache.passphrase.PassphraseMemoryCache
import com.passbolt.mobile.android.storage.encrypted.biometric.BiometricCipher
import com.passbolt.mobile.android.storage.usecase.accountdata.GetAccountDataUseCase
import com.passbolt.mobile.android.storage.usecase.accountdata.IsServerFingerprintCorrectUseCase
import com.passbolt.mobile.android.storage.usecase.accountdata.SaveServerFingerprintUseCase
import com.passbolt.mobile.android.storage.usecase.biometrickey.RemoveBiometricKeyUseCase
import com.passbolt.mobile.android.storage.usecase.passphrase.CheckIfPassphraseFileExistsUseCase
import com.passbolt.mobile.android.storage.usecase.passphrase.GetPassphraseUseCase
import com.passbolt.mobile.android.storage.usecase.passphrase.RemoveSelectedAccountPassphraseUseCase
import com.passbolt.mobile.android.storage.usecase.privatekey.GetPrivateKeyUseCase
import com.passbolt.mobile.android.storage.usecase.selectedaccount.SaveSelectedAccountUseCase
import com.passbolt.mobile.android.storage.usecase.session.GetSessionUseCase
import com.passbolt.mobile.android.storage.usecase.session.SaveSessionUseCase
import kotlinx.coroutines.launch

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
class RefreshSessionPresenter(
    private val refreshSessionUseCase: RefreshSessionUseCase,
    getServerPublicPgpKeyUseCase: GetServerPublicPgpKeyUseCase,
    getServerPublicRsaKeyUseCase: GetServerPublicRsaKeyUseCase,
    signInUseCase: SignInUseCase,
    challengeProvider: ChallengeProvider,
    challengeDecryptor: ChallengeDecryptor,
    challengeVerifier: ChallengeVerifier,
    saveSessionUseCase: SaveSessionUseCase,
    saveSelectedAccountUseCase: SaveSelectedAccountUseCase,
    getAccountDataUseCase: GetAccountDataUseCase,
    passphraseMemoryCache: PassphraseMemoryCache,
    featureFlagsInteractor: FeatureFlagsInteractor,
    signOutUseCase: SignOutUseCase,
    saveServerFingerprintUseCase: SaveServerFingerprintUseCase,
    isServerFingerprintCorrectUseCase: IsServerFingerprintCorrectUseCase,
    mfaStatusProvider: MfaStatusProvider,
    getSessionUseCase: GetSessionUseCase,
    removeSelectedAccountPassphraseUseCase: RemoveSelectedAccountPassphraseUseCase,
    biometricCipher: BiometricCipher,
    getPassphraseUseCase: GetPassphraseUseCase,
    removeBiometricKeyUseCase: RemoveBiometricKeyUseCase,
    getPrivateKeyUseCase: GetPrivateKeyUseCase,
    verifyPassphraseUseCase: VerifyPassphraseUseCase,
    fingerprintInfoProvider: FingerprintInformationProvider,
    checkIfPassphraseFileExistsUseCase: CheckIfPassphraseFileExistsUseCase,
    coroutineLaunchContext: CoroutineLaunchContext,
    authReasonMapper: AuthReasonMapper,
    rootDetector: RootDetector
) : SignInPresenter(
    getServerPublicPgpKeyUseCase,
    getServerPublicRsaKeyUseCase,
    signInUseCase,
    challengeProvider,
    challengeDecryptor,
    challengeVerifier,
    saveSessionUseCase,
    saveSelectedAccountUseCase,
    getAccountDataUseCase,
    passphraseMemoryCache,
    featureFlagsInteractor,
    signOutUseCase,
    saveServerFingerprintUseCase,
    isServerFingerprintCorrectUseCase,
    mfaStatusProvider,
    getSessionUseCase,
    removeSelectedAccountPassphraseUseCase,
    biometricCipher,
    getPassphraseUseCase,
    removeBiometricKeyUseCase,
    getPrivateKeyUseCase,
    verifyPassphraseUseCase,
    fingerprintInfoProvider,
    checkIfPassphraseFileExistsUseCase,
    coroutineLaunchContext,
    authReasonMapper,
    rootDetector
) {

    override fun performSignIn(passphrase: ByteArray) {
        scope.launch {
            when (refreshSessionUseCase.execute(Unit)) {
                is RefreshSessionUseCase.Output.Success -> view?.authSuccess()
                is RefreshSessionUseCase.Output.Failure -> super.performSignIn(passphrase)
            }
        }
    }
}
