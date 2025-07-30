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

package com.passbolt.mobile.android.feature.settings.screen.appsettings.defaultfilter

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.passbolt.mobile.android.core.compose.SideEffectDispatcher
import com.passbolt.mobile.android.core.navigation.compose.AppNavigator
import com.passbolt.mobile.android.core.ui.compose.topbar.BackNavigationIcon
import com.passbolt.mobile.android.core.ui.compose.topbar.TitleAppBar
import com.passbolt.mobile.android.feature.settings.screen.appsettings.defaultfilter.DefaultFilterIntent.GoBack
import com.passbolt.mobile.android.feature.settings.screen.appsettings.defaultfilter.model.toUiModel
import com.passbolt.mobile.android.ui.DefaultFilterModel
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import com.passbolt.mobile.android.core.localization.R as LocalizationR
import com.passbolt.mobile.android.core.ui.R as CoreUi

@Composable
internal fun DefaultFilterScreen(
    modifier: Modifier = Modifier,
    navigator: AppNavigator = koinInject(),
    viewModel: DefaultFilterViewModel = koinViewModel(),
) {
    val state = viewModel.viewState.collectAsStateWithLifecycle()

    SideEffectDispatcher(viewModel.sideEffect) {
        when (it) {
            DefaultFilterSideEffect.NavigateUp -> navigator.navigateBack()
        }
    }

    DefaultFilterScreen(
        modifier = modifier,
        state = state.value,
        onIntent = viewModel::onIntent,
    )
}

@Composable
private fun DefaultFilterScreen(
    state: DefaultFilterState,
    onIntent: (DefaultFilterIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        TitleAppBar(
            title = stringResource(LocalizationR.string.settings_app_settings_default_filter),
            navigationIcon = { BackNavigationIcon(onBackClick = { onIntent(GoBack) }) },
        )
        FilterSelectionList(
            allFilters = state.allFilters,
            selectedFilter = state.selectedFilter,
            { onIntent(DefaultFilterIntent.SelectDefaultFilter(it)) },
        )
    }
}

@Composable
private fun FilterSelectionList(
    allFilters: List<DefaultFilterModel>,
    selectedFilter: DefaultFilterModel?,
    onFilterSelect: (DefaultFilterModel) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier = modifier) {
        items(allFilters.map { it.toUiModel() }) { filterUiModel ->
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clickable { onFilterSelect(filterUiModel.filter) }
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Image(
                    painter = painterResource(filterUiModel.iconRes),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    colorFilter = ColorFilter.tint(colorResource(CoreUi.color.icon_tint)),
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(text = stringResource(filterUiModel.nameRes), color = MaterialTheme.colorScheme.onBackground)
                Spacer(modifier = Modifier.weight(1f))
                RadioButton(
                    selected = filterUiModel.filter == selectedFilter,
                    onClick = { onFilterSelect(filterUiModel.filter) },
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun DefaultFilterPreview() {
    DefaultFilterScreen(
        state =
            DefaultFilterState(
                allFilters = DefaultFilterModel.entries.toList(),
                selectedFilter = DefaultFilterModel.ALL_ITEMS,
            ),
        onIntent = {},
        modifier = Modifier,
    )
}
