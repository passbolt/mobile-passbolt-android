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

package com.passbolt.mobile.android.feature.otp.screen

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.jayway.jsonpath.Configuration
import com.jayway.jsonpath.Option
import com.jayway.jsonpath.spi.json.GsonJsonProvider
import com.jayway.jsonpath.spi.mapper.GsonMappingProvider
import com.passbolt.mobile.android.common.coroutinetimer.TimerFactory
import com.passbolt.mobile.android.common.datarefresh.DataRefreshStatus.Idle.FinishedWithSuccess
import com.passbolt.mobile.android.common.datarefresh.DataRefreshStatus.InProgress
import com.passbolt.mobile.android.common.datarefresh.DataRefreshTrackingFlow
import com.passbolt.mobile.android.common.search.SearchableMatcher
import com.passbolt.mobile.android.commontest.TestCoroutineLaunchContext
import com.passbolt.mobile.android.commontest.coroutinetimer.TestCoroutineTimerFactory
import com.passbolt.mobile.android.core.accounts.usecase.accountdata.GetSelectedAccountDataUseCase
import com.passbolt.mobile.android.core.mvp.authentication.SessionRefreshTrackingFlow
import com.passbolt.mobile.android.core.mvp.coroutinecontext.CoroutineLaunchContext
import com.passbolt.mobile.android.core.otpcore.TotpParametersProvider
import com.passbolt.mobile.android.core.resources.usecase.db.GetLocalResourcesUseCase
import com.passbolt.mobile.android.core.resourcetypes.usecase.db.ResourceTypeIdToSlugMappingProvider
import com.passbolt.mobile.android.core.ui.compose.search.SearchInputEndIconMode
import com.passbolt.mobile.android.core.ui.compose.search.SearchInputEndIconMode.AVATAR
import com.passbolt.mobile.android.core.ui.compose.search.SearchInputEndIconMode.CLEAR
import com.passbolt.mobile.android.feature.otp.screen.OtpIntent.CloseCreateResourceMenu
import com.passbolt.mobile.android.feature.otp.screen.OtpIntent.CloseOtpMoreMenu
import com.passbolt.mobile.android.feature.otp.screen.OtpIntent.CloseSwitchAccount
import com.passbolt.mobile.android.feature.otp.screen.OtpIntent.CreatePassword
import com.passbolt.mobile.android.feature.otp.screen.OtpIntent.CreateTotp
import com.passbolt.mobile.android.feature.otp.screen.OtpIntent.EditOtp
import com.passbolt.mobile.android.feature.otp.screen.OtpIntent.OpenCreateResourceMenu
import com.passbolt.mobile.android.feature.otp.screen.OtpIntent.OpenOtpMoreMenu
import com.passbolt.mobile.android.feature.otp.screen.OtpIntent.OtpQRScanReturned
import com.passbolt.mobile.android.feature.otp.screen.OtpIntent.Search
import com.passbolt.mobile.android.feature.otp.screen.OtpIntent.SearchEndIconAction
import com.passbolt.mobile.android.feature.otp.screen.OtpSideEffect.InitiateDataRefresh
import com.passbolt.mobile.android.feature.otp.screen.OtpSideEffect.NavigateToCreateResourceForm
import com.passbolt.mobile.android.feature.otp.screen.OtpSideEffect.NavigateToCreateTotp
import com.passbolt.mobile.android.feature.otp.screen.OtpSideEffect.NavigateToEditResourceForm
import com.passbolt.mobile.android.feature.otp.screen.OtpSideEffect.ShowSuccessSnackbar
import com.passbolt.mobile.android.feature.otp.screen.SnackbarSuccessType.RESOURCE_CREATED
import com.passbolt.mobile.android.feature.otp.screen.SnackbarSuccessType.RESOURCE_EDITED
import com.passbolt.mobile.android.jsonmodel.jsonpathops.JsonPathJsonPathOps
import com.passbolt.mobile.android.jsonmodel.jsonpathops.JsonPathsOps
import com.passbolt.mobile.android.mappers.OtpModelMapper
import com.passbolt.mobile.android.metadata.interactor.MetadataPrivateKeysHelperInteractor
import com.passbolt.mobile.android.ui.LeadingContentType.PASSWORD
import com.passbolt.mobile.android.ui.LeadingContentType.TOTP
import com.passbolt.mobile.android.ui.MetadataJsonModel
import com.passbolt.mobile.android.ui.OtpItemWrapper
import com.passbolt.mobile.android.ui.ResourceModel
import com.passbolt.mobile.android.ui.ResourcePermission
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.drop
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
import org.mockito.kotlin.whenever
import java.time.ZonedDateTime
import java.util.EnumSet
import kotlin.test.assertIs
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalCoroutinesApi::class, ExperimentalTime::class)
class OtpViewModelTest : KoinTest {
    @get:Rule
    val koinTestRule =
        KoinTestRule.create {
            printLogger(Level.ERROR)
            modules(
                listOf(
                    module {
                        single { mock<GetSelectedAccountDataUseCase>() }
                        single { mock<GetLocalResourcesUseCase>() }
                        single { mock<TotpParametersProvider>() }
                        single { mock<ResourceTypeIdToSlugMappingProvider>() }
                        single { mock<MetadataPrivateKeysHelperInteractor>() }
                        singleOf(::TestCoroutineTimerFactory) bind TimerFactory::class
                        singleOf(::TestCoroutineLaunchContext) bind CoroutineLaunchContext::class
                        factoryOf(::OtpViewModel)
                        singleOf(::JsonPathJsonPathOps) bind JsonPathsOps::class
                        single {
                            Configuration
                                .builder()
                                .jsonProvider(GsonJsonProvider())
                                .mappingProvider(GsonMappingProvider())
                                .options(EnumSet.noneOf(Option::class.java))
                                .build()
                        }
                        factoryOf(::OtpModelMapper)
                        factoryOf(::SearchableMatcher)
                        singleOf(::DataRefreshTrackingFlow)
                        singleOf(::SessionRefreshTrackingFlow)
                    },
                ),
            )
        }

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: OtpViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        val getSelectedAccountDataUseCase = get<GetSelectedAccountDataUseCase>()
        whenever(getSelectedAccountDataUseCase.execute(Unit)) doReturn selectedAccountData

