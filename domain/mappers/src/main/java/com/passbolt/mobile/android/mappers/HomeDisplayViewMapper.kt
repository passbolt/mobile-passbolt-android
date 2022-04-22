package com.passbolt.mobile.android.mappers

import com.passbolt.mobile.android.entity.resource.Permission
import com.passbolt.mobile.android.entity.resource.ResourceDatabaseView
import com.passbolt.mobile.android.feature.home.screen.model.HomeDisplayView

/**
 * Mapper responsible for mapping between UI related resource display view type and database related
 * ordering and filtering types.
 */
class HomeDisplayViewMapper {

    /**
     * @param homeView UI related resources display view type
     * @return Database related type for order or filter
     */
    fun map(homeView: HomeDisplayView) =
        when (homeView) {
            is HomeDisplayView.AllItems -> ResourceDatabaseView.ByNameAscending
            is HomeDisplayView.RecentlyModified -> ResourceDatabaseView.ByModifiedDateDescending
            is HomeDisplayView.Favourites -> ResourceDatabaseView.IsFavourite
            is HomeDisplayView.OwnedByMe -> ResourceDatabaseView.HasPermissions(setOf(Permission.OWNER))
            is HomeDisplayView.SharedWithMe -> ResourceDatabaseView.HasPermissions(
                setOf(Permission.READ, Permission.WRITE)
            )
            is HomeDisplayView.Folders -> ResourceDatabaseView.ByModifiedDateDescending
            is HomeDisplayView.Tags -> ResourceDatabaseView.ByModifiedDateDescending
            is HomeDisplayView.Groups -> ResourceDatabaseView.ByModifiedDateDescending
        }
}
