package com.passbolt.mobile.android.feature.auth

import com.google.gson.Gson
import com.nhaarman.mockitokotlin2.mock
import com.passbolt.mobile.android.common.TimeProvider
import com.passbolt.mobile.android.common.UuidProvider
import com.passbolt.mobile.android.feature.authentication.auth.challenge.ChallengeDecryptor
import com.passbolt.mobile.android.feature.authentication.auth.challenge.ChallengeProvider
import com.passbolt.mobile.android.feature.authentication.auth.challenge.ChallengeVerifier
import com.passbolt.mobile.android.gopenpgp.OpenPgp
import com.passbolt.mobile.android.storage.usecase.privatekey.GetPrivateKeyUseCase
import org.koin.dsl.module

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

internal val getPrivateKeyUseCase = mock<GetPrivateKeyUseCase>()
internal val openPgp = mock<OpenPgp>()
internal val timeProvider = mock<TimeProvider>()
internal val uuidProvider = mock<UuidProvider>()

val testModule = module {
    factory { Gson() }
    factory { getPrivateKeyUseCase }
    factory { openPgp }
    factory {
        ChallengeDecryptor(
            openPgp = get(),
            getPrivateKeyUseCase = get(),
            gson = get()
        )
    }
    factory {
        ChallengeProvider(
            openPgp = get(),
            privateKeyUseCase = get(),
            gson = get(),
            timeProvider = get(),
            uuidProvider = get()
        )
    }
    factory {
        ChallengeVerifier()
    }
    factory { timeProvider }
    factory { uuidProvider }
}
