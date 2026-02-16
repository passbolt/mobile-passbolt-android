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

import com.passbolt.mobile.android.permissions.permissions.PermissionsMode
import com.passbolt.mobile.android.ui.ResourceModel

sealed class ResourceDetailsSideEffect {
    data object NavigateBack : ResourceDetailsSideEffect()

    data class NavigateToEditResource(
        val resourceModel: ResourceModel,
    ) : ResourceDetailsSideEffect()

    data class NavigateToResourcePermissions(
        val resourceId: String,
        val mode: PermissionsMode,
    ) : ResourceDetailsSideEffect()

    data class NavigateToResourceTags(
        val resourceId: String,
    ) : ResourceDetailsSideEffect()

    data class NavigateToResourceLocation(
        val resourceId: String,
    ) : ResourceDetailsSideEffect()

    data class OpenWebsite(
        val url: String,
    ) : ResourceDetailsSideEffect()

    data class AddToClipboard(
        val label: String,
        val value: String,
        val isSecret: Boolean,
    ) : ResourceDetailsSideEffect()

    data class CloseWithDeleteSuccess(
        val resourceName: String,
    ) : ResourceDetailsSideEffect()

    data class SetResourceEditedResult(
        val resourceName: String,
    ) : ResourceDetailsSideEffect()

    data class ShowSuccessSnackbar(
        val type: SuccessSnackbarType,
    ) : ResourceDetailsSideEffect()

    data class ShowErrorSnackbar(
        val type: ErrorSnackbarType,
    ) : ResourceDetailsSideEffect()

    data class ShowToast(
        val type: ToastType,
    ) : ResourceDetailsSideEffect()
}

enum class SuccessSnackbarType {
    RESOURCE_EDITED,
    RESOURCE_SHARED,
}

enum class ErrorSnackbarType {
    DECRYPTION_FAILURE,
    FETCH_FAILURE,
    GENERAL_ERROR,
    DATA_REFRESH_ERROR,
    TOGGLE_FAVOURITE_FAILURE,
    CANNOT_PERFORM_ACTION,
}

enum class ToastType {
    CONTENT_NOT_AVAILABLE,
}
