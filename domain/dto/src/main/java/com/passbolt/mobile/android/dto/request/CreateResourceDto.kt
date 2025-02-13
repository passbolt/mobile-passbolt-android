package com.passbolt.mobile.android.dto.request

import com.google.gson.annotations.SerializedName
import com.passbolt.mobile.android.dto.response.MetadataKeyTypeDto
import java.time.ZonedDateTime

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
sealed class CreateResourceDto {
    abstract val resourceTypeId: String
    abstract val secrets: List<EncryptedSecret>
    abstract val folderParentId: String?
    abstract val expiry: ZonedDateTime?
}

data class CreateV4ResourceDto(
    val name: String,
    @SerializedName("resource_type_id")
    override val resourceTypeId: String,
    override val secrets: List<EncryptedSecret>,
    val username: String?,
    val uri: String?,
    val description: String?,
    @SerializedName("folder_parent_id")
    override val folderParentId: String?,
    @SerializedName("expired")
    override val expiry: ZonedDateTime?
) : CreateResourceDto()

data class CreateV5ResourceDto(
    @SerializedName("resource_type_id")
    override val resourceTypeId: String,
    override val secrets: List<EncryptedSecret>,
    @SerializedName("folder_parent_id")
    override val folderParentId: String?,
    @SerializedName("expired")
    override val expiry: ZonedDateTime?,
    val metadata: String,
    @SerializedName("metadata_key_id")
    val metadataKeyId: String?,
    @SerializedName("metadata_key_type")
    val metadataKeyType: MetadataKeyTypeDto?
) : CreateResourceDto()

data class EncryptedSecret(
    @SerializedName("user_id")
    val userId: String,
    val data: String
)

data class SecretsDto(
    val password: String,
    val description: String
)

data class TotpSecretsDto(
    val totp: Totp
) {
    data class Totp(
        val algorithm: String,
        @SerializedName("secret_key")
        val key: String,
        val digits: Int,
        val period: Long
    )
}

data class PasswordDescriptionTotpSecretsDto(
    val password: String,
    val description: String,
    val totp: TotpSecretsDto.Totp
)
