package com.passbolt.mobile.android.core.navigation.deeplinks

import android.net.Uri
import androidx.navigation.NavDeepLinkRequest

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
object NavDeepLinkProvider {

    fun permissionsDeepLinkRequest(
        permissionItemName: String,
        permissionItemId: String,
        permissionsModeName: String,
        navigationOriginName: String
    ) =
        NavDeepLinkRequest.Builder
            .fromUri(
                Uri.Builder()
                    .scheme(NAV_DEEP_LINK_SCHEME)
                    .authority(AUTHORITY_PERMISSIONS)
                    .appendPath(permissionItemName)
                    .appendPath(permissionItemId)
                    .appendQueryParameter(QUERY_PERMISSIONS_MODE, permissionsModeName)
                    .appendQueryParameter(QUERY_NAVIGATION_ORIGIN, navigationOriginName)
                    .build()
            ).build()

    fun locationDetailsDeepLinkRequest(
        locationDetailsItemName: String,
        locationDetailsItemId: String
    ) =
        NavDeepLinkRequest.Builder
            .fromUri(
                Uri.Builder()
                    .scheme(NAV_DEEP_LINK_SCHEME)
                    .authority(AUTHORITY_LOCATION_DETAILS)
                    .appendPath(locationDetailsItemName)
                    .appendPath(locationDetailsItemId)
                    .build()
            ).build()

    fun folderDetailsDeepLinkRequest(childFolderId: String) =
        NavDeepLinkRequest.Builder
            .fromUri(
                Uri.Builder()
                    .scheme(NAV_DEEP_LINK_SCHEME)
                    .authority(AUTHORITY_FOLDERS)
                    .path(childFolderId)
                    .build()
            ).build()

    fun createFolderDeepLinkRequest(parentFolderId: String?) =
        NavDeepLinkRequest.Builder
            .fromUri(
                Uri.Builder()
                    .scheme(NAV_DEEP_LINK_SCHEME)
                    .authority(AUTHORITY_CREATE_FOLDER)
                    .apply { parentFolderId?.let { appendQueryParameter(QUERY_PARENT_FOLDER_ID, it) } }
                    .build()
            )
            .build()

    fun resourceTagsDeepLinkRequest(
        resourceId: String,
        permissionsModeName: String
    ) =
        NavDeepLinkRequest.Builder
            .fromUri(
                Uri.Builder()
                    .scheme(NAV_DEEP_LINK_SCHEME)
                    .authority(AUTHORITY_TAGS_DETAILS)
                    .appendPath(resourceId)
                    .appendQueryParameter(QUERY_PERMISSIONS_MODE, permissionsModeName)
                    .build()
            ).build()

    fun resourceResourcePickerDeepLinkRequest(suggestionUri: String?) =
        NavDeepLinkRequest.Builder
            .fromUri(
                Uri.Builder()
                    .scheme(NAV_DEEP_LINK_SCHEME)
                    .authority(AUTHORITY_RESOURCE_PICKER)
                    .apply {
                        suggestionUri?.let {
                            appendQueryParameter(QUERY_SUGGESTION_URI, suggestionUri)
                        }
                    }
                    .build()
            ).build()

    fun otpManualFormDeepLinkRequest(editedResourceId: String?) =
        NavDeepLinkRequest.Builder
            .fromUri(
                Uri.Builder()
                    .scheme(NAV_DEEP_LINK_SCHEME)
                    .authority(AUTHORITY_OTP_MANUAL_FORM)
                    .apply {
                        editedResourceId?.let {
                            appendQueryParameter(QUERY_EDITED_RESOURCE_ID, editedResourceId)
                        }
                    }
                    .build()
            ).build()

    fun resourceFormDeepLinkRequest(modeName: String, leadingTypeName: String, parentFolderId: String? = null) =
        NavDeepLinkRequest.Builder
            .fromUri(
                Uri.Builder()
                    .scheme(NAV_DEEP_LINK_SCHEME)
                    .authority(AUTHORITY_RESOURCE_FORM)
                    .apply {
                        appendQueryParameter(QUERY_RESOURCE_FORM_MODE, modeName)
                        appendQueryParameter(QUERY_RESOURCE_FORM_LEADING_TYPE, leadingTypeName)
                        appendQueryParameter(QUERY_RESOURCE_FORM_PARENT_FOLDER_ID, parentFolderId)
                    }
                    .build()
            ).build()

