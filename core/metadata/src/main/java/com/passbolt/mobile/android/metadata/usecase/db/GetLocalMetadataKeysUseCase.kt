package com.passbolt.mobile.android.metadata.usecase.db

import com.passbolt.mobile.android.common.usecase.AsyncUseCase
import com.passbolt.mobile.android.core.accounts.usecase.SelectedAccountUseCase
import com.passbolt.mobile.android.database.DatabaseProvider
import com.passbolt.mobile.android.mappers.MetadataMapper
import com.passbolt.mobile.android.metadata.usecase.db.GetLocalMetadataKeysUseCase.MetadataKeyPurpose.DECRYPT
import com.passbolt.mobile.android.metadata.usecase.db.GetLocalMetadataKeysUseCase.MetadataKeyPurpose.ENCRYPT
import com.passbolt.mobile.android.ui.ParsedMetadataKeyModel

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
class GetLocalMetadataKeysUseCase(
    private val databaseProvider: DatabaseProvider,
    private val metadataMapper: MetadataMapper
) : AsyncUseCase<GetLocalMetadataKeysUseCase.Input, List<ParsedMetadataKeyModel>>,
    SelectedAccountUseCase {

    override suspend fun execute(input: Input): List<ParsedMetadataKeyModel> {
        val metadataKeysDao = databaseProvider
            .get(selectedAccountId)
            .metadataKeysDao()

        return when (input.purpose) {
            ENCRYPT -> metadataKeysDao.getEncryptionMetadataKeysWithPrivateKeys()
            DECRYPT -> metadataKeysDao.getDecryptionMetadataKeysWithPrivateKeys()
        }.map { metadataMapper.mapToUi(it) }
    }

    data class Input(val purpose: MetadataKeyPurpose)

    enum class MetadataKeyPurpose {
        /**
         * Metadata keys that are not expired and deleted. Those should be used for encryption.
         */
        ENCRYPT,

        /**
         * Metadata keys that may be expired. They may be used for decryption of not yet migrated items.
         */
        DECRYPT
    }
}
