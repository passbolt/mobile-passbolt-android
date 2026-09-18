package com.passbolt.mobile.android.feature.authentication.auth.usecase

import androidx.annotation.VisibleForTesting
import com.passbolt.mobile.android.gopenpgp.OpenPgp
import timber.log.Timber
import kotlin.math.abs

class GopenPgpTimeUpdater(
    private val openPgp: OpenPgp,
) {
    fun updateTimeIfNeeded(
        serverTimeSeconds: Long,
        deviceTimeAtFetchMillis: Long,
        getTimeRequestDurationMillis: Long,
    ): Result {
        val halfRoundTripMillis = getTimeRequestDurationMillis / 2
        val serverTimeAtFetchMillis = serverTimeSeconds * MILLIS_PER_SECOND + halfRoundTripMillis
        val timeOffsetMillis = serverTimeAtFetchMillis - deviceTimeAtFetchMillis

        return if (abs(timeOffsetMillis) <= TIME_DELTA_FOR_LOCAL_SYNC_SECS * MILLIS_PER_SECOND) {
            Timber.d("Local time sync needed. Adjusted: $timeOffsetMillis ms, uncertainty: $halfRoundTripMillis ms")
            openPgp.setTimeOffsetMillis(timeOffsetMillis, halfRoundTripMillis)
            Result.TIME_SYNCED
        } else {
            Timber.d("Time delta to big for sync: $timeOffsetMillis ms. Showing error.")
            Result.TIME_DELTA_TOO_BIG_FOR_SYNC
        }
    }

    enum class Result {
        TIME_SYNCED,
        TIME_DELTA_TOO_BIG_FOR_SYNC,
    }

    companion object {
        @VisibleForTesting
        const val TIME_DELTA_FOR_LOCAL_SYNC_SECS = 10
        private const val MILLIS_PER_SECOND = 1_000L
    }
}
