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

package com.passbolt.mobile.android.feature.otp.screen

import android.app.Activity
import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.os.Bundle
import android.os.PersistableBundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
import androidx.recyclerview.widget.LinearLayoutManager
import coil.ImageLoader
import coil.request.ImageRequest
import coil.transform.CircleCropTransformation
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.GenericItem
import com.mikepenz.fastadapter.adapters.ItemAdapter
import com.mikepenz.fastadapter.diff.FastAdapterDiffUtil
import com.passbolt.mobile.android.common.extension.gone
import com.passbolt.mobile.android.common.extension.setDebouncingOnClick
import com.passbolt.mobile.android.common.extension.visible
import com.passbolt.mobile.android.common.px
import com.passbolt.mobile.android.core.extension.setSearchEndIconWithListener
import com.passbolt.mobile.android.core.navigation.ActivityIntents
import com.passbolt.mobile.android.core.navigation.AppContext
import com.passbolt.mobile.android.core.ui.initialsicon.InitialsIconGenerator
import com.passbolt.mobile.android.feature.authentication.BindingScopedAuthenticatedFragment
import com.passbolt.mobile.android.feature.home.R
import com.passbolt.mobile.android.feature.home.screen.HomeDataRefreshExecutor
import com.passbolt.mobile.android.feature.home.switchaccount.SwitchAccountBottomSheetFragment
import com.passbolt.mobile.android.feature.otp.databinding.FragmentOtpBinding
import com.passbolt.mobile.android.feature.otp.otpmoremenu.OtpMoreMenuFragment
import com.passbolt.mobile.android.feature.otp.screen.recycler.OtpItem
import com.passbolt.mobile.android.ui.OtpListItemWrapper
import com.passbolt.mobile.android.ui.OtpMoreMenuModel
import org.koin.android.ext.android.inject

