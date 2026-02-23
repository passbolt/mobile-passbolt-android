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

package com.passbolt.mobile.android.resourcepicker.screen

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.google.gson.GsonBuilder
import com.jayway.jsonpath.Configuration
import com.jayway.jsonpath.Option
import com.jayway.jsonpath.spi.json.GsonJsonProvider
import com.jayway.jsonpath.spi.mapper.GsonMappingProvider
import com.passbolt.mobile.android.common.datarefresh.DataRefreshStatus.Idle.FinishedWithFailure
import com.passbolt.mobile.android.common.datarefresh.DataRefreshStatus.Idle.FinishedWithSuccess
import com.passbolt.mobile.android.common.datarefresh.DataRefreshStatus.InProgress
import com.passbolt.mobile.android.common.datarefresh.DataRefreshTrackingFlow
import com.passbolt.mobile.android.common.urimatcher.AutofillUriMatcher
import com.passbolt.mobile.android.commontest.TestCoroutineLaunchContext
import com.passbolt.mobile.android.core.mvp.coroutinecontext.CoroutineLaunchContext
import com.passbolt.mobile.android.core.resourcetypes.usecase.db.GetResourceTypeIdToSlugMappingUseCase
import com.passbolt.mobile.android.core.ui.compose.search.SearchInputEndIconMode.CLEAR
import com.passbolt.mobile.android.core.ui.compose.search.SearchInputEndIconMode.NONE
import com.passbolt.mobile.android.jsonmodel.JSON_MODEL_GSON
import com.passbolt.mobile.android.jsonmodel.jsonpathops.JsonPathJsonPathOps
import com.passbolt.mobile.android.jsonmodel.jsonpathops.JsonPathsOps
import com.passbolt.mobile.android.resourcepicker.model.ConfirmationType
import com.passbolt.mobile.android.resourcepicker.model.PickResourceAction
import com.passbolt.mobile.android.resourcepicker.screen.ResourcePickerIntent.ApplyClick
import com.passbolt.mobile.android.resourcepicker.screen.ResourcePickerIntent.CloseConfirmationDialog
import com.passbolt.mobile.android.resourcepicker.screen.ResourcePickerIntent.ConfirmOtpLink
import com.passbolt.mobile.android.resourcepicker.screen.ResourcePickerIntent.GoBack
import com.passbolt.mobile.android.resourcepicker.screen.ResourcePickerIntent.Initialize
import com.passbolt.mobile.android.resourcepicker.screen.ResourcePickerIntent.ResourcePicked
import com.passbolt.mobile.android.resourcepicker.screen.ResourcePickerIntent.Search
import com.passbolt.mobile.android.resourcepicker.screen.ResourcePickerIntent.SearchEndIconAction
import com.passbolt.mobile.android.resourcepicker.screen.ResourcePickerSideEffect.NavigateBackWithResult
import com.passbolt.mobile.android.resourcepicker.screen.ResourcePickerSideEffect.NavigateUp
import com.passbolt.mobile.android.resourcepicker.screen.ResourcePickerSideEffect.ShowErrorSnackbar
import com.passbolt.mobile.android.resourcepicker.screen.SnackbarErrorType.FAILED_TO_REFRESH_DATA
import com.passbolt.mobile.android.resourcepicker.screen.SnackbarErrorType.NO_PERMISSION
import com.passbolt.mobile.android.resourcepicker.screen.SnackbarErrorType.UNSUPPORTED_RESOURCE_TYPE
import com.passbolt.mobile.android.resourcepicker.screen.data.ResourcePickerData
import com.passbolt.mobile.android.resourcepicker.screen.data.ResourcePickerDataProvider
import com.passbolt.mobile.android.supportedresourceTypes.ContentType
import com.passbolt.mobile.android.ui.MetadataJsonModel
import com.passbolt.mobile.android.ui.ResourceModel
import com.passbolt.mobile.android.ui.ResourcePermission
import com.passbolt.mobile.android.ui.ResourcePickerListItem
import com.passbolt.mobile.android.ui.ResourcePickerListItem.Selection.NOT_SELECTABLE_NO_PERMISSION
import com.passbolt.mobile.android.ui.ResourcePickerListItem.Selection.NOT_SELECTABLE_UNSUPPORTED_RESOURCE_TYPE
import com.passbolt.mobile.android.ui.ResourcePickerListItem.Selection.SELECTABLE
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
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
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.koin.test.KoinTestRule
import org.koin.test.get
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import org.mockito.kotlin.whenever
import java.time.ZonedDateTime
import java.util.EnumSet
import java.util.UUID
import kotlin.test.assertIs

