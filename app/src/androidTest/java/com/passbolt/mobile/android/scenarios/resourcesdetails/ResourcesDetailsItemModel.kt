package com.passbolt.mobile.android.scenarios.resourcesdetails

import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.IdRes
import com.passbolt.mobile.android.feature.setup.R

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

enum class ResourcesDetailsItemModel(
    @IdRes val resourceId: Int,
    @DrawableRes val resourceIconId: Int,
    @ColorRes val resourceTintColorId: Int?
) {
    LAUNCH_WEBSITE(
        resourceId = R.id.launchWebsite,
        resourceIconId = R.drawable.ic_open_link,
        resourceTintColorId = R.color.icon_tint
    ),
    COPY_URL(
        resourceId = R.id.copyUrl,
        resourceIconId = R.drawable.ic_link,
        resourceTintColorId = R.color.icon_tint
    ),
    COPY_PASSWORD(
        resourceId = R.id.copyPassword,
        resourceIconId = R.drawable.ic_key,
        resourceTintColorId = R.color.icon_tint
    ),
    COPY_DESCRIPTION(
        resourceId = R.id.copyDescription,
        resourceIconId = R.drawable.ic_description,
        resourceTintColorId = R.color.icon_tint
    ),
    COPY_USERNAME(
        resourceId = R.id.copyUsername,
        resourceIconId = R.drawable.ic_user,
        resourceTintColorId = R.color.icon_tint
    ),
    ADD_TO_FAVOURITE(
        resourceId = R.id.favourite,
        resourceIconId = R.drawable.ic_add_to_favourite,
        resourceTintColorId = R.color.icon_tint
    ),
    SHARE_PASSWORD(
        resourceId = R.id.share,
        resourceIconId = R.drawable.ic_share,
        resourceTintColorId = R.color.icon_tint
    ),
    EDIT_PASSWORD(
        resourceId = R.id.edit,
        resourceIconId = R.drawable.ic_edit,
        resourceTintColorId = R.color.icon_tint
    ),
    DELETE_PASSWORD(
        resourceId = R.id.delete,
        resourceIconId = R.drawable.ic_trash,
        resourceTintColorId = null
    )
}
