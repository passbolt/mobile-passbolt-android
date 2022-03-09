package com.passbolt.mobile.android.mappers

import com.passbolt.mobile.android.entity.resource.Permission
import com.passbolt.mobile.android.entity.resource.ResourceDatabaseView
import com.passbolt.mobile.android.ui.ResourcesDisplayView

/**
 * Mapper responsible for mapping between UI related resource display view type and database related
 * ordering and filtering types.
 */
class ResourceDisplayViewMapper {

    /**
     * @param resourcesFilter UI related resources display view type
     * @return Database related type for order or filter
     */
    fun map(resourcesFilter: ResourcesDisplayView) =
        when (resourcesFilter) {
            ResourcesDisplayView.ALL -> ResourceDatabaseView.ByNameAscending
            ResourcesDisplayView.RECENTLY_MODIFIED -> ResourceDatabaseView.ByModifiedDateDescending
            ResourcesDisplayView.FAVOURITES -> ResourceDatabaseView.IsFavourite
            ResourcesDisplayView.OWNED_BY_ME -> ResourceDatabaseView.HasPermissions(setOf(Permission.OWNER))
            ResourcesDisplayView.SHARED_WITH_ME -> ResourceDatabaseView.HasPermissions(
                setOf(Permission.READ, Permission.WRITE)
            )
            ResourcesDisplayView.FOLDERS -> ResourceDatabaseView.ByModifiedDateDescending
        }
}
