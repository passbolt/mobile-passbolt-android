package com.passbolt.mobile.android.domain.secrets.usecase.decrypt

import com.passbolt.mobile.android.common.usecase.AsyncUseCase
import com.passbolt.mobile.android.core.architecture.result.DomainResult
import com.passbolt.mobile.android.core.architecture.result.DomainResult.Incomplete.Error.Reason.OFFLINE
import com.passbolt.mobile.android.core.architecture.result.DomainResult.Incomplete.Error.Reason.TIMEOUT
import com.passbolt.mobile.android.domain.accounts.usecase.GetSelectedAccountUseCase
import com.passbolt.mobile.android.domain.secrets.SecretsRepository
import com.passbolt.mobile.android.domain.secrets.offline.OfflineCacheRepository
import com.passbolt.mobile.android.domain.secrets.offline.OfflineSessionState
import com.passbolt.mobile.android.domain.secrets.usecase.offline.OfflineSignInGate
import timber.log.Timber

/**
 * Fetches the (still encrypted) secret of a resource.
 *
 * Online: from the server, as always. When the server cannot be reached and the user has
 * opted in to offline mode (with a cache inside its retention window), the app switches to
 * an offline session on the spot - the banner appears and the cached ciphertext is served.
 * During an offline session the cache is the only source - no request is attempted.
 */
class FetchSecretUseCase(
    private val secretsRepository: SecretsRepository,
    private val offlineCacheRepository: OfflineCacheRepository,
    private val offlineSessionState: OfflineSessionState,
    private val offlineSignInGate: OfflineSignInGate,
    private val getSelectedAccountUseCase: GetSelectedAccountUseCase,
) : AsyncUseCase<FetchSecretUseCase.Input, FetchSecretUseCase.Output> {
    override suspend fun execute(input: Input): Output {
        if (offlineSessionState.isOfflineSession) {
            Timber.d("Fetching secret from offline cache")
            return fromCache(input.resourceId) ?: Output.Failure(DomainResult.Incomplete.NotCached)
        }
        Timber.d("Fetching secret")
        return when (val result = secretsRepository.getSecret(input.resourceId)) {
            is DomainResult.Finished -> Output.EncryptedSecret(result.value.data)
            is DomainResult.Incomplete -> {
                Timber.e("Failed to fetch secret")
                if (result.isNetworkFailure()) {
                    enterOfflineSessionIfAllowed()
                    fromCache(input.resourceId)?.also { Timber.d("Server unreachable - using offline cache") }
                        ?: Output.Failure(result)
                } else {
                    Output.Failure(result)
                }
            }
        }
    }

    // evaluated before reading the cache: an expired cache is purged by the gate, so the
    // retention rule holds for this fallback exactly as it does for an offline sign-in
    private suspend fun enterOfflineSessionIfAllowed() {
        val userId = getSelectedAccountUseCase.execute(Unit).selectedAccount ?: return
        when (val gate = offlineSignInGate.evaluate(userId)) {
            is OfflineSignInGate.Result.Allowed -> offlineSessionState.enterOfflineSession()
            else -> Timber.d("[Offline] Server unreachable, offline session not possible: $gate")
        }
    }

    private suspend fun fromCache(resourceId: String): Output.EncryptedSecret? {
        val userId = getSelectedAccountUseCase.execute(Unit).selectedAccount ?: return null
        return offlineCacheRepository
            .getCachedSecret(resourceId, userId)
            ?.let { Output.EncryptedSecret(it.encryptedSecret, fromOfflineCache = true) }
    }

    private fun DomainResult.Incomplete.isNetworkFailure() = this is DomainResult.Incomplete.Error && reason in setOf(OFFLINE, TIMEOUT)

    data class Input(
        val resourceId: String,
    )

    sealed class Output {
        data class EncryptedSecret(
            val encryptedSecret: String,
            val fromOfflineCache: Boolean = false,
        ) : Output()

        data class Failure(
            val incomplete: DomainResult.Incomplete,
        ) : Output()
    }
}
