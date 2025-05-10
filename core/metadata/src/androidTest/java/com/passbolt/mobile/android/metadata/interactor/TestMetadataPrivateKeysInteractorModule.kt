package com.passbolt.mobile.android.metadata.interactor

import com.passbolt.mobile.android.core.accounts.usecase.accountdata.GetSelectedAccountDataUseCase
import com.passbolt.mobile.android.core.accounts.usecase.privatekey.GetSelectedUserPrivateKeyUseCase
import com.passbolt.mobile.android.core.passphrasememorycache.PassphraseMemoryCache
import com.passbolt.mobile.android.core.users.usecase.db.GetLocalUserUseCase
import com.passbolt.mobile.android.gopenpgp.OpenPgp
import com.passbolt.mobile.android.gopenpgp.exception.GopenPgpExceptionParser
import com.passbolt.mobile.android.metadata.usecase.DeleteTrustedMetadataKeyUseCase
import com.passbolt.mobile.android.metadata.usecase.GetTrustedMetadataKeyUseCase
import com.passbolt.mobile.android.metadata.usecase.SaveTrustedMetadataKeyUseCase
import com.passbolt.mobile.android.metadata.usecase.UpdateMetadataPrivateKeyUseCase
import com.passbolt.mobile.android.metadata.usecase.db.GetLocalMetadataKeysUseCase
import com.proton.gopenpgp.crypto.Crypto
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module
import org.mockito.Mockito.mock

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

internal val mockGetLocalMetadataKeysUseCase = mock<GetLocalMetadataKeysUseCase>()
internal val mockUpdateMetadataPrivateKeyUseCase = mock<UpdateMetadataPrivateKeyUseCase>()
internal val mockGetLocalUserUseCase = mock<GetLocalUserUseCase>()
internal val mockGetSelectedUserPrivateKeyUseCase = mock<GetSelectedUserPrivateKeyUseCase>()
internal val mockPassphraseMemoryCache = mock<PassphraseMemoryCache>()
internal val mockGetTrustedMetadataKeyUseCase = mock<GetTrustedMetadataKeyUseCase>()
internal val mockSaveTrustedMetadataKeyUseCase = mock<SaveTrustedMetadataKeyUseCase>()
internal val mockDeleteTrustedMetadataKeyUseCase = mock<DeleteTrustedMetadataKeyUseCase>()
internal val mockGetSelectedAccountDataUseCase = mock<GetSelectedAccountDataUseCase>()

val testMetadataPrivateKeysInteractorModule = module {
    factory { Crypto.pgp() }
    singleOf(::GopenPgpExceptionParser)
    factoryOf(::OpenPgp)
    factory {
        MetadataPrivateKeysHelperInteractor(
            openPgp = get(),
            updateMetadataPrivateKeyUseCase = mockUpdateMetadataPrivateKeyUseCase,
            getLocalUserUseCase = mockGetLocalUserUseCase,
            saveTrustedMetadataKeyUseCase = mockSaveTrustedMetadataKeyUseCase,
            getSelectedAccountDataUseCase = mockGetSelectedAccountDataUseCase
        )
    }
    factory {
        MetadataPrivateKeysInteractor(
            openPgp = get(),
            metadataPrivateKeysHelperInteractor = get(),
            getLocalMetadataKeysUseCase = mockGetLocalMetadataKeysUseCase,
            getLocalUserUseCase = mockGetLocalUserUseCase,
            getSelectedUserPrivateKeyUseCase = mockGetSelectedUserPrivateKeyUseCase,
            passphraseMemoryCache = mockPassphraseMemoryCache,
            getTrustedMetadataKeyUseCase = mockGetTrustedMetadataKeyUseCase,
            deleteTrustedMetadataKeyUseCase = mockDeleteTrustedMetadataKeyUseCase,
        )
    }
}