        val getLocalResourcesUseCase = get<GetLocalResourcesUseCase>()
        getLocalResourcesUseCase.stub {
            onBlocking { execute(any()) } doReturn GetLocalResourcesUseCase.Output(otpResources)
        }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `should be able to open and close create resource`() =
        runTest {
            viewModel = get()

            viewModel.onIntent(OpenCreateResourceMenu)

            viewModel.viewState.test {
                assertThat(expectItem().showCreateResourceBottomSheet).isTrue()

                viewModel.onIntent(CloseCreateResourceMenu)
                assertThat(expectItem().showCreateResourceBottomSheet).isFalse()
            }
        }

    @Test
    fun `should be able to open and close switch account`() =
        runTest {
            viewModel = get()

            viewModel.onIntent(SearchEndIconAction)

            viewModel.viewState.test {
                assertThat(expectItem().showAccountSwitchBottomSheet).isTrue()

                viewModel.onIntent(CloseSwitchAccount)
                assertThat(expectItem().showAccountSwitchBottomSheet).isFalse()
            }
        }

    @Test
    fun `should be able to open and close otp menu`() =
        runTest {
            viewModel = get()

            viewModel.onIntent(OpenOtpMoreMenu(clickedOtp))

            viewModel.viewState.test {
                val state = expectItem()
                assertThat(state.showOtpMoreBottomSheet).isTrue()
                assertThat(state.moreMenuResource).isEqualTo(clickedOtp)

                viewModel.onIntent(CloseOtpMoreMenu)
                val stateAfter = expectItem()
                assertThat(stateAfter.showAccountSwitchBottomSheet).isFalse()
            }
        }

    @Test
    fun `avatar should change to clear after search query entered and come back when cleared`() =
        runTest {
            viewModel = get()

            viewModel.onIntent(Search("abc"))

            viewModel.viewState.test {
                assertThat(expectItem().searchInputEndIconMode).isEqualTo(CLEAR)

                viewModel.onIntent(SearchEndIconAction)
                val state = expectItem()
                assertThat(state.searchQuery).isEmpty()
                assertThat(state.searchInputEndIconMode).isEqualTo(AVATAR)
            }
        }

    @Test
    fun `should filter otps based on search query`() =
        runTest {
            viewModel = get()

            viewModel.onIntent(Search("resource 2"))

            viewModel.viewState.test {
                val state = expectItem()
                assertThat(state.searchQuery).isEqualTo("resource 2")
                assertThat(state.searchInputEndIconMode).isEqualTo(SearchInputEndIconMode.CLEAR)
                assertThat(state.isInFilteringMode).isTrue()
                assertThat(state.filteredOtps).hasSize(1)
                assertThat(
                    state.filteredOtps
                        .first()
                        .resource.resourceId,
                ).isEqualTo("resId2")
            }
        }

    @Test
    fun `should show refresh while resources are loading`() =
        runTest {
            viewModel = get()

            viewModel.viewState.drop(1).test {
                val dataRefreshStatusFlow = get<DataRefreshTrackingFlow>()
                dataRefreshStatusFlow.updateStatus(InProgress)
                val state = expectItem()
                assertThat(state.isRefreshing).isTrue()

                dataRefreshStatusFlow.updateStatus(FinishedWithSuccess)

                val stateAfter = expectItem()
                assertThat(stateAfter.isRefreshing).isFalse()
            }
        }

    @Test
    fun `should initialize with avatar URL`() =
        runTest {
            viewModel = get()

            viewModel.viewState.test {
                val state = expectItem()
                assertThat(state.userAvatar).isEqualTo(selectedAccountData.avatarUrl)
                assertThat(state.otps).hasSize(otpResources.size)
            }
        }

