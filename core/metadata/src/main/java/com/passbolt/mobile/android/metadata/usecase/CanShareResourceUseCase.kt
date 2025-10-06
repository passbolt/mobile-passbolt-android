package com.passbolt.mobile.android.metadata.usecase

import com.passbolt.mobile.android.common.usecase.AsyncUseCase
import com.passbolt.mobile.android.metadata.usecase.db.GetLocalMetadataKeysUseCase
import com.passbolt.mobile.android.metadata.usecase.db.GetLocalMetadataKeysUseCase.MetadataKeyPurpose.ENCRYPT
import com.passbolt.mobile.android.ui.MetadataTypeModel.V4
import com.passbolt.mobile.android.ui.MetadataTypeModel.V5

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
class CanShareResourceUseCase(
    private val getMetadataTypesSettingsUseCase: GetMetadataTypesSettingsUseCase,
    private val getLocalMetadataKeysUseCase: GetLocalMetadataKeysUseCase,
) : AsyncUseCase<Unit, CanShareResourceUseCase.Output> {
    override suspend fun execute(input: Unit): Output {
        val defaultMetadata =
            getMetadataTypesSettingsUseCase
                .execute(Unit)
                .metadataTypesSettingsModel
                .defaultMetadataType

        return when (defaultMetadata) {
            V4 -> Output(canShareResource = true)
            V5 -> {
                val hasEncryptionKeys =
                    getLocalMetadataKeysUseCase
                        .execute(
                            GetLocalMetadataKeysUseCase.Input(ENCRYPT),
                        ).first()
                        .metadataPrivateKeys
                        .isNotEmpty()

                Output(canShareResource = hasEncryptionKeys)
            }
        }
    }

    data class Output(
        val canShareResource: Boolean,
    )
}
