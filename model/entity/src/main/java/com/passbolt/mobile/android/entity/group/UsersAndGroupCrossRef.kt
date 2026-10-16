package com.passbolt.mobile.android.entity.group

import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import com.passbolt.mobile.android.entity.user.User

@Entity(
    primaryKeys = ["userId", "groupId"],
    indices = [Index(value = ["groupId"])],
    foreignKeys = [
        ForeignKey(
            entity = User::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
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
data class UsersAndGroupCrossRef(
    val userId: String,
    val groupId: String,
)
