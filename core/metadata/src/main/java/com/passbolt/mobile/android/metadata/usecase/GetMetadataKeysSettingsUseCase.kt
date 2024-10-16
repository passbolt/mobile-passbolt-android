package com.passbolt.mobile.android.metadata.usecase

import com.passbolt.mobile.android.common.usecase.AsyncUseCase
import com.passbolt.mobile.android.core.accounts.usecase.SelectedAccountUseCase
import com.passbolt.mobile.android.encryptedstorage.EncryptedSharedPreferencesFactory
import com.passbolt.mobile.android.metadata.usecase.MetadataTypesStorageConstants.ALLOW_USAGE_OF_PERSONAL_KEYS
import com.passbolt.mobile.android.metadata.usecase.MetadataTypesStorageConstants.ZERO_KNOWLEDGE_KEY_SHARE
import com.passbolt.mobile.android.ui.MetadataKeysSettingsModel

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
class GetMetadataKeysSettingsUseCase(
    private val encryptedSharedPreferencesFactory: EncryptedSharedPreferencesFactory
) : AsyncUseCase<Unit, GetMetadataKeysSettingsUseCase.Output>,
    SelectedAccountUseCase {

    override suspend fun execute(input: Unit): Output {
        val fileName = MetadataSettingsFileName(selectedAccountId).name
        encryptedSharedPreferencesFactory.get("$fileName.xml").let {

            return Output(
                MetadataKeysSettingsModel(
                    allowUsageOfPersonalKeys = it.getBoolean(ALLOW_USAGE_OF_PERSONAL_KEYS, true),
                    zeroKnowledgeKeyShare = it.getBoolean(ZERO_KNOWLEDGE_KEY_SHARE, false)
                )
            )
        }
    }

    data class Output(val metadataKeysSettingsModel: MetadataKeysSettingsModel)
}
