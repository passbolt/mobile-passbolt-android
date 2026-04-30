package com.passbolt.mobile.android.feature.main.mainscreen

import com.passbolt.mobile.android.core.navigation.compose.BottomTab

sealed interface MainIntent {
    data object AppUpdateDownloaded : MainIntent

    data object GoToSettings : MainIntent

    data object CloseChromeNativeAutofill : MainIntent

    data object Resumed : MainIntent

    data class TabSelected(
        val tab: BottomTab,
    ) : MainIntent
}