@OptIn(ExperimentalCoroutinesApi::class)
class ResourcePickerViewModelTest : KoinTest {
    @get:Rule
    val koinTestRule =
        KoinTestRule.create {
            printLogger(Level.ERROR)
            modules(
                module {
                    singleOf(::TestCoroutineLaunchContext) bind CoroutineLaunchContext::class
                    singleOf(::DataRefreshTrackingFlow)
                    single { mock<ResourcePickerDataProvider>() }
                    single { mock<GetResourceTypeIdToSlugMappingUseCase>() }
                    single(named(JSON_MODEL_GSON)) { GsonBuilder().serializeNulls().create() }
                    single {
                        Configuration
                            .builder()
                            .jsonProvider(GsonJsonProvider())
                            .mappingProvider(GsonMappingProvider())
                            .options(EnumSet.noneOf(Option::class.java))
                            .build()
                    }
                    singleOf(::JsonPathJsonPathOps) bind JsonPathsOps::class
                    factoryOf(::AutofillUriMatcher)
                    factoryOf(::ResourcePickerViewModel)
                },
            )
        }

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: ResourcePickerViewModel

    private val testResourceTypeId = UUID.randomUUID()
    private val testResourceTypeIdString = testResourceTypeId.toString()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        get<ResourcePickerDataProvider>().stub {
            onBlocking {
                provideData(
                    anyOrNull(),
                    anyOrNull(),
                )
            }.doReturn(ResourcePickerData())
        }

        get<GetResourceTypeIdToSlugMappingUseCase>().stub {
            onBlocking { execute(any()) }.doReturn(
                GetResourceTypeIdToSlugMappingUseCase.Output(
                    idToSlugMapping =
                        mapOf(
                            testResourceTypeId to ContentType.PasswordAndDescription.slug,
                        ),
                ),
            )
        }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `should initialize with empty state`() =
        runTest {
            viewModel = get()

            assertThat(viewModel.viewState.value.searchQuery).isEmpty()
            assertThat(viewModel.viewState.value.isRefreshing).isFalse()
            assertThat(viewModel.viewState.value.searchInputEndIconMode).isEqualTo(NONE)
            assertThat(viewModel.viewState.value.isApplyButtonEnabled).isFalse()
            assertThat(viewModel.viewState.value.pickedResource).isNull()
            assertThat(viewModel.viewState.value.showConfirmationDialog).isFalse()
        }

    @Test
    fun `should load resources on initialize`() =
        runTest {
            val mockData = mockResourcePickerData()
            whenever(get<ResourcePickerDataProvider>().provideData(anyOrNull(), anyOrNull())).thenReturn(mockData)

            viewModel = get()

            viewModel.viewState.test {
                viewModel.onIntent(Initialize(null))

                val updatedState = awaitItem()
                assertThat(updatedState.resourcePickerData).isEqualTo(mockData)
            }
        }

    @Test
    fun `should update search state when search query changes`() =
        runTest {
            val mockData = mockResourcePickerData()
            whenever(get<ResourcePickerDataProvider>().provideData(anyOrNull(), anyOrNull())).thenReturn(mockData)

            viewModel = get()

            viewModel.viewState.drop(1).test {
                viewModel.onIntent(Search("test query"))

                val updatedState = awaitItem()
                assertThat(updatedState.searchQuery).isEqualTo("test query")
                assertThat(updatedState.searchInputEndIconMode).isEqualTo(CLEAR)
                assertThat(updatedState.resourcePickerData).isEqualTo(mockData)
            }
        }

