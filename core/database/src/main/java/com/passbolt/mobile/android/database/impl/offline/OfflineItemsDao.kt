package com.passbolt.mobile.android.database.impl.offline

import androidx.room.Dao
import androidx.room.Query
import com.passbolt.mobile.android.database.impl.base.BaseDao
import com.passbolt.mobile.android.entity.offline.OfflineItem

@Dao
interface OfflineItemsDao : BaseDao<OfflineItem> {
    @Query("SELECT resourceId FROM OfflineItem")
    suspend fun getMarkedResourceIds(): List<String>

    @Query("SELECT COUNT(*) FROM OfflineItem")
    suspend fun count(): Int

    @Query("SELECT EXISTS(SELECT 1 FROM OfflineItem WHERE resourceId = :resourceId)")
    suspend fun isMarked(resourceId: String): Boolean

    @Query("DELETE FROM OfflineItem WHERE resourceId = :resourceId")
    suspend fun delete(resourceId: String)

    @Query("DELETE FROM OfflineItem")
    suspend fun deleteAll()
}
