package com.passbolt.mobile.android.domain.secrets.usecase.offline

import com.passbolt.mobile.android.core.architecture.result.DomainResult
import com.passbolt.mobile.android.core.mvp.authentication.AuthenticatedUseCaseOutput
import com.passbolt.mobile.android.core.mvp.authentication.CompleteAuthenticatedOutput
import com.passbolt.mobile.android.core.mvp.authentication.IncompleteAuthenticatedOutput
import com.passbolt.mobile.android.domain.accounts.usecase.GetSelectedAccountUseCase
import com.passbolt.mobile.android.domain.preferences.AccountFlagsUpdate
import com.passbolt.mobile.android.domain.preferences.AccountPreferencesRepository
import com.passbolt.mobile.android.domain.secrets.offline.OfflineCacheRepository
import com.passbolt.mobile.android.domain.secrets.offline.OfflineCachedSecret
import com.passbolt.mobile.android.domain.secrets.offline.OfflineSyncStatus
import com.passbolt.mobile.android.domain.secrets.offline.OfflineSyncTracker
import com.passbolt.mobile.android.domain.secrets.offline.ResourceModifiedState
import com.passbolt.mobile.android.ui.OfflineModeSetting
import timber.log.Timber
import java.time.ZonedDateTime

/**
 * Keeps the local encrypted secret cache in step with the offline setting.
 *
 * Runs at the end of every full data refresh, after the resources table has been
 * updated. Only secrets whose resource is newer than the cached copy (or not cached
 * yet) are fetched, in batches through `resources.json?contain[secret]=1&filter[has-id][]`,
 * so a steady-state refresh costs nothing when nothing changed. Secrets of resources
 * that left the offline set are deleted. A failure leaves the existing cache in place.
 */
