package com.passbolt.mobile.android.scenarios.filters

import androidx.annotation.DrawableRes
import androidx.annotation.IdRes
import androidx.annotation.StringRes
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

enum class ResourceFilterModel(
    @IdRes val filterId: Int,
    @DrawableRes val filterIconId: Int,
    @StringRes val filterNameId: Int
) {
    ALL_ITEMS(
        filterId = R.id.allItems,
        filterIconId = R.drawable.ic_list,
        filterNameId = R.string.filters_menu_all_items
    ),
    FAVOURITES(
        filterId = R.id.favourites,
        filterIconId = R.drawable.ic_star,
        filterNameId = R.string.filters_menu_favourites
    ),
    RECENTLY_MODIFIED(
        filterId = R.id.recentlyModified,
        filterIconId = R.drawable.ic_clock,
        filterNameId = R.string.filters_menu_recently_modified
    ),
    SHARED_WITH_ME(
        filterId = R.id.sharedWithMe,
        filterIconId = R.drawable.ic_share,
        filterNameId = R.string.filters_menu_shared_with_me
    ),
    OWNED_BY_ME(
        filterId = R.id.ownedByMe,
        filterIconId = R.drawable.ic_person,
        filterNameId = R.string.filters_menu_owned_by_me
    ),
    FOLDERS(
        filterId = R.id.folders,
        filterIconId = R.drawable.ic_folder,
        filterNameId = R.string.filters_menu_folders
    ),
    TAGS(
        filterId = R.id.tags,
        filterIconId = R.drawable.ic_tag,
        filterNameId = R.string.filters_menu_tags
    ),
    GROUPS(
        filterId = R.id.groups,
        filterIconId = R.drawable.ic_group,
        filterNameId = R.string.filters_menu_groups
    )
}
