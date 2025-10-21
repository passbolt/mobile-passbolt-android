package com.passbolt.mobile.android.feature.home.filtersmenu

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.passbolt.mobile.android.commontest.TestCoroutineLaunchContext
import com.passbolt.mobile.android.core.mvp.coroutinecontext.CoroutineLaunchContext
import com.passbolt.mobile.android.core.preferences.usecase.UpdateHomeDisplayViewPrefsUseCase
import com.passbolt.mobile.android.core.rbac.usecase.GetRbacRulesUseCase
import com.passbolt.mobile.android.entity.featureflags.FeatureFlagsModel
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
import com.passbolt.mobile.android.ui.FiltersMenuModel
import com.passbolt.mobile.android.ui.HomeDisplayViewModel
import com.passbolt.mobile.android.ui.RbacModel
import com.passbolt.mobile.android.ui.RbacRuleModel.ALLOW
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.koin.core.logger.Level
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.koin.test.KoinTestRule
import org.koin.test.get
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import kotlin.test.assertIs
import kotlin.time.ExperimentalTime

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

@OptIn(ExperimentalCoroutinesApi::class, ExperimentalTime::class)
class FiltersMenuViewModelTest : KoinTest {
    @get:Rule
    val koinTestRule =
        KoinTestRule.create {
            printLogger(Level.ERROR)
            modules(
                listOf(
                    module {
                        single { mock<GetFeatureFlagsUseCase>() }
                        single { mock<GetRbacRulesUseCase>() }
                        single { mock<UpdateHomeDisplayViewPrefsUseCase>() }
                        singleOf(::HomeDisplayViewMapper)
                        singleOf(::TestCoroutineLaunchContext) bind CoroutineLaunchContext::class
                        factoryOf(::FiltersMenuViewModel)
                    },
                ),
            )
        }

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var viewModel: FiltersMenuViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        val featureFlags =
            FeatureFlagsModel(
                privacyPolicyUrl = null,
                termsAndConditionsUrl = null,
                isPreviewPasswordAvailable = true,
                areFoldersAvailable = true,
                areTagsAvailable = true,
                isTotpAvailable = false,
                isRbacAvailable = true,
                isPasswordExpiryAvailable = true,
                arePasswordPoliciesAvailable = true,
                canUpdatePasswordPolicies = true,
                isV5MetadataAvailable = true,
            )
        get<GetFeatureFlagsUseCase>().stub {
            onBlocking { execute(any()) } doReturn GetFeatureFlagsUseCase.Output(featureFlags)
        }

        val rbacModel =
            RbacModel(
                passwordPreviewRule = ALLOW,
                passwordCopyRule = ALLOW,
                tagsUseRule = ALLOW,
                shareViewRule = ALLOW,
                foldersUseRule = ALLOW,
            )
        get<GetRbacRulesUseCase>().stub {
            onBlocking { execute(any()) } doReturn GetRbacRulesUseCase.Output(rbacModel)
        }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `should initialize with display view from intent`() =
        runTest {
            val displayView = HomeDisplayViewModel.AllItems

            viewModel = get()
            viewModel.onIntent(Initialize(FiltersMenuModel(displayView)))

            assertThat(viewModel.viewState.value.activeDisplayView).isEqualTo(displayView)
        }

    @Test
    fun `should handle all home view changes and emit correct side effects`() =
        runTest {
            viewModel = get()
            val updateHomeDisplayViewPrefsUseCase = get<UpdateHomeDisplayViewPrefsUseCase>()
            val homeDisplayViewMapper = get<HomeDisplayViewMapper>()

            // (Intent, HomeDisplayView, Expected HomeDisplayViewModel)
            val testCases =
                listOf(
                    Triple(AllItemsClick, ALL_ITEMS, HomeDisplayViewModel.AllItems),
                    Triple(FavouritesClick, FAVOURITES, HomeDisplayViewModel.Favourites),
                    Triple(RecentlyModifiedClick, RECENTLY_MODIFIED, HomeDisplayViewModel.RecentlyModified),
                    Triple(SharedWithMeClick, SHARED_WITH_ME, HomeDisplayViewModel.SharedWithMe),
                    Triple(OwnedByMeClick, OWNED_BY_ME, HomeDisplayViewModel.OwnedByMe),
                    Triple(ExpiryClick, EXPIRY, HomeDisplayViewModel.Expiry),
                    Triple(FoldersClick, FOLDERS, HomeDisplayViewModel.Folders),
                    Triple(TagsClick, TAGS, HomeDisplayViewModel.Tags),
                    Triple(GroupsClick, GROUPS, HomeDisplayViewModel.Groups),
                )

            viewModel.sideEffect.test {
                testCases.forEach { (homeIntent, homeDisplayView, expectedHomeDisplayViewModel) ->
                    viewModel.onIntent(homeIntent)
                    val effect = expectItem()
                    assertIs<HomeViewChanged>(effect)
                    assertThat(effect.homeDisplay).isEqualTo(homeDisplayViewMapper.map(homeDisplayView))
                    assertIs<Dismiss>(expectItem())
                    verify(updateHomeDisplayViewPrefsUseCase).execute(
                        UpdateHomeDisplayViewPrefsUseCase.Input(lastUsedHomeView = homeDisplayView),
                    )
                }
            }
        }

    @Test
    fun `should dismiss when close intent is received`() =
        runTest {
            viewModel = get()

            viewModel.sideEffect.test {
                viewModel.onIntent(Close)
                assertIs<Dismiss>(expectItem())
            }
        }
}
