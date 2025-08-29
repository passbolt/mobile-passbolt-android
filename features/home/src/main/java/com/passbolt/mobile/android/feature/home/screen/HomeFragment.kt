package com.passbolt.mobile.android.feature.home.screen

import android.app.Activity
import android.os.Bundle
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
import coil3.ImageLoader
import coil3.asDrawable
import coil3.request.ImageRequest
import coil3.request.placeholder
import coil3.request.transformations
import coil3.transform.CircleCropTransformation
import com.google.android.material.snackbar.Snackbar
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.GenericItem
import com.mikepenz.fastadapter.adapters.ItemAdapter
import com.mikepenz.fastadapter.diff.FastAdapterDiffUtil
import com.passbolt.mobile.android.common.ExternalDeeplinkHandler
import com.passbolt.mobile.android.common.dialogs.confirmResourceDeletionAlertDialog
import com.passbolt.mobile.android.common.lifecycleawarelazy.lifecycleAwareLazy
import com.passbolt.mobile.android.core.clipboard.ClipboardAccess
import com.passbolt.mobile.android.core.extension.gone
import com.passbolt.mobile.android.core.extension.px
import com.passbolt.mobile.android.core.extension.setDebouncingOnClick
import com.passbolt.mobile.android.core.extension.setSearchEndIconWithListener
import com.passbolt.mobile.android.core.extension.showSnackbar
import com.passbolt.mobile.android.core.extension.visible
import com.passbolt.mobile.android.core.navigation.ActivityIntents
import com.passbolt.mobile.android.core.navigation.AppContext
import com.passbolt.mobile.android.core.navigation.constants.Autofillresources
import com.passbolt.mobile.android.core.navigation.deeplinks.NavDeepLinkProvider
import com.passbolt.mobile.android.core.resources.resourceicon.ResourceIconProvider
import com.passbolt.mobile.android.core.ui.progressdialog.hideProgressDialog
import com.passbolt.mobile.android.core.ui.progressdialog.showProgressDialog
import com.passbolt.mobile.android.createfolder.CreateFolderFragment
import com.passbolt.mobile.android.createresourcemenu.CreateResourceMenuFragment
import com.passbolt.mobile.android.feature.authentication.BindingScopedAuthenticatedFragment
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
import com.passbolt.mobile.android.feature.otp.scanotp.ScanOtpFragment
import com.passbolt.mobile.android.feature.otp.scanotp.ScanOtpMode
import com.passbolt.mobile.android.feature.otp.scanotp.scanotpsuccess.ScanOtpSuccessFragment
import com.passbolt.mobile.android.feature.resourcedetails.details.ResourceDetailsFragment
import com.passbolt.mobile.android.feature.resourceform.main.ResourceFormFragment
import com.passbolt.mobile.android.moremenu.FolderMoreMenuFragment
import com.passbolt.mobile.android.permissions.permissions.PermissionsFragment
import com.passbolt.mobile.android.permissions.permissions.PermissionsItem
import com.passbolt.mobile.android.permissions.permissions.PermissionsMode
import com.passbolt.mobile.android.resourcemoremenu.ResourceMoreMenuFragment
import com.passbolt.mobile.android.ui.FiltersMenuModel
import com.passbolt.mobile.android.ui.Folder
import com.passbolt.mobile.android.ui.FolderMoreMenuModel
import com.passbolt.mobile.android.ui.FolderWithCountAndPath
import com.passbolt.mobile.android.ui.GroupWithCount
import com.passbolt.mobile.android.ui.HomeDisplayViewModel
import com.passbolt.mobile.android.ui.LeadingContentType
import com.passbolt.mobile.android.ui.ResourceFormMode
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
    HomeContract.View,
    ResourceMoreMenuFragment.Listener,
    SwitchAccountBottomSheetFragment.Listener,
    FiltersMenuFragment.Listener,
    FolderMoreMenuFragment.Listener,
    CreateResourceMenuFragment.Listener {
    override val presenter: HomeContract.Presenter by inject()
    override val appContext = AppContext.APP
    private val suggestedHeaderItemAdapter: ItemAdapter<PasswordHeaderItem> by inject(
        named(SUGGESTED_HEADER_ITEM_ADAPTER),
    )
    private val suggestedItemsItemAdapter: ItemAdapter<PasswordItem> by inject(named(SUGGESTED_ITEMS_ITEM_ADAPTER))
    private val otherItemsItemAdapter: ItemAdapter<PasswordHeaderItem> by inject(
        named(OTHER_ITEMS_HEADER_ITEM_ADAPTER),
    )
    private val passwordItemAdapter: ItemAdapter<PasswordItem> by inject(named(RESOURCE_ITEM_ADAPTER))
    private val childrenPasswordItemAdapter: ItemAdapter<PasswordItem> by inject(named(SUB_RESOURCE_ITEM_ADAPTER))
    private val folderItemAdapter: ItemAdapter<FolderItem> by inject(named(FOLDER_ITEM_ADAPTER))
    private val childrenFolderItemAdapter: ItemAdapter<FolderItem> by inject(named(SUB_FOLDER_ITEM_ADAPTER))
    private val tagsItemAdapter: ItemAdapter<TagWithCountItem> by inject(named(TAGS_ITEM_ADAPTER))
    private val groupsItemAdapter: ItemAdapter<GroupWithCountItem> by inject(named(GROUPS_ITEM_ADAPTER))
    private val inSubFoldersHeaderItemAdapter: ItemAdapter<InSubFoldersHeaderItem> by inject(
        named(IN_SUB_FOLDERS_HEADER_ITEM_ADAPTER),
    )
    private val inCurrentFoldersHeaderItemAdapter: ItemAdapter<InCurrentFoldersHeaderItem> by inject(
        named(IN_CURRENT_FOLDER_HEADER_ITEM_ADAPTER),
    )

    private val snackbarAnchorView: View?
        get() {
            return requiredBinding.createResourceFab.let {
                if (it.isVisible) {
                    it
                } else {
                    null
                }
            }
        }
    private val fastAdapter: FastAdapter<GenericItem> by inject()
    private val imageLoader: ImageLoader by inject()
    private val resourceIconProvider: ResourceIconProvider by inject()
    private val clipboardAccess: ClipboardAccess by inject()
    private val externalDeeplinkHandler: ExternalDeeplinkHandler by inject()
    private val arguments: HomeFragmentArgs by navArgs()
    private val navController by lifecycleAwareLazy { findNavController() }

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
                    resourceHandlingStrategy.shouldShowResourceMoreMenu(),
                )
            }
        }

    private val detailsReturned = { _: String, result: Bundle ->
        presenter.resourceDetailsReturned(
            result.getBoolean(ResourceDetailsFragment.EXTRA_RESOURCE_EDITED, false),
            result.getBoolean(ResourceDetailsFragment.EXTRA_RESOURCE_DELETED, false),
            result.getString(ResourceFormFragment.EXTRA_RESOURCE_NAME),
        )
    }

    private val otpScanQrReturned = { _: String, result: Bundle ->
        presenter.otpQrScanReturned(
            result.getBoolean(ScanOtpSuccessFragment.EXTRA_OTP_CREATED, false),
            result.getBoolean(ScanOtpFragment.EXTRA_MANUAL_CREATION_CHOSEN),
        )
    }

    private val resourceFormReturned = { _: String, result: Bundle ->
        presenter.resourceFormReturned(
            result.getBoolean(ResourceFormFragment.EXTRA_RESOURCE_CREATED, false),
            result.getBoolean(ResourceFormFragment.EXTRA_RESOURCE_EDITED, false),
            result.getString(ResourceFormFragment.EXTRA_RESOURCE_NAME),
        )
    }

    private val shareReturned = { _: String, result: Bundle ->
        if (result.containsKey(PermissionsFragment.EXTRA_RESOURCE_SHARED)) {
            presenter.resourceShared(result.getBoolean(PermissionsFragment.EXTRA_RESOURCE_SHARED, false))
        }
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
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
            resourceHandlingStrategy.shouldShowResourceMoreMenu(),
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

    override fun createTotpClick() {
        presenter.createTotpClick()
    }

    override fun createPasswordClick() {
        presenter.createResourceClick()
    }

    override fun createFolderClick() {
        presenter.createFolderClick()
    }

    override fun onDestroyView() {
        requiredBinding.recyclerView.adapter = null
        presenter.detach()
        super.onDestroyView()
    }

    override fun displaySearchAvatar(url: String?) {
        val request =
            ImageRequest
                .Builder(requireContext())
                .data(url)
                .transformations(CircleCropTransformation())
                .size(AVATAR_SIZE, AVATAR_SIZE)
                .placeholder(CoreUiR.drawable.ic_avatar_placeholder)
                .target(
                    onError = {
                        requiredBinding.searchTextInput.setSearchEndIconWithListener(
                            ContextCompat.getDrawable(requireContext(), CoreUiR.drawable.ic_avatar_placeholder)!!,
                            presenter::searchAvatarClick,
                        )
                    },
                    onSuccess = {
                        requiredBinding.searchTextInput.setSearchEndIconWithListener(it.asDrawable(resources), presenter::searchAvatarClick)
                    },
                ).build()
        imageLoader.enqueue(request)
    }

    override fun displaySearchClearIcon() {
        requiredBinding.searchTextInput.setSearchEndIconWithListener(
            ContextCompat.getDrawable(requireContext(), CoreUiR.drawable.ic_close)!!,
            presenter::searchClearClick,
        )
    }

    private fun initAdapter() {
        requiredBinding.recyclerView.apply {
            itemAnimator = null
            layoutManager = LinearLayoutManager(requireContext())
            adapter = fastAdapter
        }
        fastAdapter.addEventHooks(
            listOf(
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
                },
            ),
        )
    }

    override fun resourceItemClick(resourceModel: ResourceModel) {
        presenter.itemClick(resourceModel)
    }

    override fun shouldShowResourceMoreMenu() = true

    override fun shouldShowFolderMoreMenu() = true

    override fun showSuggestedModel() = ShowSuggestedModel.DoNotShow

    override fun resourcePostCreateAction(resourceId: String) {
        // nothing more to do after creating resource on home fragment
    }

    override fun shouldShowCloseButton() = false

    private fun setState(state: State) {
        with(requiredBinding) {
            recyclerView.isVisible = state.listVisible
            emptyListContainer.isVisible = state.emptyVisible
        }
    }

    override fun showCloseButton() {
        requiredBinding.closeButton.visible()
    }

    override fun showEmptyList() {
        setState(State.EMPTY)
        requiredBinding.appBar.setExpanded(true)
    }

    override fun showSearchEmptyList() {
        setState(State.SEARCH_EMPTY)
        requiredBinding.appBar.setExpanded(true)
    }

    private fun setListeners() {
        with(requiredBinding) {
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
            createResourceFab.setDebouncingOnClick {
                presenter.onCreateResourceClick()
            }
            closeButton.setDebouncingOnClick {
                presenter.closeClick()
            }
        }

        setFragmentResultListeners()
    }

    private fun setFragmentResultListeners() {
        setFragmentResultListener(
            CreateFolderFragment.REQUEST_CREATE_FOLDER,
            folderCreatedListener,
        )
        setFragmentResultListener(
            ScanOtpFragment.REQUEST_SCAN_OTP_FOR_RESULT,
            otpScanQrReturned,
        )
        setFragmentResultListener(
            ResourceFormFragment.REQUEST_RESOURCE_FORM,
            resourceFormReturned,
        )
        setFragmentResultListener(
            ResourceDetailsFragment.REQUEST_RESOURCE_DETAILS,
            detailsReturned,
        )
        setFragmentResultListener(
            PermissionsFragment.REQUEST_UPDATE_PERMISSIONS,
            shareReturned,
        )
    }

    override fun showCreateResourceMenu(homeView: HomeDisplayViewModel) {
        CreateResourceMenuFragment
            .newInstance(homeView)
            .show(childFragmentManager, CreateResourceMenuFragment::class.java.name)
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
        sectionsConfiguration: HeaderSectionConfiguration,
    ) {
        setState(State.SUCCESS)
        // suggested header
        FastAdapterDiffUtil.calculateDiff(
            suggestedHeaderItemAdapter,
            if (sectionsConfiguration.isSuggestedSectionVisible) {
                listOf(PasswordHeaderItem(ResourceListUiModel.Header(getString(LocalizationR.string.suggested))))
            } else {
                emptyList()
            },
        )
        // suggested items
        FastAdapterDiffUtil.calculateDiff(
            suggestedItemsItemAdapter,
            suggestedResources.map { resourceModel ->
                PasswordItem(
                    ResourceItemWrapper(resourceModel),
                    resourceHandlingStrategy.shouldShowResourceMoreMenu(),
                    resourceIconProvider,
                )
            },
        )
        // other items header
        FastAdapterDiffUtil.calculateDiff(
            otherItemsItemAdapter,
            if (sectionsConfiguration.isOtherItemsSectionVisible) {
                listOf(PasswordHeaderItem(ResourceListUiModel.Header(getString(LocalizationR.string.other))))
            } else {
                emptyList()
            },
        )
        // "in current folder" header
        FastAdapterDiffUtil.calculateDiff(
            inCurrentFoldersHeaderItemAdapter,
            createCurrentFolderSection(sectionsConfiguration),
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
                    dotsVisible = resourceHandlingStrategy.shouldShowResourceMoreMenu(),
                    resourceIconProvider,
                )
            },
        )
        // "in sub-folders" header
        FastAdapterDiffUtil.calculateDiff(
            inSubFoldersHeaderItemAdapter,
            createInSubFoldersSection(sectionsConfiguration),
        )
        // sub-folders folders
        FastAdapterDiffUtil.calculateDiff(childrenFolderItemAdapter, filteredSubFoldersList.map { FolderItem(it) })
        // sub-folders resources
        FastAdapterDiffUtil.calculateDiff(
            childrenPasswordItemAdapter,
            filteredSubFolderResourceList.map { resourceModel ->
                PasswordItem(
                    ResourceItemWrapper(resourceModel),
                    dotsVisible = resourceHandlingStrategy.shouldShowResourceMoreMenu(),
                    resourceIconProvider,
                )
            },
        )
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
                        sectionsConfiguration.currentFolderName ?: getString(LocalizationR.string.folder_root),
                    ),
                ),
            )
        } else {
            emptyList()
        }

    override fun hideRefreshProgress() {
        requiredBinding.swipeRefresh.isRefreshing = false
    }

    override fun showRefreshProgress() {
        requiredBinding.swipeRefresh.isRefreshing = true
    }

    override fun navigateToMore(
        resourceId: String,
        resourceName: String,
    ) {
        presenter.pause()
        ResourceMoreMenuFragment
            .newInstance(resourceId, resourceName)
            .show(this@HomeFragment.childFragmentManager, ResourceMoreMenuFragment::class.java.name)
    }

    override fun navigateToDetails(resourceModel: ResourceModel) {
        findNavController().navigate(
            HomeFragmentDirections.actionHomeToDetails(resourceModel),
        )
    }

    override fun addToClipboard(
        label: String,
        value: String,
        isSecret: Boolean,
    ) {
        clipboardAccess.setPrimaryClip(requireContext(), label, value, isSecret)
    }

    override fun menuCopyPasswordClick() {
        presenter.menuCopyPasswordClick()
    }

    override fun menuCopyMetadataDescriptionClick() {
        presenter.menuCopyMetadataDescriptionClick()
    }

    override fun menuCopyNoteClick() {
        presenter.menuCopyNoteClick()
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

    override fun resourceMoreMenuDismissed() {
        presenter.resume(this)
    }

    override fun openWebsite(url: String) {
        externalDeeplinkHandler.openWebsite(requireContext(), url)
    }

    override fun showDecryptionFailure() {
        Toast
            .makeText(requireContext(), LocalizationR.string.common_decryption_failure, Toast.LENGTH_SHORT)
            .show()
    }

    override fun showFetchFailure() {
        Toast
            .makeText(requireContext(), LocalizationR.string.common_fetch_failure, Toast.LENGTH_SHORT)
            .show()
    }

    override fun showGeneralError(errorMessage: String?) {
        showSnackbar(
            LocalizationR.string.common_failure_format,
            anchorView = snackbarAnchorView,
            backgroundColor = CoreUiR.color.red,
            messageArgs = arrayOf(errorMessage.orEmpty()),
        )
    }

    override fun showResourceDeletedSnackbar(name: String) {
        showSnackbar(
            messageResId = LocalizationR.string.common_message_resource_deleted,
            messageArgs = arrayOf(name),
            anchorView = snackbarAnchorView,
            backgroundColor = CoreUiR.color.green,
        )
    }

    override fun showResourceEditedSnackbar(resourceName: String) {
        showSnackbar(
            messageResId = LocalizationR.string.common_message_resource_edited,
            messageArgs = arrayOf(resourceName),
            anchorView = snackbarAnchorView,
            backgroundColor = CoreUiR.color.green,
        )
    }

    override fun showResourceSharedSnackbar() {
        showSnackbar(
            LocalizationR.string.common_message_resource_shared,
            anchorView = snackbarAnchorView,
            backgroundColor = CoreUiR.color.green,
        )
    }

    override fun navigateToSwitchAccount() {
        SwitchAccountBottomSheetFragment
            .newInstance(resourceHandlingStrategy.appContext)
            .show(childFragmentManager, SwitchAccountBottomSheetFragment::class.java.name)
    }

    override fun clearSearchInput() {
        requiredBinding.searchEditText.setText("")
    }

    override fun showResourceCreatedSnackbar() {
        showSnackbar(
            LocalizationR.string.resource_form_create_success,
            anchorView = snackbarAnchorView,
            backgroundColor = CoreUiR.color.green,
        )
    }

    override fun menuEditClick() {
        presenter.menuEditClick()
    }

    override fun menuFavouriteClick(option: ResourceMoreMenuModel.FavouriteOption) {
        presenter.menuFavouriteClick(option)
    }

    override fun navigateToEdit(resourceModel: ResourceModel) {
        findNavController().navigate(
            HomeFragmentDirections.actionHomeToResourceForm(
                ResourceFormMode.Edit(
                    resourceModel.resourceId,
                    resourceModel.metadataJsonModel.name,
                ),
            ),
        )
    }

    override fun navigateToShare(resource: ResourceModel) {
        findNavController().navigate(
            HomeFragmentDirections.actionHomeToResourcePermissions(
                resource.resourceId,
                PermissionsMode.EDIT,
                PermissionsItem.RESOURCE,
            ),
        )
    }

    override fun hideCreateButton() {
        requiredBinding.createResourceFab.gone()
    }

    override fun showCreateButton() {
        requiredBinding.createResourceFab.visible()
    }

    override fun showDeleteConfirmationDialog() {
        confirmResourceDeletionAlertDialog(requireContext()) {
            presenter.deleteResourceConfirmed()
        }.show()
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
                ActivityIntents.AuthConfig.ManageAccount,
            ),
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
                appContext = resourceHandlingStrategy.appContext,
            ),
        )
    }

    override fun showFiltersMenu(activeDisplayView: HomeDisplayViewModel) {
        FiltersMenuFragment
            .newInstance(FiltersMenuModel(activeDisplayView))
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
        requiredBinding.searchEditText.hint = getString(LocalizationR.string.all_items_home_search_hint)
    }

    override fun showDefaultSearchHint() {
        requiredBinding.searchEditText.hint = getString(LocalizationR.string.default_home_search_hint)
    }

    override fun showHomeScreenTitle(view: HomeDisplayViewModel) {
        when (view) {
            is HomeDisplayViewModel.AllItems ->
                showScreenTitleWithStartIcon(
                    LocalizationR.string.filters_menu_all_items,
                    CoreUiR.drawable.ic_list,
                )
            is HomeDisplayViewModel.Favourites ->
                showScreenTitleWithStartIcon(
                    LocalizationR.string.filters_menu_favourites,
                    CoreUiR.drawable.ic_star,
                )
            is HomeDisplayViewModel.RecentlyModified ->
                showScreenTitleWithStartIcon(
                    LocalizationR.string.filters_menu_recently_modified,
                    CoreUiR.drawable.ic_clock,
                )
            is HomeDisplayViewModel.SharedWithMe ->
                showScreenTitleWithStartIcon(
                    LocalizationR.string.filters_menu_shared_with_me,
                    CoreUiR.drawable.ic_share,
                )
            is HomeDisplayViewModel.OwnedByMe ->
                showScreenTitleWithStartIcon(
                    LocalizationR.string.filters_menu_owned_by_me,
                    CoreUiR.drawable.ic_person,
                )
            is HomeDisplayViewModel.Folders ->
                showScreenTitleWithStartIcon(
                    LocalizationR.string.filters_menu_folders,
                    CoreUiR.drawable.ic_folder,
                )
            is HomeDisplayViewModel.Tags ->
                showScreenTitleWithStartIcon(
                    LocalizationR.string.filters_menu_tags,
                    CoreUiR.drawable.ic_tag,
                )
            is HomeDisplayViewModel.Groups ->
                showScreenTitleWithStartIcon(
                    LocalizationR.string.filters_menu_groups,
                    CoreUiR.drawable.ic_group,
                )
            HomeDisplayViewModel.Expiry ->
                showScreenTitleWithStartIcon(
                    LocalizationR.string.filters_menu_expiry,
                    CoreUiR.drawable.ic_calendar_clock,
                )
        }
    }

    override fun showChildFolderTitle(
        activeFolderName: String,
        isShared: Boolean,
    ) {
        showScreenTitleWithStartIcon(
            activeFolderName,
            if (isShared) CoreUiR.drawable.ic_shared_folder else CoreUiR.drawable.ic_folder,
        )
    }

    override fun showTagTitle(
        activeTagTitle: String,
        isShared: Boolean,
    ) {
        showScreenTitleWithStartIcon(
            activeTagTitle,
            if (isShared) CoreUiR.drawable.ic_shared_tag else CoreUiR.drawable.ic_tag,
        )
    }

    override fun showGroupTitle(groupName: String) {
        showScreenTitleWithStartIcon(groupName, CoreUiR.drawable.ic_group)
    }

    private fun showScreenTitleWithStartIcon(
        @StringRes titleRes: Int,
        @DrawableRes iconRes: Int,
    ) {
        showScreenTitleWithStartIcon(getString(titleRes), iconRes)
    }

    private fun showScreenTitleWithStartIcon(
        title: String,
        @DrawableRes iconRes: Int,
    ) {
        with(requiredBinding) {
            screenTitleLabel.text = title
            titleDrawable.setImageResource(iconRes)
        }
    }

    override fun navigateToChild(homeView: HomeDisplayViewModel) {
        navController.navigate(
            HomeFragmentDirections.actionHomeToHomeChild(homeView),
        )
    }

    override fun navigateToRootHomeFromChildHome(homeView: HomeDisplayViewModel) {
        navController.navigate(
            HomeFragmentDirections.actionHomeChildToHome(homeView),
        )
    }

    override fun navigateRootHomeFromRootHome(homeView: HomeDisplayViewModel) {
        navController.navigate(
            HomeFragmentDirections.actionHomeToHome(homeView),
        )
    }

    override fun showBackArrow() {
        requiredBinding.backButton.visible()
    }

    override fun hideBackArrow() {
        requiredBinding.backButton.gone()
    }

    override fun navigateToCreateResource(parentFolderId: String?) {
        findNavController().navigate(
            HomeFragmentDirections.actionHomeToResourceForm(
                ResourceFormMode.Create(
                    LeadingContentType.PASSWORD,
                    parentFolderId,
                ),
            ),
        )
    }

    override fun navigateToCreateTotpManually(parentFolderId: String?) {
        findNavController().navigate(
            HomeFragmentDirections.actionHomeToResourceForm(
                ResourceFormMode.Create(
                    LeadingContentType.TOTP,
                    parentFolderId,
                ),
            ),
        )
    }

    override fun finish() {
        requireActivity().finish()
    }

    override fun showToggleFavouriteFailure() {
        showSnackbar(
            LocalizationR.string.favourites_failure,
            anchorView = snackbarAnchorView,
            backgroundColor = CoreUiR.color.red,
        )
    }

    override fun showDeleteResourceFailure() {
        showSnackbar(
            LocalizationR.string.delete_failure,
            anchorView = snackbarAnchorView,
            backgroundColor = CoreUiR.color.red,
        )
    }

    override fun showFolderMoreMenuIcon() {
        requiredBinding.moreButton.visible()
    }

    override fun hideFolderMoreMenuIcon() {
        requiredBinding.moreButton.gone()
    }

    override fun menuSeeFolderDetailsClick() {
        presenter.seeFolderDetailsClick()
    }

    override fun navigateToFolderMoreMenu(folderMoreMenuModel: FolderMoreMenuModel) {
        FolderMoreMenuFragment
            .newInstance(folderMoreMenuModel)
            .show(childFragmentManager, FolderMoreMenuModel::class.java.name)
    }

    override fun navigateToFolderDetails(childFolder: Folder.Child) {
        findNavController().navigate(
            NavDeepLinkProvider.folderDetailsDeepLinkRequest(childFolder.folderId),
        )
    }

    override fun navigateToCreateFolder(folderId: String?) {
        findNavController().navigate(
            NavDeepLinkProvider.createFolderDeepLinkRequest(folderId),
        )
    }

    override fun showFolderCreated(name: String) {
        showSnackbar(
            messageResId = LocalizationR.string.common_message_folder_created,
            messageArgs = arrayOf(name),
            anchorView = snackbarAnchorView,
            backgroundColor = CoreUiR.color.green,
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
            length = Snackbar.LENGTH_LONG,
        )
    }

    override fun showEncryptionError(message: String) {
        showSnackbar(
            LocalizationR.string.common_encryption_failure,
            backgroundColor = CoreUiR.color.red,
        )
    }

    override fun showProgress() {
        showProgressDialog(childFragmentManager)
    }

    override fun hideProgress() {
        hideProgressDialog(childFragmentManager)
    }

    override fun navigateToScanTotp(parentFolderId: String?) {
        findNavController().navigate(
            HomeFragmentDirections.actionHomeToScanOtp(parentFolderId, ScanOtpMode.SCAN_WITH_SUCCESS_SCREEN),
        )
    }

    override fun showJsonResourceSchemaValidationError() {
        showSnackbar(
            LocalizationR.string.common_json_schema_resource_validation_error,
            backgroundColor = CoreUiR.color.red,
        )
    }

    override fun showJsonSecretSchemaValidationError() {
        showSnackbar(
            LocalizationR.string.common_json_schema_secret_validation_error,
            backgroundColor = CoreUiR.color.red,
        )
    }

    companion object {
        private val AVATAR_SIZE = 30.px
    }
}
