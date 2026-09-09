package com.passbolt.mobile.android.domain.secrets.offline

import com.passbolt.mobile.android.core.architecture.result.DomainResult
import java.time.ZonedDateTime

interface OfflineCacheRepository {
    // encrypted secret cache (local)
    suspend fun getCachedSecret(
        resourceId: String,
        userId: String,
    ): OfflineCachedSecret?

    suspend fun getCachedSecretsState(userId: String): List<ResourceModifiedState>

    suspend fun getLocalResourcesState(userId: String): List<ResourceModifiedState>

    suspend fun putCachedSecrets(
        secrets: List<OfflineCachedSecret>,
        userId: String,
    )

    suspend fun removeCachedSecrets(
        resourceIds: List<String>,
        userId: String,
    )

    suspend fun removeAllCachedSecrets(userId: String)

    suspend fun countCachedSecrets(userId: String): Int

    suspend fun oldestCachedAt(userId: String): ZonedDateTime?

    // "available offline" marks (local for now; server-side offline_items later)
    suspend fun markResource(
        resourceId: String,
        userId: String,
    )

    suspend fun unmarkResource(
        resourceId: String,
        userId: String,
    )

    suspend fun isResourceMarked(
        resourceId: String,
        userId: String,
    ): Boolean

    suspend fun getMarkedResourceIds(userId: String): List<String>

    suspend fun countMarkedResources(userId: String): Int

    suspend fun clearMarks(userId: String)

    // remote
    suspend fun fetchSecretsForResources(resourceIds: List<String>): DomainResult<List<RemoteResourceSecret>>
}
