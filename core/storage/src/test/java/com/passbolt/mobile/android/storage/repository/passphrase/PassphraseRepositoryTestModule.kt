package com.passbolt.mobile.android.storage.repository.passphrase

import com.nhaarman.mockitokotlin2.mock
import com.passbolt.mobile.android.storage.cache.passphrase.PassphraseMemoryCache
import com.passbolt.mobile.android.storage.usecase.GetPassphraseUseCase
import com.passbolt.mobile.android.storage.usecase.GetSelectedAccountUseCase
import com.passbolt.mobile.android.storage.usecase.RemoveSelectedAccountPassphraseUseCase
import com.passbolt.mobile.android.storage.usecase.SavePassphraseUseCase
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

internal val passphraseMemoryCacheMock = mock<PassphraseMemoryCache>()
internal val getSelectedAccountUseCaseMock = mock<GetSelectedAccountUseCase>()
internal val getPassphraseUseCaseMock = mock<GetPassphraseUseCase>()
internal val savePassphraseUseCaseMock = mock<SavePassphraseUseCase>()
internal val clearPassphraseUseCaseMock = mock<RemoveSelectedAccountPassphraseUseCase>()

val passphraseRepositoryTestModule = module {
    factory {
        PassphraseRepository(
            passphraseMemoryCache = passphraseMemoryCacheMock,
            getPassphraseUseCase = getPassphraseUseCaseMock,
            selectedAccountUseCase = getSelectedAccountUseCaseMock,
            savePassphraseUseCase = savePassphraseUseCaseMock,
            removeSelectedAccountPassphraseUseCase = clearPassphraseUseCaseMock
        )
    }
}
