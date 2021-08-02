package com.passbolt.mobile.android.feature.authentication.accountslist

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.core.os.bundleOf
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.GenericItem
import com.mikepenz.fastadapter.adapters.ModelAdapter
import com.mikepenz.fastadapter.diff.FastAdapterDiffUtil
import com.passbolt.mobile.android.common.extension.gone
import com.passbolt.mobile.android.common.extension.setDebouncingOnClick
import com.passbolt.mobile.android.common.extension.visible
import com.passbolt.mobile.android.common.lifecycleawarelazy.lifecycleAwareLazy
import com.passbolt.mobile.android.core.extension.initDefaultToolbar
import com.passbolt.mobile.android.core.mvp.scoped.BindingScopedFragment
import com.passbolt.mobile.android.core.navigation.ActivityIntents
import com.passbolt.mobile.android.core.navigation.AuthenticationTarget
import com.passbolt.mobile.android.core.navigation.AuthenticationType
import com.passbolt.mobile.android.core.ui.recyclerview.DrawableListDivider
import com.passbolt.mobile.android.feature.authentication.R
import com.passbolt.mobile.android.feature.authentication.accountslist.item.AccountItemClick
import com.passbolt.mobile.android.feature.authentication.accountslist.item.AccountUiItemsMapper
import com.passbolt.mobile.android.feature.authentication.accountslist.item.AddNewAccountItem
import com.passbolt.mobile.android.feature.authentication.accountslist.uistrategy.AccountListStrategy
import com.passbolt.mobile.android.feature.authentication.databinding.FragmentAccountsListBinding
import com.passbolt.mobile.android.ui.AccountModelUi
import org.koin.android.ext.android.get
import org.koin.android.ext.android.inject
import org.koin.core.parameter.parametersOf

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
    private val fastAdapter: FastAdapter<GenericItem> by inject()
    private val accountsUiMapper: AccountUiItemsMapper by inject()
    private val listDivider: DrawableListDivider by inject()
    private val authTarget by lifecycleAwareLazy {
        requireArguments().getSerializable(ARG_AUTH_TARGET) as AuthenticationTarget
    }
    private lateinit var uiStrategy: AccountListStrategy

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        uiStrategy = get { parametersOf(authTarget) }
        initToolbar()
        initHeader()
        initLogo()
        initAdapter()
        setListeners()
        presenter.attach(this)
    }

    override fun onDestroyView() {
        binding.recyclerView.adapter = null
        presenter.detach()
        super.onDestroyView()
    }

    private fun initLogo() {
        binding.icon.visibility = uiStrategy.logoVisibility()
    }

    private fun initHeader() {
        setOf(binding.header, binding.subtitle).forEach {
            it.visibility = uiStrategy.headerVisibility()
        }
    }

    private fun initToolbar() {
        with(binding.toolbar) {
            uiStrategy.getTitleRes()?.let { toolbarTitle = getString(it) }
            visibility = uiStrategy.toolbarVisibility()
            initDefaultToolbar(this)
            setNavigationOnClickListener { requireActivity().finish() }
        }
    }

    private fun setListeners() {
        with(binding) {
            removeAccountLabel.setDebouncingOnClick { presenter.removeAnAccountClick() }
            doneRemovingAccountsButton.setDebouncingOnClick { presenter.doneRemovingAccountsClick() }
        }
    }

    private fun initAdapter() {
        binding.recyclerView.apply {
            itemAnimator = null
            layoutManager = LinearLayoutManager(requireContext())
            adapter = fastAdapter
            addItemDecoration(listDivider)
        }

        fastAdapter.addEventHooks(
            listOf(
                AccountItemClick(
                    accountClickListener = { presenter.accountItemClick(it) },
                    removeAccountClickListener = { presenter.removeAccountClick(it) }),
                AddNewAccountItem.AddAccountItemClick {
                    presenter.addAccountClick()
                }
            )
        )
    }

    override fun navigateToSetup() {
        startActivity(ActivityIntents.setup(requireContext()))
    }

    override fun navigateToSignIn(model: AccountModelUi.AccountModel) {
        findNavController().navigate(
            AccountsListFragmentDirections.actionAccountsListFragmentToAuthFragment(
                model.userId, AuthenticationType.SignIn
            )
        )
    }

    override fun showAccounts(accounts: List<AccountModelUi>) {
        val uiItems = accounts.map { accountsUiMapper.mapModelToItem(it) }
        FastAdapterDiffUtil.calculateDiff(modelAdapter, uiItems)
        fastAdapter.notifyAdapterDataSetChanged()
    }

    override fun showDoneRemovingAccounts() {
        binding.doneRemovingAccountsButton.visible()
    }

    override fun hideDoneRemovingAccounts() {
        binding.doneRemovingAccountsButton.gone()
    }

    override fun showRemoveAccounts() {
        binding.removeAccountLabel.visible()
    }

    override fun hideRemoveAccounts() {
        binding.removeAccountLabel.gone()
    }

    override fun showRemoveAccountConfirmationDialog(model: AccountModelUi.AccountModel) {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.accounts_list_remove_account_title)
            .setMessage(R.string.accounts_list_remove_account_message)
            .setPositiveButton(R.string.accounts_list_remove_account) { _, _ ->
                presenter.confirmRemoveAccountClick(model)
            }
            .setNegativeButton(R.string.cancel) { _, _ -> }
            .show()
    }

    companion object {
        private const val ARG_AUTH_TARGET = "AUTH_TARGET"

        fun newBundle(authTarget: AuthenticationTarget) = bundleOf(
            ARG_AUTH_TARGET to authTarget
        )
    }
}
