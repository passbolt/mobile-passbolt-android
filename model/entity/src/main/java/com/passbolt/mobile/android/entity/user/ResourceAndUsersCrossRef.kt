package com.passbolt.mobile.android.entity.user

import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import com.passbolt.mobile.android.entity.resource.Permission
import com.passbolt.mobile.android.entity.resource.Resource

@Entity(
    primaryKeys = ["resourceId", "userId"],
    indices = [Index(value = ["userId"])],
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
