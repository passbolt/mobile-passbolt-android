package com.passbolt.mobile.android.mappers

import com.passbolt.mobile.android.entity.home.HomeDisplayView
import com.passbolt.mobile.android.entity.resource.Permission
import com.passbolt.mobile.android.entity.resource.ResourceDatabaseView
import com.passbolt.mobile.android.feature.home.screen.model.HomeDisplayViewModel

/**
 * Mapper responsible for mapping between UI related resource display view type and database related
 * ordering and filtering types.
 */
class HomeDisplayViewMapper {

    /**
     * @param homeView UI related resources display view type
     * @return Database related type for order or filter
     */
    fun map(homeView: HomeDisplayViewModel) =
        when (homeView) {
            is HomeDisplayViewModel.AllItems -> ResourceDatabaseView.ByNameAscending
            is HomeDisplayViewModel.RecentlyModified -> ResourceDatabaseView.ByModifiedDateDescending
            is HomeDisplayViewModel.Favourites -> ResourceDatabaseView.IsFavourite
            is HomeDisplayViewModel.OwnedByMe -> ResourceDatabaseView.HasPermissions(setOf(Permission.OWNER))
            is HomeDisplayViewModel.SharedWithMe -> ResourceDatabaseView.HasPermissions(
                setOf(Permission.READ, Permission.WRITE)
            )
            is HomeDisplayViewModel.Folders -> ResourceDatabaseView.ByModifiedDateDescending
            is HomeDisplayViewModel.Tags -> ResourceDatabaseView.ByModifiedDateDescending
            is HomeDisplayViewModel.Groups -> ResourceDatabaseView.ByModifiedDateDescending
        }

    fun map(homeView: HomeDisplayView): HomeDisplayViewModel =
        when (homeView) {
            HomeDisplayView.ALL_ITEMS -> HomeDisplayViewModel.AllItems
            HomeDisplayView.FAVOURITES -> HomeDisplayViewModel.Favourites
            HomeDisplayView.RECENTLY_MODIFIED -> HomeDisplayViewModel.RecentlyModified
            HomeDisplayView.SHARED_WITH_ME -> HomeDisplayViewModel.SharedWithMe
            HomeDisplayView.OWNED_BY_ME -> HomeDisplayViewModel.OwnedByMe
            HomeDisplayView.FOLDERS -> HomeDisplayViewModel.folderRoot()
            HomeDisplayView.TAGS -> HomeDisplayViewModel.tagsRoot()
            HomeDisplayView.GROUPS -> HomeDisplayViewModel.groupsRoot()
        }
}
