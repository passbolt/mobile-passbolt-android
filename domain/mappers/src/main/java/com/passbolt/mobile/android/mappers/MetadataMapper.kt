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

package com.passbolt.mobile.android.mappers

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.passbolt.mobile.android.dto.request.SessionKeyDto
import com.passbolt.mobile.android.dto.request.SessionKeysBundleDto
import com.passbolt.mobile.android.dto.response.MetadataKeyTypeDto
import com.passbolt.mobile.android.dto.response.MetadataKeysResponseDto
import com.passbolt.mobile.android.dto.response.MetadataKeysSettingsResponseDto
import com.passbolt.mobile.android.dto.response.MetadataSessionKeyResponseDto
import com.passbolt.mobile.android.dto.response.MetadataTypeDto
import com.passbolt.mobile.android.dto.response.MetadataTypesSettingsResponseDto
import com.passbolt.mobile.android.entity.metadata.MetadataKey
import com.passbolt.mobile.android.entity.metadata.MetadataKeyWithPrivateKeys
import com.passbolt.mobile.android.entity.metadata.MetadataPrivateKey
import com.passbolt.mobile.android.ui.MetadataKeyModel
import com.passbolt.mobile.android.ui.MetadataKeyTypeModel
import com.passbolt.mobile.android.ui.MetadataKeysSettingsModel
import com.passbolt.mobile.android.ui.MetadataPrivateKeyModel
import com.passbolt.mobile.android.ui.MetadataSessionKeysBundleModel
import com.passbolt.mobile.android.ui.MetadataTypeModel
import com.passbolt.mobile.android.ui.MetadataTypesSettingsModel
import com.passbolt.mobile.android.ui.ParsedMetadataKeyModel
import com.passbolt.mobile.android.ui.ParsedMetadataPrivateKeyModel
import com.passbolt.mobile.android.ui.SessionKeyIdentifier
import com.passbolt.mobile.android.ui.SessionKeyModel
import java.time.ZonedDateTime
import java.util.UUID

class MetadataMapper {

    fun map(dtos: List<MetadataKeysResponseDto>): List<MetadataKeyModel> = dtos.map(::mapToEntity)

    private fun mapToEntity(dto: MetadataKeysResponseDto) =
        MetadataKeyModel(
            id = dto.id,
            fingerprint = dto.fingerprint,
            armoredKey = dto.armoredKey,
            expired = dto.expired?.let { ZonedDateTime.parse(it) },
            deleted = dto.deleted?.let { ZonedDateTime.parse(it) },
            metadataPrivateKeys = dto.metadataPrivateKeys.map { metadataPrivateKey ->
                MetadataPrivateKeyModel(
                    userId = metadataPrivateKey.userId,
                    keyData = metadataPrivateKey.encryptedKeyData,
                    metadataKeyId = metadataPrivateKey.metadataKeyId
                )
            }
        )

    fun map(uiModel: MetadataKeyModel) = MetadataKey(
        id = uiModel.id.toString(),
        fingerprint = uiModel.fingerprint,
        armoredKey = uiModel.armoredKey,
        expired = uiModel.expired,
        deleted = uiModel.deleted
    )

    fun map(uiModel: MetadataPrivateKeyModel) = MetadataPrivateKey(
        metadataKeyId = uiModel.metadataKeyId.toString(),
        userId = uiModel.userId?.toString(),
        data = uiModel.keyData
    )

    fun mapToUi(daoModel: List<MetadataKeyWithPrivateKeys>): List<ParsedMetadataKeyModel> =
        daoModel.map(::mapToUi)

    fun mapToUi(dao: MetadataKeyWithPrivateKeys): ParsedMetadataKeyModel {
        // TODO(v5) inject - currently graph loop
        val gson = Gson()

        return ParsedMetadataKeyModel(
            id = UUID.fromString(dao.metadataKey.id),
            armoredKey = dao.metadataKey.armoredKey,
            metadataPrivateKeys = dao.metadataPrivateKeys.map { metadataPrivateKey ->
                val parsedPrivateKey = gson.fromJson(metadataPrivateKey.data, JsonObject::class.java)
                ParsedMetadataPrivateKeyModel(
                    userId = metadataPrivateKey.userId?.let { UUID.fromString(it) },
                    armoredKey = parsedPrivateKey.get(KEY_ARMORED_KEY).asString,
                    passphrase = parsedPrivateKey.get(KEY_PASSPHRASE).asString
                )
            }
        )
    }

    private fun MetadataTypeDto.mapToUi() = when (this) {
        MetadataTypeDto.V4 -> MetadataTypeModel.V4
        MetadataTypeDto.V5 -> MetadataTypeModel.V5
    }

    fun map(dto: MetadataTypesSettingsResponseDto) = MetadataTypesSettingsModel(
        defaultMetadataType = dto.defaultMetadataType.mapToUi(),
        defaultFolderType = dto.defaultFolderType.mapToUi(),
        defaultTagType = dto.defaultTagType.mapToUi(),
        allowCreationOfV5Resources = dto.allowCreationOfV5Resources,
        allowCreationOfV5Folders = dto.allowCreationOfV5Folders,
        allowCreationOfV5Tags = dto.allowCreationOfV5Tags,
        allowCreationOfV4Resources = dto.allowCreationOfV4Resources,
        allowCreationOfV4Folders = dto.allowCreationOfV4Folders,
        allowCreationOfV4Tags = dto.allowCreationOfV4Tags,
        allowV4V5Upgrade = dto.allowV4V5Upgrade,
        allowV5V4Downgrade = dto.allowV5V4Downgrade
    )

    fun map(dto: MetadataKeysSettingsResponseDto) =
        MetadataKeysSettingsModel(
            allowUsageOfPersonalKeys = dto.allowUsageOfPersonalKeys,
            zeroKnowledgeKeyShare = dto.zeroKnowledgeKeyShare
        )

    fun mapToDto(metadataKeyTypeModel: MetadataKeyTypeModel?) = metadataKeyTypeModel?.let {
        when (it) {
            MetadataKeyTypeModel.SHARED -> MetadataKeyTypeDto.SHARED
            MetadataKeyTypeModel.PERSONAL -> MetadataKeyTypeDto.PERSONAL
        }
    }

    fun mapToUiModel(value: List<MetadataSessionKeyResponseDto>): List<MetadataSessionKeysBundleModel> =
        value.map {
            MetadataSessionKeysBundleModel(
                id = it.id,
                userId = it.userId,
                data = it.data,
                created = ZonedDateTime.parse(it.created),
                modified = ZonedDateTime.parse(it.modified)
            )
        }

    fun map(value: HashMap<SessionKeyIdentifier, SessionKeyModel>): SessionKeysBundleDto =
        value
            .mapTo(mutableListOf()) { (id, keyModel) ->
                SessionKeyDto(
                    foreignModel = id.foreignModel,
                    foreignId = id.foreignId,
                    sessionKey = keyModel.sessionKey,
                    modified = keyModel.modified.toString()
                )
            }
            .toList()
            .let {
                SessionKeysBundleDto(
                    objectType = "PASSBOLT_SESSION_KEYS",
                    sessionKeys = it
                )
            }

    private companion object {
        private const val KEY_ARMORED_KEY = "armored_key"
        private const val KEY_PASSPHRASE = "passphrase"
    }
}
