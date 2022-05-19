package com.passbolt.mobile.android.feature.resources.permissionrecipients.recipientsrecycler

import android.view.LayoutInflater
import android.view.ViewGroup
import coil.load
import coil.transform.CircleCropTransformation
import com.mikepenz.fastadapter.binding.AbstractBindingItem
import com.passbolt.mobile.android.common.extension.visible
import com.passbolt.mobile.android.feature.resources.R
import com.passbolt.mobile.android.feature.resources.databinding.ItemPermissionRecipientUserBinding
import com.passbolt.mobile.android.ui.UserModel

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
class UserRecipientItem(
    val model: UserModel
) : AbstractBindingItem<ItemPermissionRecipientUserBinding>() {

    override val type: Int
        get() = R.id.userRecipientItem

    override fun bindView(binding: ItemPermissionRecipientUserBinding, payloads: List<Any>) {
        with(binding) {
            icon.load(model.profile.avatarUrl) {
                error(R.drawable.ic_user_avatar)
                transformations(CircleCropTransformation())
                placeholder(R.drawable.ic_user_avatar)
            }
            name.text = String.format("%s %s", model.profile.firstName, model.profile.lastName)
            userName.apply {
                text = model.userName
                visible()
            }
            radio.isChecked = root.isSelected
        }
    }

    override fun createBinding(inflater: LayoutInflater, parent: ViewGroup?): ItemPermissionRecipientUserBinding {
        return ItemPermissionRecipientUserBinding.inflate(inflater, parent, false)
    }
}
