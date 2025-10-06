package com.passbolt.mobile.android.core.mvp.authentication

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.passbolt.mobile.android.core.mvp.authentication.AuthenticationState.Unauthenticated.Reason.Session
import com.passbolt.mobile.android.core.mvp.authentication.SessionState.NeedsRefresh
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalCoroutinesApi::class, ExperimentalTime::class)
class SessionRefreshTrackingFlowTest {
    private val sessionRefreshTrackingFlow = SessionRefreshTrackingFlow()

    @Test
    fun `when notifying session refresh needed with same reason instance twice than one event is emitted`() =
        runTest {
            sessionRefreshTrackingFlow.needSessionRefreshFlow().test {
                sessionRefreshTrackingFlow.notifySessionRefreshNeeded(Session)
                sessionRefreshTrackingFlow.notifySessionRefreshNeeded(Session)

                assertThat(expectItem()).isEqualTo(NeedsRefresh(Session))

                expectNoEvents()
            }
        }
}
