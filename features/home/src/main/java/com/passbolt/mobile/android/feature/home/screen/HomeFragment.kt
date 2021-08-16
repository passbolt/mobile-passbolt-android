package com.passbolt.mobile.android.feature.home.screen

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.recyclerview.widget.LinearLayoutManager
import coil.ImageLoader
import coil.request.ImageRequest
import coil.transform.CircleCropTransformation
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputLayout
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.adapters.ItemAdapter
import com.mikepenz.fastadapter.diff.FastAdapterDiffUtil
import com.passbolt.mobile.android.common.WebsiteOpener
import com.passbolt.mobile.android.common.extension.gone
import com.passbolt.mobile.android.common.extension.setDebouncingOnClick
import com.passbolt.mobile.android.common.px
import com.passbolt.mobile.android.core.mvp.authentication.BindingScopedAuthenticatedFragment
import com.passbolt.mobile.android.feature.home.R
import com.passbolt.mobile.android.feature.home.databinding.FragmentHomeBinding
import com.passbolt.mobile.android.core.commonresource.PasswordItem
import com.passbolt.mobile.android.feature.home.screen.more.PasswordMenuFragment
import com.passbolt.mobile.android.feature.home.screen.more.PasswordMoreModel
import com.passbolt.mobile.android.feature.resources.ResourcesActivity
import com.passbolt.mobile.android.ui.ResourceModel
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
class HomeFragment :
    BindingScopedAuthenticatedFragment<FragmentHomeBinding, HomeContract.View>(FragmentHomeBinding::inflate),
    HomeContract.View, PasswordMenuFragment.Listener {

    override val presenter: HomeContract.Presenter by inject()
    private val itemAdapter: ItemAdapter<PasswordItem> by inject()
    private val fastAdapter: FastAdapter<PasswordItem> by inject()
    private val imageLoader: ImageLoader by inject()
    private val clipboardManager: ClipboardManager? by inject()
    private val websiteOpener: WebsiteOpener by inject()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initAdapter()
        setListeners()
        presenter.attach(this)
    }

    override fun displayAvatar(url: String) {
        val request = ImageRequest.Builder(requireContext())
            .data(url)
            .transformations(CircleCropTransformation())
            .size(AVATAR_SIZE, AVATAR_SIZE)
            .placeholder(R.drawable.ic_avatar_placeholder)
            .target { drawable ->
                binding.searchTextInput.endIconMode = TextInputLayout.END_ICON_CUSTOM
                binding.searchTextInput.endIconDrawable = drawable
            }
            .error(R.drawable.ic_avatar_placeholder)
            .build()
        imageLoader.enqueue(request)
    }

    override fun onDestroyView() {
        binding.recyclerView.adapter = null
        presenter.detach()
        super.onDestroyView()
    }

    private fun initAdapter() {
        binding.recyclerView.apply {
            itemAnimator = null
            layoutManager = LinearLayoutManager(requireContext())
            adapter = fastAdapter
        }
        fastAdapter.addEventHooks(listOf(
            PasswordItem.ItemClick {
                presenter.itemClick(it)
            },
            PasswordItem.MoreClick {
                presenter.moreClick(it)
            }
        ))
    }

    private fun setState(state: State) {
        with(binding) {
            recyclerView.isVisible = state.listVisible
            searchTextInput.isEnabled = state.searchEnabled
            emptyListContainer.isVisible = state.emptyVisible
            errorContainer.isVisible = state.errorVisible
            progress.isVisible = state.progressVisible
        }
    }

    override fun showEmptyList() {
        setState(State.EMPTY)
    }

    override fun showSearchEmptyList() {
        setState(State.SEARCH_EMPTY)
    }

    private fun setListeners() {
        with(binding) {
            refreshButton.setDebouncingOnClick {
                presenter.refreshClick()
            }
            swiperefresh.setOnRefreshListener {
                presenter.refreshSwipe()
            }
            searchEditText.doAfterTextChanged {
                presenter.searchTextChange(it.toString())
            }
        }
    }

    override fun showPasswords(list: List<ResourceModel>) {
        setState(State.SUCCESS)
        FastAdapterDiffUtil.calculateDiff(itemAdapter, list.map { PasswordItem(it) })
        fastAdapter.notifyAdapterDataSetChanged()
    }

    override fun hideProgress() {
        binding.progress.gone()
    }

    override fun hideRefreshProgress() {
        binding.swiperefresh.isRefreshing = false
    }

    override fun showError() {
        setState(State.ERROR)
    }

    override fun showProgress() {
        setState(State.PROGRESS)
    }

    override fun navigateToMore(resourceModel: ResourceModel) {
        val model = PasswordMoreModel(resourceModel.name)
        PasswordMenuFragment.newInstance(model)
            .show(childFragmentManager, PasswordMenuFragment::class.java.name)
    }

    override fun navigateToDetails(resourceModel: ResourceModel) {
        startActivity(Intent(requireContext(), ResourcesActivity::class.java).apply {
            putExtra(ResourcesActivity.RESOURCE_MODEL_KEY, resourceModel)
        })
    }

    override fun addToClipboard(label: String, value: String) {
        clipboardManager?.setPrimaryClip(
            ClipData.newPlainText(label, value)
        )
        Toast.makeText(requireContext(), getString(R.string.copied_info, label), Toast.LENGTH_SHORT).show()
    }

    override fun menuCopyPasswordClick() {
        presenter.menuCopyPasswordClick()
    }

    override fun menuCopyUrlClick() {
        presenter.menuCopyUrlClick()
    }

    override fun menuCopyUsernameClick() {
        presenter.menuCopyUsernameClick()
    }

    override fun menuLaunchWebsiteClick() {
        presenter.menuLaunchWebsiteClick()
    }

    override fun openWebsite(url: String) {
        websiteOpener.open(requireContext(), url)
    }

    override fun showDecryptionFailure() {
        Snackbar.make(requireView(), R.string.home_decryption_failure, Snackbar.LENGTH_LONG)
            .show()
    }

    override fun showFetchFailure() {
        Snackbar.make(requireView(), R.string.home_fetch_failure, Snackbar.LENGTH_LONG)
            .show()
    }

    companion object {
        private val AVATAR_SIZE = 30.px
    }

    enum class State(
        val progressVisible: Boolean,
        val errorVisible: Boolean,
        val emptyVisible: Boolean,
        val listVisible: Boolean,
        val searchEnabled: Boolean
    ) {
        EMPTY(false, false, true, false, false),
        SEARCH_EMPTY(false, false, true, false, true),
        ERROR(false, true, false, false, false),
        PROGRESS(true, false, false, false, false),
        SUCCESS(false, false, false, true, true)
    }
}