    fun resourceFormDescriptionDeepLinkRequest(modeName: String) =
        NavDeepLinkRequest.Builder
            .fromUri(
                Uri.Builder()
                    .scheme(NAV_DEEP_LINK_SCHEME)
                    .authority(AUTHORITY_RESOURCE_FORM)
                    .path(PATH_RESOURCE_FORM_DESCRIPTION)
                    .apply {
                        appendQueryParameter(QUERY_RESOURCE_FORM_MODE, modeName)
                    }
                    .build()
            ).build()

    fun resourceFormSecureNoteDeepLinkRequest(modeName: String) =
        NavDeepLinkRequest.Builder
            .fromUri(
                Uri.Builder()
                    .scheme(NAV_DEEP_LINK_SCHEME)
                    .authority(AUTHORITY_RESOURCE_FORM)
                    .path(PATH_RESOURCE_FORM_SECURE_NOTE)
                    .apply {
                        appendQueryParameter(QUERY_RESOURCE_FORM_MODE, modeName)
                    }
                    .build()
            ).build()

    fun resourceFormTotpSettingsDeepLinkRequest(modeName: String) =
        NavDeepLinkRequest.Builder
            .fromUri(
                Uri.Builder()
                    .scheme(NAV_DEEP_LINK_SCHEME)
                    .authority(AUTHORITY_RESOURCE_FORM)
                    .path(PATH_RESOURCE_FORM_TOTP)
                    .apply {
                        appendQueryParameter(QUERY_RESOURCE_FORM_MODE, modeName)
                    }
                    .build()
            ).build()

    fun resourceFormTotpAdvancedSettingsDeepLinkRequest(modeName: String) =
        NavDeepLinkRequest.Builder
            .fromUri(
                Uri.Builder()
                    .scheme(NAV_DEEP_LINK_SCHEME)
                    .authority(AUTHORITY_RESOURCE_FORM)
                    .path(PATH_RESOURCE_FORM_TOTP_ADVANCED_SETTINGS)
                    .apply {
                        appendQueryParameter(QUERY_RESOURCE_FORM_MODE, modeName)
                    }
                    .build()
            ).build()

    private const val NAV_DEEP_LINK_SCHEME = "passbolt"

    private const val AUTHORITY_PERMISSIONS = "permissions"
    private const val AUTHORITY_LOCATION_DETAILS = "locationDetails"
    private const val AUTHORITY_FOLDERS = "folders"
    private const val AUTHORITY_CREATE_FOLDER = "createFolder"
    private const val AUTHORITY_TAGS_DETAILS = "tagsDetails"
    private const val AUTHORITY_RESOURCE_PICKER = "resourcePicker"
    private const val AUTHORITY_OTP_MANUAL_FORM = "otpManualForm"
    private const val AUTHORITY_RESOURCE_FORM = "resourceForm"

    private const val PATH_RESOURCE_FORM_DESCRIPTION = "description"
    private const val PATH_RESOURCE_FORM_SECURE_NOTE = "secureNote"
    private const val PATH_RESOURCE_FORM_TOTP = "totp"
    private const val PATH_RESOURCE_FORM_TOTP_ADVANCED_SETTINGS = "$PATH_RESOURCE_FORM_TOTP/advancedSettings"

    private const val QUERY_PERMISSIONS_MODE = "mode"
    private const val QUERY_NAVIGATION_ORIGIN = "navigationOrigin"
    private const val QUERY_PARENT_FOLDER_ID = "parentFolderId"
    private const val QUERY_SUGGESTION_URI = "suggestionUri"
    private const val QUERY_EDITED_RESOURCE_ID = "editedOtpResourceId"
    private const val QUERY_RESOURCE_FORM_MODE = "mode"
    private const val QUERY_RESOURCE_FORM_LEADING_TYPE = "leadingType"
    private const val QUERY_RESOURCE_FORM_PARENT_FOLDER_ID = "parentFolderId"
}