    @Test
    fun `should navigate to create resource form when create password intent is received`() =
        runTest {
            viewModel = get()

            viewModel.sideEffect.test {
                viewModel.onIntent(CreatePassword)

                val sideEffect = expectItem()
                assertIs<NavigateToCreateResourceForm>(sideEffect)
                assertThat(sideEffect.leadingContentType).isEqualTo(PASSWORD)
            }
        }

    @Test
    fun `should navigate to create totp screen when create totp intent is received`() =
        runTest {
            viewModel = get()

            viewModel.sideEffect.test {
                viewModel.onIntent(CreateTotp)

                val sideEffect = expectItem()
                assertIs<NavigateToCreateTotp>(sideEffect)
            }
        }

    @Test
    fun `should process otp scan result with successful creation`() =
        runTest {
            viewModel = get()

            viewModel.onIntent(OtpQRScanReturned(otpCreated = true, otpManualCreationChosen = false))

            viewModel.sideEffect.test {
                assertIs<InitiateDataRefresh>(expectItem())
            }
        }

    @Test
    fun `should process otp scan result with manual creation chosen`() =
        runTest {
            viewModel = get()

            viewModel.sideEffect.test {
                viewModel.onIntent(OtpQRScanReturned(otpCreated = false, otpManualCreationChosen = true))

                val sideEffect = expectItem()
                assertIs<NavigateToCreateResourceForm>(sideEffect)
                assertThat(sideEffect.leadingContentType).isEqualTo(TOTP)
            }
        }

    @Test
    fun `should process resource form result with created resource`() =
        runTest {
            viewModel = get()

            viewModel.sideEffect.test {
                viewModel.onIntent(
                    OtpIntent.ResourceFormReturned(
                        resourceCreated = true,
                        resourceEdited = false,
                        resourceName = "New Resource",
                    ),
                )

                assertIs<InitiateDataRefresh>(expectItem())

                val snackbarSideEffect = expectItem()
                assertIs<ShowSuccessSnackbar>(snackbarSideEffect)
                assertThat(snackbarSideEffect.type).isEqualTo(RESOURCE_CREATED)
                assertThat(snackbarSideEffect.message).isEqualTo("New Resource")
            }
        }

    @Test
    fun `should process resource form result with edited resource`() =
        runTest {
            viewModel = get()

            viewModel.sideEffect.test {
                viewModel.onIntent(
                    OtpIntent.ResourceFormReturned(
                        resourceCreated = false,
                        resourceEdited = true,
                        resourceName = null,
                    ),
                )

                assertIs<InitiateDataRefresh>(expectItem())

                val snackbarSideEffect = expectItem()
                assertIs<ShowSuccessSnackbar>(snackbarSideEffect)
                assertThat(snackbarSideEffect.type).isEqualTo(RESOURCE_EDITED)
            }
        }

    @Test
    fun `should navigate to edit resource form when edit otp intent is received`() =
        runTest {
            viewModel = get()

            viewModel.sideEffect.test {
                viewModel.onIntent(EditOtp(clickedOtp))

                val sideEffect = expectItem()
                assertIs<NavigateToEditResourceForm>(sideEffect)
                assertThat(sideEffect.resourceId).isEqualTo(clickedOtp.resource.resourceId)
                assertThat(sideEffect.resourceName).isEqualTo(clickedOtp.resource.metadataJsonModel.name)
            }
        }

    @Test
    fun `should show delete confirmation dialog when delete otp intent is received`() =
        runTest {
            viewModel = get()

            viewModel.onIntent(OtpIntent.DeleteOtp(clickedOtp))

            viewModel.viewState.test {
                assertThat(expectItem().showDeleteTotpConfirmationDialog).isTrue()
                viewModel.onIntent(OtpIntent.CloseDeleteConfirmationDialog)
                assertThat(expectItem().showDeleteTotpConfirmationDialog).isFalse()
            }
        }

    private companion object {
        private val selectedAccountData =
            GetSelectedAccountDataUseCase.Output(
                firstName = "John",
                lastName = "Doe",
                avatarUrl = "https://passbolt.com/avatar.jpg",
                label = "John Doe",
                email = "john.doe@passbolt.com",
                url = "https://passbolt.com",
                serverId = "123e4567-e89b-12d3-a456-426614174000",
                role = "admin",
            )

        private val otpResources by lazy {
            listOf(
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
                ),
                ResourceModel(
                    resourceId = "resId2",
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
                                "name": "resource 2",
                                "uri": "",
                                "username": "",
                                "description": ""
                            }
                            """.trimIndent(),
                        ),
                    metadataKeyId = null,
                    metadataKeyType = null,
                ),
            )
        }

        private val clickedOtp by lazy {
            OtpItemWrapper(
                otpResources.first(),
                isVisible = false,
                isRefreshing = false,
                otpExpirySeconds = null,
                otpValue = null,
            )
        }
    }
}
