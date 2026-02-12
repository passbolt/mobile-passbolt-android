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

package com.passbolt.mobile.android.feature.resources.details

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.passbolt.mobile.android.core.rbac.usecase.GetRbacRulesUseCase
import com.passbolt.mobile.android.core.resources.actions.ResourceCommonActionResult
import com.passbolt.mobile.android.core.resources.actions.ResourceCommonActionsInteractor
import com.passbolt.mobile.android.core.resources.actions.ResourcePropertiesActionsInteractor
import com.passbolt.mobile.android.core.resources.actions.ResourcePropertyActionResult
import com.passbolt.mobile.android.feature.resourcedetails.details.ErrorSnackbarType
import com.passbolt.mobile.android.feature.resourcedetails.details.ResourceDetailsIntent.CloseDeleteConfirmationDialog
import com.passbolt.mobile.android.feature.resourcedetails.details.ResourceDetailsIntent.ConfirmDeleteResource
import com.passbolt.mobile.android.feature.resourcedetails.details.ResourceDetailsIntent.CopyUsername
import com.passbolt.mobile.android.feature.resourcedetails.details.ResourceDetailsIntent.DeleteClick
import com.passbolt.mobile.android.feature.resourcedetails.details.ResourceDetailsIntent.GoBack
import com.passbolt.mobile.android.feature.resourcedetails.details.ResourceDetailsIntent.Initialize
import com.passbolt.mobile.android.feature.resourcedetails.details.ResourceDetailsIntent.LaunchWebsite
import com.passbolt.mobile.android.feature.resourcedetails.details.ResourceDetailsIntent.OpenMoreMenu
import com.passbolt.mobile.android.feature.resourcedetails.details.ResourceDetailsSideEffect.AddToClipboard
import com.passbolt.mobile.android.feature.resourcedetails.details.ResourceDetailsSideEffect.CloseWithDeleteSuccess
import com.passbolt.mobile.android.feature.resourcedetails.details.ResourceDetailsSideEffect.NavigateBack
import com.passbolt.mobile.android.feature.resourcedetails.details.ResourceDetailsSideEffect.NavigateToMore
import com.passbolt.mobile.android.feature.resourcedetails.details.ResourceDetailsSideEffect.OpenWebsite
import com.passbolt.mobile.android.feature.resourcedetails.details.ResourceDetailsSideEffect.ShowErrorSnackbar
import com.passbolt.mobile.android.feature.resourcedetails.details.ResourceDetailsViewModel
import com.passbolt.mobile.android.ui.RbacRuleModel.DENY
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.drop
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
import org.koin.test.KoinTest
import org.koin.test.KoinTestRule
import org.koin.test.get
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.stub
import kotlin.test.assertIs

