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

package com.passbolt.mobile.android.logs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.passbolt.mobile.android.core.compose.SideEffectDispatcher
import com.passbolt.mobile.android.core.ui.compose.topbar.BackNavigationIcon
import com.passbolt.mobile.android.core.ui.compose.topbar.TitleAppBar
import com.passbolt.mobile.android.logs.LogsIntent.GoBack
import com.passbolt.mobile.android.logs.LogsIntent.ShareLogs
import com.passbolt.mobile.android.logs.LogsSideEffect.NavigateToLogsShareSheet
import com.passbolt.mobile.android.logs.LogsSideEffect.NavigateUp
import com.passbolt.mobile.android.logs.LogsSideEffect.ScrollLogsToLastLine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import org.koin.androidx.compose.koinViewModel
import com.passbolt.mobile.android.core.localization.R as LocalizationR
import com.passbolt.mobile.android.core.ui.R as CoreUiR

@Deprecated("Use to integrate with legacy navigation only, use LogsScreen for Compose")
@Composable
internal fun LogsScreenCompat(
    navigation: LogsNavigation,
    modifier: Modifier = Modifier,
    viewModel: LogsViewModel = koinViewModel(),
) {
    val state = viewModel.viewState.collectAsStateWithLifecycle()
    val logListState = rememberLazyListState()

    LogsScreen(
        modifier = modifier,
        state = state.value,
        logListState = logListState,
        onIntent = viewModel::onIntent,
    )

    SideEffectDispatcher(viewModel.sideEffect) {
        when (it) {
            NavigateUp -> navigation.navigateUp()
            ScrollLogsToLastLine -> {
                // wait until list is populated before scrolling
                snapshotFlow { logListState.layoutInfo.totalItemsCount }
                    .filter { it > 0 }
                    .first()
                logListState.scrollToItem(maxOf(state.value.logLines.lastIndex, 0))
            }
            is NavigateToLogsShareSheet -> navigation.navigateToLogsShareSheet(it.logsFilePath)
        }
    }
}

@Composable
private fun LogsScreen(
    state: LogsState,
    logListState: LazyListState,
    onIntent: (LogsIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .verticalScroll(rememberScrollState())
                .padding(vertical = 16.dp),
    ) {
        TitleAppBar(
            title = stringResource(LocalizationR.string.settings_logs_title),
            actions = {
                IconButton(onClick = { onIntent(ShareLogs) }) {
                    Icon(
                        painter = painterResource(CoreUiR.drawable.ic_share),
                        contentDescription = null,
                    )
                }
            },
            navigationIcon = {
                BackNavigationIcon(onBackClick = { onIntent(GoBack) })
            },
        )
        LazyColumn(
            state = logListState,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            items(state.logLines) { logLine ->
                Text(logLine, color = MaterialTheme.colorScheme.onBackground)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun LogsScreenPreview() {
    LogsScreen(
        state =
            LogsState(
                listOf(
                    "00:00:00 PM Log line 1",
                    "00:00:00 PM Log line 2",
                    "00:00:00 PM Log line 3",
                ),
            ),
        logListState = rememberLazyListState(),
        onIntent = {},
        modifier = Modifier,
    )
}
