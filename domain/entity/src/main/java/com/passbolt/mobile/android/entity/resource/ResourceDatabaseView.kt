package com.passbolt.mobile.android.entity.resource

/**
 * Objects representing different filtering and sorting options for database queries
 */
sealed class ResourceDatabaseView {

    /**
     * Ordering by resource name alphabetically
     */
    data object ByNameAscending : ResourceDatabaseView()

    /**
     * Ordering by the recently modified field from most recent one
     */
    data object ByModifiedDateDescending : ResourceDatabaseView()

    /**
     * Filters the resources to favourite ones
     */
    data object IsFavourite : ResourceDatabaseView()

    /**
     * Filters the resource to ones that have permissions defined in the list
     * @param permissions permissions used for filtering
     */
    data class HasPermissions(val permissions: Set<Permission>) : ResourceDatabaseView()
}
