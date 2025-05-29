package com.passbolt.mobile.android.permissions.permissionrecipients.recipientsrecycler

import android.view.LayoutInflater
import android.view.ViewGroup
import coil.load
import com.mikepenz.fastadapter.binding.AbstractBindingItem
import com.passbolt.mobile.android.feature.permissions.R
import com.passbolt.mobile.android.feature.permissions.databinding.ItemPermissionRecipientGroupBinding
import com.passbolt.mobile.android.ui.GroupModel
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
class GroupRecipientItem(
    val model: GroupModel,
) : AbstractBindingItem<ItemPermissionRecipientGroupBinding>() {
    override val type: Int
        get() = R.id.groupRecipientItem

    override fun bindView(
        binding: ItemPermissionRecipientGroupBinding,
        payloads: List<Any>,
    ) {
        with(binding) {
            icon.load(CoreUiR.drawable.ic_filled_group_with_bg)
            name.text = model.groupName
            radio.isChecked = root.isSelected
        }
    }

    override fun createBinding(
        inflater: LayoutInflater,
        parent: ViewGroup?,
    ): ItemPermissionRecipientGroupBinding = ItemPermissionRecipientGroupBinding.inflate(inflater, parent, false)
}
