package com.passbolt.mobile.android.feature.home.screen

import com.passbolt.mobile.android.ui.LeadingContentType
import com.passbolt.mobile.android.ui.ResourceModel

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
internal sealed interface HomeSideEffect {
    data object InitiateDataRefresh : HomeSideEffect

    data class NavigateToCreateTotp(
        val folderId: String?,
    ) : HomeSideEffect

    data class NavigateToCreateFolder(
        val folderId: String?,
    ) : HomeSideEffect

    data class NavigateToCreateResourceForm(
        val leadingContentType: LeadingContentType,
        val folderId: String?,
    ) : HomeSideEffect

    data class NavigateToEditResourceForm(
        val resourceModel: ResourceModel,
    ) : HomeSideEffect

    data class NavigateToResourceUri(
        val url: String,
    ) : HomeSideEffect

    data class NavigateToShare(
        val resourceModel: ResourceModel,
    ) : HomeSideEffect

    data class CopyToClipboard(
        val label: String,
        val value: String,
        val isSensitive: Boolean,
    ) : HomeSideEffect

    data class ShowErrorSnackbar(
        val type: SnackbarErrorType,
        val message: String? = null,
    ) : HomeSideEffect

    data class ShowSuccessSnackbar(
        val type: SnackbarSuccessType,
        val message: String? = null,
    ) : HomeSideEffect

    data class ShowToast(
        val type: ToastType,
    ) : HomeSideEffect

    data class OpenResourceMoreMenu(
        val resourceId: String,
        val resourceName: String,
    ) : HomeSideEffect
}

internal enum class SnackbarErrorType {
    ENCRYPTION_FAILURE,
    DECRYPTION_FAILURE,
    FETCH_FAILURE,
    TOGGLE_FAVOURITE_FAILURE,
    FAILED_TO_DELETE_RESOURCE,
    RESOURCE_SCHEMA_INVALID,
    SECRET_SCHEMA_INVALID,
    CANNOT_UPDATE_WITH_CURRENT_CONFIGURATION,
    FAILED_TO_VERIFY_METADATA_KEYS,
    FAILED_TO_TRUST_METADATA_KEY,
    FAILED_TO_REFRESH_DATA,
    NO_SHARED_KEY_ACCESS,
    ERROR,
}

internal enum class SnackbarSuccessType {
    RESOURCE_EDITED,
    RESOURCE_CREATED,
    RESOURCE_DELETED,
    RESOURCE_SHARED,
    METADATA_KEY_IS_TRUSTED,
    FOLDER_CREATED,
}

internal enum class ToastType {
    WAIT_FOR_DATA_REFRESH_FINISH,
}
