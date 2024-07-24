package com.passbolt.mobile.android.scenarios.home.filters

import androidx.annotation.DrawableRes
import androidx.annotation.IdRes
import androidx.annotation.StringRes
import com.passbolt.mobile.android.core.localization.R as LocalizationR
import com.passbolt.mobile.android.core.ui.R as CoreUiR

/**
 * Passbolt - Open source password manager for teams
 * Copyright (c) 2021,2024 Passbolt SA
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
        filterId = com.passbolt.mobile.android.feature.home.R.id.allItems,
        filterIconId = CoreUiR.drawable.ic_list,
        filterNameId = LocalizationR.string.filters_menu_all_items
    ),
    FAVOURITES(
        filterId = com.passbolt.mobile.android.feature.home.R.id.favourites,
        filterIconId = CoreUiR.drawable.ic_star,
        filterNameId = LocalizationR.string.filters_menu_favourites
    ),
    RECENTLY_MODIFIED(
        filterId = com.passbolt.mobile.android.feature.home.R.id.recentlyModified,
        filterIconId = CoreUiR.drawable.ic_clock,
        filterNameId = LocalizationR.string.filters_menu_recently_modified
    ),
    SHARED_WITH_ME(
        filterId = com.passbolt.mobile.android.feature.home.R.id.sharedWithMe,
        filterIconId = CoreUiR.drawable.ic_share,
        filterNameId = LocalizationR.string.filters_menu_shared_with_me
    ),
    OWNED_BY_ME(
        filterId = com.passbolt.mobile.android.feature.home.R.id.ownedByMe,
        filterIconId = CoreUiR.drawable.ic_person,
        filterNameId = LocalizationR.string.filters_menu_owned_by_me
    ),
    EXPIRY(
        filterId = com.passbolt.mobile.android.feature.home.R.id.expiry,
        filterIconId = CoreUiR.drawable.ic_calendar_clock ,
        filterNameId = LocalizationR.string.filters_menu_expiry
    ),
    FOLDERS(
        filterId = com.passbolt.mobile.android.feature.home.R.id.folders,
        filterIconId = CoreUiR.drawable.ic_folder,
        filterNameId = LocalizationR.string.filters_menu_folders
    ),
    TAGS(
        filterId = com.passbolt.mobile.android.feature.home.R.id.tags,
        filterIconId = CoreUiR.drawable.ic_tag,
        filterNameId = LocalizationR.string.filters_menu_tags
    ),
    GROUPS(
        filterId = com.passbolt.mobile.android.feature.home.R.id.groups,
        filterIconId = CoreUiR.drawable.ic_group,
        filterNameId = LocalizationR.string.filters_menu_groups
    )
}
