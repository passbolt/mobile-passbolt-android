package com.passbolt.mobile.android.scenarios.home.filters

import androidx.annotation.StringRes
import com.passbolt.mobile.android.core.localization.R as LocalizationR

/**
 * Passbolt - Open source password manager for teams
 * Copyright (c) 2021,2024-2025 Passbolt SA
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
    @StringRes val filterNameId: Int,
) {
    ALL_ITEMS(
        filterNameId = LocalizationR.string.filters_menu_all_items,
    ),
    FAVOURITES(
        filterNameId = LocalizationR.string.filters_menu_favourites,
    ),
    RECENTLY_MODIFIED(
        filterNameId = LocalizationR.string.filters_menu_recently_modified,
    ),
    SHARED_WITH_ME(
        filterNameId = LocalizationR.string.filters_menu_shared_with_me,
    ),
    OWNED_BY_ME(
        filterNameId = LocalizationR.string.filters_menu_owned_by_me,
    ),
    EXPIRY(
        filterNameId = LocalizationR.string.filters_menu_expiry,
    ),
    FOLDERS(
        filterNameId = LocalizationR.string.filters_menu_folders,
    ),
    TAGS(
        filterNameId = LocalizationR.string.filters_menu_tags,
    ),
    GROUPS(
        filterNameId = LocalizationR.string.filters_menu_groups,
    ),
}
