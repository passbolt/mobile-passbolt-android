package com.passbolt.mobile.android.feature.home.screen

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
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
import com.passbolt.mobile.android.core.commonresource.PasswordItem
import com.passbolt.mobile.android.core.commonresource.moremenu.ResourceMoreMenuFragment
import com.passbolt.mobile.android.core.navigation.ActivityIntents
import com.passbolt.mobile.android.feature.authentication.BindingScopedAuthenticatedFragment
import com.passbolt.mobile.android.feature.home.R
import com.passbolt.mobile.android.feature.home.databinding.FragmentHomeBinding
import com.passbolt.mobile.android.feature.resources.ResourceActivity
import com.passbolt.mobile.android.feature.resources.ResourceMode
import com.passbolt.mobile.android.ui.ResourceModel
import com.passbolt.mobile.android.ui.ResourceMoreMenuModel
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

@Suppress("TooManyFunctions")
class HomeFragment :
    BindingScopedAuthenticatedFragment<FragmentHomeBinding, HomeContract.View>(FragmentHomeBinding::inflate),
    HomeContract.View, ResourceMoreMenuFragment.Listener {

    override val presenter: HomeContract.Presenter by inject()
    private val itemAdapter: ItemAdapter<PasswordItem> by inject()
    private val fastAdapter: FastAdapter<PasswordItem> by inject()
    private val imageLoader: ImageLoader by inject()
    private val clipboardManager: ClipboardManager? by inject()
    private val websiteOpener: WebsiteOpener by inject()

    private val authenticationResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == Activity.RESULT_OK) {
                presenter.userAuthenticated()
            }
        }

    private val resourceDetailsResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == ResourceActivity.RESULT_RESOURCE_DELETED) {
                val name = it.data?.getStringExtra(ResourceActivity.EXTRA_RESOURCE_NAME)
                presenter.resourceDeleted(name.orEmpty())
            }
            if (it.resultCode == ResourceActivity.RESULT_RESOURCE_EDITED) {
                val name = it.data?.getStringExtra(ResourceActivity.EXTRA_RESOURCE_NAME)
                presenter.resourceEdited(name.orEmpty())
            }
            if (it.resultCode == ResourceActivity.RESULT_RESOURCE_CREATED) {
                presenter.newResourceCreated()
            }
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initAdapter()
        setListeners()
        presenter.attach(this)
    }

    override fun displaySearchAvatar(url: String?) {
        val request = ImageRequest.Builder(requireContext())
            .data(url)
            .transformations(CircleCropTransformation())
            .size(AVATAR_SIZE, AVATAR_SIZE)
            .placeholder(R.drawable.ic_avatar_placeholder)
            .target(
                onError = {
                    setSearchEndIconWithListener(
                        ContextCompat.getDrawable(requireContext(), R.drawable.ic_avatar_placeholder)!!,
                        presenter::searchAvatarClick
                    )
                },
                onSuccess = {
                    setSearchEndIconWithListener(it, presenter::searchAvatarClick)
                }
            )
            .build()
        imageLoader.enqueue(request)
    }

    private fun setSearchEndIconWithListener(icon: Drawable, listener: () -> Unit) {
        with(binding.searchTextInput) {
            endIconMode = TextInputLayout.END_ICON_CUSTOM
            endIconDrawable = icon
            setEndIconOnClickListener {
                listener.invoke()
            }
        }
    }

    override fun displaySearchClearIcon() {
        setSearchEndIconWithListener(
            ContextCompat.getDrawable(requireContext(), R.drawable.ic_close)!!,
            presenter::searchClearClick
        )
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
            swipeRefresh.setOnRefreshListener {
                presenter.refreshSwipe()
            }
            searchEditText.doAfterTextChanged {
                presenter.searchTextChange(it.toString())
            }
            updateButton.setOnClickListener {
                resourceDetailsResult.launch(
                    ResourceActivity.newInstance(
                        requireContext(),
                        ResourceMode.NEW
                    )
                )
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
        binding.swipeRefresh.isRefreshing = false
    }

    override fun showError() {
        setState(State.ERROR)
    }

    override fun showProgress() {
        setState(State.PROGRESS)
    }

    override fun navigateToMore(resourceMoreMenuModel: ResourceMoreMenuModel) {
        ResourceMoreMenuFragment.newInstance(resourceMoreMenuModel)
            .show(childFragmentManager, ResourceMoreMenuFragment::class.java.name)
    }

    override fun navigateToDetails(resourceModel: ResourceModel) {
        resourceDetailsResult.launch(
            ResourceActivity.newInstance(requireContext(), ResourceMode.DETAILS, resourceModel)
        )
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

    override fun menuCopyDescriptionClick() {
        presenter.menuCopyDescriptionClick()
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

    override fun menuDeleteClick() {
        presenter.menuDeleteClick()
    }

    override fun openWebsite(url: String) {
        websiteOpener.open(requireContext(), url)
    }

    override fun showDecryptionFailure() {
        Toast.makeText(requireContext(), R.string.home_decryption_failure, Toast.LENGTH_SHORT)
            .show()
    }

    override fun showFetchFailure() {
        Toast.makeText(requireContext(), R.string.home_fetch_failure, Toast.LENGTH_SHORT)
            .show()
    }

    override fun showGeneralError() {
        Snackbar.make(binding.rootLayout, R.string.common_failure, Snackbar.LENGTH_SHORT)
            .setAnchorView(binding.updateButton)
            .show()
    }

    override fun showResourceDeletedSnackbar(name: String) {
        Snackbar.make(
            binding.rootLayout,
            getString(R.string.home_resource_deleted_format, name),
            Snackbar.LENGTH_LONG
        )
            .setAnchorView(binding.updateButton)
            .show()
    }

    override fun showResourceEditedSnackbar(resourceName: String) {
        Snackbar.make(
            binding.rootLayout,
            getString(R.string.home_resource_edited_format, resourceName),
            Snackbar.LENGTH_LONG
        )
            .setAnchorView(binding.updateButton)
            .show()
    }

    override fun navigateToManageAccount() {
        authenticationResult.launch(
            ActivityIntents.authentication(
                requireContext(),
                ActivityIntents.AuthConfig.ManageAccount
            )
        )
    }

    override fun clearSearchInput() {
        binding.searchEditText.setText("")
    }

    override fun showResourceAddedSnackbar() {
        Snackbar.make(
            binding.root,
            R.string.resource_update_create_success,
            Snackbar.LENGTH_SHORT
        )
            .setAnchorView(binding.updateButton)
            .show()
    }

    override fun menuEditClick() {
        presenter.menuEditClick()
    }

    override fun navigateToEdit(resourceModel: ResourceModel) {
        resourceDetailsResult.launch(
            ResourceActivity.newInstance(
                requireContext(),
                ResourceMode.EDIT,
                resourceModel
            )
        )
    }

    override fun hideUpdateButton() {
        binding.updateButton.hide()
    }

    override fun showAddButton() {
        binding.updateButton.show()
    }

    override fun showDeleteConfirmationDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.are_you_sure)
            .setMessage(R.string.resource_will_be_deleted)
            .setPositiveButton(R.string.delete) { _, _ -> presenter.deleteResourceConfirmed() }
            .setNegativeButton(R.string.cancel) { _, _ -> }
            .setCancelable(false)
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
