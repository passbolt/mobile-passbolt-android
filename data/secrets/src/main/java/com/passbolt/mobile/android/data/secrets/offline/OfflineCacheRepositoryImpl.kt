package com.passbolt.mobile.android.data.secrets.offline

import com.passbolt.mobile.android.core.architecture.result.DomainResult
import com.passbolt.mobile.android.domain.secrets.offline.OfflineCacheRepository
import com.passbolt.mobile.android.domain.secrets.offline.OfflineCachedSecret
import com.passbolt.mobile.android.domain.secrets.offline.RemoteResourceSecret
import com.passbolt.mobile.android.domain.secrets.offline.ResourceModifiedState
import java.time.ZonedDateTime

internal class OfflineCacheRepositoryImpl(
    private val localDataSource: OfflineCacheLocalDataSource,
    private val remoteDataSource: OfflineCacheRemoteDataSource,
) : OfflineCacheRepository {
    override suspend fun getCachedSecret(
        resourceId: String,
        userId: String,
    ): OfflineCachedSecret? = localDataSource.getCachedSecret(resourceId, userId)

    override suspend fun getCachedSecretsState(userId: String): List<ResourceModifiedState> = localDataSource.getCachedSecretsState(userId)

    override suspend fun getLocalResourcesState(userId: String): List<ResourceModifiedState> =
        localDataSource.getLocalResourcesState(userId)

    override suspend fun putCachedSecrets(
        secrets: List<OfflineCachedSecret>,
        userId: String,
    ) = localDataSource.putCachedSecrets(secrets, userId)

    override suspend fun removeCachedSecrets(
        resourceIds: List<String>,
        userId: String,
    ) = localDataSource.removeCachedSecrets(resourceIds, userId)

    override suspend fun removeAllCachedSecrets(userId: String) = localDataSource.removeAllCachedSecrets(userId)

    override suspend fun countCachedSecrets(userId: String): Int = localDataSource.countCachedSecrets(userId)

    override suspend fun oldestCachedAt(userId: String): ZonedDateTime? = localDataSource.oldestCachedAt(userId)

    override suspend fun markResource(
        resourceId: String,
        userId: String,
    ) = localDataSource.markResource(resourceId, userId)

    override suspend fun unmarkResource(
        resourceId: String,
        userId: String,
    ) = localDataSource.unmarkResource(resourceId, userId)

    override suspend fun isResourceMarked(
        resourceId: String,
        userId: String,
    ): Boolean = localDataSource.isResourceMarked(resourceId, userId)

    override suspend fun getMarkedResourceIds(userId: String): List<String> = localDataSource.getMarkedResourceIds(userId)

    override suspend fun countMarkedResources(userId: String): Int = localDataSource.countMarkedResources(userId)

    override suspend fun clearMarks(userId: String) = localDataSource.clearMarks(userId)

    override suspend fun fetchSecretsForResources(resourceIds: List<String>): DomainResult<List<RemoteResourceSecret>> =
        remoteDataSource.fetchSecretsForResources(resourceIds)
}
