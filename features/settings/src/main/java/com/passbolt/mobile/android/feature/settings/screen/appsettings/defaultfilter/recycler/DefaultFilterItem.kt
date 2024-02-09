package com.passbolt.mobile.android.feature.settings.screen.appsettings.defaultfilter.recycler

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import com.mikepenz.fastadapter.binding.AbstractBindingItem
import com.passbolt.mobile.android.feature.settings.R
import com.passbolt.mobile.android.feature.settings.databinding.ItemDefaultFilterBinding
import com.passbolt.mobile.android.ui.DefaultFilterModel
import com.passbolt.mobile.android.core.localization.R as LocalizationR
import com.passbolt.mobile.android.core.ui.R as CoreUiR

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
class DefaultFilterItem(
    val filterModel: DefaultFilterModel
) : AbstractBindingItem<ItemDefaultFilterBinding>() {

    override val type: Int
        get() = R.id.itemDefaultFilter

    override fun createBinding(inflater: LayoutInflater, parent: ViewGroup?): ItemDefaultFilterBinding {
        return ItemDefaultFilterBinding.inflate(inflater, parent, false)
    }

    override fun bindView(binding: ItemDefaultFilterBinding, payloads: List<Any>) {
        super.bindView(binding, payloads)
        with(binding) {
            val attrs = requireNotNull(defaultFilterAttributes[filterModel])
            root.name = root.context.getString(attrs.first)
            root.icon = ContextCompat.getDrawable(root.context, attrs.second)
        }
    }

    private companion object {
        private val defaultFilterAttributes = mutableMapOf<
                DefaultFilterModel,
                Pair<@StringRes Int, @DrawableRes Int>>()
            .apply {
                DefaultFilterModel.values().forEach {
                    put(
                        it,
                        when (it) {
                            DefaultFilterModel.LAST_USED ->
                                LocalizationR.string.filters_menu_last_used to CoreUiR.drawable.ic_filter
                            DefaultFilterModel.ALL_ITEMS ->
                                LocalizationR.string.filters_menu_all_items to CoreUiR.drawable.ic_list
                            DefaultFilterModel.FAVOURITES ->
                                LocalizationR.string.filters_menu_favourites to CoreUiR.drawable.ic_star
                            DefaultFilterModel.RECENTLY_MODIFIED ->
                                LocalizationR.string.filters_menu_recently_modified to CoreUiR.drawable.ic_clock
                            DefaultFilterModel.SHARED_WITH_ME ->
                                LocalizationR.string.filters_menu_shared_with_me to CoreUiR.drawable.ic_share
                            DefaultFilterModel.OWNED_BY_ME ->
                                LocalizationR.string.filters_menu_owned_by_me to CoreUiR.drawable.ic_person
                            DefaultFilterModel.FOLDERS ->
                                LocalizationR.string.filters_menu_folders to CoreUiR.drawable.ic_folder
                            DefaultFilterModel.TAGS ->
                                LocalizationR.string.filters_menu_tags to CoreUiR.drawable.ic_tag
                            DefaultFilterModel.GROUPS ->
                                LocalizationR.string.filters_menu_groups to CoreUiR.drawable.ic_group
                            DefaultFilterModel.EXPIRY ->
                                LocalizationR.string.filters_menu_expiry to CoreUiR.drawable.ic_calendar_clock
                        }
                    )
                }
            }
    }
}
