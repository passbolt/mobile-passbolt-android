package com.passbolt.mobile.android.locationdetails

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
import com.passbolt.mobile.android.core.commonfolders.usecase.db.GetLocalFolderDetailsUseCase
import com.passbolt.mobile.android.core.commonfolders.usecase.db.GetLocalFolderLocationUseCase
import com.passbolt.mobile.android.core.mvp.coroutinecontext.CoroutineLaunchContext
import com.passbolt.mobile.android.core.resources.usecase.db.GetLocalResourceUseCase
import com.passbolt.mobile.android.jsonmodel.jsonpathops.JsonPathJsonPathOps
import com.passbolt.mobile.android.jsonmodel.jsonpathops.JsonPathsOps
import com.passbolt.mobile.android.locationdetails.LocationDetailsIntent.GoBack
import com.passbolt.mobile.android.locationdetails.LocationDetailsIntent.Initialize
import com.passbolt.mobile.android.locationdetails.LocationDetailsIntent.ToggleExpanded
import com.passbolt.mobile.android.locationdetails.LocationDetailsSideEffect.NavigateToHome
import com.passbolt.mobile.android.locationdetails.LocationDetailsSideEffect.NavigateUp
import com.passbolt.mobile.android.locationdetails.LocationDetailsSideEffect.ShowErrorSnackbar
import com.passbolt.mobile.android.locationdetails.LocationDetailsSideEffect.ShowToast
import com.passbolt.mobile.android.locationdetails.SnackbarErrorType.FAILED_TO_REFRESH_DATA
import com.passbolt.mobile.android.locationdetails.ToastType.CONTENT_NOT_AVAILABLE
import com.passbolt.mobile.android.locationdetails.data.ExpandableFolderNode
import com.passbolt.mobile.android.locationdetails.data.ExpandableFolderTree
import com.passbolt.mobile.android.locationdetails.data.ExpandableFolderTreeCreator
import com.passbolt.mobile.android.locationdetails.ui.LocationItem.FOLDER
import com.passbolt.mobile.android.locationdetails.ui.LocationItem.RESOURCE
import com.passbolt.mobile.android.ui.FolderModel
import com.passbolt.mobile.android.ui.MetadataJsonModel
import com.passbolt.mobile.android.ui.ResourceModel
import com.passbolt.mobile.android.ui.ResourcePermission
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
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalCoroutinesApi::class)
class LocationDetailsViewModelTest : KoinTest {
    @get:Rule
    val koinTestRule =
        KoinTestRule.create {
            printLogger(Level.ERROR)
            modules(
                listOf(
                    module {
                        single { mock<GetLocalFolderDetailsUseCase>() }
                        single { mock<GetLocalFolderLocationUseCase>() }
                        single { mock<GetLocalResourceUseCase>() }
                        single { mock<ExpandableFolderTreeCreator>() }
                        singleOf(::JsonPathJsonPathOps) bind JsonPathsOps::class
                        single {
                            Configuration
                                .builder()
                                .jsonProvider(GsonJsonProvider())
                                .mappingProvider(GsonMappingProvider())
                                .options(EnumSet.noneOf(Option::class.java))
                                .build()
                        }
                        singleOf(::DataRefreshTrackingFlow)
                        singleOf(::TestCoroutineLaunchContext) bind CoroutineLaunchContext::class
                        factoryOf(::LocationDetailsViewModel)
                    },
                ),
            )
        }

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var viewModel: LocationDetailsViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        val getLocalResourceUseCase = get<GetLocalResourceUseCase>()
        getLocalResourceUseCase.stub {
            onBlocking { execute(any()) } doReturn GetLocalResourceUseCase.Output(testResource)
        }

        val getLocalFolderDetailsUseCase = get<GetLocalFolderDetailsUseCase>()
        getLocalFolderDetailsUseCase.stub {
            onBlocking { execute(any()) } doReturn GetLocalFolderDetailsUseCase.Output(testFolder)
        }

        val getLocalFolderLocationUseCase = get<GetLocalFolderLocationUseCase>()
        getLocalFolderLocationUseCase.stub {
            onBlocking { execute(any()) } doReturn GetLocalFolderLocationUseCase.Output(testParentFolders)
        }