@Suppress("TooManyFunctions")
class OtpFragment :
    BindingScopedAuthenticatedFragment<FragmentOtpBinding, OtpContract.View>(FragmentOtpBinding::inflate),
    OtpContract.View, SwitchAccountBottomSheetFragment.Listener, OtpMoreMenuFragment.Listener {

    override val presenter: OtpContract.Presenter by inject()
    private val otpAdapter: ItemAdapter<OtpItem> by inject()
    private val fastAdapter: FastAdapter<GenericItem> by inject()
    private val initialsIconGenerator: InitialsIconGenerator by inject()
    private val imageLoader: ImageLoader by inject()
    private val clipboardManager: ClipboardManager? by inject()
    private val speedDialFabFactory: OtpSpeedDialFabFactory by inject()

    private val authenticationResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == Activity.RESULT_OK) {
                // reinitialize for the switched account
                presenter.detach()
                presenter.attach(this)
            }
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setUpRecycler()
        setupListeners()
        initSpeedDialFab()
        presenter.attach(this)
    }

    override fun onResume() {
        super.onResume()
        presenter.resume(this)
    }

    override fun onPause() {
        presenter.pause()
        super.onPause()
    }

    override fun onDestroyView() {
        presenter.detach()
        super.onDestroyView()
    }

    private fun initSpeedDialFab() {
        with(speedDialFabFactory) {
            scanQrCodeClick = { presenter.scanOtpQrCodeClick() }
            createManuallyClick = { presenter.createOtpManuallyClick() }

            binding.otpRootLayout.addView(
                getSpeedDialFab(requireContext(), binding.overlay)
            )
        }
    }

    private fun setUpRecycler() {
        fastAdapter.addEventHooks(
            listOf(
                OtpItem.ItemClick { presenter.otpItemClick(it) },
                OtpItem.ItemMoreClick { presenter.otpItemMoreClick(it) }
            )
        )
        with(binding.recyclerView) {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = fastAdapter
            itemAnimator = null
        }
    }

    private fun setupListeners() {
        with(binding) {
            refreshButton.setDebouncingOnClick {
                presenter.refreshClick()
            }
            swipeRefresh.setOnRefreshListener {
                presenter.refreshClick()
            }
            searchEditText.doAfterTextChanged {
                presenter.searchTextChanged(it.toString())
            }
        }
    }

    override fun hideRefreshProgress() {
        binding.swipeRefresh.isRefreshing = false
    }

    override fun showRefreshProgress() {
        binding.swipeRefresh.isRefreshing = true
    }

    override fun showOtpList(otpList: List<OtpListItemWrapper>) {
        val result = FastAdapterDiffUtil.calculateDiff(
            otpAdapter,
            otpList.map { OtpItem(it, initialsIconGenerator) }
        )
        FastAdapterDiffUtil[otpAdapter] = result
    }

    override fun showEmptyView() {
        binding.emptyListContainer.visible()
    }

    override fun hideEmptyView() {
        binding.emptyListContainer.gone()
    }

    override fun performRefreshUsingRefreshExecutor() {
        (activity as HomeDataRefreshExecutor).performFullDataRefresh()
    }

    override fun displaySearchAvatar(avatarUrl: String?) {
        val request = ImageRequest.Builder(requireContext())
            .data(avatarUrl)
            .transformations(CircleCropTransformation())
            .size(AVATAR_SIZE, AVATAR_SIZE)
            .placeholder(R.drawable.ic_avatar_placeholder)
            .target(
                onError = {
                    binding.searchTextInput.setSearchEndIconWithListener(
                        ContextCompat.getDrawable(requireContext(), R.drawable.ic_avatar_placeholder)!!,
                        presenter::searchAvatarClick
                    )
                },
                onSuccess = {
                    binding.searchTextInput.setSearchEndIconWithListener(it, presenter::searchAvatarClick)
                }
            )
            .build()
        imageLoader.enqueue(request)
    }

    override fun showFullscreenError() {
        binding.errorContainer.visible()
    }

    override fun hideFullScreenError() {
        binding.errorContainer.gone()
    }

    override fun navigateToSwitchAccount(appContext: AppContext) {
        SwitchAccountBottomSheetFragment.newInstance(appContext)
            .show(childFragmentManager, SwitchAccountBottomSheetFragment::class.java.name)
    }

    override fun showOtmMoreMenu(otpMoreMenuModel: OtpMoreMenuModel) {
        OtpMoreMenuFragment.newInstance(otpMoreMenuModel)
            .show(childFragmentManager, OtpMoreMenuFragment::class.java.name)
    }

    override fun switchAccountManageAccountClick() {
        presenter.switchAccountManageAccountClick()
    }

    override fun switchAccountClick() {
        presenter.switchAccountClick()
    }

    override fun navigateToManageAccounts() {
        authenticationResult.launch(
            ActivityIntents.authentication(
                requireContext(),
                ActivityIntents.AuthConfig.ManageAccount
            )
        )
    }

    override fun navigateToSwitchedAccountAuth(appContext: AppContext) {
        if (appContext == AppContext.APP) {
            requireActivity().finishAffinity()
        }
        // TODO handle autofill
        authenticationResult.launch(
            ActivityIntents.authentication(
                requireContext(),
                ActivityIntents.AuthConfig.Startup,
                appContext
            )
        )
    }

    override fun showPleaseWaitForDataRefresh() {
        Toast.makeText(requireContext(), R.string.home_please_wait_for_refresh, Toast.LENGTH_SHORT).show()
    }

    override fun displaySearchClearIcon() {
        binding.searchTextInput.setSearchEndIconWithListener(
            ContextCompat.getDrawable(requireContext(), R.drawable.ic_close)!!,
            presenter::searchClearClick
        )
    }

    override fun clearSearchInput() {
        binding.searchEditText.setText("")
    }

    override fun menuCopyOtpClick() {
        presenter.menuCopyOtpClick()
    }

    override fun menuShowOtpClick() {
        presenter.menuShowOtpClick()
    }

    override fun menuEditOtpClick() {
        presenter.menuEditOtpClick()
    }

    override fun menuDeleteOtpClick() {
        presenter.menuDeleteOtpClick()
    }

    override fun copySecretToClipBoard(label: String, value: String) {
        clipboardManager?.setPrimaryClip(
            ClipData.newPlainText(label, value).apply {
                description.extras = PersistableBundle().apply {
                    putBoolean(ClipDescription.EXTRA_IS_SENSITIVE, true)
                }
            }
        )
        Toast.makeText(requireContext(), getString(R.string.copied_info, label), Toast.LENGTH_SHORT).show()
    }

    private companion object {
        private val AVATAR_SIZE = 30.px
    }
}
