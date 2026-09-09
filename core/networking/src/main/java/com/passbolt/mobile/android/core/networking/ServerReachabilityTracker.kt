package com.passbolt.mobile.android.core.networking

/**
 * Remembers when the server was last seen as unreachable (DNS failure, connection refused,
 * connect or read timeout) and when a call last got through.
 *
 * Callers that only see a summarised "failed" outcome - the full data refresh is the main
 * one - can then tell "the server is gone" apart from "the server answered with an error"
 * without sending a probe of their own. Timestamps are monotonic (`System.nanoTime`).
 */
class ServerReachabilityTracker {
    @Volatile
    private var lastUnreachableNanos: Long? = null

    @Volatile
    private var lastReachableNanos: Long? = null

    fun now(): Long = System.nanoTime()

    fun markUnreachable() {
        lastUnreachableNanos = System.nanoTime()
    }

    fun markReachable() {
        lastReachableNanos = System.nanoTime()
    }

    /**
     * True when a call failed because the server was unreachable at or after [sinceNanos]
     * and no call has succeeded since that failure.
     */
    fun unreachableSince(sinceNanos: Long): Boolean {
        val unreachable = lastUnreachableNanos
        val reachable = lastReachableNanos
        return unreachable != null &&
            unreachable - sinceNanos >= 0 &&
            (reachable == null || reachable - unreachable < 0)
    }
}