    @Test
    fun `should clear search and reset icon mode when search cleared`() =
        runTest {
            viewModel = get()
            viewModel.onIntent(Search("test query"))

            viewModel.viewState.test {
                viewModel.onIntent(SearchEndIconAction)

                val updatedState = awaitItem()
                assertThat(updatedState.searchQuery).isEmpty()
                assertThat(updatedState.searchInputEndIconMode).isEqualTo(NONE)
            }
        }

    @Test
    fun `should select resource when selectable resource is picked`() =
        runTest {
            val resource = mockResourcePickerListItem("id1", "Resource 1", SELECTABLE)
            val mockData = mockResourcePickerData(listOf(resource))
            whenever(get<ResourcePickerDataProvider>().provideData(anyOrNull(), anyOrNull())).thenReturn(mockData)

            viewModel = get()
            viewModel.onIntent(Initialize(null))

            viewModel.viewState.drop(1).test {
                viewModel.onIntent(ResourcePicked(resource))

                val updatedState = awaitItem()
                assertThat(updatedState.pickedResource).isEqualTo(resource)
                assertThat(updatedState.isApplyButtonEnabled).isTrue()
            }
        }

    @Test
    fun `should show error when resource with no permission is picked`() =
        runTest {
            val resource = mockResourcePickerListItem("id1", "Resource 1", NOT_SELECTABLE_NO_PERMISSION)
            viewModel = get()

            viewModel.sideEffect.test {
                viewModel.onIntent(ResourcePicked(resource))

                val effect = awaitItem()
                assertIs<ShowErrorSnackbar>(effect)
                assertThat(effect.type).isEqualTo(NO_PERMISSION)
            }
        }

    @Test
    fun `should show error when unsupported resource type is picked`() =
        runTest {
            val resource = mockResourcePickerListItem("id1", "Resource 1", NOT_SELECTABLE_UNSUPPORTED_RESOURCE_TYPE)
            viewModel = get()

            viewModel.sideEffect.test {
                viewModel.onIntent(ResourcePicked(resource))

                val effect = awaitItem()
                assertIs<ShowErrorSnackbar>(effect)
                assertThat(effect.type).isEqualTo(UNSUPPORTED_RESOURCE_TYPE)
            }
        }

    @Test
    fun `should show confirmation dialog for TOTP link when apply clicked`() =
        runTest {
            val resource = mockResourcePickerListItem("id1", "Resource 1", SELECTABLE)
            get<GetResourceTypeIdToSlugMappingUseCase>().stub {
                onBlocking { execute(any()) }.doReturn(
                    GetResourceTypeIdToSlugMappingUseCase.Output(
                        idToSlugMapping =
                            mapOf(
                                testResourceTypeId to ContentType.PasswordAndDescription.slug,
                            ),
                    ),
                )
            }

            viewModel = get()
            viewModel.onIntent(ResourcePicked(resource))

            viewModel.viewState.drop(1).test {
                viewModel.onIntent(ApplyClick)

                val updatedState = awaitItem()
                assertThat(updatedState.showConfirmationDialog).isTrue()
                assertThat(updatedState.confirmationType).isEqualTo(ConfirmationType.LINK_TOTP)
                assertThat(updatedState.pickAction).isEqualTo(PickResourceAction.TOTP_LINK)
            }
        }

    @Test
    fun `should show confirmation dialog for TOTP replace when apply clicked`() =
        runTest {
            val resource = mockResourcePickerListItem("id1", "Resource 1", SELECTABLE)
            get<GetResourceTypeIdToSlugMappingUseCase>().stub {
                onBlocking { execute(any()) }.doReturn(
                    GetResourceTypeIdToSlugMappingUseCase.Output(
                        idToSlugMapping =
                            mapOf(
                                testResourceTypeId to ContentType.PasswordDescriptionTotp.slug,
                            ),
                    ),
                )
            }

            viewModel = get()
            viewModel.onIntent(ResourcePicked(resource))

            viewModel.viewState.drop(1).test {
                viewModel.onIntent(ApplyClick)

                val updatedState = awaitItem()
                assertThat(updatedState.showConfirmationDialog).isTrue()
                assertThat(updatedState.confirmationType).isEqualTo(ConfirmationType.REPLACE_TOTP)
                assertThat(updatedState.pickAction).isEqualTo(PickResourceAction.TOTP_REPLACE)
            }
        }

