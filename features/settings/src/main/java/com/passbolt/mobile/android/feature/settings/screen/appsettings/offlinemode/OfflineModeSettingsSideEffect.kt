package com.passbolt.mobile.android.feature.settings.screen.appsettings.offlinemode

internal sealed interface OfflineModeSettingsSideEffect {
    data object NavigateUp : OfflineModeSettingsSideEffect

    data object StartDataRefresh : OfflineModeSettingsSideEffect

    data class ShowSyncResult(
        val result: SyncResult,
    ) : OfflineModeSettingsSideEffect
}

internal sealed interface SyncResult {
    data class Success(
        val cachedCount: Int,
    ) : SyncResult

    /** The refresh went through but the secret-cache step could not fetch everything. */
    data object Partial : SyncResult

    /** The refresh itself failed - in practice: the server could not be reached. */
    data object Failed : SyncResult
}
