package com.passbolt.mobile.android.core.networking

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ServerReachabilityTrackerTest {
    private val tracker = ServerReachabilityTracker()

    @Test
    fun `nothing recorded is not unreachable`() {
        assertThat(tracker.unreachableSince(tracker.now())).isFalse()
    }

    @Test
    fun `a failure after the reference point is unreachable`() {
        val since = tracker.now()

        tracker.markUnreachable()

        assertThat(tracker.unreachableSince(since)).isTrue()
    }

    @Test
    fun `a failure before the reference point does not count`() {
        tracker.markUnreachable()
        val since = tracker.now() + 1

        assertThat(tracker.unreachableSince(since)).isFalse()
    }

    @Test
    fun `a success after the failure clears it`() {
        val since = tracker.now()
        tracker.markUnreachable()

        tracker.markReachable()

        assertThat(tracker.unreachableSince(since)).isFalse()
    }

    @Test
    fun `a failure after a success is unreachable again`() {
        val since = tracker.now()
        tracker.markReachable()

        tracker.markUnreachable()

        assertThat(tracker.unreachableSince(since)).isTrue()
    }
}
