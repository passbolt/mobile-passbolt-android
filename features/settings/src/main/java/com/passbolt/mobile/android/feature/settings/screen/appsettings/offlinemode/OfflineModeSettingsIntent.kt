package com.passbolt.mobile.android.feature.settings.screen.appsettings.offlinemode

import com.passbolt.mobile.android.ui.OfflineModeSetting

internal sealed interface OfflineModeSettingsIntent {
    data object GoBack : OfflineModeSettingsIntent

    data object ToggleEnabled : OfflineModeSettingsIntent

    data class SelectMode(
        val mode: OfflineModeSetting,
    ) : OfflineModeSettingsIntent

    data object SyncNow : OfflineModeSettingsIntent

    data object ClearClick : OfflineModeSettingsIntent

    data object ConfirmClear : OfflineModeSettingsIntent

    data object CancelClear : OfflineModeSettingsIntent
}
