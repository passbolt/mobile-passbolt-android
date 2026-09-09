package com.passbolt.mobile.android.feature.settings.screen.appsettings.offlinemode

import android.content.Context
import android.text.format.DateUtils
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.passbolt.mobile.android.core.compose.SideEffectDispatcher
import com.passbolt.mobile.android.core.fulldatarefresh.service.DataRefreshService
import com.passbolt.mobile.android.core.navigation.compose.AppNavigator
import com.passbolt.mobile.android.core.ui.banner.WarningBanner
import com.passbolt.mobile.android.core.ui.button.PrimaryButton
import com.passbolt.mobile.android.core.ui.menu.SwitchableSettingsItem
import com.passbolt.mobile.android.core.ui.snackbar.ColoredSnackbarVisuals
import com.passbolt.mobile.android.core.ui.topbar.BackNavigationIcon
import com.passbolt.mobile.android.core.ui.topbar.TitleAppBar
import com.passbolt.mobile.android.domain.secrets.usecase.offline.OfflineSignInGate
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
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import com.passbolt.mobile.android.core.localization.R as LocalizationR
import com.passbolt.mobile.android.core.ui.R as CoreUiR

@Composable
internal fun OfflineModeSettingsScreen(
    modifier: Modifier = Modifier,
    navigator: AppNavigator = koinInject(),
    viewModel: OfflineModeSettingsViewModel = koinViewModel(),
) {
    val context = LocalContext.current
    val state = viewModel.viewState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val successColor = colorResource(CoreUiR.color.green)
    val errorColor = colorResource(CoreUiR.color.red)
    OfflineModeSettingsScreen(
        modifier = modifier,
        state = state.value,
        snackbarHostState = snackbarHostState,
        onIntent = viewModel::onIntent,
    )
    SideEffectDispatcher(viewModel.sideEffect) {
        when (it) {
            NavigateUp -> navigator.navigateBack()
            StartDataRefresh -> DataRefreshService.start(context)
            is ShowSyncResult ->
                coroutineScope.launch {
                    snackbarHostState.showSnackbar(
                        ColoredSnackbarVisuals(
                            message = syncResultMessage(context, it.result),
                            backgroundColor = if (it.result is SyncResult.Success) successColor else errorColor,
                        ),
                    )
                }
        }
    }
}

private fun syncResultMessage(
    context: Context,
    result: SyncResult,
): String =
    when (result) {
        is SyncResult.Success -> context.getString(LocalizationR.string.offline_settings_sync_result_success, result.cachedCount)
        SyncResult.Partial -> context.getString(LocalizationR.string.offline_settings_sync_result_partial)
        SyncResult.Failed -> context.getString(LocalizationR.string.offline_settings_sync_result_failed)
    }

