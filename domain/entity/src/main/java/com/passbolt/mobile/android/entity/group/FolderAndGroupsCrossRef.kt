package com.passbolt.mobile.android.entity.group

import androidx.room.Entity
import androidx.room.ForeignKey
import com.passbolt.mobile.android.entity.folder.Folder
import com.passbolt.mobile.android.entity.resource.Permission

@Entity(
    primaryKeys = ["folderId", "groupId"],
    foreignKeys = [
        ForeignKey(
            entity = Folder::class,
            parentColumns = ["folderId"],
            childColumns = ["folderId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = UsersGroup::class,
            parentColumns = ["groupId"],
            childColumns = ["groupId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class FolderAndGroupsCrossRef(
    val folderId: String,
    val groupId: String,
    val permission: Permission,
    val permissionId: String
)
