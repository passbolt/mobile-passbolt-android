package com.passbolt.mobile.android.feature.home.screen

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.passbolt.mobile.android.core.localization.R
import com.passbolt.mobile.android.core.resources.resourceicon.ResourceIconProvider
import com.passbolt.mobile.android.feature.home.screen.HomeIntent.OpenResourceMenu
import com.passbolt.mobile.android.feature.home.screen.list.FolderItem
import com.passbolt.mobile.android.feature.home.screen.list.GroupItem
import com.passbolt.mobile.android.feature.home.screen.list.HeaderItem
import com.passbolt.mobile.android.feature.home.screen.list.ResourceItem
import com.passbolt.mobile.android.feature.home.screen.list.TagItem
import com.passbolt.mobile.android.ui.Folder.Child
import com.passbolt.mobile.android.ui.HomeDisplayViewModel.Folders
import com.passbolt.mobile.android.ui.HomeDisplayViewModel.Groups
import com.passbolt.mobile.android.ui.HomeDisplayViewModel.Tags
import org.koin.compose.koinInject

@Composable
fun HomeResourceList(
    state: HomeState,
    homeNavigation: HomeNavigation,
    onIntent: (HomeIntent) -> Unit,
    modifier: Modifier = Modifier,
    resourceIconProvider: ResourceIconProvider = koinInject(),
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 16.dp),
    ) {
        // suggested
        if (state.homeData.headerSectionConfiguration.isSuggestedSectionVisible) {
            item { HeaderItem(stringResource(R.string.suggested)) }
            items(state.homeData.data.suggestedResourceList) { resource ->
                ResourceItem(
                    resource = resource,
                    resourceIconProvider = resourceIconProvider,
                    onItemClick = { homeNavigation.resourceHandlingStrategy.resourceItemClick(resource) },
                    onMoreClick = { onIntent(OpenResourceMenu(resource)) },
                    showMoreMenu = homeNavigation.resourceHandlingStrategy.shouldShowResourceMoreMenu(),
                )
            }
        }

        // other items header
        if (state.homeData.headerSectionConfiguration.isOtherItemsSectionVisible) {
            item { HeaderItem(stringResource(R.string.other)) }
        }

        // in current folder header
        if (state.homeData.headerSectionConfiguration.isInCurrentFolderSectionVisible) {
            item {
                HeaderItem(
                    stringResource(
                        R.string.home_in_current_folder,
                        state.homeData.headerSectionConfiguration.currentFolderName
                            ?: stringResource(R.string.folder_root),
                    ),
                )
            }
        }

        // folders
        items(state.homeData.data.foldersList) { folder ->
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

        // tags
        items(state.homeData.data.tagsList) { tag ->
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

        // groups
        items(state.homeData.data.groupsList) { group ->
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

        // resources
        items(state.homeData.data.resourceList) { resource ->
            ResourceItem(
                resource = resource,
                resourceIconProvider = resourceIconProvider,
                onItemClick = { homeNavigation.resourceHandlingStrategy.resourceItemClick(resource) },
                onMoreClick = { onIntent(OpenResourceMenu(resource)) },
                showMoreMenu = homeNavigation.resourceHandlingStrategy.shouldShowResourceMoreMenu(),
            )
        }

        // in subfolders
        if (state.homeData.headerSectionConfiguration.isInSubFoldersSectionVisible) {
            item { HeaderItem(stringResource(R.string.home_in_sub_folders)) }
            items(state.homeData.data.filteredSubFolders) { folder ->
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
            items(state.homeData.data.filteredSubFolderResources) { resource ->
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
