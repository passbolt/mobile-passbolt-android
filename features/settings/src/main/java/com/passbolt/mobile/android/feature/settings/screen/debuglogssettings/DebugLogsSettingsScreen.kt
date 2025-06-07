/**
 * Passbolt - Open source password manager for teams
 * Copyright (c) 2021 Passbolt SA
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General
 * Public License (AGPL) as published by the Free Software Foundation version 3.
 *
 * The name "Passbolt" is a registered trademark of Passbolt SA, and Passbolt SA hereby declines to grant a trademark
 * license to "Passbolt" pursuant to the GNU Affero General Public License version 3 Section 7(e), without a separate
 * agreement with Passbolt SA.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program. If not,
 * see GNU Affero General Public License v3 (http://www.gnu.org/licenses/agpl-3.0.html).
 *
 * @copyright Copyright (c) Passbolt SA (https://www.passbolt.com)
 * @license https://opensource.org/licenses/AGPL-3.0 AGPL License
 * @link https://www.passbolt.com Passbolt (tm)
 * @since v1.0
 */

package com.passbolt.mobile.android.feature.settings.screen.debuglogssettings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.passbolt.mobile.android.core.compose.SideEffectDispatcher
import com.passbolt.mobile.android.core.ui.R
import com.passbolt.mobile.android.core.ui.compose.menu.OpenableSettingsItem
import com.passbolt.mobile.android.core.ui.compose.menu.SwitchableSettingsItem
import com.passbolt.mobile.android.core.ui.compose.topbar.BackNavigationIcon
import com.passbolt.mobile.android.core.ui.compose.topbar.TitleAppBar
import com.passbolt.mobile.android.feature.settings.screen.debuglogssettings.DebugLogsScreenSideEffect.NavigateToLogs
import com.passbolt.mobile.android.feature.settings.screen.debuglogssettings.DebugLogsScreenSideEffect.NavigateUp
import com.passbolt.mobile.android.feature.settings.screen.debuglogssettings.DebugLogsScreenSideEffect.OpenHelpWebsite
import com.passbolt.mobile.android.feature.settings.screen.debuglogssettings.DebugLogsSettingsIntent.AccessLogs
import com.passbolt.mobile.android.feature.settings.screen.debuglogssettings.DebugLogsSettingsIntent.GoBack
import com.passbolt.mobile.android.feature.settings.screen.debuglogssettings.DebugLogsSettingsIntent.ToggleDebugLogs
import org.koin.androidx.compose.koinViewModel
import com.passbolt.mobile.android.core.localization.R as LocalizationR

@Composable
internal fun DebugLogsSettingsScreen(
    navigation: DebugLogSettingsNavigation,
    modifier: Modifier = Modifier,
    viewModel: DebugLogsSettingsViewModel = koinViewModel(),
) {
    val state = viewModel.viewState.collectAsStateWithLifecycle()

    DebugLogsSettingsScreen(
        modifier = modifier,
        state = state.value,
        onIntent = viewModel::onIntent,
    )

    SideEffectDispatcher(viewModel.sideEffect) {
        when (it) {
            NavigateToLogs -> navigation.navigateToLogs()
            NavigateUp -> navigation.navigateUp()
            OpenHelpWebsite -> navigation.openHelpWebsite()
        }
    }
}

@Composable
private fun DebugLogsSettingsScreen(
    state: DebugLogsSettingsState,
    onIntent: (DebugLogsSettingsIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .verticalScroll(rememberScrollState())
                .padding(vertical = 16.dp),
    ) {
        TitleAppBar(
            title = stringResource(LocalizationR.string.settings_debug_logs),
            navigationIcon = { BackNavigationIcon(onBackClick = { onIntent(GoBack) }) },
        )
        SwitchableSettingsItem(
            iconPainter = painterResource(R.drawable.ic_bug),
            title = stringResource(LocalizationR.string.settings_debug_logs_enable_logs),
            isChecked = state.areDebugLogsEnabled,
            onCheckedChange = { onIntent(ToggleDebugLogs) },
        )

        OpenableSettingsItem(
            iconPainter = painterResource(R.drawable.ic_access_logs),
            title = stringResource(LocalizationR.string.settings_debug_logs_settings_logs),
            onClick = { onIntent(AccessLogs) },
            isEnabled = state.isAccessLogsEnabled,
        )

        OpenableSettingsItem(
            iconPainter = painterResource(R.drawable.ic_link),
            title = stringResource(LocalizationR.string.settings_debug_logs_visit_help_website),
            onClick = { onIntent(DebugLogsSettingsIntent.OpenHelpWebsite) },
            opensInternally = false,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun DebugLogsSettingsPreview() {
    DebugLogsSettingsScreen(
        state = DebugLogsSettingsState(areDebugLogsEnabled = true),
        onIntent = {},
        modifier = Modifier,
    )
}