@Suppress("LongMethod")
@Composable
private fun OfflineModeSettingsScreen(
    state: OfflineModeSettingsState,
    snackbarHostState: SnackbarHostState,
    onIntent: (OfflineModeSettingsIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TitleAppBar(
                title = stringResource(LocalizationR.string.settings_app_settings_offline_mode),
                navigationIcon = { BackNavigationIcon(onBackClick = { onIntent(GoBack) }) },
            )
        },
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                snackbar = { data ->
                    val visuals = data.visuals as? ColoredSnackbarVisuals
                    if (visuals != null) {
                        Snackbar(snackbarData = data, containerColor = visuals.backgroundColor, contentColor = visuals.contentColor)
                    } else {
                        Snackbar(snackbarData = data)
                    }
                },
            )
        },
        content = { paddingValues ->
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .verticalScroll(rememberScrollState()),
            ) {
                Text(
                    text = stringResource(LocalizationR.string.offline_settings_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                )
                SwitchableSettingsItem(
                    iconPainter = painterResource(CoreUiR.drawable.ic_lock),
                    title = stringResource(LocalizationR.string.offline_settings_enable),
                    isChecked = state.isEnabled,
                    onCheckedChange = { onIntent(ToggleEnabled) },
                    isEnabled = !state.isOfflineSession,
                )
                if (state.isEnabled) {
                    ModeOption(
                        title = stringResource(LocalizationR.string.offline_settings_mode_selected),
                        description = stringResource(LocalizationR.string.offline_settings_mode_selected_description),
                        selected = state.mode == OfflineModeSetting.SELECTED_ENTRIES,
                        enabled = !state.isOfflineSession,
                        onClick = { onIntent(SelectMode(OfflineModeSetting.SELECTED_ENTRIES)) },
                    )
                    ModeOption(
                        title = stringResource(LocalizationR.string.offline_settings_mode_all),
                        description = stringResource(LocalizationR.string.offline_settings_mode_all_description),
                        selected = state.mode == OfflineModeSetting.ALL_ENTRIES,
                        enabled = !state.isOfflineSession,
                        onClick = { onIntent(SelectMode(OfflineModeSetting.ALL_ENTRIES)) },
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    StatusSection(state)
                    Text(
                        text = stringResource(LocalizationR.string.offline_settings_retention_note),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                    if (state.isOfflineSession) {
                        WarningBanner(
                            text = stringResource(LocalizationR.string.offline_settings_offline_session_note),
                            modifier = Modifier.padding(16.dp),
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    SyncSection(state, onIntent)
                    TextButton(
                        onClick = { onIntent(ClearClick) },
                        enabled = !state.isRefreshing,
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp, bottom = 16.dp),
                    ) {
                        Text(
                            text = stringResource(LocalizationR.string.offline_settings_clear),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
            if (state.showClearConfirmation) {
                AlertDialog(
                    onDismissRequest = { onIntent(CancelClear) },
                    title = { Text(text = stringResource(LocalizationR.string.offline_settings_clear)) },
                    text = { Text(text = stringResource(LocalizationR.string.offline_settings_clear_confirmation)) },
                    confirmButton = {
                        TextButton(onClick = { onIntent(ConfirmClear) }) {
                            Text(text = stringResource(LocalizationR.string.offline_settings_clear_confirm))
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { onIntent(CancelClear) }) {
                            Text(text = stringResource(LocalizationR.string.cancel))
                        }
                    },
                )
            }
        },
    )
}

@Composable
private fun StatusSection(state: OfflineModeSettingsState) {
    Column {
        val context = LocalContext.current
        StatusLine(
            label = stringResource(LocalizationR.string.offline_settings_status_cached),
            value = state.cachedCount.toString(),
        )
        if (state.mode == OfflineModeSetting.SELECTED_ENTRIES) {
            StatusLine(
                label = stringResource(LocalizationR.string.offline_settings_status_marked),
                value = state.markedCount.toString(),
            )
        }
        StatusLine(
            label = stringResource(LocalizationR.string.offline_settings_status_last_sync),
            value =
                state.lastSyncEpochMillis
                    ?.let { relativeTime(context, it) }
                    ?: stringResource(LocalizationR.string.offline_settings_status_never),
        )
        // the retention window is renewed by every update - showing the concrete date makes
        // "7 days" tangible and shows that it moves forward
        state.lastSyncEpochMillis?.let { lastSync ->
            StatusLine(
                label = stringResource(LocalizationR.string.offline_settings_status_valid_until),
                value =
                    DateUtils.formatDateTime(
                        context,
                        lastSync + OfflineSignInGate.DATA_RETENTION.toMillis(),
                        DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_TIME or DateUtils.FORMAT_ABBREV_MONTH,
                    ),
            )
        }
    }
}

@Composable
private fun SyncSection(
    state: OfflineModeSettingsState,
    onIntent: (OfflineModeSettingsIntent) -> Unit,
) {
    Column {
        PrimaryButton(
            text =
                stringResource(
                    if (state.isRefreshing) {
                        LocalizationR.string.offline_settings_syncing
                    } else {
                        LocalizationR.string.offline_settings_sync_now
                    },
                ),
            isEnabled = !state.isRefreshing && !state.isOfflineSession,
            onClick = { onIntent(SyncNow) },
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        if (state.isRefreshing) {
            LinearProgressIndicator(
                progress = { state.refreshProgress },
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
            )
            Text(
                text =
                    state.syncProgress
                        ?.takeIf { it.total > 0 }
                        ?.let { stringResource(LocalizationR.string.offline_settings_syncing_progress, it.done, it.total) }
                        ?: stringResource(LocalizationR.string.offline_settings_syncing_indeterminate),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        } else {
            Text(
                text = stringResource(LocalizationR.string.offline_settings_sync_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
    }
}

private fun relativeTime(
    context: Context,
    epochMillis: Long,
): String {
    val age = System.currentTimeMillis() - epochMillis
    return if (age in 0 until DateUtils.MINUTE_IN_MILLIS) {
        context.getString(LocalizationR.string.offline_settings_status_just_now)
    } else {
        DateUtils.getRelativeTimeSpanString(epochMillis).toString()
    }
}

@Composable
private fun ModeOption(
    title: String,
    description: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(enabled = enabled, onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = onClick, enabled = enabled)
        Column(modifier = Modifier.padding(start = 8.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun StatusLine(
    label: String,
    value: String,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun OfflineModeSettingsPreview() {
    OfflineModeSettingsScreen(
        state =
            OfflineModeSettingsState(
                mode = OfflineModeSetting.SELECTED_ENTRIES,
                cachedCount = 12,
                markedCount = 12,
                lastSyncEpochMillis = System.currentTimeMillis(),
            ),
        snackbarHostState = SnackbarHostState(),
        onIntent = {},
    )
}