    @Test
    fun `should close confirmation dialog when CloseConfirmationDialog intent received`() =
        runTest {
            viewModel = get()

            viewModel.viewState.test {
                viewModel.onIntent(CloseConfirmationDialog)

                val updatedState = awaitItem()
                assertThat(updatedState.showConfirmationDialog).isFalse()
            }
        }

    @Test
    fun `should navigate back with result when OTP link confirmed`() =
        runTest {
            val resource = mockResourcePickerListItem("id1", "Resource 1", SELECTABLE)
            viewModel = get()
            viewModel.onIntent(ResourcePicked(resource))

            viewModel.sideEffect.test {
                viewModel.onIntent(ConfirmOtpLink(PickResourceAction.TOTP_LINK))

                val effect = awaitItem()
                assertIs<NavigateBackWithResult>(effect)
                assertThat(effect.pickAction).isEqualTo(PickResourceAction.TOTP_LINK)
                assertThat(effect.resourceModel).isEqualTo(resource.resourceModel)
            }

            assertThat(viewModel.viewState.value.showConfirmationDialog).isFalse()
        }

    @Test
    fun `should navigate up when GoBack intent received`() =
        runTest {
            viewModel = get()

            viewModel.sideEffect.test {
                viewModel.onIntent(GoBack)

                val effect = awaitItem()
                assertIs<NavigateUp>(effect)
            }
        }

    @Test
    fun `should update state during data refresh`() =
        runTest {
            val dataRefreshFlow: DataRefreshTrackingFlow = get()
            val mockData = mockResourcePickerData()
            whenever(get<ResourcePickerDataProvider>().provideData(anyOrNull(), anyOrNull())).thenReturn(mockData)

            viewModel = get()
            viewModel.onIntent(Initialize(null))

            viewModel.viewState.drop(1).test {
                dataRefreshFlow.updateStatus(InProgress)
                val inProgress = awaitItem()
                assertThat(inProgress.isRefreshing).isTrue()

                dataRefreshFlow.updateStatus(FinishedWithSuccess)
                val finished = awaitItem()
                assertThat(finished.isRefreshing).isFalse()
                assertThat(finished.resourcePickerData).isEqualTo(mockData)
            }
        }

    @Test
    fun `should show error on refresh failure`() =
        runTest {
            val dataRefreshFlow: DataRefreshTrackingFlow = get()
            viewModel = get()
            viewModel.onIntent(Initialize(null))

            viewModel.viewState.drop(1).test {
                dataRefreshFlow.updateStatus(InProgress)
                val inProgress = awaitItem()
                assertThat(inProgress.isRefreshing).isTrue()

                dataRefreshFlow.updateStatus(FinishedWithFailure)
                val finished = awaitItem()
                assertThat(finished.isRefreshing).isFalse()

                viewModel.sideEffect.test {
                    val effect = awaitItem()
                    assertIs<ShowErrorSnackbar>(effect)
                    assertThat(effect.type).isEqualTo(FAILED_TO_REFRESH_DATA)
                }
            }
        }

    @Test
    fun `should initialize with suggestion URI and load resources`() =
        runTest {
            val suggestionUri = "https://example.com"
            val mockData = mockResourcePickerData()
            whenever(get<ResourcePickerDataProvider>().provideData(anyOrNull(), any())).thenReturn(mockData)

            viewModel = get()

            viewModel.viewState.test {
                viewModel.onIntent(Initialize(suggestionUri))

                val updatedState = awaitItem()
                assertThat(updatedState.resourcePickerData).isEqualTo(mockData)
            }
        }

