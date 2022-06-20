package com.passbolt.mobile.android.feature.home.screen

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import coil.ImageLoader
import coil.request.ImageRequest
import coil.transform.CircleCropTransformation
import com.google.android.material.snackbar.Snackbar
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.GenericItem
import com.mikepenz.fastadapter.adapters.ItemAdapter
import com.mikepenz.fastadapter.diff.FastAdapterDiffUtil
import com.passbolt.mobile.android.common.WebsiteOpener
import com.passbolt.mobile.android.common.extension.gone
import com.passbolt.mobile.android.common.extension.setDebouncingOnClick
import com.passbolt.mobile.android.common.extension.visible
import com.passbolt.mobile.android.common.lifecycleawarelazy.lifecycleAwareLazy
import com.passbolt.mobile.android.common.px
import com.passbolt.mobile.android.core.commonresource.FolderItem
import com.passbolt.mobile.android.core.commonresource.GroupWithCountItem
import com.passbolt.mobile.android.core.commonresource.InCurrentFoldersHeaderItem
import com.passbolt.mobile.android.core.commonresource.InSubFoldersHeaderItem
import com.passbolt.mobile.android.core.commonresource.PasswordItem
import com.passbolt.mobile.android.core.commonresource.TagWithCountItem
import com.passbolt.mobile.android.core.commonresource.moremenu.ResourceMoreMenuFragment
import com.passbolt.mobile.android.core.extension.setSearchEndIconWithListener
import com.passbolt.mobile.android.core.navigation.ActivityIntents
import com.passbolt.mobile.android.feature.authentication.BindingScopedAuthenticatedFragment
import com.passbolt.mobile.android.feature.home.R
import com.passbolt.mobile.android.feature.home.databinding.FragmentHomeBinding
import com.passbolt.mobile.android.feature.home.filtersmenu.FiltersMenuFragment
import com.passbolt.mobile.android.feature.home.screen.model.HomeDisplayView
import com.passbolt.mobile.android.feature.home.switchaccount.SwitchAccountBottomSheetFragment
import com.passbolt.mobile.android.feature.resources.ResourceActivity
import com.passbolt.mobile.android.feature.resources.ResourceMode
import com.passbolt.mobile.android.ui.FiltersMenuModel
import com.passbolt.mobile.android.ui.FolderWithCount
import com.passbolt.mobile.android.ui.GroupWithCount
import com.passbolt.mobile.android.ui.ResourceModel
import com.passbolt.mobile.android.ui.ResourceMoreMenuModel
import com.passbolt.mobile.android.ui.TagWithCount
import org.koin.android.ext.android.inject
import org.koin.core.qualifier.named

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
@Suppress("TooManyFunctions") // TODO MOB-321
class HomeFragment :
    BindingScopedAuthenticatedFragment<FragmentHomeBinding, HomeContract.View>(FragmentHomeBinding::inflate),
    HomeContract.View, ResourceMoreMenuFragment.Listener, SwitchAccountBottomSheetFragment.Listener,
    FiltersMenuFragment.Listener {

    override val presenter: HomeContract.Presenter by inject()
    private val passwordItemAdapter: ItemAdapter<PasswordItem> by inject(named(RESOURCE_ITEM_ADAPTER))
    private val childrenPasswordItemAdapter: ItemAdapter<PasswordItem> by inject(named(SUB_RESOURCE_ITEM_ADAPTER))
    private val folderItemAdapter: ItemAdapter<FolderItem> by inject(named(FOLDER_ITEM_ADAPTER))
    private val childrenFolderItemAdapter: ItemAdapter<FolderItem> by inject(named(SUB_FOLDER_ITEM_ADAPTER))
    private val tagsItemAdapter: ItemAdapter<TagWithCountItem> by inject(named(TAGS_ITEM_ADAPTER))
    private val groupsItemAdapter: ItemAdapter<GroupWithCountItem> by inject(named(GROUPS_ITEM_ADAPTER))
    private val inSubFoldersHeaderItemAdapter: ItemAdapter<InSubFoldersHeaderItem> by inject(
        named(IN_SUB_FOLDERS_HEADER_ITEM_ADAPTER)
    )
    private val inCurrentFoldersHeaderItemAdapter: ItemAdapter<InCurrentFoldersHeaderItem> by inject(
        named(IN_CURRENT_FOLDER_HEADER_ITEM_ADAPTER)
    )
    private val fastAdapter: FastAdapter<GenericItem> by inject()
    private val imageLoader: ImageLoader by inject()
    private val clipboardManager: ClipboardManager? by inject()
    private val websiteOpener: WebsiteOpener by inject()
    private val arguments: HomeFragmentArgs by navArgs()
    private val navController by lifecycleAwareLazy { findNavController() }

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
            if (it.resultCode == ResourceActivity.RESULT_RESOURCE_SHARED) {
                presenter.resourceShared()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        presenter.viewCreate((activity as HomeDataRefreshExecutor).supplyFullDataRefreshStatusFlow())
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initAdapter()
        setListeners()
        val hasPreviousEntry = navController.previousBackStackEntry != null
        presenter.attach(this)
        presenter.argsRetrieved(arguments.homeView, hasPreviousEntry)
    }

    override fun onDestroyView() {
        binding.recyclerView.adapter = null
        presenter.detach()
        super.onDestroyView()
    }

    override fun displaySearchAvatar(url: String?) {
        val request = ImageRequest.Builder(requireContext())
            .data(url)
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

    override fun displaySearchClearIcon() {
        binding.searchTextInput.setSearchEndIconWithListener(
            ContextCompat.getDrawable(requireContext(), R.drawable.ic_close)!!,
            presenter::searchClearClick
        )
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
            },
            FolderItem.ItemClick {
                presenter.folderItemClick(it)
            },
            TagWithCountItem.ItemClick {
                presenter.tagItemClick(it)
            },
            GroupWithCountItem.ItemClick {
                presenter.groupItemClick(it)
            }
        ))
    }

    private fun setState(state: State) {
        with(binding) {
            recyclerView.isVisible = state.listVisible
            emptyListContainer.isVisible = state.emptyVisible
            errorContainer.isVisible = state.errorVisible
            progress.isVisible = state.progressVisible
        }
    }

    override fun showEmptyList() {
        setState(State.EMPTY)
        binding.appBar.setExpanded(true)
    }

    override fun showSearchEmptyList() {
        setState(State.SEARCH_EMPTY)
        binding.appBar.setExpanded(true)
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
                presenter.createResourceClick()
            }
            searchTextInput.setStartIconOnClickListener {
                presenter.filtersClick()
            }
            backButton.setOnClickListener {
                navController.popBackStack()
            }
        }
    }

    override fun showItems(
        resourceList: List<ResourceModel>,
        foldersList: List<FolderWithCount>,
        tagsList: List<TagWithCount>,
        groupsList: List<GroupWithCount>,
        filteredSubFoldersList: List<FolderWithCount>,
        filteredSubFolderResourceList: List<ResourceModel>,
        sectionsConfiguration: HeaderSectionConfiguration
    ) {
        setState(State.SUCCESS)
        // "in current folder" header
        FastAdapterDiffUtil.calculateDiff(
            inCurrentFoldersHeaderItemAdapter,
            createCurrentFolderSection(sectionsConfiguration)
        )
        // current folder folders
        FastAdapterDiffUtil.calculateDiff(folderItemAdapter, foldersList.map { FolderItem(it) })
        // tags
        FastAdapterDiffUtil.calculateDiff(tagsItemAdapter, tagsList.map { TagWithCountItem(it) })
        // groups
        FastAdapterDiffUtil.calculateDiff(groupsItemAdapter, groupsList.map { GroupWithCountItem(it) })
        // current folder resources
        FastAdapterDiffUtil.calculateDiff(passwordItemAdapter, resourceList.map { PasswordItem(it) })
        // "in sub-folders" header
        FastAdapterDiffUtil.calculateDiff(
            inSubFoldersHeaderItemAdapter,
            createInSubFoldersSection(sectionsConfiguration)
        )
        // sub-folders folders
        FastAdapterDiffUtil.calculateDiff(childrenFolderItemAdapter, filteredSubFoldersList.map { FolderItem(it) })
        // sub-folders resources
        FastAdapterDiffUtil.calculateDiff(
            childrenPasswordItemAdapter,
            filteredSubFolderResourceList.map { PasswordItem(it) })
        fastAdapter.notifyAdapterDataSetChanged()
    }

    private fun createInSubFoldersSection(sectionsConfiguration: HeaderSectionConfiguration) =
        if (sectionsConfiguration.isInSubFoldersSectionVisible) {
            listOf(InSubFoldersHeaderItem())
        } else {
            emptyList()
        }

    private fun createCurrentFolderSection(sectionsConfiguration: HeaderSectionConfiguration) =
        if (sectionsConfiguration.isInCurrentFolderSectionVisible) {
            listOf(
                InCurrentFoldersHeaderItem(
                    getString(
                        R.string.home_in_current_folder,
                        sectionsConfiguration.currentFolderName ?: getString(R.string.folder_root)
                    )
                )
            )
        } else {
            emptyList()
        }

    override fun hideProgress() {
        binding.progress.gone()
    }

    override fun hideRefreshProgress() {
        binding.swipeRefresh.isRefreshing = false
    }

    override fun showError() {
        setState(State.ERROR)
        binding.appBar.setExpanded(true)
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
            ResourceActivity.newInstance(requireContext(), ResourceMode.DETAILS, resourceModel.folderId, resourceModel)
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

    override fun menuShareClick() {
        presenter.menuShareClick()
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
        showSnackbar(R.string.common_failure)
    }

    override fun showResourceDeletedSnackbar(name: String) {
        showSnackbar(R.string.common_message_resource_deleted, name)
    }

    override fun showResourceEditedSnackbar(resourceName: String) {
        showSnackbar(R.string.common_message_resource_edited, resourceName)
    }

    override fun showResourceSharedSnackbar() {
        showSnackbar(R.string.common_message_resource_shared)
    }

    override fun navigateToSwitchAccount() {
        SwitchAccountBottomSheetFragment()
            .show(childFragmentManager, SwitchAccountBottomSheetFragment::class.java.name)
    }

    override fun clearSearchInput() {
        binding.searchEditText.setText("")
    }

    override fun showResourceAddedSnackbar() {
        showSnackbar(R.string.resource_update_create_success)
    }

    private fun showSnackbar(@StringRes messageResId: Int, vararg messageArgs: String) {
        Snackbar.make(binding.root, getString(messageResId, messageArgs), Snackbar.LENGTH_SHORT)
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
                resourceModel.folderId,
                resourceModel
            )
        )
    }

    override fun navigateToEditResourcePermissions(resource: ResourceModel) {
        resourceDetailsResult.launch(
            ResourceActivity.newInstance(requireContext(), ResourceMode.SHARE, existingResource = resource)
        )
    }

    override fun hideAddButton() {
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

    override fun switchAccountManageAccountClick() {
        presenter.switchAccountManageAccountClick()
    }

    override fun navigateToManageAccounts() {
        authenticationResult.launch(
            ActivityIntents.authentication(
                requireContext(),
                ActivityIntents.AuthConfig.ManageAccount
            )
        )
    }

    override fun showFiltersMenu(activeDisplayView: HomeDisplayView) {
        FiltersMenuFragment.newInstance(FiltersMenuModel(activeDisplayView))
            .show(childFragmentManager, FiltersMenuFragment::class.java.name)
    }

    override fun menuAllItemsClick() {
        presenter.allItemsClick()
    }

    override fun menuFavouritesClick() {
        presenter.favouritesClick()
    }

    override fun menuRecentlyModifiedClick() {
        presenter.recentlyModifiedClick()
    }

    override fun menuSharedWithMeClick() {
        presenter.sharedWithMeClick()
    }

    override fun menuOwnedByMeClick() {
        presenter.ownedByMeClick()
    }

    override fun menuFoldersClick() {
        presenter.foldersClick()
    }

    override fun menuTagsClick() {
        presenter.tagsClick()
    }

    override fun menuGroupsClick() {
        presenter.groupsClick()
    }

    override fun showAllItemsSearchHint() {
        binding.searchEditText.hint = getString(R.string.all_items_home_search_hint)
    }

    override fun showDefaultSearchHint() {
        binding.searchEditText.hint = getString(R.string.default_home_search_hint)
    }

    override fun showHomeScreenTitle(view: HomeDisplayView) {
        when (view) {
            is HomeDisplayView.AllItems -> showScreenTitleWithStartIcon(
                R.string.filters_menu_all_items,
                R.drawable.ic_list
            )
            is HomeDisplayView.Favourites -> showScreenTitleWithStartIcon(
                R.string.filters_menu_favourites,
                R.drawable.ic_star
            )
            is HomeDisplayView.RecentlyModified -> showScreenTitleWithStartIcon(
                R.string.filters_menu_recently_modified,
                R.drawable.ic_clock
            )
            is HomeDisplayView.SharedWithMe -> showScreenTitleWithStartIcon(
                R.string.filters_menu_shared_with_me,
                R.drawable.ic_share
            )
            is HomeDisplayView.OwnedByMe -> showScreenTitleWithStartIcon(
                R.string.filters_menu_owned_by_me,
                R.drawable.ic_person
            )
            is HomeDisplayView.Folders -> showScreenTitleWithStartIcon(
                R.string.filters_menu_folders,
                R.drawable.ic_folder
            )
            is HomeDisplayView.Tags -> showScreenTitleWithStartIcon(
                R.string.filters_menu_tags,
                R.drawable.ic_tag
            )
            is HomeDisplayView.Groups -> showScreenTitleWithStartIcon(
                R.string.filters_menu_groups,
                R.drawable.ic_group
            )
        }
    }

    override fun showChildFolderTitle(activeFolderName: String, isShared: Boolean) {
        showScreenTitleWithStartIcon(
            activeFolderName,
            if (isShared) R.drawable.ic_shared_folder else R.drawable.ic_folder
        )
    }

    override fun showTagTitle(activeTagTitle: String, isShared: Boolean) {
        showScreenTitleWithStartIcon(
            activeTagTitle,
            if (isShared) R.drawable.ic_shared_tag else R.drawable.ic_tag
        )
    }

    override fun showGroupTitle(groupName: String) {
        showScreenTitleWithStartIcon(groupName, R.drawable.ic_group)
    }

    private fun showScreenTitleWithStartIcon(@StringRes titleRes: Int, @DrawableRes iconRes: Int) {
        showScreenTitleWithStartIcon(getString(titleRes), iconRes)
    }

    private fun showScreenTitleWithStartIcon(title: String, @DrawableRes iconRes: Int) {
        with(binding.screenTitleLabel) {
            text = title
            setCompoundDrawablesWithIntrinsicBounds(iconRes, 0, 0, 0)
        }
    }

    override fun navigateToChild(homeView: HomeDisplayView) {
        navController.navigate(
            HomeFragmentDirections.actionHomeToHomeChild(homeView)
        )
    }

    override fun navigateToRootHomeFromChildHome(homeView: HomeDisplayView) {
        navController.navigate(
            HomeFragmentDirections.actionHomeChildToHome(homeView)
        )
    }

    override fun navigateRootHomeFromRootHome(homeView: HomeDisplayView) {
        navController.navigate(
            HomeFragmentDirections.actionHomeToHome(homeView)
        )
    }

    override fun performRefreshUsingRefreshExecutor() {
        (activity as HomeDataRefreshExecutor).performFullDataRefresh()
    }

    override fun showBackArrow() {
        binding.backButton.visible()
    }

    override fun hideBackArrow() {
        binding.backButton.gone()
    }

    override fun navigateToCreateResource(parentFolderId: String?) {
        resourceDetailsResult.launch(
            ResourceActivity.newInstance(
                requireContext(),
                ResourceMode.NEW,
                parentFolderId
            )
        )
    }

    companion object {
        private val AVATAR_SIZE = 30.px
    }

    enum class State(
        val progressVisible: Boolean,
        val errorVisible: Boolean,
        val emptyVisible: Boolean,
        val listVisible: Boolean
    ) {
        EMPTY(false, false, true, false),
        SEARCH_EMPTY(false, false, true, false),
        ERROR(false, true, false, false),
        PROGRESS(true, false, false, false),
        SUCCESS(false, false, false, true)
    }

    data class HeaderSectionConfiguration(
        val isInCurrentFolderSectionVisible: Boolean,
        val isInSubFoldersSectionVisible: Boolean,
        val currentFolderName: String? = null
    )
}
