package com.passbolt.mobile.android.database.impl.offline

import androidx.room.Dao
import androidx.room.Query
import com.passbolt.mobile.android.database.impl.base.BaseDao
import com.passbolt.mobile.android.entity.offline.OfflineSecret
import com.passbolt.mobile.android.entity.offline.ResourceIdWithModified

@Dao
interface OfflineSecretsDao : BaseDao<OfflineSecret> {
    @Query("SELECT * FROM OfflineSecret WHERE resourceId = :resourceId")
    suspend fun get(resourceId: String): OfflineSecret?

    @Query("SELECT resourceId, resourceModified AS modified FROM OfflineSecret")
    suspend fun getCachedState(): List<ResourceIdWithModified>

    @Query("SELECT COUNT(*) FROM OfflineSecret")
    suspend fun count(): Int

    @Query("SELECT MIN(cachedAt) FROM OfflineSecret")
    suspend fun oldestCachedAtMillis(): Long?

    @Query("DELETE FROM OfflineSecret WHERE resourceId = :resourceId")
    suspend fun delete(resourceId: String)

    @Query("DELETE FROM OfflineSecret WHERE resourceId IN (:resourceIds)")
    suspend fun deleteAll(resourceIds: List<String>)

    @Query("DELETE FROM OfflineSecret")
    suspend fun deleteAll()

    /**
     * All resources known locally with their server `modified` stamp - the offline
     * sync compares this against [getCachedState] to fetch only changed secrets.
     */
    @Query("SELECT resourceId, modified FROM Resource")
    suspend fun getAllResourcesModifiedState(): List<ResourceIdWithModified>
}