    @Test
    fun `should preserve selection when loading resources`() =
        runTest {
            val resource1 = mockResourcePickerListItem("id1", "Resource 1", SELECTABLE)
            val resource2 = mockResourcePickerListItem("id2", "Resource 2", SELECTABLE)
            val mockData = mockResourcePickerData(listOf(resource1, resource2))
            whenever(get<ResourcePickerDataProvider>().provideData(anyOrNull(), anyOrNull())).thenReturn(mockData)

            viewModel = get()
            viewModel.onIntent(Initialize(null))

            viewModel.onIntent(ResourcePicked(resource1))

            viewModel.viewState.drop(1).test {
                viewModel.onIntent(Search("test"))

                val updatedState = awaitItem()
                assertThat(updatedState.pickedResource).isEqualTo(resource1)
            }
        }

    @Test
    fun `should handle V5Default resource type for TOTP link`() =
        runTest {
            val resource = mockResourcePickerListItem("id1", "Resource 1", SELECTABLE)
            get<GetResourceTypeIdToSlugMappingUseCase>().stub {
                onBlocking { execute(any()) }.doReturn(
                    GetResourceTypeIdToSlugMappingUseCase.Output(
                        idToSlugMapping =
                            mapOf(
                                testResourceTypeId to ContentType.V5Default.slug,
                            ),
                    ),
                )
            }

            viewModel = get()
            viewModel.onIntent(ResourcePicked(resource))
            advanceUntilIdle()

            viewModel.viewState.drop(1).test {
                viewModel.onIntent(ApplyClick)
                advanceUntilIdle()

                val updatedState = awaitItem()
                assertThat(updatedState.showConfirmationDialog).isTrue()
                assertThat(updatedState.confirmationType).isEqualTo(ConfirmationType.LINK_TOTP)
                assertThat(updatedState.pickAction).isEqualTo(PickResourceAction.TOTP_LINK)
            }
        }

    @Test
    fun `should handle V5DefaultWithTotp resource type for TOTP replace`() =
        runTest {
            val resource = mockResourcePickerListItem("id1", "Resource 1", SELECTABLE)
            get<GetResourceTypeIdToSlugMappingUseCase>().stub {
                onBlocking { execute(any()) }.doReturn(
                    GetResourceTypeIdToSlugMappingUseCase.Output(
                        idToSlugMapping =
                            mapOf(
                                testResourceTypeId to ContentType.V5DefaultWithTotp.slug,
                            ),
                    ),
                )
            }

            viewModel = get()
            viewModel.onIntent(ResourcePicked(resource))
            advanceUntilIdle()

            viewModel.viewState.drop(1).test {
                viewModel.onIntent(ApplyClick)
                advanceUntilIdle()

                val updatedState = awaitItem()
                assertThat(updatedState.showConfirmationDialog).isTrue()
                assertThat(updatedState.confirmationType).isEqualTo(ConfirmationType.REPLACE_TOTP)
                assertThat(updatedState.pickAction).isEqualTo(PickResourceAction.TOTP_REPLACE)
            }
        }

    private fun mockResourcePickerData(
        resources: List<ResourcePickerListItem> = emptyList(),
        suggestedResources: List<ResourcePickerListItem> = emptyList(),
    ) = ResourcePickerData(
        resources = resources,
        suggestedResources = suggestedResources,
    )

    private fun mockResourcePickerListItem(
        id: String,
        name: String,
        selection: ResourcePickerListItem.Selection,
    ) = ResourcePickerListItem(
        resourceModel = mockResourceModel(id, name),
        selection = selection,
        isSelected = false,
    )

    private fun mockResourceModel(
        id: String,
        name: String,
    ) = ResourceModel(
        resourceId = id,
        resourceTypeId = testResourceTypeIdString,
        folderId = "folderId",
        permission = ResourcePermission.OWNER,
        favouriteId = null,
        modified = ZonedDateTime.now(),
        expiry = null,
        metadataJsonModel =
            MetadataJsonModel(
                """
                {
                    "name": "$name",
                    "uri": "https://example.com",
                    "username": "testuser",
                    "description": "Test description"
                }
                """.trimIndent(),
            ),
        metadataKeyId = null,
        metadataKeyType = null,
    )
}
