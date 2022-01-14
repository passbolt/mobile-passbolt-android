package com.passbolt.mobile.android.feature.authentication.auth

import androidx.biometric.BiometricPrompt
import com.passbolt.mobile.android.core.navigation.ActivityIntents
import com.passbolt.mobile.android.feature.authentication.MfaProviderHandler
import com.passbolt.mobile.android.feature.authentication.auth.challenge.ChallengeDecryptor
import com.passbolt.mobile.android.feature.authentication.auth.challenge.ChallengeProvider
import com.passbolt.mobile.android.feature.authentication.auth.challenge.ChallengeVerifier
import com.passbolt.mobile.android.feature.authentication.auth.challenge.MfaStatusProvider
import com.passbolt.mobile.android.feature.authentication.auth.presenter.AuthReasonMapper
import com.passbolt.mobile.android.feature.authentication.auth.presenter.PassphrasePresenter
import com.passbolt.mobile.android.feature.authentication.auth.presenter.RefreshSessionPresenter
import com.passbolt.mobile.android.feature.authentication.auth.presenter.SignInPresenter
import com.passbolt.mobile.android.feature.authentication.auth.uistrategy.AuthStrategyFactory
import com.passbolt.mobile.android.feature.authentication.auth.usecase.BiometryInteractor
import com.passbolt.mobile.android.feature.authentication.auth.usecase.GetAndVerifyServerKeysInteractor
import com.passbolt.mobile.android.feature.authentication.auth.usecase.GetServerPublicPgpKeyUseCase
import com.passbolt.mobile.android.feature.authentication.auth.usecase.GetServerPublicRsaKeyUseCase
import com.passbolt.mobile.android.feature.authentication.auth.usecase.SignInUseCase
import com.passbolt.mobile.android.feature.authentication.auth.usecase.SignInVerifyInteractor
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
            SignInUseCase(
                authRepository = get(),
                signInMapper = get(),
                cookieExtractor = get()
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
            AuthReasonMapper()
        }
        scoped {
            MfaStatusProvider()
        }
        scoped {
            GetAndVerifyServerKeysInteractor(
                getServerPublicPgpKeyUseCase = get(),
                getServerPublicRsaKeyUseCase = get(),
                getAccountDataUseCase = get(),
                isServerFingerprintCorrectUseCase = get()
            )
        }
        scoped {
            SignInVerifyInteractor(
                getAccountDataUseCase = get(),
                challengeProvider = get(),
                getSessionUseCase = get(),
                signInUseCase = get(),
                challengeDecryptor = get(),
                challengeVerifier = get()
            )
        }
    }
    single { MfaProviderHandler() }
    single {
        SignOutUseCase(
            passphraseMemoryCache = get(),
            removeSelectedAccountUseCase = get(),
            getSelectedAccountUseCase = get(),
            authRepository = get(),
            signOutMapper = get(),
            getSessionUseCase = get()
        )
    }
    single {
        BiometryInteractor(
            checkIfPassphraseFileExistsUseCase = get(),
            removeBiometricKeyUseCase = get(),
            removeAllAccountsPassphrasesUseCase = get(),
            fingerprintInfoProvider = get()
        )
    }
}

private fun ScopeDSL.authPresenter() {
    scoped<AuthContract.Presenter> { (authConfig: ActivityIntents.AuthConfig) ->
        when (authConfig) {
            is ActivityIntents.AuthConfig.Startup -> signInPresenter()
            is ActivityIntents.AuthConfig.Setup -> signInPresenter()
            is ActivityIntents.AuthConfig.ManageAccount -> signInPresenter()
            is ActivityIntents.AuthConfig.SignIn -> signInPresenter()
            is ActivityIntents.AuthConfig.RefreshPassphrase -> passphrasePresenter()
            is ActivityIntents.AuthConfig.Mfa -> passphrasePresenter()
            is ActivityIntents.AuthConfig.RefreshSession -> refreshSessionPresenter()
        }
    }
}

private fun Scope.passphrasePresenter() = PassphrasePresenter(
    passphraseMemoryCache = get(),
    getPrivateKeyUseCase = get(),
    verifyPassphraseUseCase = get(),
    getAccountDataUseCase = get(),
    coroutineLaunchContext = get(),
    biometricCipher = get(),
    getPassphraseUseCase = get(),
    authReasonMapper = get(),
    rootDetector = get(),
    biometryInteractor = get()
)

private fun Scope.signInPresenter() = SignInPresenter(
    saveSessionUseCase = get(),
    saveSelectedAccountUseCase = get(),
    passphraseMemoryCache = get(),
    signOutUseCase = get(),
    saveServerFingerprintUseCase = get(),
    mfaStatusProvider = get(),
    featureFlagsInteractor = get(),
    getAndVerifyServerKeysInteractor = get(),
    signInVerifyInteractor = get(),
    getAccountDataUseCase = get(),
    biometricCipher = get(),
    getPassphraseUseCase = get(),
    getPrivateKeyUseCase = get(),
    verifyPassphraseUseCase = get(),
    coroutineLaunchContext = get(),
    authReasonMapper = get(),
    rootDetector = get(),
    biometryInteractor = get()
)

private fun Scope.refreshSessionPresenter() = RefreshSessionPresenter(
    refreshSessionUseCase = get(),
    saveSessionUseCase = get(),
    saveSelectedAccountUseCase = get(),
    getAccountDataUseCase = get(),
    passphraseMemoryCache = get(),
    featureFlagsInteractor = get(),
    signOutUseCase = get(),
    saveServerFingerprintUseCase = get(),
    mfaStatusProvider = get(),
    biometricCipher = get(),
    getPassphraseUseCase = get(),
    getPrivateKeyUseCase = get(),
    verifyPassphraseUseCase = get(),
    coroutineLaunchContext = get(),
    authReasonMapper = get(),
    rootDetector = get(),
    getAndVerifyServerKeysInteractor = get(),
    signInVerifyInteractor = get(),
    biometryInteractor = get()
)
