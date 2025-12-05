package com.passbolt.mobile.android.feature.home.screen

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.passbolt.mobile.android.core.localization.R
import com.passbolt.mobile.android.core.resources.resourceicon.ResourceIconProvider
import com.passbolt.mobile.android.core.ui.compose.empty.EmptyResourceListState
import com.passbolt.mobile.android.feature.home.screen.HomeIntent.OpenResourceMenu
import com.passbolt.mobile.android.feature.home.screen.data.HeaderSectionConfiguration
import com.passbolt.mobile.android.feature.home.screen.list.FolderItem
import com.passbolt.mobile.android.feature.home.screen.list.GroupItem
import com.passbolt.mobile.android.feature.home.screen.list.HeaderItem
import com.passbolt.mobile.android.feature.home.screen.list.ResourceItem
import com.passbolt.mobile.android.feature.home.screen.list.TagItem
import com.passbolt.mobile.android.ui.Folder.Child
import com.passbolt.mobile.android.ui.FolderWithCountAndPath
import com.passbolt.mobile.android.ui.GroupWithCount
import com.passbolt.mobile.android.ui.HomeDisplayViewModel.Folders
import com.passbolt.mobile.android.ui.HomeDisplayViewModel.Groups
import com.passbolt.mobile.android.ui.HomeDisplayViewModel.Tags
import com.passbolt.mobile.android.ui.ResourceModel
import com.passbolt.mobile.android.ui.TagWithCount
import org.koin.compose.koinInject
import com.passbolt.mobile.android.core.localization.R as LocalizationR

@Suppress("CyclomaticComplexMethod")
@Composable
fun HomeResourceList(
    state: HomeState,
    homeNavigation: HomeNavigation,
    onIntent: (HomeIntent) -> Unit,
    modifier: Modifier = Modifier,
    resourceIconProvider: ResourceIconProvider = koinInject(),
) {
    val homeListData = rememberHomeListData(state)
    val headerConfig = rememberHeaderConfig(state, homeListData)
    val listState = rememberLazyListState()

    // Auto-scroll to top when suggested section is visible and first items load
    // resources and suggested are emitted at the same time - there can be race condition that makes list scrolled to resources
    // and then suggested section appears above resources section
    LaunchedEffect(headerConfig.isSuggestedSectionVisible) {
        if (headerConfig.isSuggestedSectionVisible) {
            listState.scrollToItem(0)
        }
    }

    if (headerConfig.areAllSectionsEmpty) {
        EmptyResourceListState(title = stringResource(LocalizationR.string.no_passwords))
    } else {
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = 16.dp),
            state = listState,
        ) {
            // suggested
            if (headerConfig.isSuggestedSectionVisible) {
                item { HeaderItem(stringResource(R.string.suggested)) }
                items(
                    count = homeListData.suggestedResources.itemCount,
                    key = homeListData.suggestedResources.itemKey { "suggested_${it.resourceId}" },
                ) { index ->
                    homeListData.suggestedResources[index]?.let { resource ->
                        ResourceItem(
                            resource = resource,
                            resourceIconProvider = resourceIconProvider,
                            onItemClick = { homeNavigation.resourceHandlingStrategy.resourceItemClick(resource) },
                            onMoreClick = { onIntent(OpenResourceMenu(resource)) },
                            showMoreMenu = homeNavigation.resourceHandlingStrategy.shouldShowResourceMoreMenu(),
                        )
                    }
                }
            }

            // other items header
            if (headerConfig.isOtherItemsSectionVisible) {
                item { HeaderItem(stringResource(R.string.other)) }
            }

            // in current folder header
            if (headerConfig.isInCurrentFolderSectionVisible) {
                item {
                    HeaderItem(
                        stringResource(
                            R.string.home_in_current_folder,
                            headerConfig.currentFolderName
                                ?: stringResource(R.string.folder_root),
                        ),
                    )
                }
            }

            // folders
            items(
                count = homeListData.folders.itemCount,
                key = homeListData.folders.itemKey { "folder_${it.folderId}" },
            ) { folder ->
                homeListData.folders[folder]?.let { folder ->
                    FolderItem(
                        folder = folder,
                        onFolderClick = {
                            Folders(
                                activeFolder = Child(folder.folderId),
                                activeFolderName = folder.name,
                                isActiveFolderShared = folder.isShared,
                            ).let {
                                homeNavigation.navigateToChild(it)
                            }
                        },
                    )
                }
            }

            // tags
            items(
                count = homeListData.tags.itemCount,
                key = homeListData.tags.itemKey { "tag_${it.id}" },
            ) { tag ->
                homeListData.tags[tag]?.let { tag ->
                    TagItem(
                        tag = tag,
                        onClick = {
                            Tags(
                                activeTagId = tag.id,
                                activeTagName = tag.slug,
                                isActiveTagShared = tag.isShared,
                            ).let {
                                homeNavigation.navigateToChild(it)
                            }
                        },
                    )
                }
            }

            // groups
            items(
                count = homeListData.groups.itemCount,
                key = homeListData.groups.itemKey { "group_${it.groupId}" },
            ) { group ->
                homeListData.groups[group]?.let { group ->
                    GroupItem(
                        group = group,
                        onClick = {
                            Groups(
                                activeGroupId = group.groupId,
                                activeGroupName = group.groupName,
                            ).let {
                                homeNavigation.navigateToChild(it)
                            }
                        },
                    )
                }
            }

            // resources
            items(
                count = homeListData.resources.itemCount,
                key = homeListData.resources.itemKey { "resource_${it.resourceId}" },
            ) { index ->
                homeListData.resources[index]?.let { resource ->
                    ResourceItem(
                        resource = resource,
                        resourceIconProvider = resourceIconProvider,
                        onItemClick = { homeNavigation.resourceHandlingStrategy.resourceItemClick(resource) },
                        onMoreClick = { onIntent(OpenResourceMenu(resource)) },
                        showMoreMenu = homeNavigation.resourceHandlingStrategy.shouldShowResourceMoreMenu(),
                    )
                }
            }

            // in subfolders
            if (headerConfig.isInSubFoldersSectionVisible) {
                item { HeaderItem(stringResource(R.string.home_in_sub_folders)) }
                items(
                    count = homeListData.filteredSubfolders.itemCount,
                    key = homeListData.filteredSubfolders.itemKey { "subfolder_folder_${it.folderId}" },
                ) { folder ->
                    homeListData.filteredSubfolders[folder]?.let { folder ->
                        FolderItem(
                            folder = folder,
                            onFolderClick = {
                                Folders(
                                    activeFolder = Child(folder.folderId),
                                    activeFolderName = folder.name,
                                    isActiveFolderShared = folder.isShared,
                                ).let {
                                    homeNavigation.navigateToChild(it)
                                }
                            },
                        )
                    }
                }
                items(
                    count = homeListData.filteredSubfoldersResources.itemCount,
                    key = homeListData.filteredSubfoldersResources.itemKey { "subfolder_resource_${it.resourceId}" },
                ) { resource ->
                    homeListData.filteredSubfoldersResources[resource]?.let { resource ->
                        ResourceItem(
                            resource = resource,
                            resourceIconProvider = resourceIconProvider,
                            onItemClick = { homeNavigation.resourceHandlingStrategy.resourceItemClick(resource) },
                            onMoreClick = { onIntent(OpenResourceMenu(resource)) },
                            showMoreMenu = homeNavigation.resourceHandlingStrategy.shouldShowResourceMoreMenu(),
                        )
                    }
                }
            }
        }
    }
}

