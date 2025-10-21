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

package com.passbolt.mobile.android.feature.home.screen

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.google.gson.GsonBuilder
import com.jayway.jsonpath.Configuration
import com.jayway.jsonpath.Option
import com.jayway.jsonpath.spi.json.GsonJsonProvider
import com.jayway.jsonpath.spi.mapper.GsonMappingProvider
import com.passbolt.mobile.android.common.datarefresh.DataRefreshTrackingFlow
import com.passbolt.mobile.android.commontest.TestCoroutineLaunchContext
import com.passbolt.mobile.android.core.accounts.usecase.accountdata.GetSelectedAccountDataUseCase
import com.passbolt.mobile.android.core.commonfolders.usecase.db.GetLocalFolderDetailsUseCase
import com.passbolt.mobile.android.core.mvp.authentication.SessionRefreshTrackingFlow
import com.passbolt.mobile.android.core.mvp.coroutinecontext.CoroutineLaunchContext
import com.passbolt.mobile.android.core.preferences.usecase.GetHomeDisplayViewPrefsUseCase
import com.passbolt.mobile.android.core.resources.actions.ResourceCommonActionResult
import com.passbolt.mobile.android.core.resources.actions.ResourceCommonActionsInteractor
import com.passbolt.mobile.android.core.resources.actions.ResourcePropertiesActionsInteractor
import com.passbolt.mobile.android.core.resources.actions.ResourcePropertyActionResult
import com.passbolt.mobile.android.core.resources.actions.SecretPropertiesActionsInteractor
import com.passbolt.mobile.android.core.resources.actions.SecretPropertyActionResult
import com.passbolt.mobile.android.entity.home.HomeDisplayView
import com.passbolt.mobile.android.feature.home.screen.HomeIntent.CopyNote
import com.passbolt.mobile.android.feature.home.screen.HomeIntent.CopyPassword
import com.passbolt.mobile.android.feature.home.screen.HomeIntent.CopyResourceMetadataDescription
import com.passbolt.mobile.android.feature.home.screen.HomeIntent.CopyResourceUri
import com.passbolt.mobile.android.feature.home.screen.HomeIntent.CopyResourceUsername
import com.passbolt.mobile.android.feature.home.screen.HomeIntent.LaunchResourceWebsite
import com.passbolt.mobile.android.feature.home.screen.HomeIntent.OpenResourceMenu
import com.passbolt.mobile.android.feature.home.screen.HomeIntent.ToggleResourceFavourite
import com.passbolt.mobile.android.feature.home.screen.HomeSideEffect.CopyToClipboard
import com.passbolt.mobile.android.feature.home.screen.HomeSideEffect.NavigateToResourceUri
import com.passbolt.mobile.android.feature.home.screen.HomeSideEffect.OpenResourceMoreMenu
import com.passbolt.mobile.android.feature.home.screen.HomeSideEffect.ShowErrorSnackbar
import com.passbolt.mobile.android.feature.home.screen.SnackbarErrorType.TOGGLE_FAVOURITE_FAILURE
import com.passbolt.mobile.android.feature.home.screen.data.HomeDataProvider
import com.passbolt.mobile.android.feature.home.screen.data.HomeDataWithHeader
import com.passbolt.mobile.android.jsonmodel.JSON_MODEL_GSON
import com.passbolt.mobile.android.jsonmodel.jsonpathops.JsonPathJsonPathOps
import com.passbolt.mobile.android.jsonmodel.jsonpathops.JsonPathsOps
import com.passbolt.mobile.android.mappers.HomeDisplayViewMapper
import com.passbolt.mobile.android.metadata.usecase.CanCreateResourceUseCase
import com.passbolt.mobile.android.metadata.usecase.CanShareResourceUseCase
import com.passbolt.mobile.android.ui.DefaultFilterModel
import com.passbolt.mobile.android.ui.HomeDisplayViewModel.AllItems
import com.passbolt.mobile.android.ui.MetadataJsonModel
import com.passbolt.mobile.android.ui.ResourceModel
import com.passbolt.mobile.android.ui.ResourceMoreMenuModel.FavouriteOption.ADD_TO_FAVOURITES
import com.passbolt.mobile.android.ui.ResourceMoreMenuModel.FavouriteOption.REMOVE_FROM_FAVOURITES
import com.passbolt.mobile.android.ui.ResourcePermission
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
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
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.koin.test.KoinTestRule
import org.koin.test.get
import org.koin.test.mock.declare
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import org.mockito.kotlin.whenever
import java.time.ZonedDateTime
import java.util.EnumSet
import kotlin.test.assertIs
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalCoroutinesApi::class, ExperimentalTime::class)
class HomeViewModelMenuTest : KoinTest {
    @get:Rule
    val koinTestRule =
        KoinTestRule.create {
            printLogger(Level.ERROR)
            modules(
                module {
                    singleOf(::TestCoroutineLaunchContext) bind CoroutineLaunchContext::class
                    singleOf(::DataRefreshTrackingFlow)
                    singleOf(::SessionRefreshTrackingFlow)
                    single { mock<GetSelectedAccountDataUseCase>() }
                    single { mock<GetHomeDisplayViewPrefsUseCase>() }
                    single { mock<HomeDisplayViewMapper>() }
                    single { mock<HomeDataProvider>() }
                    single { mock<GetLocalFolderDetailsUseCase>() }
                    single { mock<CanCreateResourceUseCase>() }
                    single { mock<CanShareResourceUseCase>() }
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
                    factoryOf(::HomeViewModel)
                },
            )
        }

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: HomeViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        whenever(get<GetSelectedAccountDataUseCase>().execute(anyOrNull())).thenReturn(
            GetSelectedAccountDataUseCase.Output(
                firstName = "First",
                lastName = "Last",
                email = "first@passbolt.com",
                avatarUrl = "www.passbolt.com/avatar.png",
                url = "www.passbolt.com",
                serverId = "1",
                label = "label",
                role = "user",
            ),
        )

