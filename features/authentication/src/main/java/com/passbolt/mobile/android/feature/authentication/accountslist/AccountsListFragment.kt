package com.passbolt.mobile.android.feature.authentication.accountslist

import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.os.BundleCompat
import androidx.core.os.bundleOf
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.GenericItem
import com.mikepenz.fastadapter.adapters.ModelAdapter
import com.mikepenz.fastadapter.diff.FastAdapterDiffUtil
import com.passbolt.mobile.android.common.lifecycleawarelazy.lifecycleAwareLazy
import com.passbolt.mobile.android.core.extension.gone
import com.passbolt.mobile.android.core.extension.initDefaultToolbar
import com.passbolt.mobile.android.core.extension.setDebouncingOnClick
import com.passbolt.mobile.android.core.extension.visible
import com.passbolt.mobile.android.core.mvp.scoped.BindingScopedFragment
import com.passbolt.mobile.android.core.navigation.ActivityIntents
import com.passbolt.mobile.android.core.navigation.AppContext
import com.passbolt.mobile.android.core.ui.progressdialog.hideProgressDialog
import com.passbolt.mobile.android.core.ui.progressdialog.showProgressDialog
import com.passbolt.mobile.android.core.ui.recyclerview.DrawableListDivider
import com.passbolt.mobile.android.feature.authentication.R
import com.passbolt.mobile.android.feature.authentication.accountslist.item.AccountItemClick
import com.passbolt.mobile.android.feature.authentication.accountslist.item.AccountUiItemsMapper
import com.passbolt.mobile.android.feature.authentication.accountslist.item.AddNewAccountItem
import com.passbolt.mobile.android.feature.authentication.accountslist.uistrategy.AccountListStrategy
import com.passbolt.mobile.android.feature.authentication.auth.AuthFragment
import com.passbolt.mobile.android.feature.authentication.databinding.FragmentAccountsListBinding
import com.passbolt.mobile.android.ui.AccountModelUi
import org.koin.android.ext.android.get
import org.koin.android.ext.android.inject
import org.koin.core.parameter.parametersOf
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
class AccountsListFragment :
    BindingScopedFragment<FragmentAccountsListBinding>(FragmentAccountsListBinding::inflate),
    AccountsListContract.View {
    private val presenter: AccountsListContract.Presenter by inject()
    private val modelAdapter: ModelAdapter<AccountModelUi, GenericItem> by inject()
    private val fastAdapter: FastAdapter<GenericItem> by inject()
    private val accountsUiMapper: AccountUiItemsMapper by inject()
    private val listDivider: DrawableListDivider by inject()
    private val authConfig by lifecycleAwareLazy {
        requireNotNull(
            BundleCompat.getSerializable(requireArguments(), ARG_AUTH_CONFIG, ActivityIntents.AuthConfig::class.java),
        )
    }
    private val context by lifecycleAwareLazy {
        requireNotNull(
            BundleCompat.getSerializable(requireArguments(), ARG_CONTEXT, AppContext::class.java),
        )
    }
    private lateinit var uiStrategy: AccountListStrategy
    private val backPressedCallback =
        object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                presenter.backClick()
            }
        }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        uiStrategy = get { parametersOf(this, authConfig) }
        initToolbar()
        initHeader()
        initLogo()
        initAdapter()
        setListeners()
        presenter.attach(this)
    }

    override fun onDestroyView() {
        requiredBinding.recyclerView.adapter = null
        presenter.detach()
        super.onDestroyView()
    }

    private fun initLogo() {
        requiredBinding.icon.visibility = uiStrategy.logoVisibility()
    }

    private fun initHeader() {
        setOf(requiredBinding.header, requiredBinding.subtitle).forEach {
            it.visibility = uiStrategy.headerVisibility()
        }
    }

    private fun initToolbar() {
        with(requiredBinding.toolbar) {
            uiStrategy.getTitleRes()?.let { toolbarTitle = getString(it) }
            visibility = uiStrategy.toolbarVisibility()
            initDefaultToolbar(this)
            setNavigationOnClickListener { presenter.backClick() }
        }
    }

    private fun setListeners() {
        with(requiredBinding) {
            removeAccountLabel.setDebouncingOnClick { presenter.removeAnAccountClick() }
            doneRemovingAccountsButton.setDebouncingOnClick { presenter.doneRemovingAccountsClick() }
        }
        requireActivity().onBackPressedDispatcher.addCallback(backPressedCallback)
    }

    private fun initAdapter() {
        requiredBinding.recyclerView.apply {
            itemAnimator = null
            layoutManager = LinearLayoutManager(requireContext())
            adapter = fastAdapter
            addItemDecoration(listDivider)
        }

        fastAdapter.addEventHooks(
            listOf(
                AccountItemClick(
                    accountClickListener = { presenter.accountItemClick(it) },
                    removeAccountClickListener = { presenter.removeAccountClick(it) },
                ),
                AddNewAccountItem.AddAccountItemClick {
                    presenter.addAccountClick()
                },
            ),
        )
    }

    override fun navigateToSetup() {
        startActivity(ActivityIntents.setup(requireContext()))
    }

    override fun navigateToStartUp() {
        startActivity(ActivityIntents.start(requireContext()))
    }

    override fun navigateToSignIn(model: AccountModelUi.AccountModel) {
        findNavController().navigate(
            R.id.authFragment,
            AuthFragment.newBundle(authConfig, context, model.userId),
            NavOptions
                .Builder()
                .setEnterAnim(CoreUiR.anim.slide_in_right)
                .setExitAnim(CoreUiR.anim.slide_out_left)
                .setPopEnterAnim(CoreUiR.anim.slide_in_left)
                .setPopExitAnim(CoreUiR.anim.slide_out_right)
                .build(),
        )
    }

    override fun navigateToNewAccountSignIn(model: AccountModelUi.AccountModel) {
        findNavController().navigate(
            R.id.authFragment,
            AuthFragment.newBundle(ActivityIntents.AuthConfig.Startup, context, model.userId),
            NavOptions
                .Builder()
                .setEnterAnim(CoreUiR.anim.slide_in_right)
                .setExitAnim(CoreUiR.anim.slide_out_left)
                .setPopEnterAnim(CoreUiR.anim.slide_in_left)
                .setPopExitAnim(CoreUiR.anim.slide_out_right)
                .build(),
        )
    }

    override fun showAccounts(accounts: List<AccountModelUi>) {
        val uiItems = accounts.map { accountsUiMapper.mapModelToItem(it) }
        FastAdapterDiffUtil.calculateDiff(modelAdapter, uiItems)
        fastAdapter.notifyAdapterDataSetChanged()
    }

    override fun showDoneRemovingAccounts() {
        requiredBinding.doneRemovingAccountsButton.visible()
    }

    override fun hideDoneRemovingAccounts() {
        requiredBinding.doneRemovingAccountsButton.gone()
    }

    override fun showRemoveAccounts() {
        requiredBinding.removeAccountLabel.visible()
    }

    override fun hideRemoveAccounts() {
        requiredBinding.removeAccountLabel.gone()
    }

    override fun showRemoveAccountConfirmationDialog(model: AccountModelUi.AccountModel) {
        AlertDialog
            .Builder(requireContext())
            .setTitle(LocalizationR.string.are_you_sure)
            .setMessage(LocalizationR.string.accounts_list_remove_account_message)
            .setPositiveButton(LocalizationR.string.accounts_list_remove_account) { _, _ ->
                presenter.confirmRemoveAccountClick(model)
            }.setNegativeButton(LocalizationR.string.cancel) { _, _ -> }
            .show()
    }

    override fun finishAffinity() {
        requireActivity().finishAffinity()
    }

    override fun finish() {
        requireActivity().finish()
    }

    override fun showProgress() {
        showProgressDialog(childFragmentManager)
    }

    override fun hideProgress() {
        hideProgressDialog(childFragmentManager)
    }

    override fun showAccountRemovedSnackbar() {
        Snackbar
            .make(
                requiredBinding.root,
                LocalizationR.string.accounts_list_account_removed,
                Snackbar.LENGTH_SHORT,
            ).setAnchorView(requiredBinding.doneRemovingAccountsButton)
            .setBackgroundTint(
                ContextCompat.getColor(
                    requireContext(),
                    CoreUiR.color.background_gray_dark,
                ),
            ).show()
    }

    override fun notifySelectedAccountChanged() {
        uiStrategy.notifySelectedAccountChanged()
    }

    override fun navigateBack(isSelectedAccountAvailable: Boolean) {
        uiStrategy.navigateBack(isSelectedAccountAvailable)
    }

    companion object {
        const val ARG_AUTH_CONFIG = "AUTH_CONFIG"
        const val ARG_CONTEXT = "CONTEXT"

        fun newBundle(
            authConfig: ActivityIntents.AuthConfig,
            context: AppContext,
        ) = bundleOf(
            ARG_AUTH_CONFIG to authConfig,
            ARG_CONTEXT to context,
        )
    }
}
