package com.passbolt.mobile.android.feature.authentication.auth.presenter

import com.passbolt.mobile.android.core.mvp.coroutinecontext.CoroutineLaunchContext
import com.passbolt.mobile.android.core.security.rootdetection.RootDetector
import com.passbolt.mobile.android.feature.authentication.auth.challenge.MfaStatusProvider
import com.passbolt.mobile.android.feature.authentication.auth.usecase.BiometryInteractor
import com.passbolt.mobile.android.feature.authentication.auth.usecase.GetAndVerifyServerKeysInteractor
import com.passbolt.mobile.android.feature.authentication.auth.usecase.RefreshSessionUseCase
import com.passbolt.mobile.android.feature.authentication.auth.usecase.SignInVerifyInteractor
import com.passbolt.mobile.android.feature.authentication.auth.usecase.SignOutUseCase
import com.passbolt.mobile.android.feature.setup.enterpassphrase.VerifyPassphraseUseCase
import com.passbolt.mobile.android.featureflags.usecase.FeatureFlagsInteractor
import com.passbolt.mobile.android.storage.cache.passphrase.PassphraseMemoryCache
import com.passbolt.mobile.android.storage.encrypted.biometric.BiometricCipher
import com.passbolt.mobile.android.storage.usecase.accountdata.GetAccountDataUseCase
import com.passbolt.mobile.android.storage.usecase.accountdata.SaveServerFingerprintUseCase
import com.passbolt.mobile.android.storage.usecase.passphrase.GetPassphraseUseCase
import com.passbolt.mobile.android.storage.usecase.privatekey.GetPrivateKeyUseCase
import com.passbolt.mobile.android.storage.usecase.selectedaccount.SaveSelectedAccountUseCase
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
// presenter for sign in view used for refreshing the session using dedicated endpoint
class RefreshSessionPresenter(
    private val refreshSessionUseCase: RefreshSessionUseCase,
    saveSessionUseCase: SaveSessionUseCase,
    saveSelectedAccountUseCase: SaveSelectedAccountUseCase,
    getAccountDataUseCase: GetAccountDataUseCase,
    passphraseMemoryCache: PassphraseMemoryCache,
    featureFlagsInteractor: FeatureFlagsInteractor,
    signOutUseCase: SignOutUseCase,
    saveServerFingerprintUseCase: SaveServerFingerprintUseCase,
    mfaStatusProvider: MfaStatusProvider,
    biometricCipher: BiometricCipher,
    getPassphraseUseCase: GetPassphraseUseCase,
    getPrivateKeyUseCase: GetPrivateKeyUseCase,
    verifyPassphraseUseCase: VerifyPassphraseUseCase,
    coroutineLaunchContext: CoroutineLaunchContext,
    authReasonMapper: AuthReasonMapper,
    rootDetector: RootDetector,
    getAndVerifyServerKeysInteractor: GetAndVerifyServerKeysInteractor,
    signInVerifyInteractor: SignInVerifyInteractor,
    biometryInteractor: BiometryInteractor
) : SignInPresenter(
    saveSessionUseCase,
    saveSelectedAccountUseCase,
    passphraseMemoryCache,
    signOutUseCase,
    saveServerFingerprintUseCase,
    mfaStatusProvider,
    featureFlagsInteractor,
    getAndVerifyServerKeysInteractor,
    signInVerifyInteractor,
    biometryInteractor,
    getAccountDataUseCase,
    biometricCipher,
    getPassphraseUseCase,
    getPrivateKeyUseCase,
    verifyPassphraseUseCase,
    coroutineLaunchContext,
    authReasonMapper,
    rootDetector
) {

    override fun performSignIn(passphrase: ByteArray) {
        view?.showProgress()
        scope.launch {
            val refreshSessionResult = refreshSessionUseCase.execute(Unit)
            view?.hideProgress()
            when (refreshSessionResult) {
                is RefreshSessionUseCase.Output.Success -> {
                    view?.authSuccess()
                }
                is RefreshSessionUseCase.Output.Failure -> {
                    super.performSignIn(passphrase)
                }
            }
        }
    }
}
