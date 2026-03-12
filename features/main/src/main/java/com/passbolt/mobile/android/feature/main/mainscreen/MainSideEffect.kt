package com.passbolt.mobile.android.feature.main.mainscreen

sealed interface MainSideEffect {
    data object CheckForAppUpdates : MainSideEffect

    data object TryLaunchReviewFlow : MainSideEffect

    data object PerformFullDataRefresh : MainSideEffect

    data object LaunchChromeNativeAutofillDeeplink : MainSideEffect

    data class ShowSnackbar(
        val message: SnackbarType,
    ) : MainSideEffect
}

enum class SnackbarType {
    APP_UPDATE_DOWNLOADED,
    CHROME_NATIVE_AUTOFILL_SETUP_SUCCESS,
}
