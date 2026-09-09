package com.passbolt.mobile.android.domain.secrets.usecase.offline

import com.passbolt.mobile.android.common.usecase.AsyncUseCase
import com.passbolt.mobile.android.core.architecture.result.DomainResult
import com.passbolt.mobile.android.domain.accounts.usecase.GetSelectedAccountUseCase
import com.passbolt.mobile.android.domain.preferences.AccountFlagsUpdate
import com.passbolt.mobile.android.domain.preferences.AccountPreferencesRepository
import com.passbolt.mobile.android.domain.secrets.offline.OfflineCacheRepository
import com.passbolt.mobile.android.domain.secrets.offline.OfflineCachedSecret
import com.passbolt.mobile.android.domain.secrets.offline.OfflineSessionState
import com.passbolt.mobile.android.ui.OfflineModeSetting
import timber.log.Timber
import java.time.ZonedDateTime

/**
 * Marks a resource as available offline and, when the server is reachable, caches its
 * secret right away so the entry is usable offline without waiting for the next refresh.
 */
class MarkResourceOfflineUseCase(
    private val offlineCacheRepository: OfflineCacheRepository,
    private val getSelectedAccountUseCase: GetSelectedAccountUseCase,
    private val offlineSessionState: OfflineSessionState,
) : AsyncUseCase<MarkResourceOfflineUseCase.Input, MarkResourceOfflineUseCase.Output> {
    override suspend fun execute(input: Input): Output {
        val userId = requireNotNull(getSelectedAccountUseCase.execute(Unit).selectedAccount)
        offlineCacheRepository.markResource(input.resourceId, userId)
        if (offlineSessionState.isOfflineSession) {
            return Output.MarkedNotCached
        }
        return when (val result = offlineCacheRepository.fetchSecretsForResources(listOf(input.resourceId))) {
            is DomainResult.Finished -> {
                val now = ZonedDateTime.now()
                offlineCacheRepository.putCachedSecrets(
                    result.value.map {
                        OfflineCachedSecret(
                            resourceId = it.resourceId,
                            encryptedSecret = it.encryptedSecret,
                            resourceModified = input.resourceModified ?: it.resourceModified,
                            cachedAt = now,
                        )
                    },
                    userId,
                )
                Output.Success
            }
            is DomainResult.Incomplete -> {
                Timber.e("[Offline] Could not cache secret after marking: $result")
                Output.MarkedNotCached
            }
        }
    }

    data class Input(
        val resourceId: String,
        val resourceModified: ZonedDateTime?,
    )

    sealed class Output {
        data object Success : Output()

        // the mark is stored; the secret will be cached by the next refresh
        data object MarkedNotCached : Output()
    }
}

class UnmarkResourceOfflineUseCase(
    private val offlineCacheRepository: OfflineCacheRepository,
    private val getSelectedAccountUseCase: GetSelectedAccountUseCase,
    private val accountPreferencesRepository: AccountPreferencesRepository,
) : AsyncUseCase<UnmarkResourceOfflineUseCase.Input, Unit> {
    override suspend fun execute(input: Input) {
        val userId = requireNotNull(getSelectedAccountUseCase.execute(Unit).selectedAccount)
        offlineCacheRepository.unmarkResource(input.resourceId, userId)
        // with "all entries" the secret stays cached regardless of the mark
        if (accountPreferencesRepository.getAccountFlags(userId).offlineMode != OfflineModeSetting.ALL_ENTRIES) {
            offlineCacheRepository.removeCachedSecrets(listOf(input.resourceId), userId)
        }
    }

    data class Input(
        val resourceId: String,
    )
}

class IsResourceMarkedOfflineUseCase(
    private val offlineCacheRepository: OfflineCacheRepository,
    private val getSelectedAccountUseCase: GetSelectedAccountUseCase,
) : AsyncUseCase<IsResourceMarkedOfflineUseCase.Input, Boolean> {
    override suspend fun execute(input: Input): Boolean {
        val userId = requireNotNull(getSelectedAccountUseCase.execute(Unit).selectedAccount)
        return offlineCacheRepository.isResourceMarked(input.resourceId, userId)
    }

    data class Input(
        val resourceId: String,
    )
}

