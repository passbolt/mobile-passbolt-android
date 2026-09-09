package com.passbolt.mobile.android.feature.settings.screen.appsettings.offlinemode

import androidx.lifecycle.viewModelScope
import com.passbolt.mobile.android.common.datarefresh.DataRefreshStatus
import com.passbolt.mobile.android.common.datarefresh.DataRefreshTrackingFlow
import com.passbolt.mobile.android.core.compose.SideEffectViewModel
import com.passbolt.mobile.android.core.mvp.coroutinecontext.CoroutineLaunchContext
import com.passbolt.mobile.android.domain.secrets.offline.OfflineSessionState
import com.passbolt.mobile.android.domain.secrets.offline.OfflineSyncStatus
import com.passbolt.mobile.android.domain.secrets.offline.OfflineSyncTracker
import com.passbolt.mobile.android.domain.secrets.usecase.offline.ClearOfflineCacheUseCase
import com.passbolt.mobile.android.domain.secrets.usecase.offline.GetOfflineCacheStatusUseCase
import com.passbolt.mobile.android.domain.secrets.usecase.offline.SetOfflineModeUseCase
import com.passbolt.mobile.android.feature.settings.screen.appsettings.offlinemode.OfflineModeSettingsIntent.CancelClear
import com.passbolt.mobile.android.feature.settings.screen.appsettings.offlinemode.OfflineModeSettingsIntent.ClearClick
import com.passbolt.mobile.android.feature.settings.screen.appsettings.offlinemode.OfflineModeSettingsIntent.ConfirmClear
import com.passbolt.mobile.android.feature.settings.screen.appsettings.offlinemode.OfflineModeSettingsIntent.GoBack
import com.passbolt.mobile.android.feature.settings.screen.appsettings.offlinemode.OfflineModeSettingsIntent.SelectMode
import com.passbolt.mobile.android.feature.settings.screen.appsettings.offlinemode.OfflineModeSettingsIntent.SyncNow
import com.passbolt.mobile.android.feature.settings.screen.appsettings.offlinemode.OfflineModeSettingsIntent.ToggleEnabled
import com.passbolt.mobile.android.feature.settings.screen.appsettings.offlinemode.OfflineModeSettingsSideEffect.NavigateUp
import com.passbolt.mobile.android.feature.settings.screen.appsettings.offlinemode.OfflineModeSettingsSideEffect.ShowSyncResult
import com.passbolt.mobile.android.feature.settings.screen.appsettings.offlinemode.OfflineModeSettingsSideEffect.StartDataRefresh
import com.passbolt.mobile.android.ui.OfflineModeSetting
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch

