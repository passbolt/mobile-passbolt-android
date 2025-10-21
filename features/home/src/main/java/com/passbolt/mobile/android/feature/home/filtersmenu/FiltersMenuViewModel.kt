package com.passbolt.mobile.android.feature.home.filtersmenu

import androidx.lifecycle.viewModelScope
import com.passbolt.mobile.android.core.compose.SideEffectViewModel
import com.passbolt.mobile.android.core.mvp.coroutinecontext.CoroutineLaunchContext
import com.passbolt.mobile.android.core.preferences.usecase.UpdateHomeDisplayViewPrefsUseCase
import com.passbolt.mobile.android.core.rbac.usecase.GetRbacRulesUseCase
import com.passbolt.mobile.android.entity.home.HomeDisplayView
import com.passbolt.mobile.android.entity.home.HomeDisplayView.ALL_ITEMS
import com.passbolt.mobile.android.entity.home.HomeDisplayView.EXPIRY
import com.passbolt.mobile.android.entity.home.HomeDisplayView.FAVOURITES
import com.passbolt.mobile.android.entity.home.HomeDisplayView.FOLDERS
import com.passbolt.mobile.android.entity.home.HomeDisplayView.GROUPS
import com.passbolt.mobile.android.entity.home.HomeDisplayView.OWNED_BY_ME
import com.passbolt.mobile.android.entity.home.HomeDisplayView.RECENTLY_MODIFIED
import com.passbolt.mobile.android.entity.home.HomeDisplayView.SHARED_WITH_ME
import com.passbolt.mobile.android.entity.home.HomeDisplayView.TAGS
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
import com.passbolt.mobile.android.featureflags.usecase.GetFeatureFlagsUseCase
import com.passbolt.mobile.android.mappers.HomeDisplayViewMapper
import com.passbolt.mobile.android.ui.RbacRuleModel.ALLOW
import kotlinx.coroutines.launch

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

class FiltersMenuViewModel(
    private val getFeatureFlagsUseCase: GetFeatureFlagsUseCase,
    private val getRbacRulesUseCase: GetRbacRulesUseCase,
    private val updateHomeDisplayViewPrefsUseCase: UpdateHomeDisplayViewPrefsUseCase,
    private val homeDisplayViewMapper: HomeDisplayViewMapper,
    private val coroutineLaunchContext: CoroutineLaunchContext,
) : SideEffectViewModel<FiltersMenuState, FiltersMenuSideEffect>(FiltersMenuState()) {
    init {
        processAdditionalItemsVisibility()
    }

    fun onIntent(intent: FiltersMenuIntent) {
        when (intent) {
            is Initialize -> updateViewState { copy(activeDisplayView = intent.menuModel.activeDisplayView) }
            is AllItemsClick -> handleHomeViewChanged(ALL_ITEMS)
            is FavouritesClick -> handleHomeViewChanged(FAVOURITES)
            is RecentlyModifiedClick -> handleHomeViewChanged(RECENTLY_MODIFIED)
            is SharedWithMeClick -> handleHomeViewChanged(SHARED_WITH_ME)
            is OwnedByMeClick -> handleHomeViewChanged(OWNED_BY_ME)
            is ExpiryClick -> handleHomeViewChanged(EXPIRY)
            is FoldersClick -> handleHomeViewChanged(FOLDERS)
            is TagsClick -> handleHomeViewChanged(TAGS)
            is GroupsClick -> handleHomeViewChanged(GROUPS)
            is Close -> emitSideEffect(Dismiss)
        }
    }

    private fun handleHomeViewChanged(homeDisplayView: HomeDisplayView) {
        viewModelScope.launch(coroutineLaunchContext.io) {
            updateHomeDisplayViewPrefsUseCase.execute(
                UpdateHomeDisplayViewPrefsUseCase.Input(lastUsedHomeView = homeDisplayView),
            )
            emitSideEffect(HomeViewChanged(homeDisplayViewMapper.map(homeDisplayView)))
            emitSideEffect(Dismiss)
        }
    }

    private fun processAdditionalItemsVisibility() {
        launch {
            val featureFlags = getFeatureFlagsUseCase.execute(Unit).featureFlags
            val rbac = getRbacRulesUseCase.execute(Unit).rbacModel

            updateViewState {
                copy(
                    showFoldersMenuItem = featureFlags.areFoldersAvailable && rbac.foldersUseRule == ALLOW,
                    showTagsMenuItem = featureFlags.areTagsAvailable && rbac.tagsUseRule == ALLOW,
                    showExpiryMenuItem = featureFlags.isPasswordExpiryAvailable,
                )
            }
        }
    }
}
