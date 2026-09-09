package com.passbolt.mobile.android.ui

data class AccountFlagsUiModel(
    val wasChromeNativeAutofillDialogShown: Boolean,
    val offlineMode: OfflineModeSetting = OfflineModeSetting.OFF,
    val offlineLastSyncEpochMillis: Long? = null,
)

/**
 * Which entries are cached (still PGP-encrypted) on the device for offline use.
 * OFF is the default; nothing is cached unless the user opts in before going offline.
 */
enum class OfflineModeSetting {
    OFF,
    SELECTED_ENTRIES,
    ALL_ENTRIES,
    ;

    val isEnabled: Boolean
        get() = this != OFF
}
