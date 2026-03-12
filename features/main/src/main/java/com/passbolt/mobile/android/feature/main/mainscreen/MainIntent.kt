package com.passbolt.mobile.android.feature.main.mainscreen

sealed interface MainIntent {
    data object AppUpdateDownloaded : MainIntent

    data object PerformFullDataRefresh : MainIntent

    data object GoToSettings : MainIntent

    data object CloseChromeNativeAutofill : MainIntent

    data object Resumed : MainIntent
}
