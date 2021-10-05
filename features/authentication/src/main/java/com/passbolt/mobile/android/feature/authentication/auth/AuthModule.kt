package com.passbolt.mobile.android.feature.authentication.auth

import androidx.biometric.BiometricPrompt
import com.passbolt.mobile.android.core.navigation.ActivityIntents
import com.passbolt.mobile.android.feature.authentication.auth.challenge.ChallengeDecryptor
import com.passbolt.mobile.android.feature.authentication.auth.challenge.ChallengeProvider
import com.passbolt.mobile.android.feature.authentication.auth.challenge.ChallengeVerifier
import com.passbolt.mobile.android.feature.authentication.auth.challenge.MfaStatusProvider
import com.passbolt.mobile.android.feature.authentication.auth.presenter.AuthReasonMapper
import com.passbolt.mobile.android.feature.authentication.auth.presenter.PassphrasePresenter
import com.passbolt.mobile.android.feature.authentication.auth.presenter.SignInPresenter
import com.passbolt.mobile.android.feature.authentication.auth.uistrategy.AuthStrategyFactory
import com.passbolt.mobile.android.feature.authentication.auth.usecase.GetServerPublicPgpKeyUseCase
import com.passbolt.mobile.android.feature.authentication.auth.usecase.GetServerPublicRsaKeyUseCase
import com.passbolt.mobile.android.feature.authentication.auth.usecase.SiginInUseCase
import com.passbolt.mobile.android.feature.authentication.auth.usecase.SignOutUseCase
import com.passbolt.mobile.android.feature.setup.enterpassphrase.VerifyPassphraseUseCase
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.core.scope.Scope
import org.koin.dsl.ScopeDSL

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

@Suppress("LongMethod")
fun Module.authModule() {
    scope(named<AuthFragment>()) {
        authPresenter()
        scoped {
            GetServerPublicPgpKeyUseCase(
                authRepository = get()
            )
        }
        scoped {
            GetServerPublicRsaKeyUseCase(
                authRepository = get()
            )
        }
        scoped {
            SiginInUseCase(
                authRepository = get(),
                signInMapper = get(),
                mfaTokenExtractor = get()
            )
        }
        scoped {
            ChallengeProvider(
                gson = get(),
                openPgp = get(),
                privateKeyUseCase = get(),
                timeProvider = get(),
                uuidProvider = get()
            )
        }
        scoped {
            ChallengeDecryptor(
                openPgp = get(),
                getPrivateKeyUseCase = get(),
                gson = get()
            )
        }
        scoped {
            ChallengeVerifier()
        }
        scoped {
            AuthStrategyFactory()
        }
        scoped {
            VerifyPassphraseUseCase(
                openPgp = get()
            )
        }
        scoped {
            BiometricPrompt.PromptInfo.Builder()
        }
        scoped {
            SignOutUseCase(
                passphraseMemoryCache = get(),
                removeSelectedAccountUseCase = get(),
                getSelectedAccountUseCase = get(),
                authRepository = get(),
                signOutMapper = get(),
                getSessionUseCase = get()
            )
        }
        scoped {
            AuthReasonMapper()
        }
        scoped {
            MfaStatusProvider()
        }
    }
}

private fun ScopeDSL.authPresenter() {
    scoped<AuthContract.Presenter> { (authConfig: ActivityIntents.AuthConfig) ->
        when (authConfig) {
            ActivityIntents.AuthConfig.STARTUP -> signInPresenter()
            ActivityIntents.AuthConfig.SETUP -> signInPresenter()
            ActivityIntents.AuthConfig.MANAGE_ACCOUNT -> signInPresenter()
            ActivityIntents.AuthConfig.REFRESH_FULL -> signInPresenter()
            ActivityIntents.AuthConfig.REFRESH_PASSPHRASE -> passphrasePresenter()
        }
    }
}

private fun Scope.passphrasePresenter() = PassphrasePresenter(
    passphraseMemoryCache = get(),
    getPrivateKeyUseCase = get(),
    verifyPassphraseUseCase = get(),
    fingerprintInfoProvider = get(),
    removeSelectedAccountPassphraseUseCase = get(),
    checkIfPassphraseFileExistsUseCase = get(),
    getAccountDataUseCase = get(),
    coroutineLaunchContext = get(),
    biometricCipher = get(),
    getPassphraseUseCase = get(),
    removeBiometricKeyUseCase = get(),
    authReasonMapper = get()
)

private fun Scope.signInPresenter() = SignInPresenter(
    getServerPublicPgpKeyUseCase = get(),
    getServerPublicRsaKeyUseCase = get(),
    signInUseCase = get(),
    coroutineLaunchContext = get(),
    challengeProvider = get(),
    challengeDecryptor = get(),
    challengeVerifier = get(),
    getAccountDataUseCase = get(),
    saveSessionUseCase = get(),
    saveSelectedAccountUseCase = get(),
    checkIfPassphraseFileExistsUseCase = get(),
    passphraseMemoryCache = get(),
    removeSelectedAccountPassphraseUseCase = get(),
    fingerprintInfoProvider = get(),
    featureFlagsInteractor = get(),
    signOutUseCase = get(),
    getPrivateKeyUseCase = get(),
    verifyPassphraseUseCase = get(),
    biometricCipher = get(),
    getPassphraseUseCase = get(),
    removeBiometricKeyUseCase = get(),
    saveServerFingerprintUseCase = get(),
    isServerFingerprintCorrectUseCase = get(),
    authReasonMapper = get(),
    mfaStatusProvider = get()
)
