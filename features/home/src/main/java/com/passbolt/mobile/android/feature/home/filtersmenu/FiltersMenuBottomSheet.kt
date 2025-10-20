package com.passbolt.mobile.android.feature.home.filtersmenu

import PassboltTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.passbolt.mobile.android.core.compose.SideEffectDispatcher
import com.passbolt.mobile.android.core.ui.compose.bottomsheet.BottomSheetHeader
import com.passbolt.mobile.android.core.ui.compose.menu.MenuItem
import com.passbolt.mobile.android.feature.home.filtersmenu.FiltersMenuIntent.AllItemsClick
import com.passbolt.mobile.android.feature.home.filtersmenu.FiltersMenuIntent.Close
import com.passbolt.mobile.android.feature.home.filtersmenu.FiltersMenuIntent.ExpiryClick
import com.passbolt.mobile.android.feature.home.filtersmenu.FiltersMenuIntent.FavouritesClick
import com.passbolt.mobile.android.feature.home.filtersmenu.FiltersMenuIntent.FoldersClick
import com.passbolt.mobile.android.feature.home.filtersmenu.FiltersMenuIntent.GroupsClick
import com.passbolt.mobile.android.feature.home.filtersmenu.FiltersMenuIntent.Initialize
import com.passbolt.mobile.android.feature.home.filtersmenu.FiltersMenuIntent.OwnedByMeClick
import com.passbolt.mobile.android.feature.home.filtersmenu.FiltersMenuIntent.RecentlyModifiedClick
import com.passbolt.mobile.android.feature.home.filtersmenu.FiltersMenuIntent.SharedWithMeClick
import com.passbolt.mobile.android.feature.home.filtersmenu.FiltersMenuIntent.TagsClick
import com.passbolt.mobile.android.feature.home.filtersmenu.FiltersMenuSideEffect.Dismiss
import com.passbolt.mobile.android.feature.home.filtersmenu.FiltersMenuSideEffect.HomeViewChanged
import com.passbolt.mobile.android.ui.FiltersMenuModel
import com.passbolt.mobile.android.ui.HomeDisplayViewModel
import com.passbolt.mobile.android.ui.HomeDisplayViewModel.AllItems
import com.passbolt.mobile.android.ui.HomeDisplayViewModel.Expiry
import com.passbolt.mobile.android.ui.HomeDisplayViewModel.Favourites
import com.passbolt.mobile.android.ui.HomeDisplayViewModel.Folders
import com.passbolt.mobile.android.ui.HomeDisplayViewModel.Groups
import com.passbolt.mobile.android.ui.HomeDisplayViewModel.OwnedByMe
import com.passbolt.mobile.android.ui.HomeDisplayViewModel.RecentlyModified
import com.passbolt.mobile.android.ui.HomeDisplayViewModel.SharedWithMe
import com.passbolt.mobile.android.ui.HomeDisplayViewModel.Tags
import org.koin.androidx.compose.koinViewModel
import com.passbolt.mobile.android.core.localization.R as LocalizationR
import com.passbolt.mobile.android.core.ui.R as CoreUiR

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FiltersMenuBottomSheet(
    filtersMenuModel: FiltersMenuModel,
    onDismissRequest: () -> Unit,
    onHomeViewChange: (HomeDisplayViewModel) -> Unit,
    viewModel: FiltersMenuViewModel = koinViewModel(),
) {
    viewModel.onIntent(Initialize(filtersMenuModel))

    val state by viewModel.viewState.collectAsState()

    SideEffectDispatcher(viewModel.sideEffect) { sideEffect ->
        when (sideEffect) {
            Dismiss -> onDismissRequest()
            is HomeViewChanged -> onHomeViewChange(sideEffect.homeDisplay)
        }
    }

    FiltersMenuBottomSheet(
        state = state,
        onIntent = viewModel::onIntent,
        onDismissRequest = onDismissRequest,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FiltersMenuBottomSheet(
    state: FiltersMenuState,
    onIntent: (FiltersMenuIntent) -> Unit,
    onDismissRequest: () -> Unit,
) {
    val sheetState =
        rememberModalBottomSheetState(
            skipPartiallyExpanded = true,
        )

    ModalBottomSheet(
        containerColor = colorResource(CoreUiR.color.elevated_background),
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .testTag("filters_menu_sheet"),
        ) {
            BottomSheetHeader(
                title = stringResource(LocalizationR.string.filters_menu_title),
                onClose = { onIntent(Close) },
            )

            HorizontalDivider(
                color = colorResource(CoreUiR.color.divider),
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
            )

            MenuItem(
                title = stringResource(LocalizationR.string.filters_menu_all_items),
                iconResId = CoreUiR.drawable.ic_list,
                isSelected = state.activeDisplayView is AllItems,
                onClick = { onIntent(AllItemsClick) },
            )

            MenuItem(
                title = stringResource(LocalizationR.string.filters_menu_favourites),
                iconResId = CoreUiR.drawable.ic_star,
                isSelected = state.activeDisplayView is Favourites,
                onClick = { onIntent(FavouritesClick) },
            )

            MenuItem(
                title = stringResource(LocalizationR.string.filters_menu_recently_modified),
                iconResId = CoreUiR.drawable.ic_clock,
                isSelected = state.activeDisplayView is RecentlyModified,
                onClick = { onIntent(RecentlyModifiedClick) },
            )

            MenuItem(
                title = stringResource(LocalizationR.string.filters_menu_shared_with_me),
                iconResId = CoreUiR.drawable.ic_share,
                isSelected = state.activeDisplayView is SharedWithMe,
                onClick = { onIntent(SharedWithMeClick) },
            )

            MenuItem(
                title = stringResource(LocalizationR.string.filters_menu_owned_by_me),
                iconResId = CoreUiR.drawable.ic_person,
                isSelected = state.activeDisplayView is OwnedByMe,
                onClick = { onIntent(OwnedByMeClick) },
            )

            if (state.showExpiryMenuItem) {
                MenuItem(
                    title = stringResource(LocalizationR.string.filters_menu_expiry),
                    iconResId = CoreUiR.drawable.ic_calendar_clock,
                    isSelected = state.activeDisplayView is Expiry,
                    onClick = { onIntent(ExpiryClick) },
                )
            }

            HorizontalDivider(
                color = colorResource(CoreUiR.color.divider),
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 16.dp),
            )

            if (state.showFoldersMenuItem) {
                MenuItem(
                    title = stringResource(LocalizationR.string.filters_menu_folders),
                    iconResId = CoreUiR.drawable.ic_folder,
                    isSelected = state.activeDisplayView is Folders,
                    onClick = { onIntent(FoldersClick) },
                )
            }

            if (state.showTagsMenuItem) {
                MenuItem(
                    title = stringResource(LocalizationR.string.filters_menu_tags),
                    iconResId = CoreUiR.drawable.ic_tag,
                    isSelected = state.activeDisplayView is Tags,
                    onClick = { onIntent(TagsClick) },
                )
            }

            MenuItem(
                title = stringResource(LocalizationR.string.filters_menu_groups),
                iconResId = CoreUiR.drawable.ic_group,
                isSelected = state.activeDisplayView is Groups,
                onClick = { onIntent(GroupsClick) },
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun FiltersMenuBottomSheetPreview() {
    val previewState =
        FiltersMenuState(
            activeDisplayView = AllItems,
            showFoldersMenuItem = true,
            showTagsMenuItem = true,
            showExpiryMenuItem = true,
        )

    PassboltTheme {
        FiltersMenuBottomSheet(
            state = previewState,
            onIntent = {},
            onDismissRequest = {},
        )
    }
}
