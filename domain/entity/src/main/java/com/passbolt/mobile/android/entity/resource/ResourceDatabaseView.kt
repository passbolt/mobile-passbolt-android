package com.passbolt.mobile.android.entity.resource

/**
 * Objects representing different filtering and sorting options for database queries
 */
sealed class ResourceDatabaseView {

    /**
     * Ordering by resource name alphabetically
     */
    object ByNameAscending : ResourceDatabaseView()

    /**
     * Ordering by the recently modified field from most recent one
     */
    object ByModifiedDateDescending : ResourceDatabaseView()

    /**
     * Filters the resources to favourite ones
     */
    object IsFavourite : ResourceDatabaseView()
}
