package com.passbolt.mobile.android.entity.offline

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.ForeignKey.Companion.CASCADE
import androidx.room.PrimaryKey
import com.passbolt.mobile.android.entity.resource.Resource
import java.time.ZonedDateTime

/**
 * Locally cached, still PGP-encrypted secret of a resource that is available offline.
 *
 * [encryptedSecret] is the armored OpenPGP message exactly as the server returns it
 * from `/secrets/resource/{id}.json` - encrypted to the user's public key. It can only
 * be read with the user's private key and passphrase, i.e. it enjoys the same
 * protection as the private key that already lives on the device. Nothing is stored
 * in clear text.
 *
 * [resourceModified] is the `modified` timestamp of the resource at the time the
 * secret was cached; the cache is refreshed whenever the resource is newer than that.
 * [cachedAt] drives the data-retention limit.
 *
 * The row is deleted together with its resource (cascade).
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
data class OfflineSecret(
    @PrimaryKey
    val resourceId: String,
    val encryptedSecret: String,
    val resourceModified: ZonedDateTime,
    val cachedAt: ZonedDateTime,
)

/**
 * Projection used by the offline sync to decide which secrets are stale.
 */
data class ResourceIdWithModified(
    val resourceId: String,
    val modified: ZonedDateTime,
)
