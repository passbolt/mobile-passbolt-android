package com.passbolt.mobile.android.ui

data class ResourceMoreMenuModel(
    val title: String,
    val canCopy: Boolean,
    val canDelete: Boolean,
    val canEdit: Boolean,
    val canShare: Boolean,
    val favouriteOption: FavouriteOption,
    val descriptionOptions: List<DescriptionOption>,
    // null when offline mode is off or every entry is cached anyway
    val offlineOption: OfflineOption? = null,
) {
    enum class FavouriteOption {
        ADD_TO_FAVOURITES,
        REMOVE_FROM_FAVOURITES,
    }

    enum class OfflineOption {
        MAKE_AVAILABLE_OFFLINE,
        REMOVE_OFFLINE_AVAILABILITY,
    }

    enum class DescriptionOption {
        HAS_NOTE,
        HAS_METADATA_DESCRIPTION,
    }
}
