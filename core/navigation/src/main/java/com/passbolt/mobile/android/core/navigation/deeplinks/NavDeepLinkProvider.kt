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
    ) = NavDeepLinkRequest.Builder
        .fromUri(
            Uri
                .Builder()
                .scheme(NAV_DEEP_LINK_SCHEME)
                .authority(AUTHORITY_PERMISSIONS)
                .appendPath(permissionItemName)
                .appendPath(permissionItemId)
                .appendQueryParameter(QUERY_PERMISSIONS_MODE, permissionsModeName)
                .build(),
        ).build()

    fun locationDetailsDeepLinkRequest(
        locationDetailsItemName: String,
        locationDetailsItemId: String,
    ) = NavDeepLinkRequest.Builder
        .fromUri(
            Uri
                .Builder()
                .scheme(NAV_DEEP_LINK_SCHEME)
                .authority(AUTHORITY_LOCATION_DETAILS)
                .appendPath(locationDetailsItemName)
                .appendPath(locationDetailsItemId)
                .build(),
        ).build()

    private const val NAV_DEEP_LINK_SCHEME = "passbolt"

    private const val AUTHORITY_PERMISSIONS = "permissions"
    private const val AUTHORITY_LOCATION_DETAILS = "locationDetails"

    private const val QUERY_PERMISSIONS_MODE = "mode"
}
