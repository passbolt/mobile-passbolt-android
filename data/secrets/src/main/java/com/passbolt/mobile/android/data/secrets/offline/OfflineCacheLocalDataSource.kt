package com.passbolt.mobile.android.data.secrets.offline

import com.passbolt.mobile.android.database.DatabaseProvider
import com.passbolt.mobile.android.domain.secrets.offline.OfflineCachedSecret
import com.passbolt.mobile.android.domain.secrets.offline.ResourceModifiedState
import com.passbolt.mobile.android.entity.offline.OfflineItem
import com.passbolt.mobile.android.entity.offline.OfflineSecret
import com.passbolt.mobile.android.entity.offline.ResourceIdWithModified
import java.time.Instant
import java.time.ZoneOffset
import java.time.ZonedDateTime

internal class OfflineCacheLocalDataSource(
    private val databaseProvider: DatabaseProvider,
) {
    private fun secretsDao(userId: String) = databaseProvider.get(userId).offlineSecretsDao()

    private fun itemsDao(userId: String) = databaseProvider.get(userId).offlineItemsDao()

    suspend fun getCachedSecret(
        resourceId: String,
        userId: String,
    ): OfflineCachedSecret? = secretsDao(userId).get(resourceId)?.toDomain()

    suspend fun getCachedSecretsState(userId: String): List<ResourceModifiedState> =
        secretsDao(userId).getCachedState().map { it.toDomain() }

    suspend fun getLocalResourcesState(userId: String): List<ResourceModifiedState> =
        secretsDao(userId).getAllResourcesModifiedState().map { it.toDomain() }

    suspend fun putCachedSecrets(
        secrets: List<OfflineCachedSecret>,
        userId: String,
    ) {
        if (secrets.isNotEmpty()) {
            secretsDao(userId).upsertAll(secrets.map { it.toEntity() })
        }
    }

    suspend fun removeCachedSecrets(
        resourceIds: List<String>,
        userId: String,
    ) {
        resourceIds.chunked(SQLITE_VARIABLE_LIMIT).forEach { secretsDao(userId).deleteAll(it) }
    }

    suspend fun removeAllCachedSecrets(userId: String) = secretsDao(userId).deleteAll()

    suspend fun countCachedSecrets(userId: String): Int = secretsDao(userId).count()

    suspend fun oldestCachedAt(userId: String): ZonedDateTime? =
        secretsDao(userId).oldestCachedAtMillis()?.let { ZonedDateTime.ofInstant(Instant.ofEpochMilli(it), ZoneOffset.UTC) }

    suspend fun markResource(
        resourceId: String,
        userId: String,
    ) {
        itemsDao(userId).upsert(OfflineItem(resourceId = resourceId, markedAt = ZonedDateTime.now()))
    }

    suspend fun unmarkResource(
        resourceId: String,
        userId: String,
    ) = itemsDao(userId).delete(resourceId)

    suspend fun isResourceMarked(
        resourceId: String,
        userId: String,
    ): Boolean = itemsDao(userId).isMarked(resourceId)

    suspend fun getMarkedResourceIds(userId: String): List<String> = itemsDao(userId).getMarkedResourceIds()

    suspend fun countMarkedResources(userId: String): Int = itemsDao(userId).count()

    suspend fun clearMarks(userId: String) = itemsDao(userId).deleteAll()

    private fun OfflineSecret.toDomain() =
        OfflineCachedSecret(
            resourceId = resourceId,
            encryptedSecret = encryptedSecret,
            resourceModified = resourceModified,
            cachedAt = cachedAt,
        )

    private fun OfflineCachedSecret.toEntity() =
        OfflineSecret(
            resourceId = resourceId,
            encryptedSecret = encryptedSecret,
            resourceModified = resourceModified,
            cachedAt = cachedAt,
        )

    private fun ResourceIdWithModified.toDomain() = ResourceModifiedState(resourceId = resourceId, modified = modified)

    private companion object {
        private const val SQLITE_VARIABLE_LIMIT = 500
    }
}
