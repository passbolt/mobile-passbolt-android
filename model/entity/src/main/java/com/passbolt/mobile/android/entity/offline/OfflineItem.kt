package com.passbolt.mobile.android.entity.offline

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.ForeignKey.Companion.CASCADE
import androidx.room.PrimaryKey
import com.passbolt.mobile.android.entity.resource.Resource
import java.time.ZonedDateTime

/**
 * A resource the user explicitly marked as "available offline".
 *
 * Mirrors the server-side `offline_items` model that the passbolt browser
 * extension introduces in 5.16 (one row per user and resource), so that the
 * marks can later be synchronised with the server instead of being local only.
 *
 * The row is deleted together with its resource (cascade), so a resource
 * removed on the server drops out of the offline set on the next refresh.
 */
@Entity(
    foreignKeys = [
        ForeignKey(
            entity = Resource::class,
            parentColumns = ["resourceId"],
            childColumns = ["resourceId"],
            onDelete = CASCADE,
        ),
    ],
)
data class OfflineItem(
    @PrimaryKey
    val resourceId: String,
    val markedAt: ZonedDateTime,
)
