package com.passbolt.mobile.android.feature.authentication.auth.challenge

sealed class MfaStatus {

    object NotRequired : MfaStatus()

    data class Required(val mfaProviders: List<String>) : MfaStatus()
}
