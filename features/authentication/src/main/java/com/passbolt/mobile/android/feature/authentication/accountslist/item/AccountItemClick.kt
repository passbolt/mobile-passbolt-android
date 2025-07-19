package com.passbolt.mobile.android.feature.authentication.accountslist.item

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.listeners.ClickEventHook
import com.passbolt.mobile.android.core.extension.asBinding
import com.passbolt.mobile.android.feature.authentication.R
import com.passbolt.mobile.android.feature.authentication.databinding.ItemAccountBinding
import com.passbolt.mobile.android.ui.AccountModelUi

class AccountItemClick(
    private val accountClickListener: (AccountModelUi.AccountModel) -> Unit,
    private val removeAccountClickListener: (AccountModelUi.AccountModel) -> Unit,
) : ClickEventHook<AccountItem>() {
    override fun onBindMany(viewHolder: RecyclerView.ViewHolder): List<View> {
        viewHolder.asBinding<ItemAccountBinding> {
            return listOf(it.itemAccount, it.trashImage)
        }
        return emptyList()
    }

    override fun onClick(
        v: View,
        position: Int,
        fastAdapter: FastAdapter<AccountItem>,
        item: AccountItem,
    ) = when (v.id) {
        R.id.itemAccount -> accountClickListener.invoke(item.accountModel)
        R.id.trashImage -> removeAccountClickListener.invoke(item.accountModel)
        else -> { // ignore the rest of the views
        }
    }
}
