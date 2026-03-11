package com.passbolt.mobile.android.feature.autofill.resources

sealed interface AutofillResourcesSideEffect {
    data object NavigateToAuth : AutofillResourcesSideEffect

    data object NavigateToSetup : AutofillResourcesSideEffect

    data class ShowToast(
        val type: ToastType,
    ) : AutofillResourcesSideEffect

    data class AutofillReturn(
        val username: String,
        val password: String,
        val uri: String?,
    ) : AutofillResourcesSideEffect
}

enum class ToastType {
    DECRYPTION_FAILURE,
    FETCH_FAILURE,
}
