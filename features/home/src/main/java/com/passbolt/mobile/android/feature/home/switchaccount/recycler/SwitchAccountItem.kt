package com.passbolt.mobile.android.feature.home.switchaccount.recycler

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.transform.CircleCropTransformation
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.binding.AbstractBindingItem
import com.mikepenz.fastadapter.listeners.ClickEventHook
import com.passbolt.mobile.android.core.extension.asBinding
import com.passbolt.mobile.android.feature.home.R
import com.passbolt.mobile.android.feature.home.databinding.ItemSwitchAccountItemBinding
import com.passbolt.mobile.android.ui.SwitchAccountUiModel
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
class SwitchAccountItem(
    val model: SwitchAccountUiModel.AccountItem
) : AbstractBindingItem<ItemSwitchAccountItemBinding>() {

    override val type: Int
        get() = R.id.switchAccountItem

    override fun bindView(binding: ItemSwitchAccountItemBinding, payloads: List<Any>) {
        with(binding) {
            title.text = model.label
            email.text = model.email
            icon.load(model.avatarUrl) {
                error(CoreUiR.drawable.ic_avatar_placeholder)
                transformations(CircleCropTransformation())
                placeholder(CoreUiR.drawable.ic_avatar_placeholder)
            }
        }
    }

    override fun createBinding(inflater: LayoutInflater, parent: ViewGroup?): ItemSwitchAccountItemBinding {
        return ItemSwitchAccountItemBinding.inflate(inflater, parent, false)
    }
}

class SwitchAccountClick(
    private val clickAction: (SwitchAccountUiModel.AccountItem) -> Unit
) : ClickEventHook<SwitchAccountItem>() {

    override fun onBind(viewHolder: RecyclerView.ViewHolder): View? {
        return viewHolder.asBinding<ItemSwitchAccountItemBinding> {
            it.root
        }
    }

    override fun onClick(
        v: View,
        position: Int,
        fastAdapter: FastAdapter<SwitchAccountItem>,
        item: SwitchAccountItem
    ) {
        clickAction(item.model)
    }
}
