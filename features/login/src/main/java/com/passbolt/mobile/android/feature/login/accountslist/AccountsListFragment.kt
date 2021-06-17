package com.passbolt.mobile.android.feature.login.accountslist

import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.GenericItem
import com.mikepenz.fastadapter.adapters.ModelAdapter
import com.passbolt.mobile.android.core.mvp.scoped.BindingScopedFragment
import com.passbolt.mobile.android.core.ui.recyclerview.DrawableListDivider
import com.passbolt.mobile.android.feature.login.R
import com.passbolt.mobile.android.feature.login.accountslist.item.AccountItem
import com.passbolt.mobile.android.feature.login.accountslist.item.AddNewAccountItem
import com.passbolt.mobile.android.feature.login.databinding.FragmentAccountsListBinding
import com.passbolt.mobile.android.ui.AccountModelUi
import org.koin.android.ext.android.inject

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
class AccountsListFragment : BindingScopedFragment<FragmentAccountsListBinding>(FragmentAccountsListBinding::inflate),
    AccountsListContract.View {

    private val presenter: AccountsListContract.Presenter by inject()
    private val modelAdapter: ModelAdapter<AccountModelUi, GenericItem> by inject()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initAdapter()
        presenter.attach(this)
    }

    private fun initAdapter() {
        val fastAdapter = FastAdapter.with(modelAdapter)

        binding.recyclerView.apply {
            itemAnimator = null
            layoutManager = LinearLayoutManager(requireContext())
            adapter = fastAdapter
            val divider = DrawableListDivider(ContextCompat.getDrawable(requireContext(), R.drawable.grey_divider))
            addItemDecoration(divider)
        }

        fastAdapter.addEventHooks(
            listOf(
                AccountItem.AccountItemClick {
                    presenter.accountItemClick(it)
                },
                AddNewAccountItem.AddAccountItemClick {
                    presenter.addAccountClick()
                }
            )
        )
    }

    override fun showAccounts(accounts: List<AccountModelUi>) {
        modelAdapter.add(accounts)
    }
}