        whenever(get<GetHomeDisplayViewPrefsUseCase>().execute(any())).thenReturn(
            GetHomeDisplayViewPrefsUseCase.Output(
                lastUsedHomeView = HomeDisplayView.ALL_ITEMS,
                userSetHomeView = DefaultFilterModel.ALL_ITEMS,
            ),
        )

        whenever(get<HomeDisplayViewMapper>().map(any(), any())).thenReturn(AllItems)

        get<HomeDataProvider>().stub {
            onBlocking {
                provideData(
                    any(),
                    any(),
                    any(),
                )
            }.doReturn(HomeDataWithHeader())
        }

        get<CanCreateResourceUseCase>().stub {
            onBlocking { execute(any()) }.doReturn(CanCreateResourceUseCase.Output(canCreateResource = true))
        }

        get<CanShareResourceUseCase>().stub {
            onBlocking { execute(any()) }.doReturn(CanShareResourceUseCase.Output(canShareResource = true))
        }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `should copy username to clipboard`() =
        runTest {
            val resourcePropertiesInteractor = mock<ResourcePropertiesActionsInteractor>()
            whenever(resourcePropertiesInteractor.provideUsername()).thenReturn(
                flowOf(
                    ResourcePropertyActionResult(
                        label = "Username",
                        result = "testuser",
                        isSecret = false,
                    ),
                ),
            )
            declare { resourcePropertiesInteractor }

            viewModel = get()
            viewModel.onIntent(OpenResourceMenu(mockResourceModel("Resource 1")))

            viewModel.sideEffect.test {
                assertIs<OpenResourceMoreMenu>(expectItem())

                viewModel.onIntent(CopyResourceUsername)

                val effect = expectItem()
                assertIs<CopyToClipboard>(effect)
                assertThat(effect.label).isEqualTo("Username")
                assertThat(effect.value).isEqualTo("testuser")
                assertThat(effect.isSensitive).isFalse()
            }
        }