@OptIn(ExperimentalCoroutinesApi::class)
class ResourceDetailsMainViewModelTest : KoinTest {
    @get:Rule
    val koinTestRule =
        KoinTestRule.create {
            printLogger(Level.ERROR)
            modules(testModule)
        }

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: ResourceDetailsViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        getKoin().setupDefaultMocks()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state should have default values`() =
        runTest {
            viewModel = get()

            viewModel.viewState.test {
                val state = awaitItem()
                assertThat(state.isRefreshing).isFalse()
                assertThat(state.isLoading).isFalse()
                assertThat(state.showDeleteResourceConfirmationDialog).isFalse()
            }
        }

    @Test
    fun `go back intent should emit navigate back side effect`() =
        runTest {
            viewModel = get()

            viewModel.sideEffect.test {
                viewModel.onIntent(GoBack)
                assertIs<NavigateBack>(awaitItem())
            }
        }

    @Test
    fun `open more menu should emit navigate to more side effect`() =
        runTest {
            viewModel = get()

            viewModel.onIntent(Initialize(DEFAULT_RESOURCE_MODEL))

            viewModel.sideEffect.test {
                viewModel.onIntent(OpenMoreMenu)

                val effect = awaitItem()
                assertIs<NavigateToMore>(effect)
                assertThat(effect.resourceId).isEqualTo(DEFAULT_RESOURCE_MODEL.resourceId)
            }
        }

    @Test
    fun `initialize should load resource details and update state`() =
        runTest {
            viewModel = get()

            viewModel.viewState.drop(1).test {
                viewModel.onIntent(Initialize(DEFAULT_RESOURCE_MODEL))

                val state = awaitItem()
                assertThat(state.resourceData.resourceModel).isEqualTo(DEFAULT_RESOURCE_MODEL)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `copy username should emit add to clipboard side effect`() =
        runTest {
            val username = "john.doe@example.com"
            val resourcePropertiesActionsInteractor: ResourcePropertiesActionsInteractor = get()
            resourcePropertiesActionsInteractor.stub {
                onBlocking { provideUsername() } doReturn
                    flowOf(
                        ResourcePropertyActionResult(
                            ResourcePropertiesActionsInteractor.USERNAME_LABEL,
                            isSecret = false,
                            username,
                        ),
                    )
            }

            viewModel = get()
            viewModel.onIntent(Initialize(DEFAULT_RESOURCE_MODEL))

            viewModel.sideEffect.test {
                viewModel.onIntent(CopyUsername)

                val effect = awaitItem()
                assertIs<AddToClipboard>(effect)
                assertThat(effect.value).isEqualTo(username)
                assertThat(effect.isSecret).isFalse()
            }
        }

    @Test
    fun `launch website should emit open website side effect`() =
        runTest {
            val url = "https://www.passbolt.com"
            val resourcePropertiesActionsInteractor: ResourcePropertiesActionsInteractor = get()
            resourcePropertiesActionsInteractor.stub {
                onBlocking { provideMainUri() } doReturn
                    flowOf(
                        ResourcePropertyActionResult(
                            ResourcePropertiesActionsInteractor.URL_LABEL,
                            isSecret = false,
                            url,
                        ),
                    )
            }

            viewModel = get()
            viewModel.onIntent(Initialize(DEFAULT_RESOURCE_MODEL))

            viewModel.sideEffect.test {
                viewModel.onIntent(LaunchWebsite)

                val effect = awaitItem()
                assertIs<OpenWebsite>(effect)
                assertThat(effect.url).isEqualTo(url)
            }
        }

    @Test
    fun `delete click should show confirmation dialog`() =
        runTest {
            viewModel = get()
            viewModel.onIntent(Initialize(DEFAULT_RESOURCE_MODEL))

            viewModel.viewState.drop(1).test {
                viewModel.onIntent(DeleteClick)
                assertThat(awaitItem().showDeleteResourceConfirmationDialog).isTrue()
            }
        }

    @Test
    fun `close delete confirmation dialog should hide dialog`() =
        runTest {
            viewModel = get()
            viewModel.onIntent(Initialize(DEFAULT_RESOURCE_MODEL))
            viewModel.onIntent(DeleteClick)

            viewModel.viewState.drop(1).test {
                viewModel.onIntent(CloseDeleteConfirmationDialog)
                assertThat(awaitItem().showDeleteResourceConfirmationDialog).isFalse()
            }
        }

    @Test
    fun `confirm delete resource should delete and emit close with success`() =
        runTest {
            val resourceName = DEFAULT_RESOURCE_MODEL.metadataJsonModel.name
            val resourceCommonActionsInteractor: ResourceCommonActionsInteractor = get()
            resourceCommonActionsInteractor.stub {
                onBlocking { deleteResource() } doReturn
                    flowOf(
                        ResourceCommonActionResult.Success(resourceName),
                    )
            }

            viewModel = get()
            viewModel.onIntent(Initialize(DEFAULT_RESOURCE_MODEL))
            viewModel.onIntent(DeleteClick)

            viewModel.sideEffect.test {
                viewModel.onIntent(ConfirmDeleteResource)

                val effect = awaitItem()
                assertIs<CloseWithDeleteSuccess>(effect)
                assertThat(effect.resourceName).isEqualTo(resourceName)
            }
        }

    @Test
    fun `confirm delete resource failure should show error snackbar`() =
        runTest {
            val resourceCommonActionsInteractor: ResourceCommonActionsInteractor = get()
            resourceCommonActionsInteractor.stub {
                onBlocking { deleteResource() } doReturn flowOf(ResourceCommonActionResult.Failure)
            }

            viewModel = get()
            viewModel.onIntent(Initialize(DEFAULT_RESOURCE_MODEL))
            viewModel.onIntent(DeleteClick)

            viewModel.sideEffect.test {
                viewModel.onIntent(ConfirmDeleteResource)

                val effect = awaitItem()
                assertIs<ShowErrorSnackbar>(effect)
                assertThat(effect.type).isEqualTo(ErrorSnackbarType.GENERAL_ERROR)
            }
        }

    @Test
    fun `tags should not be shown when disabled by rbac`() =
        runTest {
            val getRbacRulesUseCase: GetRbacRulesUseCase = get()
            getRbacRulesUseCase.stub {
                onBlocking { execute(Unit) } doReturn
                    GetRbacRulesUseCase.Output(
                        DEFAULT_RBAC.copy(tagsUseRule = DENY),
                    )
            }

            viewModel = get()

            viewModel.viewState.drop(2).test {
                viewModel.onIntent(Initialize(DEFAULT_RESOURCE_MODEL))

                val state = awaitItem()
                assertThat(state.metadataData.canViewTags).isFalse()
                assertThat(state.metadataData.tags).isEmpty()
            }
        }

    @Test
    fun `permissions should not be shown when disabled by rbac`() =
        runTest {
            val getRbacRulesUseCase: GetRbacRulesUseCase = get()
            getRbacRulesUseCase.stub {
                onBlocking { execute(Unit) } doReturn
                    GetRbacRulesUseCase.Output(
                        DEFAULT_RBAC.copy(shareViewRule = DENY),
                    )
            }

            viewModel = get()

            viewModel.viewState.drop(2).test {
                viewModel.onIntent(Initialize(DEFAULT_RESOURCE_MODEL))

                val state = awaitItem()
                assertThat(state.sharedWithData.canViewPermissions).isFalse()
                assertThat(state.sharedWithData.permissions).isEmpty()
            }
        }

    @Test
    fun `folder location should not be shown when disabled by rbac`() =
        runTest {
            val getRbacRulesUseCase: GetRbacRulesUseCase = get()
            getRbacRulesUseCase.stub {
                onBlocking { execute(Unit) } doReturn
                    GetRbacRulesUseCase.Output(
                        DEFAULT_RBAC.copy(foldersUseRule = DENY),
                    )
            }

            viewModel = get()

            viewModel.viewState.drop(2).test {
                viewModel.onIntent(Initialize(DEFAULT_RESOURCE_MODEL))

                val state = awaitItem()
                assertThat(state.metadataData.canViewLocation).isFalse()
                assertThat(state.metadataData.locationPath).isEmpty()
            }
        }
}
