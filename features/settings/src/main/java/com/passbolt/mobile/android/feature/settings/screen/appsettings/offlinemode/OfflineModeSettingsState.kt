package com.passbolt.mobile.android.feature.settings.screen.appsettings.offlinemode

import com.passbolt.mobile.android.ui.OfflineModeSetting

internal data class OfflineModeSettingsState(
    val mode: OfflineModeSetting = OfflineModeSetting.OFF,
    val cachedCount: Int = 0,
    val markedCount: Int = 0,
    val lastSyncEpochMillis: Long? = null,
    val isOfflineSession: Boolean = false,
    val isRefreshing: Boolean = false,
    val refreshProgress: Float = 0f,
    val syncProgress: SyncProgress? = null,
    val showClearConfirmation: Boolean = false,
) {
    val isEnabled: Boolean
        get() = mode.isEnabled
}

/** Progress of the secret-cache step itself (the last step of a full refresh). */
internal data class SyncProgress(
    val done: Int,
    val total: Int,
)
