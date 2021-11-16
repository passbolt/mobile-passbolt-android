package com.passbolt.mobile.android.feature.authentication.accountslist.item

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import coil.load
import coil.transform.CircleCropTransformation
import com.mikepenz.fastadapter.binding.AbstractBindingItem
import com.passbolt.mobile.android.feature.authentication.R
import com.passbolt.mobile.android.feature.authentication.databinding.ItemAccountBinding
import com.passbolt.mobile.android.ui.AccountModelUi

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
class AccountItem(
    val accountModel: AccountModelUi.AccountModel
) : AbstractBindingItem<ItemAccountBinding>() {

    override val type: Int
        get() = R.id.itemAccount

    override fun createBinding(inflater: LayoutInflater, parent: ViewGroup?): ItemAccountBinding {
        return ItemAccountBinding.inflate(inflater, parent, false)
    }

    override fun bindView(binding: ItemAccountBinding, payloads: List<Any>) {
        super.bindView(binding, payloads)
        with(binding) {
            title.text = accountModel.title
            email.text = accountModel.email
            trashImage.visibility = if (accountModel.isTrashIconVisible) View.VISIBLE else View.GONE
            root.setBackgroundResource(
                if (accountModel.isFirstItem) {
                    R.drawable.background_ripple_top_radius
                } else {
                    R.drawable.background_ripple
                }
            )
            icon.load(accountModel.avatar) {
                error(R.drawable.ic_avatar_placeholder)
                transformations(CircleCropTransformation())
                placeholder(R.drawable.ic_avatar_placeholder)
            }
            currentAccountIcon.isVisible = accountModel.isCurrentUser
        }
    }
}
