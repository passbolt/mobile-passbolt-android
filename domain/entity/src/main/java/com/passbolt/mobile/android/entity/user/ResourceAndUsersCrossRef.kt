package com.passbolt.mobile.android.entity.user

import androidx.room.Entity
import androidx.room.ForeignKey
import com.passbolt.mobile.android.entity.resource.Permission
import com.passbolt.mobile.android.entity.resource.Resource

@Entity(
    primaryKeys = ["resourceId", "userId"],
    foreignKeys = [
        ForeignKey(
            entity = Resource::class,
            parentColumns = ["resourceId"],
            childColumns = ["resourceId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = User::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class ResourceAndUsersCrossRef(
    val resourceId: String,
    val userId: String,
    val permission: Permission,
    val permissionId: String,
)
