package com.passbolt.mobile.android.entity.resource

import androidx.room.Entity
import androidx.room.ForeignKey

@Entity(
    primaryKeys = ["resourceTypeId", "resourceFieldId"],
    foreignKeys = [
        ForeignKey(
            entity = ResourceType::class,
            parentColumns = ["resourceTypeId"],
            childColumns = ["resourceTypeId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ResourceField::class,
            parentColumns = ["resourceFieldId"],
            childColumns = ["resourceFieldId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class ResourceTypesAndFieldsCrossRef(
    val resourceTypeId: String,
    val resourceFieldId: Long
)
