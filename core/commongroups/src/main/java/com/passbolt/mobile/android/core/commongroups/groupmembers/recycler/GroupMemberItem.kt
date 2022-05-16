package com.passbolt.mobile.android.core.commongroups.groupmembers.recycler

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.transform.CircleCropTransformation
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.binding.AbstractBindingItem
import com.mikepenz.fastadapter.listeners.ClickEventHook
import com.passbolt.mobile.android.common.extension.asBinding
import com.passbolt.mobile.android.commongroups.R
import com.passbolt.mobile.android.commongroups.databinding.ItemGroupMemberBinding
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
class GroupMemberItem(
    val model: UserModel
) : AbstractBindingItem<ItemGroupMemberBinding>() {

    override val type: Int
        get() = R.id.groupMemberItem

    override fun bindView(binding: ItemGroupMemberBinding, payloads: List<Any>) {
        with(binding) {
            icon.load(model.profile.avatarUrl) {
                error(R.drawable.ic_user_avatar)
                transformations(CircleCropTransformation())
                placeholder(R.drawable.ic_user_avatar)
            }
            name.text = String.format("%s %s", model.profile.firstName, model.profile.lastName)
            userName.apply {
                text = model.userName
            }
        }
    }

    override fun createBinding(inflater: LayoutInflater, parent: ViewGroup?): ItemGroupMemberBinding {
        return ItemGroupMemberBinding.inflate(inflater, parent, false)
    }

    class ItemClick(
        private val clickListener: (UserModel) -> Unit
    ) : ClickEventHook<GroupMemberItem>() {

        override fun onBind(viewHolder: RecyclerView.ViewHolder): View? {
            return viewHolder.asBinding<ItemGroupMemberBinding> {
                it.groupMemberItem
            }
        }

        override fun onClick(
            v: View,
            position: Int,
            fastAdapter: FastAdapter<GroupMemberItem>,
            item: GroupMemberItem
        ) {
            clickListener.invoke(item.model)
        }
    }
}
