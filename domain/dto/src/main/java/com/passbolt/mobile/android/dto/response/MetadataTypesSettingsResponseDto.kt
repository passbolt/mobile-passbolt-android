package com.passbolt.mobile.android.dto.response

import com.google.gson.annotations.SerializedName

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

data class MetadataTypesSettingsResponseDto(
    @SerializedName("default_resource_types")
    val defaultMetadataType: MetadataTypeDto,
    @SerializedName("default_folder_type")
    val defaultFolderType: MetadataTypeDto,
    @SerializedName("default_tag_type")
    val defaultTagType: MetadataTypeDto,
    @SerializedName("allow_creation_of_v5_resources")
    val allowCreationOfV5Resources: Boolean,
    @SerializedName("allow_creation_of_v5_folders")
    val allowCreationOfV5Folders: Boolean,
    @SerializedName("allow_creation_of_v5_tags")
    val allowCreationOfV5Tags: Boolean,
    @SerializedName("allow_creation_of_v4_resources")
    val allowCreationOfV4Resources: Boolean,
    @SerializedName("allow_creation_of_v4_folders")
    val allowCreationOfV4Folders: Boolean,
    @SerializedName("allow_creation_of_v4_tags")
    val allowCreationOfV4Tags: Boolean,
    @SerializedName("allow_v4_v5_upgrade")
    val allowV4V5Upgrade: Boolean,
    @SerializedName("allow_v5_v4_downgrade")
    val allowV5V4Downgrade: Boolean,
)

enum class MetadataTypeDto {
    @SerializedName("v4")
    V4,

    @SerializedName("v5")
    V5,
}
