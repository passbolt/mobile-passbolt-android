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

package com.passbolt.mobile.android.feature.resourcedetails.details

import com.passbolt.mobile.android.ui.CustomFieldModel
import com.passbolt.mobile.android.ui.OtpItemWrapper
import com.passbolt.mobile.android.ui.PermissionModelUi
import com.passbolt.mobile.android.ui.ResourceModel
import java.util.UUID

data class ResourceDetailsState(
    val isRefreshing: Boolean = false,
    val isLoading: Boolean = false,
    val resourceData: ResourceData = ResourceData(),
    val totpData: TotpData = TotpData(),
    val noteData: NoteData = NoteData(),
    val passwordData: PasswordData = PasswordData(),
    val metadataData: MetadataData = MetadataData(),
    val sharedWithData: SharedWithData = SharedWithData(),
    val customFieldsData: CustomFieldsData = CustomFieldsData(),
    val showDeleteResourceConfirmationDialog: Boolean = false,
) {
    val requiredResourceModel: ResourceModel
        get() = requireNotNull(resourceData.resourceModel)

    val showPasswordSection: Boolean
        get() =
            resourceData.resourceModel
                ?.metadataJsonModel
                ?.username
                ?.isNotBlank() == true ||
                passwordData.showPasswordItem ||
                metadataData.mainUri.isNotEmpty()

    val showMetadataSection =
        metadataData.showMetadataDescriptionItem ||
            metadataData.canViewLocation ||
            metadataData.canViewTags ||
            metadataData.additionalUris.isNotEmpty() ||
            resourceData.resourceModel?.expiry != null
}

data class ResourceData(
    val resourceModel: ResourceModel? = null,
)

data class TotpData(
    val showTotpSection: Boolean = false,
    val totpModel: OtpItemWrapper? = null,
)

data class NoteData(
    val showNoteSection: Boolean = false,
    val isNoteVisible: Boolean = false,
    val note: String = "",
)

data class PasswordData(
    val showPasswordItem: Boolean = false,
    val showPasswordEyeIcon: Boolean = false,
    val isPasswordVisible: Boolean = false,
    val password: String = "",
)

data class MetadataData(
    val showMetadataDescriptionItem: Boolean = false,
    val canViewTags: Boolean = false,
    val tags: List<String> = emptyList(),
    val canViewLocation: Boolean = false,
    val locationPath: List<String> = emptyList(),
    val mainUri: String = "",
    val additionalUris: List<String> = emptyList(),
)

data class CustomFieldsData(
    val showCustomFieldsSection: Boolean = false,
    val customFields: Map<UUID, String> = emptyMap(),
    val visibleCustomFields: Map<UUID, CustomFieldModel?> = emptyMap(),
)

data class SharedWithData(
    val canViewPermissions: Boolean = false,
    val permissions: List<PermissionModelUi> = emptyList(),
)