private data class HomeListData(
    val suggestedResources: LazyPagingItems<ResourceModel>,
    val resources: LazyPagingItems<ResourceModel>,
    val tags: LazyPagingItems<TagWithCount>,
    val groups: LazyPagingItems<GroupWithCount>,
    val folders: LazyPagingItems<FolderWithCountAndPath>,
    val filteredSubfolders: LazyPagingItems<FolderWithCountAndPath>,
    val filteredSubfoldersResources: LazyPagingItems<ResourceModel>,
)

@Composable
private fun rememberHomeListData(state: HomeState): HomeListData {
    val suggestedResources = state.homeData.suggestedResourceList.collectAsLazyPagingItems()
    val resources = state.homeData.resourceList.collectAsLazyPagingItems()
    val tags = state.homeData.tagsList.collectAsLazyPagingItems()
    val groups = state.homeData.groupsList.collectAsLazyPagingItems()
    val folders = state.homeData.foldersList.collectAsLazyPagingItems()
    val filteredSubfolders = state.homeData.filteredSubFolders.collectAsLazyPagingItems()
    val filteredSubfoldersResources = state.homeData.filteredSubFolderResources.collectAsLazyPagingItems()

    return remember(
        suggestedResources,
        resources,
        tags,
        groups,
        folders,
        filteredSubfolders,
        filteredSubfoldersResources,
    ) {
        HomeListData(
            suggestedResources = suggestedResources,
            resources = resources,
            tags = tags,
            groups = groups,
            folders = folders,
            filteredSubfolders = filteredSubfolders,
            filteredSubfoldersResources = filteredSubfoldersResources,
        )
    }
}

@Composable
private fun rememberHeaderConfig(
    state: HomeState,
    homeListData: HomeListData,
): HeaderSectionConfiguration {
    val headerConfig by remember(
        homeListData.resources.itemSnapshotList.isEmpty(),
        homeListData.folders.itemSnapshotList.isEmpty(),
        homeListData.tags.itemSnapshotList.isEmpty(),
        homeListData.groups.itemSnapshotList.isEmpty(),
        homeListData.filteredSubfolders.itemSnapshotList.isEmpty(),
        homeListData.filteredSubfoldersResources.itemSnapshotList.isEmpty(),
        homeListData.suggestedResources.itemSnapshotList.isEmpty(),
        state.searchQuery,
        state.homeView,
        state.showSuggestedModel,
    ) {
        derivedStateOf {
            getHeaderConfig(
                resources = homeListData.resources,
                folders = homeListData.folders,
                tags = homeListData.tags,
                groups = homeListData.groups,
                filteredSubfolders = homeListData.filteredSubfolders,
                filteredSubfoldersResources = homeListData.filteredSubfoldersResources,
                suggestedResources = homeListData.suggestedResources,
                searchQuery = state.searchQuery,
                homeView = state.homeView,
                showSuggestedModel = state.showSuggestedModel,
            )
        }
    }
    return headerConfig
}
