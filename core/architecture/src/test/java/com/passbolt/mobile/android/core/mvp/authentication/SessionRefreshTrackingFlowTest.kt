package com.passbolt.mobile.android.core.mvp.authentication

import app.cash.turbine.test
import com.passbolt.mobile.android.core.mvp.authentication.AuthenticationState.Unauthenticated.Reason.Mfa
import com.passbolt.mobile.android.core.mvp.authentication.AuthenticationState.Unauthenticated.Reason.Mfa.MfaProvider.TOTP
import com.passbolt.mobile.android.core.mvp.authentication.AuthenticationState.Unauthenticated.Reason.Passphrase
import com.passbolt.mobile.android.core.mvp.authentication.AuthenticationState.Unauthenticated.Reason.Session
import com.passbolt.mobile.android.core.mvp.authentication.SessionState.NeedsRefresh
import com.passbolt.mobile.android.core.mvp.authentication.SessionState.Valid
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue
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

                assertEquals(NeedsRefresh(Session), awaitItem())

                expectNoEvents()
            }
        }

    @Test
    fun `should emit session reason when session refresh is needed`() =
        runTest {
            sessionRefreshTrackingFlow.needSessionRefreshFlow().test {
                sessionRefreshTrackingFlow.notifySessionRefreshNeeded(Session)

                assertEquals(NeedsRefresh(Session), awaitItem())
            }
        }

    @Test
    fun `should emit passphrase reason when passphrase refresh is needed`() =
        runTest {
            sessionRefreshTrackingFlow.needSessionRefreshFlow().test {
                sessionRefreshTrackingFlow.notifySessionRefreshNeeded(Passphrase)

                assertEquals(NeedsRefresh(Passphrase), awaitItem())
            }
        }

    @Test
    fun `should emit mfa reason when mfa refresh is needed`() =
        runTest {
            sessionRefreshTrackingFlow.needSessionRefreshFlow().test {
                sessionRefreshTrackingFlow.notifySessionRefreshNeeded(Mfa(providers = listOf(TOTP)))

                val item = awaitItem()
                assertIs<NeedsRefresh>(item)
                val reason = assertIs<Mfa>(item.reason)
                assertEquals(listOf(TOTP), reason.providers)
            }
        }

    @Test
    fun `should emit valid state when session is refreshed`() =
        runTest {
            sessionRefreshTrackingFlow.sessionRefreshedFlow().test {
                sessionRefreshTrackingFlow.notifySessionRefreshed()

                assertIs<Valid>(awaitItem())
            }
        }

    @Test
    fun `full cycle - need refresh then refreshed should unblock waiter`() =
        runTest {
            var waiterCompleted = false

            // Simulate AuthenticatedOperationRunner waiting for refresh
            val waiterJob =
                launch {
                    sessionRefreshTrackingFlow.notifySessionRefreshNeeded(Session)
                    sessionRefreshTrackingFlow.sessionRefreshedFlow().first()
                    waiterCompleted = true
                }

            // Let the waiter start
            advanceUntilIdle()
            assertFalse(waiterCompleted)

            // Simulate ActivityAuthenticationHandler completing auth
            sessionRefreshTrackingFlow.notifySessionRefreshed()

            waiterJob.join()
            assertTrue(waiterCompleted)
        }

    @Test
    fun `multiple waiters should all be unblocked by single refresh`() =
        runTest {
            var waiter1Completed = false
            var waiter2Completed = false

            sessionRefreshTrackingFlow.notifySessionRefreshNeeded(Session)

            val waiterJob1 =
                launch {
                    sessionRefreshTrackingFlow.sessionRefreshedFlow().first()
                    waiter1Completed = true
                }
            val waiterJob2 =
                launch {
                    sessionRefreshTrackingFlow.sessionRefreshedFlow().first()
                    waiter2Completed = true
                }

            advanceUntilIdle()
            assertFalse(waiter1Completed)
            assertFalse(waiter2Completed)

            sessionRefreshTrackingFlow.notifySessionRefreshed()

            waiterJob1.join()
            waiterJob2.join()
            assertTrue(waiter1Completed)
            assertTrue(waiter2Completed)
        }

    @Test
    fun `different reasons should emit separate events`() =
        runTest {
            sessionRefreshTrackingFlow.needSessionRefreshFlow().test {
                sessionRefreshTrackingFlow.notifySessionRefreshNeeded(Session)
                assertEquals(NeedsRefresh(Session), awaitItem())

                // Reset to Valid so next NeedsRefresh is a change
                sessionRefreshTrackingFlow.notifySessionRefreshed()

                sessionRefreshTrackingFlow.notifySessionRefreshNeeded(Passphrase)
                assertEquals(NeedsRefresh(Passphrase), awaitItem())
            }
        }

    @Test
    fun `initial state should be valid`() =
        runTest {
            sessionRefreshTrackingFlow.sessionRefreshedFlow().test {
                assertIs<Valid>(awaitItem())
            }
        }

    @Test
    fun `need refresh flow should not emit valid state`() =
        runTest {
            sessionRefreshTrackingFlow.needSessionRefreshFlow().test {
                sessionRefreshTrackingFlow.notifySessionRefreshed()

                expectNoEvents()
            }
        }

    @Test
    fun `session refreshed flow should not emit needs refresh state`() =
        runTest {
            sessionRefreshTrackingFlow.notifySessionRefreshNeeded(Session)

            sessionRefreshTrackingFlow.sessionRefreshedFlow().test {
                expectNoEvents()
            }
        }
}
