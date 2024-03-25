package com.passbolt.mobile.android.feature.home.screen

import android.app.Activity
import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.os.Bundle
import android.os.PersistableBundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.setFragmentResultListener
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import coil.ImageLoader
import coil.request.ImageRequest
import coil.transform.CircleCropTransformation
import com.gaelmarhic.quadrant.Autofillresources
import com.google.android.material.snackbar.Snackbar
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.GenericItem
import com.mikepenz.fastadapter.adapters.ItemAdapter
import com.mikepenz.fastadapter.diff.FastAdapterDiffUtil
import com.passbolt.mobile.android.common.WebsiteOpener
import com.passbolt.mobile.android.common.dialogs.confirmResourceDeletionAlertDialog
import com.passbolt.mobile.android.common.dialogs.confirmTotpDeletionAlertDialog
import com.passbolt.mobile.android.common.lifecycleawarelazy.lifecycleAwareLazy
import com.passbolt.mobile.android.core.extension.gone
import com.passbolt.mobile.android.core.extension.px
import com.passbolt.mobile.android.core.extension.setDebouncingOnClick
import com.passbolt.mobile.android.core.extension.setSearchEndIconWithListener
import com.passbolt.mobile.android.core.extension.showSnackbar
import com.passbolt.mobile.android.core.extension.visible
import com.passbolt.mobile.android.core.navigation.ActivityIntents
import com.passbolt.mobile.android.core.navigation.ActivityResults
import com.passbolt.mobile.android.core.navigation.AppContext
import com.passbolt.mobile.android.core.navigation.deeplinks.NavDeepLinkProvider
import com.passbolt.mobile.android.core.ui.initialsicon.InitialsIconGenerator
import com.passbolt.mobile.android.core.ui.progressdialog.hideProgressDialog
import com.passbolt.mobile.android.core.ui.progressdialog.showProgressDialog
import com.passbolt.mobile.android.createfolder.CreateFolderFragment
import com.passbolt.mobile.android.feature.authentication.BindingScopedAuthenticatedFragment
import com.passbolt.mobile.android.feature.home.R
import com.passbolt.mobile.android.feature.home.databinding.FragmentHomeBinding
import com.passbolt.mobile.android.feature.home.filtersmenu.FiltersMenuFragment
import com.passbolt.mobile.android.feature.home.screen.model.HeaderSectionConfiguration
import com.passbolt.mobile.android.feature.home.screen.model.State
import com.passbolt.mobile.android.feature.home.screen.recycler.FolderItem
import com.passbolt.mobile.android.feature.home.screen.recycler.GroupWithCountItem
import com.passbolt.mobile.android.feature.home.screen.recycler.InCurrentFoldersHeaderItem
import com.passbolt.mobile.android.feature.home.screen.recycler.InSubFoldersHeaderItem
import com.passbolt.mobile.android.feature.home.screen.recycler.PasswordHeaderItem
import com.passbolt.mobile.android.feature.home.screen.recycler.PasswordItem
import com.passbolt.mobile.android.feature.home.screen.recycler.TagWithCountItem
import com.passbolt.mobile.android.feature.home.switchaccount.SwitchAccountBottomSheetFragment
import com.passbolt.mobile.android.feature.otp.createotpmanually.CreateOtpFragment
import com.passbolt.mobile.android.feature.otp.scanotp.ScanOtpFragment
import com.passbolt.mobile.android.feature.resourcedetails.ResourceActivity
import com.passbolt.mobile.android.feature.resourcedetails.ResourceMode
import com.passbolt.mobile.android.moremenu.FolderMoreMenuFragment
import com.passbolt.mobile.android.otpcreatemoremenu.OtpCreateMoreMenuFragment
import com.passbolt.mobile.android.otpeditmoremenu.OtpUpdateMoreMenuFragment
import com.passbolt.mobile.android.otpmoremenu.OtpMoreMenuFragment
import com.passbolt.mobile.android.resourcemoremenu.ResourceMoreMenuFragment
import com.passbolt.mobile.android.ui.FiltersMenuModel
import com.passbolt.mobile.android.ui.Folder
import com.passbolt.mobile.android.ui.FolderMoreMenuModel
import com.passbolt.mobile.android.ui.FolderWithCountAndPath
import com.passbolt.mobile.android.ui.GroupWithCount
import com.passbolt.mobile.android.ui.HomeDisplayViewModel
import com.passbolt.mobile.android.ui.ResourceItemWrapper
import com.passbolt.mobile.android.ui.ResourceListUiModel
import com.passbolt.mobile.android.ui.ResourceModel
import com.passbolt.mobile.android.ui.ResourceMoreMenuModel
import com.passbolt.mobile.android.ui.TagWithCount
import org.koin.android.ext.android.inject
import org.koin.core.qualifier.named
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
@Suppress("TooManyFunctions", "LargeClass") // TODO MOB-321
class HomeFragment :
    BindingScopedAuthenticatedFragment<FragmentHomeBinding, HomeContract.View>(FragmentHomeBinding::inflate),
    HomeContract.View, ResourceMoreMenuFragment.Listener, SwitchAccountBottomSheetFragment.Listener,
    FiltersMenuFragment.Listener, FolderMoreMenuFragment.Listener, OtpCreateMoreMenuFragment.Listener,
    OtpMoreMenuFragment.Listener, OtpUpdateMoreMenuFragment.Listener {

    override val presenter: HomeContract.Presenter by inject()
    override val appContext = AppContext.APP
    private val suggestedHeaderItemAdapter: ItemAdapter<PasswordHeaderItem> by inject(
        named(SUGGESTED_HEADER_ITEM_ADAPTER)
    )
    private val suggestedItemsItemAdapter: ItemAdapter<PasswordItem> by inject(named(SUGGESTED_ITEMS_ITEM_ADAPTER))
    private val otherItemsItemAdapter: ItemAdapter<PasswordHeaderItem> by inject(
        named(OTHER_ITEMS_HEADER_ITEM_ADAPTER)
    )
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
    private val initialsIconGenerator: InitialsIconGenerator by inject()

    private val snackbarAnchorView: View?
        get() {
            val speedDialView: View = binding.rootLayout.findViewById(R.id.homeSpeedDialViewId)
            return if (speedDialView.isVisible) {
                speedDialView
            } else {
                null
            }
        }
    private val fastAdapter: FastAdapter<GenericItem> by inject()
    private val imageLoader: ImageLoader by inject()
    private val clipboardManager: ClipboardManager? by inject()
    private val websiteOpener: WebsiteOpener by inject()
    private val arguments: HomeFragmentArgs by navArgs()
    private val navController by lifecycleAwareLazy { findNavController() }
    private val speedDialFabFactory: HomeSpeedDialFabFactory by inject()

    private val folderCreatedListener = { _: String, bundle: Bundle ->
        val name = requireNotNull(bundle.getString(CreateFolderFragment.EXTRA_CREATED_FOLDER_NAME))
        presenter.folderCreated(name)
    }

    // home fragment is used both here and in autofill resources activity
    private val resourceHandlingStrategy: ResourceHandlingStrategy by lifecycleAwareLazy {
        if (requireActivity().javaClass.name == Autofillresources.AUTOFILL_RESOURCES_ACTIVITY) {
            requireActivity() as ResourceHandlingStrategy
        } else {
            this
        }
    }

    private val authenticationResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == Activity.RESULT_OK) {
                // reinitialize for the switched account
                presenter.detach()
                val hasPreviousEntry = navController.previousBackStackEntry != null
                presenter.attach(this)
                presenter.argsRetrieved(
                    resourceHandlingStrategy.showSuggestedModel(),
                    homeDisplayView = null,
                    hasPreviousEntry,
                    resourceHandlingStrategy.shouldShowCloseButton(),
                    resourceHandlingStrategy.shouldShowResourceMoreMenu()
                )
            }
        }

    private val resourceDetailsResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == ActivityResults.RESULT_RESOURCE_DELETED) {
                val name = it.data?.getStringExtra(ResourceActivity.EXTRA_RESOURCE_NAME)
                presenter.resourceDeleted(name.orEmpty())
            }
            if (it.resultCode == ActivityResults.RESULT_RESOURCE_EDITED) {
                val name = it.data?.getStringExtra(ResourceActivity.EXTRA_RESOURCE_NAME)
                presenter.resourceEdited(name.orEmpty())
            }
            if (it.resultCode == ActivityResults.RESULT_RESOURCE_CREATED) {
                val resourceId = it.data?.getStringExtra(ResourceActivity.EXTRA_RESOURCE_ID)
                presenter.newResourceCreated(resourceId)
            }
            if (it.resultCode == ActivityResults.RESULT_RESOURCE_SHARED) {
                presenter.resourceShared()
            }
        }

    private val otpEdited = { _: String, result: Bundle ->
        if (result.containsKey(CreateOtpFragment.EXTRA_OTP_UPDATED) &&
            result.containsKey(CreateOtpFragment.EXTRA_RESOURCE_NAME)
        ) {
            presenter.resourceEdited(result.getString(CreateOtpFragment.EXTRA_RESOURCE_NAME).orEmpty())
        }
    }

    private val otpQrScanned = { _: String, result: Bundle ->
        if (result.containsKey(ScanOtpFragment.EXTRA_SCANNED_OTP)) {
            presenter.otpScanned(
                result.getParcelable(ScanOtpFragment.EXTRA_SCANNED_OTP)
            )
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initAdapter()
        setListeners()
        val hasPreviousEntry = navController.previousBackStackEntry != null
        presenter.attach(this)
        presenter.argsRetrieved(
            resourceHandlingStrategy.showSuggestedModel(),
            arguments.homeView,
            hasPreviousEntry,
            resourceHandlingStrategy.shouldShowCloseButton(),
            resourceHandlingStrategy.shouldShowResourceMoreMenu()
        )
    }

    override fun onResume() {
        super.onResume()
        presenter.resume(this)
    }

    override fun onPause() {
        presenter.pause()
        super.onPause()
    }

    override fun initSpeedDialFab(homeView: HomeDisplayViewModel) {
        with(speedDialFabFactory) {
            addPasswordClick = { presenter.createResourceClick() }
            addFolderClick = { presenter.createFolderClick() }

            binding.rootLayout.addView(
                getSpeedDialFab(requireContext(), binding.overlay, homeView)
            )
        }
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
            .placeholder(CoreUiR.drawable.ic_avatar_placeholder)
            .target(
                onError = {
                    binding.searchTextInput.setSearchEndIconWithListener(
                        ContextCompat.getDrawable(requireContext(), CoreUiR.drawable.ic_avatar_placeholder)!!,
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
            ContextCompat.getDrawable(requireContext(), CoreUiR.drawable.ic_close)!!,
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
                resourceHandlingStrategy.resourceItemClick(it)
            },
            PasswordItem.MoreClick {
                presenter.resourceMoreClick(it)
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

    override fun resourceItemClick(resourceModel: ResourceModel) {
        presenter.itemClick(resourceModel)
    }

    override fun shouldShowResourceMoreMenu() = true

    override fun shouldShowFolderMoreMenu() = true

    override fun showSuggestedModel() =
        ShowSuggestedModel.DoNotShow

    override fun resourcePostCreateAction(resourceId: String) {
        // nothing more to do after creating resource on home fragment
    }

    override fun shouldShowCloseButton() = false

    private fun setState(state: State) {
        with(binding) {
            recyclerView.isVisible = state.listVisible
            emptyListContainer.isVisible = state.emptyVisible
        }
    }

    override fun showCloseButton() {
        binding.closeButton.visible()
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
            swipeRefresh.setOnRefreshListener {
                presenter.refreshSwipe()
            }
            searchEditText.doAfterTextChanged {
                presenter.searchTextChange(it.toString())
            }
            searchTextInput.setStartIconOnClickListener {
                presenter.filtersClick()
            }
            moreButton.setDebouncingOnClick {
                presenter.moreClick()
            }
            backButton.setDebouncingOnClick {
                navController.popBackStack()
            }
            closeButton.setDebouncingOnClick {
                presenter.closeClick()
            }
        }
    }

    @Suppress("LongMethod") // will be refactored in Q2 - MOB-1029
    override fun showItems(
        suggestedResources: List<ResourceModel>,
        resourceList: List<ResourceModel>,
        foldersList: List<FolderWithCountAndPath>,
        tagsList: List<TagWithCount>,
        groupsList: List<GroupWithCount>,
        filteredSubFoldersList: List<FolderWithCountAndPath>,
        filteredSubFolderResourceList: List<ResourceModel>,
        sectionsConfiguration: HeaderSectionConfiguration
    ) {
        setState(State.SUCCESS)
        // suggested header
        FastAdapterDiffUtil.calculateDiff(
            suggestedHeaderItemAdapter,
            if (sectionsConfiguration.isSuggestedSectionVisible) {
                listOf(PasswordHeaderItem(ResourceListUiModel.Header(getString(LocalizationR.string.suggested))))
            } else {
                emptyList()
            }
        )
        // suggested items
        FastAdapterDiffUtil.calculateDiff(
            suggestedItemsItemAdapter,
            suggestedResources.map { resourceModel ->
                PasswordItem(
                    ResourceItemWrapper(resourceModel),
                    initialsIconGenerator,
                    resourceHandlingStrategy.shouldShowResourceMoreMenu()
                )
            }
        )
        // other items header
        FastAdapterDiffUtil.calculateDiff(
            otherItemsItemAdapter,
            if (sectionsConfiguration.isOtherItemsSectionVisible) {
                listOf(PasswordHeaderItem(ResourceListUiModel.Header(getString(LocalizationR.string.other))))
            } else {
                emptyList()
            }
        )
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
        FastAdapterDiffUtil.calculateDiff(
            passwordItemAdapter,
            resourceList.map { resourceModel ->
                PasswordItem(
                    ResourceItemWrapper(resourceModel),
                    initialsIconGenerator,
                    dotsVisible = resourceHandlingStrategy.shouldShowResourceMoreMenu()
                )
            })
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
            filteredSubFolderResourceList.map { resourceModel ->
                PasswordItem(
                    ResourceItemWrapper(resourceModel),
                    initialsIconGenerator,
                    dotsVisible = resourceHandlingStrategy.shouldShowResourceMoreMenu()
                )
            })
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
                        LocalizationR.string.home_in_current_folder,
                        sectionsConfiguration.currentFolderName ?: getString(LocalizationR.string.folder_root)
                    )
                )
            )
        } else {
            emptyList()
        }

    override fun hideRefreshProgress() {
        binding.swipeRefresh.isRefreshing = false
    }

    override fun showRefreshProgress() {
        binding.swipeRefresh.isRefreshing = true
    }

    override fun navigateToMore(resourceId: String, resourceName: String) {
        presenter.pause()
        ResourceMoreMenuFragment.newInstance(resourceId, resourceName)
            .show(this@HomeFragment.childFragmentManager, ResourceMoreMenuFragment::class.java.name)
    }

    override fun navigateToDetails(resourceModel: ResourceModel) {
        resourceDetailsResult.launch(
            ResourceActivity.newInstance(requireContext(), ResourceMode.DETAILS, resourceModel.folderId, resourceModel)
        )
    }

    override fun addToClipboard(label: String, value: String, isSecret: Boolean) {
        clipboardManager?.setPrimaryClip(
            ClipData.newPlainText(label, value).apply {
                description.extras = PersistableBundle().apply {
                    putBoolean(ClipDescription.EXTRA_IS_SENSITIVE, isSecret)
                }
            }
        )
        Toast.makeText(requireContext(), getString(LocalizationR.string.copied_info, label), Toast.LENGTH_SHORT).show()
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

    override fun menuAddTotpClick() {
        presenter.menuAddTotpClick()
    }

    override fun navigateToOtpCreateMenu() {
        OtpCreateMoreMenuFragment()
            .show(childFragmentManager, OtpCreateMoreMenuFragment::class.java.name)
    }

    override fun menuManageTotpClick() {
        presenter.manageTotpClick()
    }

    override fun resourceMoreMenuDismissed() {
        presenter.resume(this)
    }

    override fun otpMenuDismissed() {
        presenter.resume(this)
    }

    override fun navigateToOtpMoreMenu(resourceId: String, resourceName: String) {
        OtpMoreMenuFragment.newInstance(resourceId, resourceName, canShowTotp = false)
            .show(childFragmentManager, OtpMoreMenuFragment::class.java.name)
    }

    override fun openWebsite(url: String) {
        websiteOpener.open(requireContext(), url)
    }

    override fun showDecryptionFailure() {
        Toast.makeText(requireContext(), LocalizationR.string.common_decryption_failure, Toast.LENGTH_SHORT)
            .show()
    }

    override fun showFetchFailure() {
        Toast.makeText(requireContext(), LocalizationR.string.common_fetch_failure, Toast.LENGTH_SHORT)
            .show()
    }

    override fun showGeneralError(errorMessage: String?) {
        showSnackbar(
            LocalizationR.string.common_failure_format,
            anchorView = snackbarAnchorView,
            backgroundColor = CoreUiR.color.red,
            messageArgs = arrayOf(errorMessage.orEmpty())
        )
    }

    override fun showResourceDeletedSnackbar(name: String) {
        showSnackbar(
            messageResId = LocalizationR.string.common_message_resource_deleted,
            messageArgs = arrayOf(name),
            anchorView = snackbarAnchorView,
            backgroundColor = CoreUiR.color.green
        )
    }

    override fun showResourceEditedSnackbar(resourceName: String) {
        showSnackbar(
            messageResId = LocalizationR.string.common_message_resource_edited,
            messageArgs = arrayOf(resourceName),
            anchorView = snackbarAnchorView,
            backgroundColor = CoreUiR.color.green
        )
    }

    override fun showResourceSharedSnackbar() {
        showSnackbar(
            LocalizationR.string.common_message_resource_shared,
            anchorView = snackbarAnchorView,
            backgroundColor = CoreUiR.color.green
        )
    }

    override fun showTotpDeletionFailed() {
        showSnackbar(
            LocalizationR.string.home_failed_to_delete_totp,
            anchorView = snackbarAnchorView,
            backgroundColor = CoreUiR.color.red
        )
    }

    override fun navigateToSwitchAccount() {
        SwitchAccountBottomSheetFragment.newInstance(resourceHandlingStrategy.appContext)
            .show(childFragmentManager, SwitchAccountBottomSheetFragment::class.java.name)
    }

    override fun clearSearchInput() {
        binding.searchEditText.setText("")
    }

    override fun showResourceAddedSnackbar() {
        showSnackbar(
            LocalizationR.string.resource_update_create_success,
            anchorView = snackbarAnchorView,
            backgroundColor = CoreUiR.color.green
        )
    }

    override fun menuEditClick() {
        presenter.menuEditClick()
    }

    override fun menuFavouriteClick(option: ResourceMoreMenuModel.FavouriteOption) {
        presenter.menuFavouriteClick(option)
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
        binding.rootLayout.findViewById<View>(R.id.homeSpeedDialViewId).gone()
    }

    override fun showAddButton() {
        binding.rootLayout.findViewById<View>(R.id.homeSpeedDialViewId).visible()
    }

    override fun showDeleteConfirmationDialog() {
        confirmResourceDeletionAlertDialog(requireContext()) {
            presenter.deleteResourceConfirmed()
        }
            .show()
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

    override fun navigateToSwitchedAccountAuth() {
        if (resourceHandlingStrategy.appContext == AppContext.APP) {
            requireActivity().finishAffinity()
        }
        authenticationResult.launch(
            ActivityIntents.authentication(
                requireContext(),
                when (resourceHandlingStrategy.appContext) {
                    AppContext.APP -> ActivityIntents.AuthConfig.Startup
                    AppContext.AUTOFILL -> ActivityIntents.AuthConfig.RefreshSession
                },
                appContext = resourceHandlingStrategy.appContext
            )
        )
    }

    override fun showFiltersMenu(activeDisplayView: HomeDisplayViewModel) {
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

    override fun menuExpiryClick() {
        presenter.expiryClick()
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
        binding.searchEditText.hint = getString(LocalizationR.string.all_items_home_search_hint)
    }

    override fun showDefaultSearchHint() {
        binding.searchEditText.hint = getString(LocalizationR.string.default_home_search_hint)
    }

    override fun showHomeScreenTitle(view: HomeDisplayViewModel) {
        when (view) {
            is HomeDisplayViewModel.AllItems -> showScreenTitleWithStartIcon(
                LocalizationR.string.filters_menu_all_items,
                CoreUiR.drawable.ic_list
            )
            is HomeDisplayViewModel.Favourites -> showScreenTitleWithStartIcon(
                LocalizationR.string.filters_menu_favourites,
                CoreUiR.drawable.ic_star
            )
            is HomeDisplayViewModel.RecentlyModified -> showScreenTitleWithStartIcon(
                LocalizationR.string.filters_menu_recently_modified,
                CoreUiR.drawable.ic_clock
            )
            is HomeDisplayViewModel.SharedWithMe -> showScreenTitleWithStartIcon(
                LocalizationR.string.filters_menu_shared_with_me,
                CoreUiR.drawable.ic_share
            )
            is HomeDisplayViewModel.OwnedByMe -> showScreenTitleWithStartIcon(
                LocalizationR.string.filters_menu_owned_by_me,
                CoreUiR.drawable.ic_person
            )
            is HomeDisplayViewModel.Folders -> showScreenTitleWithStartIcon(
                LocalizationR.string.filters_menu_folders,
                CoreUiR.drawable.ic_folder
            )
            is HomeDisplayViewModel.Tags -> showScreenTitleWithStartIcon(
                LocalizationR.string.filters_menu_tags,
                CoreUiR.drawable.ic_tag
            )
            is HomeDisplayViewModel.Groups -> showScreenTitleWithStartIcon(
                LocalizationR.string.filters_menu_groups,
                CoreUiR.drawable.ic_group
            )
            HomeDisplayViewModel.Expiry -> showScreenTitleWithStartIcon(
                LocalizationR.string.filters_menu_expiry,
                CoreUiR.drawable.ic_calendar_clock
            )
        }
    }

    override fun showChildFolderTitle(activeFolderName: String, isShared: Boolean) {
        showScreenTitleWithStartIcon(
            activeFolderName,
            if (isShared) CoreUiR.drawable.ic_shared_folder else CoreUiR.drawable.ic_folder
        )
    }

    override fun showTagTitle(activeTagTitle: String, isShared: Boolean) {
        showScreenTitleWithStartIcon(
            activeTagTitle,
            if (isShared) CoreUiR.drawable.ic_shared_tag else CoreUiR.drawable.ic_tag
        )
    }

    override fun showGroupTitle(groupName: String) {
        showScreenTitleWithStartIcon(groupName, CoreUiR.drawable.ic_group)
    }

    private fun showScreenTitleWithStartIcon(@StringRes titleRes: Int, @DrawableRes iconRes: Int) {
        showScreenTitleWithStartIcon(getString(titleRes), iconRes)
    }

    private fun showScreenTitleWithStartIcon(title: String, @DrawableRes iconRes: Int) {
        with(binding) {
            screenTitleLabel.text = title
            titleDrawable.setImageResource(iconRes)
        }
    }

    override fun navigateToChild(homeView: HomeDisplayViewModel) {
        navController.navigate(
            HomeFragmentDirections.actionHomeToHomeChild(homeView)
        )
    }

    override fun navigateToRootHomeFromChildHome(homeView: HomeDisplayViewModel) {
        navController.navigate(
            HomeFragmentDirections.actionHomeChildToHome(homeView)
        )
    }

    override fun navigateRootHomeFromRootHome(homeView: HomeDisplayViewModel) {
        navController.navigate(
            HomeFragmentDirections.actionHomeToHome(homeView)
        )
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

    override fun finish() {
        requireActivity().finish()
    }

    override fun showToggleFavouriteFailure() {
        showSnackbar(
            LocalizationR.string.favourites_failure,
            anchorView = snackbarAnchorView,
            backgroundColor = CoreUiR.color.red
        )
    }

    override fun showDeleteResourceFailure() {
        showSnackbar(
            LocalizationR.string.delete_failure,
            anchorView = snackbarAnchorView,
            backgroundColor = CoreUiR.color.red
        )
    }

    override fun showFolderMoreMenuIcon() {
        binding.moreButton.visible()
    }

    override fun hideFolderMoreMenuIcon() {
        binding.moreButton.gone()
    }

    override fun menuSeeFolderDetailsClick() {
        presenter.seeFolderDetailsClick()
    }

    override fun navigateToFolderMoreMenu(folderMoreMenuModel: FolderMoreMenuModel) {
        FolderMoreMenuFragment.newInstance(folderMoreMenuModel)
            .show(childFragmentManager, FolderMoreMenuModel::class.java.name)
    }

    override fun navigateToFolderDetails(childFolder: Folder.Child) {
        findNavController().navigate(
            NavDeepLinkProvider.folderDetailsDeepLinkRequest(childFolder.folderId)
        )
    }

    override fun navigateToCreateFolder(folderId: String?) {
        setFragmentResultListener(
            CreateFolderFragment.REQUEST_CREATE_FOLDER,
            folderCreatedListener
        )
        findNavController().navigate(
            NavDeepLinkProvider.createFolderDeepLinkRequest(folderId)
        )
    }

    override fun showFolderCreated(name: String) {
        showSnackbar(
            messageResId = LocalizationR.string.common_message_folder_created,
            messageArgs = arrayOf(name),
            anchorView = snackbarAnchorView,
            backgroundColor = CoreUiR.color.green
        )
    }

    override fun showContentNotAvailable() {
        Toast.makeText(requireContext(), LocalizationR.string.content_not_available, Toast.LENGTH_SHORT).show()
    }

    override fun showPleaseWaitForDataRefresh() {
        Toast.makeText(requireContext(), LocalizationR.string.home_please_wait_for_refresh, Toast.LENGTH_SHORT).show()
    }

    override fun showDataRefreshError() {
        showSnackbar(
            messageResId = LocalizationR.string.common_data_refresh_error,
            backgroundColor = CoreUiR.color.red,
            length = Snackbar.LENGTH_LONG
        )
    }

    override fun menuCreateOtpManuallyClick() {
        presenter.menuAddTotpManuallyClick()
    }

    override fun navigateToOtpCreate(resourceId: String) {
        setFragmentResultListener(
            CreateOtpFragment.REQUEST_UPDATE_OTP,
            otpEdited
        )

        findNavController().navigate(
            NavDeepLinkProvider.otpManualFormDeepLinkRequest(resourceId)
        )
    }

    override fun menuCreateByNewOtpScanClick() {
        setFragmentResultListener(
            ScanOtpFragment.REQUEST_SCAN_OTP_FOR_RESULT,
            otpQrScanned
        )
        findNavController().navigate(
            HomeFragmentDirections.actionHomeToScanOtp()
        )
    }

    override fun showEncryptionError(message: String) {
        showSnackbar(
            LocalizationR.string.common_encryption_failure,
            backgroundColor = CoreUiR.color.red
        )
    }

    override fun showInvalidTotpScanned() {
        showSnackbar(
            LocalizationR.string.resource_details_invalid_totp_scanned,
            backgroundColor = CoreUiR.color.red
        )
    }

    override fun showProgress() {
        showProgressDialog(childFragmentManager)
    }

    override fun hideProgress() {
        hideProgressDialog(childFragmentManager)
    }

    override fun menuCopyOtpClick() {
        presenter.menuCopyOtpClick()
    }

    override fun menuEditOtpClick() {
        presenter.menuEditOtpClick()
    }

    override fun menuDeleteOtpClick() {
        presenter.menuDeleteOtpClick()
    }

    override fun navigateToOtpEdit() {
        OtpUpdateMoreMenuFragment()
            .show(childFragmentManager, OtpUpdateMoreMenuFragment::class.java.name)
    }

    override fun menuEditOtpManuallyClick() {
        presenter.editOtpManuallyClick()
    }

    override fun menuEditByNewOtpScanClick() {
        navigateToScanOtpForResult()
    }

    private fun navigateToScanOtpForResult() {
        setFragmentResultListener(
            ScanOtpFragment.REQUEST_SCAN_OTP_FOR_RESULT,
            otpQrScanned
        )
        findNavController().navigate(
            HomeFragmentDirections.actionHomeToScanOtp()
        )
    }

    override fun showDeleteTotpConfirmationDialog() {
        confirmTotpDeletionAlertDialog(requireContext()) {
            presenter.totpDeletionConfirmed()
        }
            .show()
    }

    override fun showTotpDeleted() {
        showSnackbar(LocalizationR.string.otp_deleted)
    }

    override fun showJsonResourceSchemaValidationError() {
        showSnackbar(
            LocalizationR.string.common_json_schema_resource_validation_error,
            backgroundColor = CoreUiR.color.red
        )
    }

    override fun showJsonSecretSchemaValidationError() {
        showSnackbar(
            LocalizationR.string.common_json_schema_secret_validation_error,
            backgroundColor = CoreUiR.color.red
        )
    }

    companion object {
        private val AVATAR_SIZE = 30.px
    }
}
