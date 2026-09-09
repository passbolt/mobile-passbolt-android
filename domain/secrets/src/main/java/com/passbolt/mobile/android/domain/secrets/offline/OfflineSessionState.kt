package com.passbolt.mobile.android.domain.secrets.offline

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import java.time.ZonedDateTime

/**
 * Process-wide flag: the current session runs against the local encrypted cache because
 * the server cannot be reached - either the sign-in itself happened locally (passphrase
 * verified against the private key, no JWT) or a server session existed and the server
 * disappeared under it (a refresh or a secret fetch failed with an unreachable server).
 *
 * While an offline session is active
 *  - secrets are served from the local encrypted cache only,
 *  - the authenticated-operation runner does not force a server sign-in,
 *  - the UI is read-only (no create / edit / share) and shows the offline banner.
 *
 * The state is left when a server session is established again (full sign-in, a
 * successful session refresh, the probe before a data refresh) or on sign-out.
 * Screens observe [isOfflineSessionFlow] so the banner appears the moment the state flips.
 */
class OfflineSessionState {
    private val _isOfflineSessionFlow = MutableStateFlow(false)

    val isOfflineSessionFlow: StateFlow<Boolean> = _isOfflineSessionFlow.asStateFlow()

    val isOfflineSession: Boolean
        get() = _isOfflineSessionFlow.value

    @Volatile
    var offlineSince: ZonedDateTime? = null
        private set

    fun enterOfflineSession() {
        if (!isOfflineSession) {
            Timber.d("[Offline] Entering offline session")
            offlineSince = ZonedDateTime.now()
        }
        _isOfflineSessionFlow.value = true
    }

    fun exitOfflineSession() {
        if (isOfflineSession) {
            Timber.d("[Offline] Leaving offline session")
        }
        offlineSince = null
        _isOfflineSessionFlow.value = false
    }
}
