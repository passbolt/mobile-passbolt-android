package com.passbolt.mobile.android.domain.secrets.offline

import java.time.ZonedDateTime

/** A secret cached locally for offline use - still an armored OpenPGP message. */
data class OfflineCachedSecret(
    val resourceId: String,
    val encryptedSecret: String,
    val resourceModified: ZonedDateTime,
    val cachedAt: ZonedDateTime,
)

/** Resource id with the server `modified` stamp used to detect stale cache entries. */
data class ResourceModifiedState(
    val resourceId: String,
    val modified: ZonedDateTime,
)

/** A secret as returned by the server together with its resource's `modified` stamp. */
data class RemoteResourceSecret(
    val resourceId: String,
    val resourceModified: ZonedDateTime,
    val encryptedSecret: String,
)