class SetOfflineModeUseCase(
    private val offlineCacheRepository: OfflineCacheRepository,
    private val getSelectedAccountUseCase: GetSelectedAccountUseCase,
    private val accountPreferencesRepository: AccountPreferencesRepository,
) : AsyncUseCase<SetOfflineModeUseCase.Input, Unit> {
    override suspend fun execute(input: Input) {
        val userId = requireNotNull(getSelectedAccountUseCase.execute(Unit).selectedAccount)
        accountPreferencesRepository.updateAccountFlags(AccountFlagsUpdate(offlineMode = input.mode), userId)
        if (!input.mode.isEnabled) {
            Timber.d("[Offline] Offline mode disabled - dropping cached secrets")
            offlineCacheRepository.removeAllCachedSecrets(userId)
            accountPreferencesRepository.updateAccountFlags(AccountFlagsUpdate(clearOfflineLastSync = true), userId)
        }
    }

    data class Input(
        val mode: OfflineModeSetting,
    )
}

class ClearOfflineCacheUseCase(
    private val offlineCacheRepository: OfflineCacheRepository,
    private val getSelectedAccountUseCase: GetSelectedAccountUseCase,
    private val accountPreferencesRepository: AccountPreferencesRepository,
) : AsyncUseCase<Unit, Unit> {
    override suspend fun execute(input: Unit) {
        val userId = getSelectedAccountUseCase.execute(Unit).selectedAccount ?: return
        Timber.d("[Offline] Clearing offline cache and marks")
        offlineCacheRepository.removeAllCachedSecrets(userId)
        offlineCacheRepository.clearMarks(userId)
        accountPreferencesRepository.updateAccountFlags(AccountFlagsUpdate(clearOfflineLastSync = true), userId)
    }
}

/** Drops cached ciphertext (keeps the marks) - used on sign-out. */
class ClearOfflineSecretsUseCase(
    private val offlineCacheRepository: OfflineCacheRepository,
    private val getSelectedAccountUseCase: GetSelectedAccountUseCase,
    private val accountPreferencesRepository: AccountPreferencesRepository,
    private val offlineSessionState: OfflineSessionState,
) : AsyncUseCase<Unit, Unit> {
    override suspend fun execute(input: Unit) {
        offlineSessionState.exitOfflineSession()
        val userId = getSelectedAccountUseCase.execute(Unit).selectedAccount ?: return
        Timber.d("[Offline] Clearing cached secrets")
        offlineCacheRepository.removeAllCachedSecrets(userId)
        accountPreferencesRepository.updateAccountFlags(AccountFlagsUpdate(clearOfflineLastSync = true), userId)
    }
}

class GetOfflineCacheStatusUseCase(
    private val offlineCacheRepository: OfflineCacheRepository,
    private val getSelectedAccountUseCase: GetSelectedAccountUseCase,
    private val accountPreferencesRepository: AccountPreferencesRepository,
    private val offlineSessionState: OfflineSessionState,
) : AsyncUseCase<Unit, GetOfflineCacheStatusUseCase.Output> {
    override suspend fun execute(input: Unit): Output {
        val userId = requireNotNull(getSelectedAccountUseCase.execute(Unit).selectedAccount)
        val flags = accountPreferencesRepository.getAccountFlags(userId)
        return Output(
            mode = flags.offlineMode,
            lastSyncEpochMillis = flags.offlineLastSyncEpochMillis,
            cachedCount = offlineCacheRepository.countCachedSecrets(userId),
            markedCount = offlineCacheRepository.countMarkedResources(userId),
            isOfflineSession = offlineSessionState.isOfflineSession,
        )
    }

    data class Output(
        val mode: OfflineModeSetting,
        val lastSyncEpochMillis: Long?,
        val cachedCount: Int,
        val markedCount: Int,
        val isOfflineSession: Boolean,
    )
}
