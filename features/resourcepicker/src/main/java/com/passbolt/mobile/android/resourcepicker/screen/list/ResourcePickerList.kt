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

package com.passbolt.mobile.android.resourcepicker.screen.list

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.passbolt.mobile.android.core.compose.rememberDebouncedBoolean
import com.passbolt.mobile.android.core.resources.resourceicon.ResourceIconProvider
import com.passbolt.mobile.android.core.ui.empty.EmptyResourceListState
import com.passbolt.mobile.android.core.ui.lists.HeaderItem
import com.passbolt.mobile.android.resourcepicker.screen.ResourcePickerIntent
import com.passbolt.mobile.android.resourcepicker.screen.ResourcePickerIntent.ResourcePicked
import com.passbolt.mobile.android.resourcepicker.screen.ResourcePickerState
import org.koin.compose.koinInject
import com.passbolt.mobile.android.core.localization.R as LocalizationR

@Composable
fun ResourcePickerList(
    state: ResourcePickerState,
    onIntent: (ResourcePickerIntent) -> Unit,
    modifier: Modifier = Modifier,
    resourceIconProvider: ResourceIconProvider = koinInject(),
) {
    val showEmpty = rememberDebouncedBoolean(!state.hasResources)

    if (showEmpty) {
        EmptyResourceListState(title = stringResource(LocalizationR.string.no_passwords))
    } else {
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = 16.dp),
        ) {
            // Suggested
            if (state.resourcePickerData.suggestedResources.isNotEmpty()) {
                item {
                    HeaderItem(stringResource(LocalizationR.string.suggested))
                }
                items(
                    count = state.resourcePickerData.suggestedResources.size,
                    key = { index -> "suggested_${state.resourcePickerData.suggestedResources[index].resourceModel.resourceId}" },
                ) { index ->
                    val resource = state.resourcePickerData.suggestedResources[index]
                    ResourcePickerItem(
                        resource = resource,
                        resourceIconProvider = resourceIconProvider,
                        onItemClick = { onIntent(ResourcePicked(resource)) },
                    )
                }
            }

            // Other section header (only if there are suggested items)
            if (state.resourcePickerData.suggestedResources.isNotEmpty() &&
                state.resourcePickerData.resources.isNotEmpty()
            ) {
                item {
                    HeaderItem(stringResource(LocalizationR.string.other))
                }
            }

            // Resources section
            items(
                count = state.resourcePickerData.resources.size,
                key = { index -> "resource_${state.resourcePickerData.resources[index].resourceModel.resourceId}" },
            ) { index ->
                val resource = state.resourcePickerData.resources[index]
                ResourcePickerItem(
                    resource = resource,
                    resourceIconProvider = resourceIconProvider,
                    onItemClick = { onIntent(ResourcePicked(resource)) },
                )
            }
        }
    }
}
