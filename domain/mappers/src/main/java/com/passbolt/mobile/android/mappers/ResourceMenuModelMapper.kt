package com.passbolt.mobile.android.mappers

import com.passbolt.mobile.android.ui.ResourceModel
import com.passbolt.mobile.android.ui.ResourceMoreMenuModel
import com.passbolt.mobile.android.ui.ResourceMoreMenuModel.FavouriteOption.ADD_TO_FAVOURITES
import com.passbolt.mobile.android.ui.ResourceMoreMenuModel.FavouriteOption.REMOVE_FROM_FAVOURITES
import com.passbolt.mobile.android.ui.ResourcePermission
import com.passbolt.mobile.android.ui.isFavourite

class ResourceMenuModelMapper {

    fun map(resource: ResourceModel) =
        ResourceMoreMenuModel(
            title = resource.name,
            canDelete = resource.permission in WRITE_PERMISSIONS,
            canEdit = resource.permission in WRITE_PERMISSIONS,
            canShare = resource.permission == ResourcePermission.OWNER,
            favouriteOption = if (resource.isFavourite()) {
                REMOVE_FROM_FAVOURITES
            } else {
                ADD_TO_FAVOURITES
            }
        )

    private companion object {
        private val WRITE_PERMISSIONS = setOf(
            ResourcePermission.OWNER,
            ResourcePermission.UPDATE
        )
    }
}
