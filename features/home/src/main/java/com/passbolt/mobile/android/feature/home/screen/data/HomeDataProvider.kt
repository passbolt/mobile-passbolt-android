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
package com.passbolt.mobile.android.feature.home.screen.data

import com.passbolt.mobile.android.common.extension.areListsEmpty
import com.passbolt.mobile.android.core.autofill.urlmatcher.AutofillUriMatcher
import com.passbolt.mobile.android.core.commonfolders.usecase.db.GetLocalResourcesAndFoldersUseCase
import com.passbolt.mobile.android.core.commonfolders.usecase.db.GetLocalSubFolderResourcesFilteredUseCase
import com.passbolt.mobile.android.core.commonfolders.usecase.db.GetLocalSubFoldersForFolderUseCase
import com.passbolt.mobile.android.core.commongroups.usecase.db.GetLocalGroupsWithShareItemsCountUseCase
import com.passbolt.mobile.android.core.rbac.usecase.GetRbacRulesUseCase
import com.passbolt.mobile.android.core.resources.usecase.db.GetLocalResourcesUseCase
import com.passbolt.mobile.android.core.resources.usecase.db.GetLocalResourcesWithGroupUseCase
import com.passbolt.mobile.android.core.resources.usecase.db.GetLocalResourcesWithTagUseCase
import com.passbolt.mobile.android.core.tags.usecase.db.GetLocalTagsUseCase
import com.passbolt.mobile.android.feature.home.screen.ShowSuggestedModel
import com.passbolt.mobile.android.feature.home.screen.data.HeaderSectionConfiguration
import com.passbolt.mobile.android.supportedresourceTypes.SupportedContentTypes.homeSlugs
import com.passbolt.mobile.android.ui.Folder
import com.passbolt.mobile.android.ui.HomeDisplayViewModel
import com.passbolt.mobile.android.ui.HomeDisplayViewModel.AllItems
import com.passbolt.mobile.android.ui.HomeDisplayViewModel.Expiry
import com.passbolt.mobile.android.ui.HomeDisplayViewModel.Favourites
import com.passbolt.mobile.android.ui.HomeDisplayViewModel.Folders
import com.passbolt.mobile.android.ui.HomeDisplayViewModel.Groups
import com.passbolt.mobile.android.ui.HomeDisplayViewModel.NotLoaded
import com.passbolt.mobile.android.ui.HomeDisplayViewModel.OwnedByMe
import com.passbolt.mobile.android.ui.HomeDisplayViewModel.RecentlyModified
import com.passbolt.mobile.android.ui.HomeDisplayViewModel.SharedWithMe
import com.passbolt.mobile.android.ui.HomeDisplayViewModel.Tags
import com.passbolt.mobile.android.ui.RbacRuleModel.ALLOW
import com.passbolt.mobile.android.ui.ResourceModel

