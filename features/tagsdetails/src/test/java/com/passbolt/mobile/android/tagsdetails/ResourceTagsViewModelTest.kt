package com.passbolt.mobile.android.tagsdetails

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

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.jayway.jsonpath.Configuration
import com.jayway.jsonpath.Option
import com.jayway.jsonpath.spi.json.GsonJsonProvider
import com.jayway.jsonpath.spi.mapper.GsonMappingProvider
import com.passbolt.mobile.android.common.datarefresh.DataRefreshStatus.Idle.FinishedWithFailure
import com.passbolt.mobile.android.common.datarefresh.DataRefreshStatus.Idle.NotCompleted
import com.passbolt.mobile.android.common.datarefresh.DataRefreshStatus.InProgress
import com.passbolt.mobile.android.common.datarefresh.DataRefreshTrackingFlow
import com.passbolt.mobile.android.commontest.TestCoroutineLaunchContext
import com.passbolt.mobile.android.core.mvp.coroutinecontext.CoroutineLaunchContext
import com.passbolt.mobile.android.core.resources.usecase.db.GetLocalResourceTagsUseCase
import com.passbolt.mobile.android.core.resources.usecase.db.GetLocalResourceUseCase
import com.passbolt.mobile.android.jsonmodel.jsonpathops.JsonPathJsonPathOps
import com.passbolt.mobile.android.jsonmodel.jsonpathops.JsonPathsOps
import com.passbolt.mobile.android.tagsdetails.ResourceTagsIntent.GoBack
import com.passbolt.mobile.android.tagsdetails.ResourceTagsIntent.Initialize
import com.passbolt.mobile.android.tagsdetails.ResourceTagsSideEffect.NavigateToHome
import com.passbolt.mobile.android.tagsdetails.ResourceTagsSideEffect.NavigateUp
import com.passbolt.mobile.android.tagsdetails.ResourceTagsSideEffect.ShowContentNotAvailable
import com.passbolt.mobile.android.tagsdetails.ResourceTagsSideEffect.ShowErrorSnackbar
import com.passbolt.mobile.android.tagsdetails.SnackbarErrorType.FAILED_TO_REFRESH_DATA
import com.passbolt.mobile.android.ui.MetadataJsonModel
import com.passbolt.mobile.android.ui.ResourceModel
import com.passbolt.mobile.android.ui.ResourcePermission
import com.passbolt.mobile.android.ui.TagModel
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
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.stub
import java.time.ZonedDateTime
import java.util.EnumSet
import java.util.UUID
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalCoroutinesApi::class)
class ResourceTagsViewModelTest : KoinTest {
    @get:Rule
    val koinTestRule =
        KoinTestRule.create {
            printLogger(Level.ERROR)
            modules(
                listOf(
                    module {
                        single { mock<GetLocalResourceUseCase>() }
                        single { mock<GetLocalResourceTagsUseCase>() }
                        singleOf(::DataRefreshTrackingFlow)
                        singleOf(::TestCoroutineLaunchContext) bind CoroutineLaunchContext::class
                        singleOf(::JsonPathJsonPathOps) bind JsonPathsOps::class
                        single {
                            Configuration
                                .builder()
                                .jsonProvider(GsonJsonProvider())
                                .mappingProvider(GsonMappingProvider())
                                .options(EnumSet.noneOf(Option::class.java))
                                .build()
                        }
                        factoryOf(::ResourceTagsViewModel)
                    },
                ),
            )
        }

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var viewModel: ResourceTagsViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        val getLocalResourceUseCase = get<GetLocalResourceUseCase>()
        getLocalResourceUseCase.stub {
            onBlocking { execute(any()) } doReturn GetLocalResourceUseCase.Output(testResource)
        }

