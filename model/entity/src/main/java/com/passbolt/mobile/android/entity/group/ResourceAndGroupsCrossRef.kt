package com.passbolt.mobile.android.entity.group

import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.ForeignKey.Companion.CASCADE
import androidx.room3.Index
import com.passbolt.mobile.android.entity.resource.Permission
import com.passbolt.mobile.android.entity.resource.Resource

@Entity(
    primaryKeys = ["resourceId", "groupId"],
    indices = [Index(value = ["groupId"])],
    foreignKeys = [
        ForeignKey(
            entity = Resource::class,
            parentColumns = ["resourceId"],
            childColumns = ["resourceId"],
            onDelete = CASCADE,
        ),
        ForeignKey(
            entity = UsersGroup::class,
            parentColumns = ["groupId"],
            childColumns = ["groupId"],
            onDelete = CASCADE,
        ),
    ],
)
data class ResourceAndGroupsCrossRef(
    val resourceId: String,
    val groupId: String,
    val permission: Permission,
    val permissionId: String,
)
