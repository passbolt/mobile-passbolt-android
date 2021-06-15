package com.passbolt.mobile.android.feature.login.accountslist.item

import android.graphics.drawable.Drawable
import android.view.View
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.items.AbstractItem
import com.mikepenz.fastadapter.listeners.ClickEventHook
import com.passbolt.mobile.android.common.extension.asBinding
import com.passbolt.mobile.android.feature.login.R
import com.passbolt.mobile.android.feature.login.databinding.ItemAccountBinding
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
    private val accountModel: AccountModelUi.AccountModel
) : AbstractItem<AccountItem.ViewHolder>() {

    override val type: Int
        get() = R.id.itemAccount

    override val layoutRes: Int
        get() = R.layout.item_account

    override fun bindView(holder: ViewHolder, payloads: List<Any>) {
        super.bindView(holder, payloads)
        holder.title.text = accountModel.title
        holder.email.text = accountModel.email
        if (accountModel.isFirstItem) {
            holder.setRippleBackgroundTop()
        } else {
            holder.setRippleBackground()
        }
        // holder.icon.load(accountModel.avatar) // TODO
    }

    override fun getViewHolder(v: View): ViewHolder {
        return ViewHolder(v)
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {

        var title: TextView = view.findViewById(R.id.title)
        var email: TextView = view.findViewById(R.id.email)
        var itemAccountContainer: View = view.findViewById(R.id.itemAccount)
        private var rippleBackground: Drawable =
            ContextCompat.getDrawable(view.context, R.drawable.background_ripple)!!
        private var rippleBackgroundTop: Drawable =
            ContextCompat.getDrawable(view.context, R.drawable.background_ripple_top_radius)!!

        fun setRippleBackgroundTop() {
            itemAccountContainer.background = rippleBackgroundTop
        }

        fun setRippleBackground() {
            itemAccountContainer.background = rippleBackground
        }
    }

    class AccountItemClick(
        private val clickListener: (AccountModelUi.AccountModel) -> Unit
    ) : ClickEventHook<AccountItem>() {

        override fun onBind(viewHolder: RecyclerView.ViewHolder): View? {
            return viewHolder.asBinding<ItemAccountBinding> {
                it.itemAccount
            }
        }

        override fun onClick(
            v: View,
            position: Int,
            fastAdapter: FastAdapter<AccountItem>,
            item: AccountItem
        ) {
            clickListener.invoke(item.accountModel)
        }
    }
}
