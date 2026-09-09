package com.passbolt.mobile.android.domain.secrets.offline

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Outcome of the most recent offline secret sync in this process, so the settings screen
 * can tell the user what a "Sync now" (or an automatic refresh) actually did. The sync is
 * a non-fatal step of the full data refresh, which only reports success or failure as a
 * whole - without this the sync's own result would be invisible.
 */
class OfflineSyncTracker {
    private val _status = MutableStateFlow<OfflineSyncStatus>(OfflineSyncStatus.Idle)

    val status: StateFlow<OfflineSyncStatus> = _status.asStateFlow()

    fun update(status: OfflineSyncStatus) {
        _status.value = status
    }
}

sealed class OfflineSyncStatus {
    data object Idle : OfflineSyncStatus()

    data class InProgress(
        val done: Int,
        val total: Int,
    ) : OfflineSyncStatus()

    data class Success(
        val cachedCount: Int,
        val fetchedCount: Int,
        val atEpochMillis: Long,
    ) : OfflineSyncStatus()

    data class Failure(
        val atEpochMillis: Long,
    ) : OfflineSyncStatus()
}
