/**
 * Passbolt - Open source password manager for teams
 * Copyright (c) 2021,2025 Passbolt SA
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

package com.passbolt.mobile.android.scenarios.resource.details

import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.IdRes
import com.passbolt.mobile.android.feature.resourcemoremenu.R
import com.passbolt.mobile.android.core.ui.R as CoreUiR

enum class ResourcesDetailsItemModel(
    @IdRes val resourceId: Int,
    @DrawableRes val resourceIconId: Int,
    @ColorRes val resourceTintColorId: Int?,
) {
    LAUNCH_WEBSITE(
        resourceId = R.id.launchWebsite,
        resourceIconId = CoreUiR.drawable.ic_open_link,
        resourceTintColorId = CoreUiR.color.icon_tint,
    ),
    COPY_URI(
        resourceId = R.id.copyUrl,
        resourceIconId = CoreUiR.drawable.ic_link,
        resourceTintColorId = CoreUiR.color.icon_tint,
    ),
    COPY_PASSWORD(
        resourceId = R.id.copyPassword,
        resourceIconId = CoreUiR.drawable.ic_key,
        resourceTintColorId = CoreUiR.color.icon_tint,
    ),
    COPY_METADATA_DESCRIPTION(
        resourceId = R.id.copyMetadataDescription,
        resourceIconId = CoreUiR.drawable.ic_description,
        resourceTintColorId = CoreUiR.color.icon_tint,
    ),
    COPY_NOTE(
        resourceId = R.id.copy_note,
        resourceIconId = CoreUiR.drawable.ic_notes,
        resourceTintColorId = CoreUiR.color.icon_tint,
    ),
    COPY_USERNAME(
        resourceId = R.id.copyUsername,
        resourceIconId = CoreUiR.drawable.ic_user,
        resourceTintColorId = CoreUiR.color.icon_tint,
    ),
    ADD_TO_FAVOURITE(
        resourceId = R.id.favourite,
        resourceIconId = CoreUiR.drawable.ic_add_to_favourite,
        resourceTintColorId = CoreUiR.color.icon_tint,
    ),
    SHARE_PASSWORD(
        resourceId = R.id.share,
        resourceIconId = CoreUiR.drawable.ic_share,
        resourceTintColorId = CoreUiR.color.icon_tint,
    ),
    EDIT(
        resourceId = R.id.edit,
        resourceIconId = CoreUiR.drawable.ic_edit,
        resourceTintColorId = CoreUiR.color.icon_tint,
    ),
    DELETE_PASSWORD(
        resourceId = R.id.delete,
        resourceIconId = CoreUiR.drawable.ic_trash,
        resourceTintColorId = null,
    ),
}