        val getLocalResourceTagsUseCase = get<GetLocalResourceTagsUseCase>()
        getLocalResourceTagsUseCase.stub {
            onBlocking { execute(any()) } doReturn GetLocalResourceTagsUseCase.Output(testTags)
        }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `resource and tags data should be loaded and displayed when initialized`() =
        runTest {
            viewModel = get()

            viewModel.viewState.test {
                val initialState = awaitItem()
                assertThat(initialState).isEqualTo(ResourceTagsState())

                viewModel.onIntent(Initialize(testResource.resourceId))

                val updatedState = awaitItem()
                assertThat(updatedState.resourceModel).isEqualTo(testResource)
                assertThat(updatedState.tags).isEqualTo(testTags)
                assertThat(updatedState.isRefreshing).isFalse()
            }
        }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `go back intent should emit navigate up side effect`() =
        runTest {
            viewModel = get()

            viewModel.sideEffect.test {
                viewModel.onIntent(GoBack)
                assertThat(awaitItem()).isEqualTo(NavigateUp)
            }
        }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `should show refreshing state during data refresh`() =
        runTest {
            viewModel = get()

            viewModel.viewState.test {
                val initialState = awaitItem()
                assertThat(initialState.isRefreshing).isFalse()

                viewModel.onIntent(Initialize(testResource.resourceId))
                awaitItem()

                val dataRefreshTrackingFlow = get<DataRefreshTrackingFlow>()
                dataRefreshTrackingFlow.updateStatus(InProgress)

                val refreshingState = awaitItem()
                assertThat(refreshingState.isRefreshing).isTrue()
            }
        }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `should handle data refresh failure and show error`() =
        runTest {
            viewModel = get()

            viewModel.viewState.test {
                awaitItem()

                viewModel.onIntent(Initialize(testResource.resourceId))
                awaitItem()

                viewModel.sideEffect.test {
                    val dataRefreshTrackingFlow = get<DataRefreshTrackingFlow>()
                    dataRefreshTrackingFlow.updateStatus(FinishedWithFailure)

                    assertThat(awaitItem()).isEqualTo(ShowErrorSnackbar(FAILED_TO_REFRESH_DATA))
                }
            }
        }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `should handle null pointer exception and navigate to home`() =
        runTest {
            val getLocalResourceUseCase = get<GetLocalResourceUseCase>()
            getLocalResourceUseCase.stub {
                onBlocking { execute(any()) } doThrow NullPointerException("Resource not found")
            }

            viewModel = get()

            viewModel.sideEffect.test {
                viewModel.onIntent(Initialize(testResource.resourceId))

                assertThat(awaitItem()).isEqualTo(ShowContentNotAvailable)
                assertThat(awaitItem()).isEqualTo(NavigateToHome)
            }
        }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `should handle empty tags list`() =
        runTest {
            val getLocalResourceTagsUseCase = get<GetLocalResourceTagsUseCase>()
            getLocalResourceTagsUseCase.stub {
                onBlocking { execute(any()) } doReturn GetLocalResourceTagsUseCase.Output(emptyList())
            }

            viewModel = get()

            viewModel.viewState.test {
                val initialState = awaitItem()
                assertThat(initialState.tags).isEmpty()

                viewModel.onIntent(Initialize(testResource.resourceId))

                val updatedState = awaitItem()
                assertThat(updatedState.resourceModel).isEqualTo(testResource)
                assertThat(updatedState.tags).isEmpty()
            }
        }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `should not change state when data refresh status is not completed`() =
        runTest {
            viewModel = get()

            viewModel.viewState.test {
                awaitItem()

                viewModel.onIntent(Initialize(testResource.resourceId))
                awaitItem()

                val dataRefreshTrackingFlow = get<DataRefreshTrackingFlow>()
                dataRefreshTrackingFlow.updateStatus(NotCompleted)

                expectNoEvents()
            }
        }

    private companion object {
        private val testResource by lazy {
            ResourceModel(
                resourceId = "resId",
                resourceTypeId = "resTypeId",
                folderId = null,
                permission = ResourcePermission.READ,
                favouriteId = null,
                modified = ZonedDateTime.now(),
                expiry = null,
                metadataJsonModel =
                    MetadataJsonModel(
                        """
                        {
                            "name": "resource 1",
                            "uri": "",
                            "username": "",
                            "description": ""
                        }
                        """.trimIndent(),
                    ),
                metadataKeyId = null,
                metadataKeyType = null,
            )
        }

        private val testTags =
            listOf(
                TagModel(
                    id = UUID.randomUUID().toString(),
                    slug = "work",
                    isShared = true,
                ),
                TagModel(
                    id = UUID.randomUUID().toString(),
                    slug = "personal",
                    isShared = false,
                ),
            )
    }
}