internal class OfflineModeSettingsViewModel(
    private val getOfflineCacheStatusUseCase: GetOfflineCacheStatusUseCase,
    private val setOfflineModeUseCase: SetOfflineModeUseCase,
    private val clearOfflineCacheUseCase: ClearOfflineCacheUseCase,
    private val dataRefreshTrackingFlow: DataRefreshTrackingFlow,
    private val offlineSyncTracker: OfflineSyncTracker,
    private val offlineSessionState: OfflineSessionState,
    private val coroutineLaunchContext: CoroutineLaunchContext,
) : SideEffectViewModel<OfflineModeSettingsState, OfflineModeSettingsSideEffect>(OfflineModeSettingsState()) {
    init {
        loadStatus()
        observeDataRefresh()
        observeSyncProgress()
        observeOfflineSession()
    }

    fun onIntent(intent: OfflineModeSettingsIntent) {
        when (intent) {
            GoBack -> emitSideEffect(NavigateUp)
            ToggleEnabled -> setMode(if (viewState.value.isEnabled) OfflineModeSetting.OFF else OfflineModeSetting.SELECTED_ENTRIES)
            is SelectMode -> setMode(intent.mode)
            SyncNow -> startRefresh()
            ClearClick -> updateViewState { copy(showClearConfirmation = true) }
            CancelClear -> updateViewState { copy(showClearConfirmation = false) }
            ConfirmClear -> clearCache()
        }
    }

    private fun loadStatus() {
        viewModelScope.launch(coroutineLaunchContext.io) {
            val status = getOfflineCacheStatusUseCase.execute(Unit)
            updateViewState {
                copy(
                    mode = status.mode,
                    cachedCount = status.cachedCount,
                    markedCount = status.markedCount,
                    lastSyncEpochMillis = status.lastSyncEpochMillis,
                    isOfflineSession = status.isOfflineSession,
                )
            }
        }
    }

    // the refresh runs in a foreground service; the user only sees that something happened
    // if this screen mirrors its progress and says how it ended
    private fun startRefresh() {
        updateViewState { copy(isRefreshing = true, refreshProgress = 0f) }
        emitSideEffect(StartDataRefresh)
    }

    private fun observeDataRefresh() {
        viewModelScope.launch(coroutineLaunchContext.io) {
            // the tracking flow replays its last value - only a refresh seen starting while
            // this screen is open gets a result message
            var sawInProgress = false
            dataRefreshTrackingFlow.dataRefreshStatusFlow.collect { status ->
                when (status) {
                    is DataRefreshStatus.InProgress -> {
                        sawInProgress = true
                        updateViewState { copy(isRefreshing = true, refreshProgress = status.progress) }
                    }
                    DataRefreshStatus.Idle.FinishedWithSuccess -> {
                        updateViewState { copy(isRefreshing = false, syncProgress = null) }
                        loadStatus()
                        if (sawInProgress) {
                            sawInProgress = false
                            emitSideEffect(ShowSyncResult(syncResultAfterSuccessfulRefresh()))
                        }
                    }
                    DataRefreshStatus.Idle.FinishedWithFailure -> {
                        updateViewState { copy(isRefreshing = false, syncProgress = null) }
                        loadStatus()
                        if (sawInProgress) {
                            sawInProgress = false
                            emitSideEffect(ShowSyncResult(SyncResult.Failed))
                        }
                    }
                    DataRefreshStatus.Idle.NotCompleted -> updateViewState { copy(isRefreshing = false) }
                }
            }
        }
    }

    private suspend fun syncResultAfterSuccessfulRefresh(): SyncResult =
        when (val sync = offlineSyncTracker.status.value) {
            is OfflineSyncStatus.Failure -> SyncResult.Partial
            is OfflineSyncStatus.Success -> SyncResult.Success(sync.cachedCount)
            else -> SyncResult.Success(getOfflineCacheStatusUseCase.execute(Unit).cachedCount)
        }

    private fun observeSyncProgress() {
        viewModelScope.launch(coroutineLaunchContext.io) {
            offlineSyncTracker.status.collect { status ->
                updateViewState {
                    copy(
                        syncProgress =
                            (status as? OfflineSyncStatus.InProgress)?.let { SyncProgress(done = it.done, total = it.total) },
                    )
                }
            }
        }
    }

    private fun observeOfflineSession() {
        viewModelScope.launch(coroutineLaunchContext.io) {
            offlineSessionState.isOfflineSessionFlow.drop(1).collect { loadStatus() }
        }
    }

    // a change of mode is followed by a refresh so the cache matches the new choice right away
    private fun setMode(mode: OfflineModeSetting) {
        viewModelScope.launch(coroutineLaunchContext.io) {
            setOfflineModeUseCase.execute(SetOfflineModeUseCase.Input(mode))
            updateViewState { copy(mode = mode) }
            loadStatus()
            if (mode.isEnabled && !viewState.value.isOfflineSession) {
                startRefresh()
            }
        }
    }

    private fun clearCache() {
        viewModelScope.launch(coroutineLaunchContext.io) {
            clearOfflineCacheUseCase.execute(Unit)
            updateViewState { copy(showClearConfirmation = false) }
            loadStatus()
        }
    }
}
