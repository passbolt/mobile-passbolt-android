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

import com.passbolt.mobile.android.ui.ResourceModel
import com.passbolt.mobile.android.ui.ResourceMoreMenuModel
import java.util.UUID

sealed class ResourceDetailsIntent {
    data class Initialize(
        val resourceModel: ResourceModel,
    ) : ResourceDetailsIntent()

    data object GoBack : ResourceDetailsIntent()

    data object OpenMoreMenu : ResourceDetailsIntent()

    data object CloseMoreMenu : ResourceDetailsIntent()

    // Copy actions
    data object CopyUsername : ResourceDetailsIntent()

    data object CopyUrl : ResourceDetailsIntent()

    data object CopyPassword : ResourceDetailsIntent()

    data object CopyMetadataDescription : ResourceDetailsIntent()

    data object CopyNote : ResourceDetailsIntent()

    data object CopyTotp : ResourceDetailsIntent()

    data class CopyCustomField(
        val key: UUID,
    ) : ResourceDetailsIntent()

    // Show/hide secret actions
    data object TogglePasswordVisibility : ResourceDetailsIntent()

    data object ToggleNoteVisibility : ResourceDetailsIntent()

    data object ToggleTotpVisibility : ResourceDetailsIntent()

    data class ToggleCustomField(
        val key: UUID,
    ) : ResourceDetailsIntent()

    // Navigation actions
    data object ViewPermissions : ResourceDetailsIntent()

    data object GoToTags : ResourceDetailsIntent()

    data object GoToLocation : ResourceDetailsIntent()

    data object Edit : ResourceDetailsIntent()

    data object EditPermissions : ResourceDetailsIntent()

    data object DeleteClick : ResourceDetailsIntent()

    data object ConfirmDeleteResource : ResourceDetailsIntent()

    data object CloseDeleteConfirmationDialog : ResourceDetailsIntent()

    data object LaunchWebsite : ResourceDetailsIntent()

    data class ToggleFavourite(
        val option: ResourceMoreMenuModel.FavouriteOption,
    ) : ResourceDetailsIntent()

    // Result handling
    data class ResourceEdited(
        val resourceName: String?,
    ) : ResourceDetailsIntent()

    data object ResourceShared : ResourceDetailsIntent()

    data object Dispose : ResourceDetailsIntent()
}