class HomeDataProvider(
    private val getLocalResourcesUseCase: GetLocalResourcesUseCase,
    private val getLocalTagsUseCase: GetLocalTagsUseCase,
    private val getLocalResourcesWithTagUseCase: GetLocalResourcesWithTagUseCase,
    private val getLocalGroupsWithShareItemsCountUseCase: GetLocalGroupsWithShareItemsCountUseCase,
    private val getLocalResourcesWithGroupsUseCase: GetLocalResourcesWithGroupUseCase,
    private val getLocalResourcesAndFoldersUseCase: GetLocalResourcesAndFoldersUseCase,
    private val getLocalResourcesFiltered: GetLocalSubFolderResourcesFilteredUseCase,
    private val getLocalSubFoldersForFolderUseCase: GetLocalSubFoldersForFolderUseCase,
    private val autofillMatcher: AutofillUriMatcher,
    private val getRbacRulesUseCase: GetRbacRulesUseCase,
) {
    suspend fun provideData(
        searchQuery: String?,
        homeView: HomeDisplayViewModel,
        showSuggestedModel: ShowSuggestedModel,
    ): HomeDataWithHeader {
        val homeData = getHomeData(searchQuery, homeView, showSuggestedModel)
        val headerData = getHeaderConfig(homeData, searchQuery, homeView, showSuggestedModel)
        return HomeDataWithHeader(homeData, headerData)
    }

    private suspend fun getHomeData(
        searchQuery: String?,
        homeView: HomeDisplayViewModel,
        showSuggestedModel: ShowSuggestedModel,
    ): HomeData =
        when (homeView) {
            AllItems,
            Expiry,
            Favourites,
            OwnedByMe,
            RecentlyModified,
            SharedWithMe,
            -> getResourcesHomeData(searchQuery, homeView, showSuggestedModel)

            is Tags -> getTagsHomeData(searchQuery, homeView, showSuggestedModel)
            is Groups -> getGroupsHomeData(searchQuery, homeView, showSuggestedModel)
            is Folders -> getFoldersHomeData(searchQuery, homeView, showSuggestedModel)
            NotLoaded -> HomeData()
        }

    private suspend fun getFoldersHomeData(
        searchQuery: String?,
        foldersView: Folders,
        showSuggestedModel: ShowSuggestedModel,
    ): HomeData {
        if (getRbacRulesUseCase.execute(Unit).rbacModel.foldersUseRule != ALLOW) {
            return HomeData()
        }

        val data =
            getLocalResourcesAndFoldersUseCase.execute(
                GetLocalResourcesAndFoldersUseCase.Input(
                    foldersView.activeFolder,
                    homeSlugs,
                    searchQuery,
                ),
            ) as GetLocalResourcesAndFoldersUseCase.Output.Success

        return if (searchQuery.isNullOrBlank()) {
            HomeData(
                resourceList = data.resources,
                foldersList = data.folders,
            )
        } else {
            // resources need to be shown for all child folders
            val allSubFolders =
                getLocalSubFoldersForFolderUseCase
                    .execute(
                        GetLocalSubFoldersForFolderUseCase.Input(foldersView.activeFolder, searchQuery),
                    ).folders

            val filteredSubFolderResources =
                getLocalResourcesFiltered
                    .execute(
                        GetLocalSubFolderResourcesFilteredUseCase.Input(
                            allSubFolders.map { it.folderId },
                            searchQuery,
                            homeSlugs,
                        ),
                    ).resources

            HomeData(
                resourceList = data.resources,
                foldersList = data.folders,
                filteredSubFolderResources = filteredSubFolderResources,
                filteredSubFolders = allSubFolders,
                suggestedResourceList = getSuggestedList(data.resources, searchQuery, foldersView, showSuggestedModel),
            )
        }
    }

    private suspend fun getGroupsHomeData(
        searchQuery: String?,
        groupsView: Groups,
        showSuggestedModel: ShowSuggestedModel,
    ): HomeData {
        val groups =
            getLocalGroupsWithShareItemsCountUseCase.execute(
                GetLocalGroupsWithShareItemsCountUseCase.Input(searchQuery),
            )
        return if (groupsView.activeGroupId == null) { // groups root - list of groups
            HomeData(groupsList = groups)
        } else { // resources shared with group
            val groupResources =
                getLocalResourcesWithGroupsUseCase
                    .execute(
                        GetLocalResourcesWithGroupUseCase.Input(
                            groupsView,
                            homeSlugs,
                            searchQuery,
                        ),
                    ).resources

            return HomeData(
                resourceList = groupResources,
                suggestedResourceList = getSuggestedList(groupResources, searchQuery, groupsView, showSuggestedModel),
            )
        }
    }

    private suspend fun getResourcesHomeData(
        searchQuery: String?,
        homeView: HomeDisplayViewModel,
        showSuggestedModel: ShowSuggestedModel,
    ): HomeData {
        val resourceList =
            getLocalResourcesUseCase
                .execute(
                    GetLocalResourcesUseCase.Input(
                        homeSlugs,
                        homeView,
                        searchQuery,
                    ),
                ).resources

        return HomeData(
            resourceList = resourceList,
            suggestedResourceList = getSuggestedList(resourceList, searchQuery, homeView, showSuggestedModel),
        )
    }

    private fun getSuggestedList(
        resourceList: List<ResourceModel>,
        searchQuery: String?,
        homeView: HomeDisplayViewModel,
        showSuggestedModel: ShowSuggestedModel,
    ): List<ResourceModel> =
        if (shouldShowSuggested(homeView, searchQuery)) {
            resourceList
                .filter {
                    val autofillUrl = (showSuggestedModel as? ShowSuggestedModel.Show)?.suggestedUri
                    autofillMatcher.isMatching(autofillUrl, it)
                }
        } else {
            emptyList()
        }

    private suspend fun getTagsHomeData(
        searchQuery: String?,
        tagsView: Tags,
        showSuggestedModel: ShowSuggestedModel,
    ): HomeData {
        if (getRbacRulesUseCase.execute(Unit).rbacModel.tagsUseRule != ALLOW) {
            return HomeData()
        }

        val tags = getLocalTagsUseCase.execute(GetLocalTagsUseCase.Input(searchQuery))
        return if (tagsView.activeTagId == null) { // tags root - list of tags
            HomeData(tagsList = tags)
        } else { // resources with active tag
            val taggedResources =
                getLocalResourcesWithTagUseCase
                    .execute(
                        GetLocalResourcesWithTagUseCase.Input(
                            tagsView,
                            homeSlugs,
                            searchQuery,
                        ),
                    ).resources
            HomeData(
                resourceList = taggedResources,
                suggestedResourceList = getSuggestedList(taggedResources, searchQuery, tagsView, showSuggestedModel),
            )
        }
    }

    private fun getHeaderConfig(
        homeData: HomeData,
        searchQuery: String?,
        homeView: HomeDisplayViewModel,
        showSuggestedModel: ShowSuggestedModel,
    ): HeaderSectionConfiguration =
        if (searchQuery.isNullOrBlank()) {
            HeaderSectionConfiguration(
                isInCurrentFolderSectionVisible = false,
                isInSubFoldersSectionVisible = false,
                isOtherItemsSectionVisible =
                    !areListsEmpty(
                        homeData.resourceList,
                        homeData.foldersList,
                        homeData.tagsList,
                        homeData.groupsList,
                        homeData.filteredSubFolders,
                        homeData.filteredSubFolderResources,
                    ) &&
                        showSuggestedModel is ShowSuggestedModel.Show,
                isSuggestedSectionVisible = homeData.suggestedResourceList.isNotEmpty(),
            )
        } else {
            HeaderSectionConfiguration(
                isInCurrentFolderSectionVisible =
                    homeView is Folders &&
                        !areListsEmpty(
                            homeData.resourceList,
                            homeData.foldersList,
                        ),
                isInSubFoldersSectionVisible =
                    homeView is Folders &&
                        !areListsEmpty(
                            homeData.filteredSubFolderResources,
                            homeData.filteredSubFolders,
                        ),
                (homeView as? Folders)?.activeFolderName,
                isSuggestedSectionVisible = false,
                isOtherItemsSectionVisible = false,
            )
        }

    private fun shouldShowSuggested(
        homeView: HomeDisplayViewModel,
        searchQuery: String?,
    ) = if (searchQuery.isNullOrBlank()) {
        when (val activeHomeView = homeView) {
            is AllItems -> true
            is Favourites -> true
            is Folders ->
                when (activeHomeView.activeFolder) {
                    is Folder.Child -> false
                    is Folder.Root -> true
                }
            is Groups -> activeHomeView.activeGroupId == null // groups root
            is OwnedByMe -> true
            is RecentlyModified -> true
            is SharedWithMe -> true
            is Tags -> activeHomeView.activeTagId == null // tags root
            is Expiry -> true
            NotLoaded -> false
        }
    } else {
        false
    }
}
