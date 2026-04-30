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

package com.passbolt.mobile.android.resourcemoremenu

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.passbolt.mobile.android.common.datarefresh.DataRefreshTrackingFlow
import com.passbolt.mobile.android.commontest.TestCoroutineLaunchContext
import com.passbolt.mobile.android.core.idlingresource.CreateMenuModelIdlingResource
import com.passbolt.mobile.android.core.mvp.coroutinecontext.CoroutineLaunchContext
import com.passbolt.mobile.android.resourcemoremenu.usecase.CreateResourceMoreMenuModelUseCase
import com.passbolt.mobile.android.ui.ResourceMoreMenuModel
import com.passbolt.mobile.android.ui.ResourceMoreMenuModel.DescriptionOption.HAS_METADATA_DESCRIPTION
import com.passbolt.mobile.android.ui.ResourceMoreMenuModel.DescriptionOption.HAS_NOTE
import com.passbolt.mobile.android.ui.ResourceMoreMenuModel.FavouriteOption.ADD_TO_FAVOURITES
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
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.stub

@OptIn(ExperimentalCoroutinesApi::class)
class ResourceMoreMenuTest : KoinTest {
    private val mockCreateResourceMoreMenuModelUseCase = mock<CreateResourceMoreMenuModelUseCase>()

    @get:Rule
    val koinTestRule =
        KoinTestRule.create {
            printLogger(Level.ERROR)
            modules(
                module {
                    single { mockCreateResourceMoreMenuModelUseCase }
                    factoryOf(::TestCoroutineLaunchContext) bind CoroutineLaunchContext::class
                    singleOf(::DataRefreshTrackingFlow)
                    singleOf(::CreateMenuModelIdlingResource)
                    factoryOf(::ResourceMoreMenuBottomSheetViewModel)
                },
            )
        }

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: ResourceMoreMenuBottomSheetViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `all enabled items should be displayed according to state`() =
        runTest {
            mockCreateResourceMoreMenuModelUseCase.stub {
                onBlocking { execute(any()) } doReturn
                    CreateResourceMoreMenuModelUseCase.Output(
                        ResourceMoreMenuModel(
                            title = "title",
                            canCopy = true,
                            canDelete = true,
                            canEdit = true,
                            canShare = true,
                            favouriteOption = ADD_TO_FAVOURITES,
                            descriptionOptions = listOf(HAS_NOTE, HAS_METADATA_DESCRIPTION),
                        ),
                    )
            }

            viewModel = get()

            viewModel.viewState.test {
                skipItems(1)

                viewModel.onIntent(ResourceMoreMenuBottomSheetIntent.Initialize("resourceId"))

                val state = awaitItem()
                assertThat(state.title).isEqualTo("title")
                assertThat(state.isLoading).isFalse()
                assertThat(state.showCopyPassword).isTrue()
                assertThat(state.showCopyNote).isTrue()
                assertThat(state.showCopyMetadataDescription).isTrue()
                assertThat(state.showSeparator).isTrue()
                assertThat(state.showDelete).isTrue()
                assertThat(state.showEdit).isTrue()
                assertThat(state.showShare).isTrue()
                assertThat(state.favouriteOption).isEqualTo(ADD_TO_FAVOURITES)
            }
        }

    @Test
    fun `initial state should have loading true and all items hidden`() =
        runTest {
            viewModel = get()

            viewModel.viewState.test {
                val state = awaitItem()
                assertThat(state.title).isEmpty()
                assertThat(state.isLoading).isTrue()
                assertThat(state.showCopyPassword).isFalse()
                assertThat(state.showCopyNote).isFalse()
                assertThat(state.showCopyMetadataDescription).isFalse()
                assertThat(state.showSeparator).isFalse()
                assertThat(state.showDelete).isFalse()
                assertThat(state.showEdit).isFalse()
                assertThat(state.showShare).isFalse()
                assertThat(state.favouriteOption).isNull()
            }
        }
}
