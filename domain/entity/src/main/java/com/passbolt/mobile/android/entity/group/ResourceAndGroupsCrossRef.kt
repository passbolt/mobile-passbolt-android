package com.passbolt.mobile.android.entity.group

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.ForeignKey.CASCADE
import com.passbolt.mobile.android.entity.resource.Permission
import com.passbolt.mobile.android.entity.resource.Resource

@Entity(
    primaryKeys = ["resourceId", "groupId"],
    foreignKeys = [
        ForeignKey(
            entity = Resource::class,
            parentColumns = ["resourceId"],
            childColumns = ["resourceId"],
            onDelete = CASCADE
        ),
        ForeignKey(
            entity = UsersGroup::class,
            parentColumns = ["groupId"],
            childColumns = ["groupId"],
            onDelete = CASCADE
        )
    ]
)
data class ResourceAndGroupsCrossRef(
    val resourceId: String,
    val groupId: String,
    val permission: Permission,
    val permissionId: String
)
