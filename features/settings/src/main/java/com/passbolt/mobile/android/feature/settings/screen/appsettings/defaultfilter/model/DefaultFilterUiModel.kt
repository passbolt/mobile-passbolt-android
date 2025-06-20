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

package com.passbolt.mobile.android.feature.settings.screen.appsettings.defaultfilter.model

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.passbolt.mobile.android.ui.DefaultFilterModel
import com.passbolt.mobile.android.core.localization.R as LocalizationR
import com.passbolt.mobile.android.core.ui.R as CoreUiR

internal data class DefaultFilterUiModel(
    val filter: DefaultFilterModel,
    @StringRes
    val nameRes: Int,
    @DrawableRes
    val iconRes: Int,
)

@Suppress("LongMethod")
internal fun DefaultFilterModel.toUiModel(): DefaultFilterUiModel =
    when (this) {
        DefaultFilterModel.LAST_USED ->
            DefaultFilterUiModel(
                filter = this,
                nameRes = LocalizationR.string.filters_menu_last_used,
                iconRes = CoreUiR.drawable.ic_filter,
            )
        DefaultFilterModel.ALL_ITEMS ->
            DefaultFilterUiModel(
                filter = this,
                nameRes = LocalizationR.string.filters_menu_all_items,
                iconRes = CoreUiR.drawable.ic_list,
            )
        DefaultFilterModel.FAVOURITES ->
            DefaultFilterUiModel(
                filter = this,
                nameRes = LocalizationR.string.filters_menu_favourites,
                iconRes = CoreUiR.drawable.ic_star,
            )
        DefaultFilterModel.RECENTLY_MODIFIED ->
            DefaultFilterUiModel(
                filter = this,
                nameRes = LocalizationR.string.filters_menu_recently_modified,
                iconRes = CoreUiR.drawable.ic_clock,
            )
        DefaultFilterModel.SHARED_WITH_ME ->
            DefaultFilterUiModel(
                filter = this,
                nameRes = LocalizationR.string.filters_menu_shared_with_me,
                iconRes = CoreUiR.drawable.ic_share,
            )
        DefaultFilterModel.OWNED_BY_ME ->
            DefaultFilterUiModel(
                filter = this,
                nameRes = LocalizationR.string.filters_menu_owned_by_me,
                iconRes = CoreUiR.drawable.ic_person,
            )
        DefaultFilterModel.EXPIRY ->
            DefaultFilterUiModel(
                filter = this,
                nameRes = LocalizationR.string.filters_menu_expiry,
                iconRes = CoreUiR.drawable.ic_calendar_clock,
            )
        DefaultFilterModel.FOLDERS ->
            DefaultFilterUiModel(
                filter = this,
                nameRes = LocalizationR.string.filters_menu_folders,
                iconRes = CoreUiR.drawable.ic_folder,
            )
        DefaultFilterModel.TAGS ->
            DefaultFilterUiModel(
                filter = this,
                nameRes = LocalizationR.string.filters_menu_tags,
                iconRes = CoreUiR.drawable.ic_tag,
            )
        DefaultFilterModel.GROUPS ->
            DefaultFilterUiModel(
                filter = this,
                nameRes = LocalizationR.string.filters_menu_groups,
                iconRes = CoreUiR.drawable.ic_group,
            )
    }
