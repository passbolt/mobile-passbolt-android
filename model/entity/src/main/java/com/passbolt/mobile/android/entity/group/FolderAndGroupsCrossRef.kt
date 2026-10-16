package com.passbolt.mobile.android.entity.group

import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import com.passbolt.mobile.android.entity.folder.Folder
import com.passbolt.mobile.android.entity.resource.Permission

@Entity(
    primaryKeys = ["folderId", "groupId"],
    indices = [Index(value = ["groupId"])],
    foreignKeys = [
        ForeignKey(
            entity = Folder::class,
            parentColumns = ["folderId"],
            childColumns = ["folderId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = UsersGroup::class,
            parentColumns = ["groupId"],
            childColumns = ["groupId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class FolderAndGroupsCrossRef(
    val folderId: String,
    val groupId: String,
    val permission: Permission,
    val permissionId: String,
)
