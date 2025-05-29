package com.passbolt.mobile.android.ui

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class ResourceMoreMenuModel(
    val title: String,
    val canCopy: Boolean,
    val canDelete: Boolean,
    val canEdit: Boolean,
    val canShare: Boolean,
    val favouriteOption: FavouriteOption,
    val descriptionOptions: List<DescriptionOption>,
) : Parcelable {
    enum class FavouriteOption {
        ADD_TO_FAVOURITES,
        REMOVE_FROM_FAVOURITES,
    }

    enum class DescriptionOption {
        HAS_NOTE,
        HAS_METADATA_DESCRIPTION,
    }
}
