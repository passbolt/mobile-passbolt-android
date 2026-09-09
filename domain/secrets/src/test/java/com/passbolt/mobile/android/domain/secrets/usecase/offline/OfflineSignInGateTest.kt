package com.passbolt.mobile.android.domain.secrets.usecase.offline

import com.google.common.truth.Truth.assertThat
import com.passbolt.mobile.android.domain.preferences.AccountPreferencesRepository
import com.passbolt.mobile.android.domain.secrets.offline.OfflineCacheRepository
import com.passbolt.mobile.android.ui.AccountFlagsUiModel
import com.passbolt.mobile.android.ui.OfflineModeSetting
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.Duration
import kotlin.test.assertIs

class OfflineSignInGateTest {
    private val accountPreferencesRepository = mock<AccountPreferencesRepository>()
    private val offlineCacheRepository = mock<OfflineCacheRepository>()
    private lateinit var gate: OfflineSignInGate

    @Before
    fun setUp() {
        gate = OfflineSignInGate(accountPreferencesRepository, offlineCacheRepository)
        offlineCacheRepository.stub { onBlocking { countCachedSecrets(USER_ID) } doReturn CACHED_COUNT }
    }

    @Test
    fun `offline mode off is Disabled and leaves the cache alone`() =
        runTest {
            stubFlags(OfflineModeSetting.OFF, lastSync = recent())

            assertIs<OfflineSignInGate.Result.Disabled>(gate.evaluate(USER_ID))
            verify(offlineCacheRepository, never()).removeAllCachedSecrets(USER_ID)
        }

    @Test
    fun `no sync yet is NoCache`() =
        runTest {
            stubFlags(OfflineModeSetting.SELECTED_ENTRIES, lastSync = null)

            assertIs<OfflineSignInGate.Result.NoCache>(gate.evaluate(USER_ID))
        }

    @Test
    fun `sync older than the retention window is Expired and purges the cache`() =
        runTest {
            stubFlags(
                OfflineModeSetting.ALL_ENTRIES,
                lastSync = System.currentTimeMillis() - OfflineSignInGate.DATA_RETENTION.plus(Duration.ofHours(1)).toMillis(),
            )

            assertIs<OfflineSignInGate.Result.Expired>(gate.evaluate(USER_ID))
            verify(offlineCacheRepository).removeAllCachedSecrets(USER_ID)
        }

    @Test
    fun `a clock set back in time counts as expired, not as fresh`() =
        runTest {
            stubFlags(OfflineModeSetting.ALL_ENTRIES, lastSync = System.currentTimeMillis() + Duration.ofHours(2).toMillis())

            assertIs<OfflineSignInGate.Result.Expired>(gate.evaluate(USER_ID))
            verify(offlineCacheRepository).removeAllCachedSecrets(USER_ID)
        }

    @Test
    fun `recent sync with an empty cache is NoCache`() =
        runTest {
            stubFlags(OfflineModeSetting.SELECTED_ENTRIES, lastSync = recent())
            offlineCacheRepository.stub { onBlocking { countCachedSecrets(USER_ID) } doReturn 0 }

            assertIs<OfflineSignInGate.Result.NoCache>(gate.evaluate(USER_ID))
        }

    @Test
    fun `recent sync with cached secrets is Allowed and reports the sync time`() =
        runTest {
            val lastSync = recent()
            stubFlags(OfflineModeSetting.SELECTED_ENTRIES, lastSync = lastSync)

            val result = gate.evaluate(USER_ID)

            assertIs<OfflineSignInGate.Result.Allowed>(result)
            assertThat(result.lastSyncEpochMillis).isEqualTo(lastSync)
            verify(offlineCacheRepository, never()).removeAllCachedSecrets(USER_ID)
        }

    private fun stubFlags(
        mode: OfflineModeSetting,
        lastSync: Long?,
    ) {
        whenever(accountPreferencesRepository.getAccountFlags(USER_ID)).thenReturn(
            AccountFlagsUiModel(
                wasChromeNativeAutofillDialogShown = false,
                offlineMode = mode,
                offlineLastSyncEpochMillis = lastSync,
            ),
        )
    }

    private fun recent() = System.currentTimeMillis() - Duration.ofHours(1).toMillis()

    private companion object {
        private const val USER_ID = "user-id"
        private const val CACHED_COUNT = 3
    }
}
