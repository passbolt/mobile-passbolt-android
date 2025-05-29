package com.passbolt.mobile.android.core.mvp.authentication

import com.passbolt.mobile.android.core.mvp.authentication.AuthenticationState.Unauthenticated.Reason.Mfa.MfaProvider

class MfaProvidersHandler {
    private lateinit var providers: List<MfaProvider?>

    fun setProviders(providers: List<MfaProvider?>) {
        this.providers = providers
    }

    fun firstMfaProvider(): MfaProvider? {
        require(::providers.isInitialized) { "Update with latest state using #setState before usage" }
        return providers.firstOrNull()
    }

    // returns next MFA provider for changing with "change provider button"
    fun nextMfaProvider(currentProvider: MfaProvider): MfaProvider? {
        require(::providers.isInitialized) { "Update with latest state using #setState before usage" }
        val indexOfCurrentProvider = providers.indexOf(currentProvider)
        return if (providers.size == 1) {
            null
        } else {
            if (indexOfCurrentProvider == providers.lastIndex) {
                providers[0]
            } else {
                providers[indexOfCurrentProvider + 1]
            }
        }
    }

    fun hasMultipleProviders() = providers.size > 1
}
