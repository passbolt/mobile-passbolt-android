package com.passbolt.mobile.android.domain.secrets.usecase.offline

import com.passbolt.mobile.android.domain.preferences.AccountPreferencesRepository
import com.passbolt.mobile.android.domain.secrets.offline.OfflineCacheRepository
import timber.log.Timber
import java.time.Duration
import java.time.Instant

/**
 * Decides whether an account may be unlocked without the server.
 *
 * Offline sign-in is allowed only when the user opted in, a sync completed within the
 * data-retention window (7 days - the community-edition value of the browser extension's
 * offline mode) and something is actually cached. An expired cache is purged on the spot so
 * stale ciphertext does not outlive the retention period.
 *
 * The passphrase itself is verified against the local private key by the caller, exactly
 * as for an online sign-in - this gate adds no new way to test passphrases.
 */
class OfflineSignInGate(
    private val accountPreferencesRepository: AccountPreferencesRepository,
    private val offlineCacheRepository: OfflineCacheRepository,
) {
    suspend fun evaluate(userId: String): Result {
        val flags = accountPreferencesRepository.getAccountFlags(userId)
        val lastSync = flags.offlineLastSyncEpochMillis?.let { Instant.ofEpochMilli(it) }
        val now = Instant.now()
        return when {
            !flags.offlineMode.isEnabled -> Result.Disabled
            lastSync == null -> Result.NoCache
            // a clock set back in time is treated as expired, never as "fresh again"
            now.isBefore(lastSync) || Duration.between(lastSync, now) > DATA_RETENTION -> {
                Timber.d("[Offline] Cache expired (last sync $lastSync) - purging")
                offlineCacheRepository.removeAllCachedSecrets(userId)
                Result.Expired
            }
            offlineCacheRepository.countCachedSecrets(userId) == 0 -> Result.NoCache
            else -> Result.Allowed(lastSyncEpochMillis = lastSync.toEpochMilli())
        }
    }

    sealed class Result {
        data class Allowed(
            val lastSyncEpochMillis: Long,
        ) : Result()

        data object Disabled : Result()

        data object NoCache : Result()

        data object Expired : Result()
    }

    companion object {
        val DATA_RETENTION: Duration = Duration.ofDays(7)
    }
}