        val expandableFolderTreeCreator = get<ExpandableFolderTreeCreator>()
        expandableFolderTreeCreator.stub {
            on { create(any()) } doReturn testFolderTree
        }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `resource location data should be loaded and displayed when initialized with resource`() =
        runTest {
            viewModel = get()

            viewModel.viewState.test {
                val initialState = awaitItem()
                assertThat(initialState).isEqualTo(LocationDetailsState())

                viewModel.onIntent(Initialize(RESOURCE, testResource.resourceId))

                val updatedState = awaitItem()
                assertThat(updatedState.itemName).isEqualTo(testResource.metadataJsonModel.name)
                assertThat(updatedState.isSharedFolder).isFalse()
                assertThat(updatedState.resource).isEqualTo(testResource)
                assertThat(updatedState.parentFolders).isEqualTo(testParentFolders)
                assertThat(updatedState.folderTree).isEqualTo(testFolderTree)
                assertThat(updatedState.expandedItemIds).isEqualTo(testExpandedIds)
                assertThat(updatedState.isRefreshing).isFalse()
            }
        }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `folder location data should be loaded and displayed when initialized with folder`() =
        runTest {
            viewModel = get()

            viewModel.viewState.test {
                val initialState = awaitItem()
                assertThat(initialState).isEqualTo(LocationDetailsState())

                viewModel.onIntent(Initialize(FOLDER, testFolder.folderId))

                val updatedState = awaitItem()
                assertThat(updatedState.itemName).isEqualTo(testFolder.name)
                assertThat(updatedState.isSharedFolder).isEqualTo(testFolder.isShared)
                assertThat(updatedState.resource).isNull()
                assertThat(updatedState.parentFolders).isEqualTo(testParentFolders)
                assertThat(updatedState.folderTree).isEqualTo(testFolderTree)
                assertThat(updatedState.expandedItemIds).isEqualTo(testExpandedIds)
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
    fun `toggle expanded should expand folder when it is collapsed`() =
        runTest {
            viewModel = get()

            viewModel.viewState.test {
                awaitItem()

                viewModel.onIntent(Initialize(FOLDER, testFolder.folderId))
                awaitItem()

                val folderId = "folder-to-expand"
                viewModel.onIntent(ToggleExpanded(folderId))

                val updatedState = awaitItem()
                assertThat(updatedState.expandedItemIds).contains(folderId)
            }
        }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `toggle expanded should collapse folder when it is expanded`() =
        runTest {
            viewModel = get()

            viewModel.viewState.test {
                awaitItem()

                viewModel.onIntent(Initialize(FOLDER, testFolder.folderId))
                awaitItem()

                val folderId = testExpandedIds.first()
                viewModel.onIntent(ToggleExpanded(folderId))

                val updatedState = awaitItem()
                assertThat(updatedState.expandedItemIds).doesNotContain(folderId)
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

                viewModel.onIntent(Initialize(RESOURCE, testResource.resourceId))
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

                viewModel.onIntent(Initialize(RESOURCE, testResource.resourceId))
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
    fun `should handle null pointer exception when loading resource and navigate to home`() =
        runTest {
            val getLocalResourceUseCase = get<GetLocalResourceUseCase>()
            getLocalResourceUseCase.stub {
                onBlocking { execute(any()) } doThrow NullPointerException("Resource not found")
            }

            viewModel = get()

            viewModel.sideEffect.test {
                viewModel.onIntent(Initialize(RESOURCE, testResource.resourceId))

                assertThat(awaitItem()).isEqualTo(ShowToast(CONTENT_NOT_AVAILABLE))
                assertThat(awaitItem()).isEqualTo(NavigateToHome)
            }
        }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `should handle null pointer exception when loading folder and navigate to home`() =
        runTest {
            val getLocalFolderDetailsUseCase = get<GetLocalFolderDetailsUseCase>()
            getLocalFolderDetailsUseCase.stub {
                onBlocking { execute(any()) } doThrow NullPointerException("Folder not found")
            }

            viewModel = get()

            viewModel.sideEffect.test {
                viewModel.onIntent(Initialize(FOLDER, testFolder.folderId))

                assertThat(awaitItem()).isEqualTo(ShowToast(CONTENT_NOT_AVAILABLE))
                assertThat(awaitItem()).isEqualTo(NavigateToHome)
            }
        }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `should handle resource without folder parent`() =
        runTest {
            val resourceWithoutFolder = testResource.copy(folderId = null)
            val getLocalResourceUseCase = get<GetLocalResourceUseCase>()
            getLocalResourceUseCase.stub {
                onBlocking { execute(any()) } doReturn GetLocalResourceUseCase.Output(resourceWithoutFolder)
            }

            val expandableFolderTreeCreator = get<ExpandableFolderTreeCreator>()
            val emptyFolderTree = ExpandableFolderTree(emptyList(), null)
            expandableFolderTreeCreator.stub {
                on { create(emptyList()) } doReturn emptyFolderTree
            }

            viewModel = get()

            viewModel.viewState.test {
                awaitItem()

                viewModel.onIntent(Initialize(RESOURCE, resourceWithoutFolder.resourceId))

                val updatedState = awaitItem()
                assertThat(updatedState.itemName).isEqualTo(resourceWithoutFolder.metadataJsonModel.name)
                assertThat(updatedState.resource).isEqualTo(resourceWithoutFolder)
                assertThat(updatedState.parentFolders).isEmpty()
                assertThat(updatedState.folderTree).isEqualTo(emptyFolderTree)
                assertThat(updatedState.expandedItemIds).isEmpty()
            }
        }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `should not change state when data refresh status is not completed`() =
        runTest {
            viewModel = get()

            viewModel.viewState.test {
                awaitItem()

                viewModel.onIntent(Initialize(RESOURCE, testResource.resourceId))
                awaitItem()

                val dataRefreshTrackingFlow = get<DataRefreshTrackingFlow>()
                dataRefreshTrackingFlow.updateStatus(NotCompleted)

                expectNoEvents()
            }
        }

    private companion object {
        private val testResource by lazy {
            ResourceModel(
                resourceId = "resource-id-123",
                resourceTypeId = "resource-type-id",
                folderId = "parent-folder-id",
                permission = ResourcePermission.READ,
                favouriteId = null,
                modified = ZonedDateTime.now(),
                expiry = null,
                metadataJsonModel =
                    MetadataJsonModel(
                        """
                        {
                            "name": "Test Resource",
                            "uri": "https://example.com",
                            "username": "test.user",
                            "description": "Test description"
                        }
                        """.trimIndent(),
                    ),
                metadataKeyId = null,
                metadataKeyType = null,
            )
        }

        private val testFolder by lazy {
            FolderModel(
                folderId = "folder-id-123",
                parentFolderId = "parent-folder-id",
                name = "Test Folder",
                isShared = true,
                permission = ResourcePermission.OWNER,
            )
        }

        private val testParentFolders by lazy {
            listOf(
                FolderModel(
                    folderId = "root-folder",
                    parentFolderId = null,
                    name = "Root",
                    isShared = false,
                    permission = ResourcePermission.OWNER,
                ),
                FolderModel(
                    folderId = "parent-folder-id",
                    parentFolderId = "root-folder",
                    name = "Parent Folder",
                    isShared = true,
                    permission = ResourcePermission.UPDATE,
                ),
            )
        }

        private val testFolderTree by lazy {
            val rootNode =
                ExpandableFolderNode(
                    id = "root-folder",
                    folderModel = testParentFolders[0],
                    children =
                        listOf(
                            ExpandableFolderNode(
                                id = "parent-folder-id",
                                folderModel = testParentFolders[1],
                                children = emptyList(),
                                depth = 1,
                            ),
                        ),
                    depth = 0,
                )
            ExpandableFolderTree(
                rootNodes = listOf(rootNode),
                expandToNode = rootNode.children.first(),
            )
        }

        private val testExpandedIds by lazy {
            setOf("root-folder")
        }
    }
}