class OfflineSecretsSyncInteractor(
    private val offlineCacheRepository: OfflineCacheRepository,
    private val accountPreferencesRepository: AccountPreferencesRepository,
    private val getSelectedAccountUseCase: GetSelectedAccountUseCase,
    private val offlineSyncTracker: OfflineSyncTracker,
) {
    suspend fun sync(onProgress: suspend (done: Int, total: Int) -> Unit = { _, _ -> }): Output {
        val userId = requireNotNull(getSelectedAccountUseCase.execute(Unit).selectedAccount)
        val mode = accountPreferencesRepository.getAccountFlags(userId).offlineMode
        if (!mode.isEnabled) {
            if (offlineCacheRepository.countCachedSecrets(userId) > 0) {
                Timber.d("[Offline] Mode is off - dropping cached secrets")
                offlineCacheRepository.removeAllCachedSecrets(userId)
            }
            offlineSyncTracker.update(OfflineSyncStatus.Idle)
            return Output.Disabled
        }
        offlineSyncTracker.update(OfflineSyncStatus.InProgress(done = 0, total = 0))

        val localResources = offlineCacheRepository.getLocalResourcesState(userId).associateBy { it.resourceId }
        val targetIds = targetIds(mode, localResources.keys, userId)
        val stale = removeLeftoversAndFindStale(targetIds, localResources, userId)
        Timber.d("[Offline] ${targetIds.size} in offline set, ${stale.size} to fetch")

        val fetched =
            when (val fetch = fetchInBatches(stale, localResources, userId, onProgress)) {
                is Fetch.Failed -> return Output.Failure(fetch.incomplete)
                is Fetch.Done -> fetch.count
            }

        val finishedAt = System.currentTimeMillis()
        accountPreferencesRepository.updateAccountFlags(
            AccountFlagsUpdate(offlineLastSyncEpochMillis = finishedAt),
            userId,
        )
        val cachedCount = offlineCacheRepository.countCachedSecrets(userId)
        Timber.d("[Offline] Sync finished - $cachedCount secrets cached")
        offlineSyncTracker.update(
            OfflineSyncStatus.Success(cachedCount = cachedCount, fetchedCount = fetched, atEpochMillis = finishedAt),
        )
        return Output.Success(cachedCount)
    }

    private suspend fun targetIds(
        mode: OfflineModeSetting,
        localResourceIds: Set<String>,
        userId: String,
    ): Set<String> =
        when (mode) {
            OfflineModeSetting.ALL_ENTRIES -> localResourceIds
            OfflineModeSetting.SELECTED_ENTRIES ->
                offlineCacheRepository.getMarkedResourceIds(userId).filter { it in localResourceIds }.toSet()
            OfflineModeSetting.OFF -> emptySet()
        }

    /** Drops cached secrets that left the offline set; returns the ids whose cached copy is missing or outdated. */
    private suspend fun removeLeftoversAndFindStale(
        targetIds: Set<String>,
        localResources: Map<String, ResourceModifiedState>,
        userId: String,
    ): List<String> {
        val cached = offlineCacheRepository.getCachedSecretsState(userId).associateBy { it.resourceId }
        val toRemove = cached.keys - targetIds
        if (toRemove.isNotEmpty()) {
            Timber.d("[Offline] Removing ${toRemove.size} cached secrets no longer in the offline set")
            offlineCacheRepository.removeCachedSecrets(toRemove.toList(), userId)
        }
        return targetIds.filter { id ->
            val cachedState = cached[id]
            val resource = requireNotNull(localResources[id])
            cachedState == null || cachedState.modified.isBefore(resource.modified)
        }
    }

    /** Fetches and stores the stale secrets batch by batch; a failed batch stops the sync and leaves the previous cache in place. */
    private suspend fun fetchInBatches(
        stale: List<String>,
        localResources: Map<String, ResourceModifiedState>,
        userId: String,
        onProgress: suspend (done: Int, total: Int) -> Unit,
    ): Fetch {
        var done = 0
        stale.chunked(BATCH_SIZE).forEach { batch ->
            when (val result = offlineCacheRepository.fetchSecretsForResources(batch)) {
                is DomainResult.Incomplete -> {
                    Timber.e("[Offline] Secret batch fetch failed: $result")
                    offlineSyncTracker.update(OfflineSyncStatus.Failure(atEpochMillis = System.currentTimeMillis()))
                    return Fetch.Failed(result)
                }
                is DomainResult.Finished -> {
                    val now = ZonedDateTime.now()
                    offlineCacheRepository.putCachedSecrets(
                        result.value.map {
                            OfflineCachedSecret(
                                resourceId = it.resourceId,
                                encryptedSecret = it.encryptedSecret,
                                resourceModified = resourceModifiedFor(it.resourceId, localResources, it.resourceModified),
                                cachedAt = now,
                            )
                        },
                        userId,
                    )
                    done += batch.size
                    offlineSyncTracker.update(OfflineSyncStatus.InProgress(done = done, total = stale.size))
                    onProgress(done, stale.size)
                }
            }
        }
        return Fetch.Done(done)
    }

    private sealed class Fetch {
        data class Done(
            val count: Int,
        ) : Fetch()

        data class Failed(
            val incomplete: DomainResult.Incomplete,
        ) : Fetch()
    }

    // Prefer the local resource's stamp (the one the next comparison will use) so a
    // clock difference between the two responses cannot make an entry look stale forever.
    private fun resourceModifiedFor(
        resourceId: String,
        localResources: Map<String, ResourceModifiedState>,
        fromServer: ZonedDateTime,
    ): ZonedDateTime = localResources[resourceId]?.modified ?: fromServer

    sealed class Output : AuthenticatedUseCaseOutput {
        data object Disabled : Output(), CompleteAuthenticatedOutput

        data class Success(
            val cachedCount: Int,
        ) : Output(),
            CompleteAuthenticatedOutput

        data class Failure(
            override val incomplete: DomainResult.Incomplete,
        ) : Output(),
            IncompleteAuthenticatedOutput
    }

    private companion object {
        // ~55 chars per uuid in the query string - 40 keeps the request URL short
        private const val BATCH_SIZE = 40
    }
}
