package com.passbolt.mobile.android.feature.authentication.auth

import com.passbolt.mobile.android.feature.authentication.AuthenticationType
import com.passbolt.mobile.android.feature.authentication.auth.challenge.ChallengeDecryptor
import com.passbolt.mobile.android.feature.authentication.auth.challenge.ChallengeProvider
import com.passbolt.mobile.android.feature.authentication.auth.challenge.ChallengeVerifier
import com.passbolt.mobile.android.feature.authentication.auth.presenter.PassphrasePresenter
import com.passbolt.mobile.android.feature.authentication.auth.presenter.SignInPresenter
import com.passbolt.mobile.android.feature.authentication.auth.uistrategy.AuthStrategyFactory
import com.passbolt.mobile.android.feature.authentication.auth.usecase.GetServerPublicPgpKeyUseCase
import com.passbolt.mobile.android.feature.authentication.auth.usecase.GetServerPublicRsaKeyUseCase
import com.passbolt.mobile.android.feature.authentication.auth.usecase.SiginInUseCase
import com.passbolt.mobile.android.feature.setup.enterpassphrase.VerifyPassphraseUseCase
import org.koin.core.module.Module
import org.koin.core.qualifier.named
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
fun Module.authModule() {
    scope(named<AuthFragment>()) {
        authPresenters()
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
                signInMapper = get()
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
    }
}

private fun ScopeDSL.authPresenters() {
    scoped<AuthContract.Presenter>(named(AuthenticationType.SIGN_IN.name)) {
        SignInPresenter(
            getServerPublicPgpKeyUseCase = get(),
            getServerPublicRsaKeyUseCase = get(),
            signInUseCase = get(),
            coroutineLaunchContext = get(),
            challengeProvider = get(),
            challengeDecryptor = get(),
            challengeVerifier = get(),
            getAccountDataUseCase = get()
        )
    }
    scoped<AuthContract.Presenter>(named(AuthenticationType.PASSPHRASE.name)) {
        PassphrasePresenter(
            getAccountDataUseCase = get(),
            coroutineLaunchContext = get(),
            passphraseRepository = get(),
            getSelectedUserPrivateKeyUseCase = get(),
            verifyPassphraseUseCase = get()
        )
    }
}