    @Test
    fun `should copy metadata description to clipboard`() =
        runTest {
            val resourcePropertiesInteractor = mock<ResourcePropertiesActionsInteractor>()
            whenever(resourcePropertiesInteractor.provideDescription()).thenReturn(
                flowOf(
                    ResourcePropertyActionResult(
                        label = "Description",
                        result = "description text",
                        isSecret = false,
                    ),
                ),
            )
            declare { resourcePropertiesInteractor }

            viewModel = get()
            viewModel.onIntent(OpenResourceMenu(mockResourceModel("Resource 1")))

            viewModel.sideEffect.test {
                assertIs<OpenResourceMoreMenu>(expectItem())

                viewModel.onIntent(CopyResourceMetadataDescription)

                val effect = expectItem()
                assertIs<CopyToClipboard>(effect)
                assertThat(effect.label).isEqualTo("Description")
                assertThat(effect.value).isEqualTo("description text")
                assertThat(effect.isSensitive).isFalse()
            }
        }

    @Test
    fun `should copy resource uri to clipboard`() =
        runTest {
            val resourcePropertiesInteractor = mock<ResourcePropertiesActionsInteractor>()
            whenever(resourcePropertiesInteractor.provideMainUri()).thenReturn(
                flowOf(
                    ResourcePropertyActionResult(
                        label = "Uri",
                        result = "main uri",
                        isSecret = false,
                    ),
                ),
            )
            declare { resourcePropertiesInteractor }

            viewModel = get()
            viewModel.onIntent(OpenResourceMenu(mockResourceModel("Resource 1")))

            viewModel.sideEffect.test {
                assertIs<OpenResourceMoreMenu>(expectItem())

                viewModel.onIntent(CopyResourceUri)

                val effect = expectItem()
                assertIs<CopyToClipboard>(effect)
                assertThat(effect.label).isEqualTo("Uri")
                assertThat(effect.value).isEqualTo("main uri")
                assertThat(effect.isSensitive).isFalse()
            }
        }

    @Test
    fun `should copy password to clipboard`() =
        runTest {
            val secretPropertiesInteractor = mock<SecretPropertiesActionsInteractor>()
            whenever(secretPropertiesInteractor.providePassword()).thenReturn(
                flowOf(
                    SecretPropertyActionResult.Success(
                        label = "Password",
                        result = "password",
                        isSecret = true,
                    ),
                ),
            )
            declare { secretPropertiesInteractor }

            viewModel = get()
            viewModel.onIntent(OpenResourceMenu(mockResourceModel("Resource 1")))

            viewModel.sideEffect.test {
                assertIs<OpenResourceMoreMenu>(expectItem())

                viewModel.onIntent(CopyPassword)

                val effect = expectItem()
                assertIs<CopyToClipboard>(effect)
                assertThat(effect.label).isEqualTo("Password")
                assertThat(effect.value).isEqualTo("password")
                assertThat(effect.isSensitive).isTrue()
            }
        }

    @Test
    fun `should copy note to clipboard`() =
        runTest {
            val secretPropertiesInteractor = mock<SecretPropertiesActionsInteractor>()
            whenever(secretPropertiesInteractor.provideNote()).thenReturn(
                flowOf(
                    SecretPropertyActionResult.Success(
                        label = "Note",
                        result = "note content",
                        isSecret = true,
                    ),
                ),
            )
            declare { secretPropertiesInteractor }

            viewModel = get()
            viewModel.onIntent(OpenResourceMenu(mockResourceModel("Resource 1")))

            viewModel.sideEffect.test {
                assertIs<OpenResourceMoreMenu>(expectItem())

                viewModel.onIntent(CopyNote)

                val effect = expectItem()
                assertIs<CopyToClipboard>(effect)
                assertThat(effect.label).isEqualTo("Note")
                assertThat(effect.value).isEqualTo("note content")
                assertThat(effect.isSensitive).isTrue()
            }
        }

