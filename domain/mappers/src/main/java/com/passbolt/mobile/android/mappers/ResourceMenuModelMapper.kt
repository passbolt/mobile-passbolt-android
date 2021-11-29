package com.passbolt.mobile.android.mappers

import com.passbolt.mobile.android.ui.ResourceModel
import com.passbolt.mobile.android.ui.ResourceMoreMenuModel
import com.passbolt.mobile.android.ui.ResourcePermission

class ResourceMenuModelMapper {

    fun map(resourceModel: ResourceModel) =
        ResourceMoreMenuModel(
            title = resourceModel.name,
            canDelete = resourceModel.permission in WRITE_PERMISSIONS,
            canEdit = resourceModel.permission in WRITE_PERMISSIONS
        )

    private companion object {
        private val WRITE_PERMISSIONS = setOf(
            ResourcePermission.OWNER,
            ResourcePermission.UPDATE
        )
    }
}
