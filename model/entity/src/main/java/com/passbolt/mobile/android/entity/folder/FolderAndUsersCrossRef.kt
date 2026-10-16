package com.passbolt.mobile.android.entity.folder

import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import com.passbolt.mobile.android.entity.resource.Permission
import com.passbolt.mobile.android.entity.user.User

@Entity(
    primaryKeys = ["userId", "folderId"],
    indices = [Index(value = ["folderId"])],
    foreignKeys = [
        ForeignKey(
            entity = Folder::class,
            parentColumns = ["folderId"],
            childColumns = ["folderId"],
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
data class FolderAndUsersCrossRef(
    val folderId: String,
    val userId: String,
    val permission: Permission,
    val permissionId: String,
)