    @Test
    fun `should launch website uri`() =
        runTest {
            val resourcePropertiesInteractor = mock<ResourcePropertiesActionsInteractor>()
            whenever(resourcePropertiesInteractor.provideMainUri()).thenReturn(
                flowOf(
                    ResourcePropertyActionResult(
                        label = "Uri",
                        result = "main uri",
                        isSecret = false,
                    ),
                ),
            )
            declare { resourcePropertiesInteractor }

            viewModel = get()
            viewModel.onIntent(OpenResourceMenu(mockResourceModel("Resource 1")))

            viewModel.sideEffect.test {
                assertIs<OpenResourceMoreMenu>(expectItem())

                viewModel.onIntent(LaunchResourceWebsite)

                val effect = expectItem()
                assertIs<NavigateToResourceUri>(effect)
                assertThat(effect.url).isEqualTo("main uri")
            }
        }

    @Test
    fun `should toggle resource favourite successfully when adding to favourites`() =
        runTest {
            mockResourceModel("Test Resource")
            val resourceCommonActionsInteractor = mock<ResourceCommonActionsInteractor>()
            whenever(resourceCommonActionsInteractor.toggleFavourite(ADD_TO_FAVOURITES)).thenReturn(
                flowOf(
                    ResourceCommonActionResult.Success("Test Resource"),
                ),
            )
            declare { resourceCommonActionsInteractor }

            viewModel = get()
            viewModel.onIntent(OpenResourceMenu(mockResourceModel("Test Resource")))

            viewModel.sideEffect.test {
                assertIs<OpenResourceMoreMenu>(expectItem())

                viewModel.onIntent(ToggleResourceFavourite(ADD_TO_FAVOURITES))
            }
        }

    @Test
    fun `should toggle resource favourite successfully when removing from favourites`() =
        runTest {
            mockResourceModel("Test Resource")
            val resourceCommonActionsInteractor = mock<ResourceCommonActionsInteractor>()
            whenever(resourceCommonActionsInteractor.toggleFavourite(REMOVE_FROM_FAVOURITES)).thenReturn(
                flowOf(
                    ResourceCommonActionResult.Success("Test Resource"),
                ),
            )
            declare { resourceCommonActionsInteractor }

            viewModel = get()
            viewModel.onIntent(OpenResourceMenu(mockResourceModel("Test Resource")))

            viewModel.sideEffect.test {
                assertIs<OpenResourceMoreMenu>(expectItem())

                viewModel.onIntent(ToggleResourceFavourite(REMOVE_FROM_FAVOURITES))
            }
        }

    @Test
    fun `should show error snackbar when toggle favourite fails`() =
        runTest {
            mockResourceModel("Test Resource")
            val resourceCommonActionsInteractor = mock<ResourceCommonActionsInteractor>()
            whenever(resourceCommonActionsInteractor.toggleFavourite(any())).thenReturn(
                flowOf(ResourceCommonActionResult.Failure),
            )
            declare { resourceCommonActionsInteractor }

            viewModel = get()
            viewModel.onIntent(OpenResourceMenu(mockResourceModel("Test Resource")))

            viewModel.sideEffect.test {
                assertIs<OpenResourceMoreMenu>(expectItem())

                viewModel.onIntent(ToggleResourceFavourite(ADD_TO_FAVOURITES))

                val effect = expectItem()
                assertIs<ShowErrorSnackbar>(effect)
                assertThat(effect.type).isEqualTo(TOGGLE_FAVOURITE_FAILURE)
            }
        }

    private fun mockResourceModel(name: String) =
        ResourceModel(
            resourceId = "id1",
            resourceTypeId = "resTypeId",
            folderId = "folderId",
            permission = ResourcePermission.READ,
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
