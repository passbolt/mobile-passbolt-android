package com.passbolt.mobile.android.feature.home.navigation

import PassboltTheme
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation.fragment.findNavController
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSavedStateNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import androidx.navigation3.ui.rememberSceneSetupNavEntryDecorator
import com.passbolt.mobile.android.common.lifecycleawarelazy.lifecycleAwareLazy
import com.passbolt.mobile.android.core.navigation.AppContext
import com.passbolt.mobile.android.core.navigation.constants.Autofillresources
import com.passbolt.mobile.android.core.navigation.deeplinks.NavDeepLinkProvider
import com.passbolt.mobile.android.core.preferences.usecase.GetHomeDisplayViewPrefsUseCase
import com.passbolt.mobile.android.createfolder.CreateFolderFragment
import com.passbolt.mobile.android.feature.home.filtersmenu.FiltersMenuFragment
import com.passbolt.mobile.android.feature.home.screen.HomeIntent
import com.passbolt.mobile.android.feature.home.screen.HomeIntent.CopyNote
import com.passbolt.mobile.android.feature.home.screen.HomeIntent.CopyPassword
import com.passbolt.mobile.android.feature.home.screen.HomeIntent.CopyResourceMetadataDescription
import com.passbolt.mobile.android.feature.home.screen.HomeIntent.CopyResourceUri
import com.passbolt.mobile.android.feature.home.screen.HomeIntent.CopyResourceUsername
import com.passbolt.mobile.android.feature.home.screen.HomeIntent.DeleteResource
import com.passbolt.mobile.android.feature.home.screen.HomeIntent.EditResource
import com.passbolt.mobile.android.feature.home.screen.HomeIntent.LaunchResourceWebsite
import com.passbolt.mobile.android.feature.home.screen.HomeIntent.OtpQRScanReturned
import com.passbolt.mobile.android.feature.home.screen.HomeIntent.ResourceFormReturned
import com.passbolt.mobile.android.feature.home.screen.HomeIntent.ShareResource
import com.passbolt.mobile.android.feature.home.screen.HomeNavigation
import com.passbolt.mobile.android.feature.home.screen.HomeScreen
import com.passbolt.mobile.android.feature.home.screen.HomeViewModel
import com.passbolt.mobile.android.feature.home.screen.ResourceHandlingStrategy
import com.passbolt.mobile.android.feature.home.screen.ShowSuggestedModel
import com.passbolt.mobile.android.feature.otp.scanotp.ScanOtpFragment
import com.passbolt.mobile.android.feature.otp.scanotp.ScanOtpMode
import com.passbolt.mobile.android.feature.otp.scanotp.scanotpsuccess.ScanOtpSuccessFragment
import com.passbolt.mobile.android.feature.resourcedetails.details.ResourceDetailsFragment
import com.passbolt.mobile.android.feature.resourceform.main.ResourceFormFragment
import com.passbolt.mobile.android.mappers.HomeDisplayViewMapper
import com.passbolt.mobile.android.moremenu.FolderMoreMenuFragment
import com.passbolt.mobile.android.permissions.permissions.PermissionsFragment
import com.passbolt.mobile.android.permissions.permissions.PermissionsItem
import com.passbolt.mobile.android.permissions.permissions.PermissionsMode
import com.passbolt.mobile.android.resourcemoremenu.ResourceMoreMenuFragment
import com.passbolt.mobile.android.ui.FiltersMenuModel
import com.passbolt.mobile.android.ui.Folder
import com.passbolt.mobile.android.ui.FolderMoreMenuModel
import com.passbolt.mobile.android.ui.HomeDisplayViewModel
import com.passbolt.mobile.android.ui.LeadingContentType
import com.passbolt.mobile.android.ui.ResourceFormMode
import com.passbolt.mobile.android.ui.ResourceModel
import com.passbolt.mobile.android.ui.ResourceMoreMenuModel
import org.koin.android.ext.android.inject
import org.koin.androidx.compose.koinViewModel

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
class HomeBottomNavigationContainerFragment :
    Fragment(),
    HomeNavigation,
    ResourceHandlingStrategy,
    FiltersMenuFragment.Listener,
    ResourceMoreMenuFragment.Listener {
    private val otpScanQrReturned = { _: String, result: Bundle ->
        viewModel.onIntent(
            OtpQRScanReturned(
                otpCreated = result.getBoolean(ScanOtpSuccessFragment.EXTRA_OTP_CREATED, false),
                otpManualCreationChosen = result.getBoolean(ScanOtpFragment.EXTRA_MANUAL_CREATION_CHOSEN),
            ),
        )
    }

    private val resourceFormReturned = { _: String, result: Bundle ->
        val resourceCreated = result.getBoolean(ResourceFormFragment.EXTRA_RESOURCE_CREATED, false)
        viewModel.onIntent(
            ResourceFormReturned(
                resourceCreated,
                result.getBoolean(ResourceFormFragment.EXTRA_RESOURCE_EDITED, false),
                result.getString(ResourceFormFragment.EXTRA_RESOURCE_NAME),
            ),
        )
    }

    private val detailsReturned = { _: String, result: Bundle ->
        viewModel.onIntent(
            HomeIntent.ResourceDetailsReturned(
                result.getBoolean(ResourceDetailsFragment.EXTRA_RESOURCE_EDITED, false),
                result.getBoolean(ResourceDetailsFragment.EXTRA_RESOURCE_DELETED, false),
                result.getString(ResourceDetailsFragment.EXTRA_RESOURCE_NAME),
            ),
        )
    }

    private val shareReturned = { _: String, result: Bundle ->
        viewModel.onIntent(
            HomeIntent.ResourceShareReturned(
                result.getBoolean(PermissionsFragment.EXTRA_RESOURCE_SHARED, false),
            ),
        )
    }

    private val folderCreatedListener = { _: String, bundle: Bundle ->
        val name = requireNotNull(bundle.getString(CreateFolderFragment.EXTRA_CREATED_FOLDER_NAME))
        viewModel.onIntent(
            HomeIntent.FolderCreateReturned(name),
        )
    }

    private lateinit var viewModel: HomeViewModel

    private val filterPreferencesUseCase: GetHomeDisplayViewPrefsUseCase by inject()
    private val homeDisplayMapper: HomeDisplayViewMapper by inject()

    override val resourceHandlingStrategy: ResourceHandlingStrategy by lifecycleAwareLazy {
        if (requireActivity().javaClass.name == Autofillresources.AUTOFILL_RESOURCES_ACTIVITY) {
            requireActivity() as ResourceHandlingStrategy
        } else {
            this
        }
    }

    lateinit var backstackList: NavBackStack

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val filterPreferences = filterPreferencesUseCase.execute(Unit)
        val initialHomeDisplay =
            homeDisplayMapper.map(
                filterPreferences.userSetHomeView,
                filterPreferences.lastUsedHomeView,
            )

        return ComposeView(requireContext()).apply {
            setContent {
                val backStack =
                    rememberNavBackStack<NavKey>(Home(initialHomeDisplay)).apply {
                        backstackList = this
                    }

                NavDisplay(
                    backStack = backStack,
                    onBack = { backStack.removeLastOrNull() },
                    entryDecorators =
                        listOf(
                            rememberSceneSetupNavEntryDecorator(),
                            rememberSavedStateNavEntryDecorator(),
                            rememberViewModelStoreNavEntryDecorator(),
                        ),
                    entryProvider = { key ->
                        when (key) {
                            is Home ->
                                NavEntry(key) {
                                    viewModel = koinViewModel()
                                    PassboltTheme {
                                        HomeScreen(
                                            navigation = this@HomeBottomNavigationContainerFragment,
                                            showSuggestedModel = resourceHandlingStrategy.showSuggestedModel(),
                                            homeView = key.homeDisplayViewModel,
                                            viewModel = viewModel,
                                        )
                                    }
                                }
                            else -> error("Unsupported home key: $key")
                        }
                    },
                )
            }
        }
    }

    override fun navigateToChild(homeView: HomeDisplayViewModel) {
        backstackList.add(Home(homeView))
    }

    override fun navigateBack() {
        backstackList.removeLastOrNull()
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        setFragmentResultListeners()
    }

    private fun setFragmentResultListeners() {
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
        setFragmentResultListener(
            CreateFolderFragment.REQUEST_CREATE_FOLDER,
            folderCreatedListener,
        )
    }

    override fun navigateToCreateResourceForm(
        leadingContentType: LeadingContentType,
        folderId: String?,
    ) {
        findNavController().navigate(
            HomeBottomNavigationContainerFragmentDirections.actionHomeComposeToResourceForm(
                ResourceFormMode.Create(
                    leadingContentType,
                    parentFolderId = folderId,
                ),
            ),
        )
    }

    override fun navigateToEditResourceForm(
        resourceId: String,
        resourceName: String,
    ) {
        findNavController().navigate(
            HomeBottomNavigationContainerFragmentDirections.actionHomeComposeToResourceForm(
                ResourceFormMode.Edit(
                    resourceId = resourceId,
                    resourceName = resourceName,
                ),
            ),
        )
    }

    override fun navigateToScanOtpCodeForResult(folderId: String?) {
        findNavController().navigate(
            HomeBottomNavigationContainerFragmentDirections.actionHomeComposeToScanOtpFragment(
                ScanOtpMode.SCAN_WITH_SUCCESS_SCREEN,
                parentFolderId = folderId,
            ),
        )
    }

    // resource handling strategy
    override val appContext: AppContext = AppContext.APP

    override fun resourceItemClick(resourceModel: ResourceModel) {
        findNavController().navigate(
            HomeBottomNavigationContainerFragmentDirections.actionHomeComposeToDetails(
                resourceModel,
            ),
        )
    }

    override fun shouldShowResourceMoreMenu() = true

    override fun shouldShowFolderMoreMenu() = true

    override fun showSuggestedModel() = ShowSuggestedModel.DoNotShow

    override fun shouldShowCloseButton() = false

    // filters
    override fun openFiltersBottomSheet(homeView: HomeDisplayViewModel) {
        FiltersMenuFragment
            .newInstance(FiltersMenuModel(homeView))
            .show(childFragmentManager, FiltersMenuFragment::class.java.name)
    }

    override fun filterChanged(filter: HomeDisplayViewModel) {
        backstackList.clear()
        backstackList.add(Home(filter))
    }

    override fun openFolderMoreMenu(homeView: HomeDisplayViewModel) {
        (homeView as? HomeDisplayViewModel.Folders)?.let { folder ->
            if (folder.activeFolder is Folder.Child) {
                val childFolder = folder.activeFolder as Folder.Child
                FolderMoreMenuFragment
                    .newInstance(FolderMoreMenuModel(folder.activeFolderName, childFolder.folderId))
                    .show(childFragmentManager, FolderMoreMenuModel::class.java.name)
            }
        }
    }

    // more menu
    override fun openResourceMoreMenu(
        resourceId: String,
        resourceName: String,
    ) {
        ResourceMoreMenuFragment
            .newInstance(resourceId, resourceName)
            .show(childFragmentManager, ResourceMoreMenuFragment::class.java.name)
    }

    override fun menuCopyPasswordClick() {
        viewModel.onIntent(CopyPassword)
    }

    override fun menuCopyMetadataDescriptionClick() {
        viewModel.onIntent(CopyResourceMetadataDescription)
    }

    override fun menuCopyNoteClick() {
        viewModel.onIntent(CopyNote)
    }

    override fun menuCopyUrlClick() {
        viewModel.onIntent(CopyResourceUri)
    }

    override fun menuCopyUsernameClick() {
        viewModel.onIntent(CopyResourceUsername)
    }

    override fun menuLaunchWebsiteClick() {
        viewModel.onIntent(LaunchResourceWebsite)
    }

    override fun menuDeleteClick() {
        viewModel.onIntent(DeleteResource)
    }

    override fun menuEditClick() {
        viewModel.onIntent(EditResource)
    }

    override fun menuShareClick() {
        viewModel.onIntent(ShareResource)
    }

    override fun menuFavouriteClick(option: ResourceMoreMenuModel.FavouriteOption) {
        viewModel.onIntent(HomeIntent.ToggleResourceFavourite(option))
    }

    override fun resourceMoreMenuDismissed() {
        // no-op
    }

    override fun navigateToShare(resourceId: String) {
        findNavController().navigate(
            HomeBottomNavigationContainerFragmentDirections.actionHomeComposeToResourcePermissions(
                resourceId,
                PermissionsMode.EDIT,
                PermissionsItem.RESOURCE,
            ),
        )
    }

    override fun navigateToCreateFolder(folderId: String?) {
        findNavController().navigate(
            NavDeepLinkProvider.createFolderDeepLinkRequest(folderId),
        )
    }
}
