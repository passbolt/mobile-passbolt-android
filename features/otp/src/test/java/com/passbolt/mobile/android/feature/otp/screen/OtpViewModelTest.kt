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
import com.passbolt.mobile.android.common.time.TimeProvider
import com.passbolt.mobile.android.common.urimatcher.AutofillUriMatcher
import com.passbolt.mobile.android.commontest.TestCoroutineLaunchContext
import com.passbolt.mobile.android.commontest.coroutinetimer.TestCoroutineTimerFactory
import com.passbolt.mobile.android.core.mvp.authentication.SessionRefreshTrackingFlow
import com.passbolt.mobile.android.core.mvp.coroutinecontext.CoroutineLaunchContext
import com.passbolt.mobile.android.core.otpcore.TotpParametersProvider
import com.passbolt.mobile.android.core.otpcore.TotpParametersProvider.OtpParametersResult.OtpParameters
import com.passbolt.mobile.android.core.resourcetypes.graph.redesigned.UpdateAction
import com.passbolt.mobile.android.core.ui.search.SearchInputEndIconMode.AVATAR
import com.passbolt.mobile.android.core.ui.search.SearchInputEndIconMode.CLEAR
import com.passbolt.mobile.android.domain.accounts.usecase.GetSelectedAccountDataUseCase
import com.passbolt.mobile.android.domain.metadata.interactor.MetadataPrivateKeysHelperInteractor
import com.passbolt.mobile.android.domain.metadata.interactor.ResourceAccessInteractor
import com.passbolt.mobile.android.domain.resources.actions.ResourceUpdateActionResult
import com.passbolt.mobile.android.domain.resources.actions.ResourceUpdateActionsInteractor
import com.passbolt.mobile.android.domain.resources.actions.ResourceUpdateActionsInteractorFactory
import com.passbolt.mobile.android.domain.resources.actions.SecretPropertiesActionsInteractor
import com.passbolt.mobile.android.domain.resources.actions.SecretPropertiesActionsInteractorFactory
import com.passbolt.mobile.android.domain.resources.actions.SecretPropertyActionResult
import com.passbolt.mobile.android.domain.resources.usecase.EditPermissionsConfirmationInteractor
import com.passbolt.mobile.android.domain.resources.usecase.db.GetLocalResourcesUseCase
import com.passbolt.mobile.android.feature.home.screen.ShowSuggestedModel
import com.passbolt.mobile.android.feature.otp.screen.OtpIntent.CloseOtpMoreMenu
import com.passbolt.mobile.android.feature.otp.screen.OtpIntent.CloseSwitchAccount
import com.passbolt.mobile.android.feature.otp.screen.OtpIntent.ConfirmDeleteTotp
import com.passbolt.mobile.android.feature.otp.screen.OtpIntent.CreateTotp
import com.passbolt.mobile.android.feature.otp.screen.OtpIntent.Dispose
import com.passbolt.mobile.android.feature.otp.screen.OtpIntent.EditOtp
import com.passbolt.mobile.android.feature.otp.screen.OtpIntent.OpenOtpMoreMenu
import com.passbolt.mobile.android.feature.otp.screen.OtpIntent.OtpQRScanReturned
import com.passbolt.mobile.android.feature.otp.screen.OtpIntent.RevealOtp
import com.passbolt.mobile.android.feature.otp.screen.OtpIntent.Search
import com.passbolt.mobile.android.feature.otp.screen.OtpIntent.SearchEndIconAction
import com.passbolt.mobile.android.feature.otp.screen.OtpSideEffect.InitiateDataRefresh
import com.passbolt.mobile.android.feature.otp.screen.OtpSideEffect.NavigateToCreateResourceForm
import com.passbolt.mobile.android.feature.otp.screen.OtpSideEffect.NavigateToCreateTotp
import com.passbolt.mobile.android.feature.otp.screen.OtpSideEffect.NavigateToEditResourceForm
import com.passbolt.mobile.android.feature.otp.screen.OtpSideEffect.ShowSuccessSnackbar
import com.passbolt.mobile.android.feature.otp.screen.SnackbarSuccessType.OTP_CREATED
import com.passbolt.mobile.android.feature.otp.screen.SnackbarSuccessType.RESOURCE_CREATED
import com.passbolt.mobile.android.feature.otp.screen.SnackbarSuccessType.RESOURCE_EDITED
import com.passbolt.mobile.android.jsonmodel.delegates.TotpSecret
import com.passbolt.mobile.android.jsonmodel.jsonpathops.JsonPathJsonPathOps
import com.passbolt.mobile.android.jsonmodel.jsonpathops.JsonPathsOps
import com.passbolt.mobile.android.ui.LeadingContentType.TOTP
import com.passbolt.mobile.android.ui.MetadataJsonModel
import com.passbolt.mobile.android.ui.OtpItemWrapper
import com.passbolt.mobile.android.ui.ResourcePermission
import com.passbolt.mobile.android.ui.ResourceUiModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.flowOf
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
import org.koin.core.module.dsl.singleOf
import org.koin.core.parameter.parametersOf
import org.koin.dsl.bind
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.koin.test.KoinTestRule
import org.koin.test.get
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
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
                        single { mock<MetadataPrivateKeysHelperInteractor>() }
                        single { mock<ResourceAccessInteractor>() }
                        single { mock<ResourceUpdateActionsInteractorFactory>() }
                        single { mock<EditPermissionsConfirmationInteractor>() }
                        single { mock<ResourceUpdateActionsInteractor>() }
                        single { mock<SecretPropertiesActionsInteractorFactory>() }
                        single { mock<AutofillUriMatcher>() }
                        single { mock<TimeProvider>() }
                        singleOf(::TestCoroutineTimerFactory) bind TimerFactory::class
                        singleOf(::TestCoroutineLaunchContext) bind CoroutineLaunchContext::class
                        factory { (showSuggestedModel: ShowSuggestedModel) ->
                            OtpViewModel(
                                showSuggestedModel = showSuggestedModel,
                                getSelectedAccountDataUseCase = get(),
                                getLocalResourcesUseCase = get(),
                                totpParametersProvider = get(),
                                coroutineLaunchContext = get(),
                                dataRefreshTrackingFlow = get(),
                                metadataPrivateKeysHelperInteractor = get(),
                                timerFactory = get(),
                                resourceAccessInteractor = get(),
                                resourceUpdateActionsInteractorFactory = get(),
                                editPermissionsConfirmationInteractor = get(),
                                secretPropertiesActionsInteractorFactory = get(),
                                autofillUriMatcher = get(),
                                timeProvider = get(),
                            )
                        }
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
            on { execute(any()) } doReturn GetLocalResourcesUseCase.Output(otpResources)
        }

        get<ResourceAccessInteractor>().stub {
            on { canCreateResource(anyOrNull()) } doReturn true
        }

        get<EditPermissionsConfirmationInteractor>().stub {
            on { shouldConfirmPermissions(any()) } doReturn false
        }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `should be able to open and close switch account`() =
        runTest {
            viewModel = get { parametersOf(ShowSuggestedModel.DoNotShow) }

            viewModel.onIntent(SearchEndIconAction)

            viewModel.viewState.test {
                assertThat(awaitItem().showAccountSwitchBottomSheet).isTrue()

                viewModel.onIntent(CloseSwitchAccount)
                assertThat(awaitItem().showAccountSwitchBottomSheet).isFalse()
            }
        }

    @Test
    fun `should be able to open and close otp menu`() =
        runTest {
            viewModel = get { parametersOf(ShowSuggestedModel.DoNotShow) }

            viewModel.onIntent(OpenOtpMoreMenu(clickedOtp))

            viewModel.viewState.test {
                val state = awaitItem()
                assertThat(state.showOtpMoreBottomSheet).isTrue()
                assertThat(state.moreMenuResource).isEqualTo(clickedOtp)

                viewModel.onIntent(CloseOtpMoreMenu)
                val stateAfter = awaitItem()
                assertThat(stateAfter.showAccountSwitchBottomSheet).isFalse()
            }
        }

    @Test
    fun `avatar should change to clear after search query entered and come back when cleared`() =
        runTest {
            viewModel = get { parametersOf(ShowSuggestedModel.DoNotShow) }

            viewModel.onIntent(Search("abc"))
            advanceUntilIdle()

            viewModel.viewState.test {
                val searchedState = awaitItem()
                assertThat(searchedState.searchInputEndIconMode).isEqualTo(CLEAR)
                assertThat(searchedState.searchQuery).isEqualTo("abc")

                viewModel.onIntent(SearchEndIconAction)
                advanceUntilIdle()

                val state = expectMostRecentItem()
                assertThat(state.searchQuery).isEmpty()
                assertThat(state.searchInputEndIconMode).isEqualTo(AVATAR)
            }
        }

    @Test
    fun `should enter filtering mode on search query`() =
        runTest {
            viewModel = get { parametersOf(ShowSuggestedModel.DoNotShow) }

            viewModel.onIntent(Search("resource 2"))
            advanceUntilIdle()

            viewModel.viewState.test {
                val state = awaitItem()
                assertThat(state.searchQuery).isEqualTo("resource 2")
                assertThat(state.searchInputEndIconMode).isEqualTo(CLEAR)
                assertThat(state.isInFilteringMode).isTrue()
                assertThat(state.isSearching).isFalse()
            }
        }

    @Test
    fun `should mark searching and keep previous results until the query is applied`() =
        runTest {
            viewModel = get { parametersOf(ShowSuggestedModel.DoNotShow) }
            advanceUntilIdle()
            val otpsBeforeSearch = viewModel.viewState.value.uiOtps

            viewModel.onIntent(Search("resource 2"))

            val searchingState = viewModel.viewState.value
            assertThat(searchingState.isSearching).isTrue()
            assertThat(searchingState.isInFilteringMode).isFalse()
            assertThat(searchingState.uiOtps).isEqualTo(otpsBeforeSearch)

            advanceUntilIdle()

            val appliedState = viewModel.viewState.value
            assertThat(appliedState.isSearching).isFalse()
            assertThat(appliedState.isInFilteringMode).isTrue()
        }

    @Test
    fun `should reveal otp on filtered list when search query is active`() =
        runTest {
            mockSuccessfulTotpFetch()
            viewModel = get { parametersOf(ShowSuggestedModel.DoNotShow) }

            viewModel.onIntent(Search("resource"))
            advanceUntilIdle()
            viewModel.onIntent(RevealOtp(otpResources.first()))

            viewModel.viewState.test {
                val state = expectMostRecentItem()
                assertThat(state.isInFilteringMode).isTrue()
                val revealedItem = state.uiOtps.first { it.resource.resourceId == otpResources.first().resourceId }
                assertThat(revealedItem.isVisible).isTrue()
                assertThat(revealedItem.otpValue).isEqualTo(OTP_VALUE)
            }
        }

    @Test
    fun `should reset revealed otp but keep search filter on dispose`() =
        runTest {
            mockSuccessfulTotpFetch()
            viewModel = get { parametersOf(ShowSuggestedModel.DoNotShow) }

            viewModel.onIntent(Search("resource"))
            advanceUntilIdle()
            viewModel.onIntent(RevealOtp(otpResources.first()))

            viewModel.viewState.test {
                assertThat(expectMostRecentItem().uiOtps.any { it.isVisible }).isTrue()

                viewModel.onIntent(Dispose)

                val state = expectMostRecentItem()
                assertThat(state.searchQuery).isEqualTo("resource")
                assertThat(state.isInFilteringMode).isTrue()
                assertThat(state.otps.none { it.isVisible }).isTrue()
                assertThat(state.filteredOtps.none { it.isVisible }).isTrue()
            }
        }

    private fun mockSuccessfulTotpFetch(otpFlow: Flow<SecretPropertyActionResult<TotpSecret>> = flowOf(totpFetchSuccess)) {
        val secretPropertiesActionsInteractor =
            mock<SecretPropertiesActionsInteractor> {
                on { provideOtp() } doReturn otpFlow
            }
        val secretPropertiesActionsInteractorFactory = get<SecretPropertiesActionsInteractorFactory>()
        whenever(secretPropertiesActionsInteractorFactory.create(any())) doReturn secretPropertiesActionsInteractor

        val totpParametersProvider = get<TotpParametersProvider>()
        whenever(totpParametersProvider.provideOtpParameters(any(), any(), any(), any())) doReturn
            OtpParameters(OTP_VALUE, secondsValid = 25)
    }

    @Test
    fun `should show refresh while resources are loading`() =
        runTest {
            viewModel = get { parametersOf(ShowSuggestedModel.DoNotShow) }

            viewModel.viewState.drop(1).test {
                val dataRefreshStatusFlow = get<DataRefreshTrackingFlow>()
                dataRefreshStatusFlow.updateStatus(InProgress(progress = 0f))
                val state = awaitItem()
                assertThat(state.isRefreshing).isTrue()

                dataRefreshStatusFlow.updateStatus(FinishedWithSuccess)

                val stateAfter = awaitItem()
                assertThat(stateAfter.isRefreshing).isFalse()
            }
        }

    @Test
    fun `should initialize with avatar URL`() =
        runTest {
            viewModel = get { parametersOf(ShowSuggestedModel.DoNotShow) }

            viewModel.viewState.test {
                val state = awaitItem()
                assertThat(state.userAvatar).isEqualTo(selectedAccountData.avatarUrl)
                assertThat(state.otps).hasSize(otpResources.size)
            }
        }

    @Test
    fun `should navigate to create totp screen when create totp intent is received`() =
        runTest {
            viewModel = get { parametersOf(ShowSuggestedModel.DoNotShow) }

            viewModel.sideEffect.test {
                viewModel.onIntent(CreateTotp)

                val sideEffect = awaitItem()
                assertIs<NavigateToCreateTotp>(sideEffect)
            }
        }

    @Test
    fun `should process otp scan result with successful creation`() =
        runTest {
            viewModel = get { parametersOf(ShowSuggestedModel.DoNotShow) }

            viewModel.sideEffect.test {
                viewModel.onIntent(OtpQRScanReturned(otpCreated = true, otpManualCreationChosen = false))

                assertIs<InitiateDataRefresh>(awaitItem())

                val snackbarSideEffect = awaitItem()
                assertIs<ShowSuccessSnackbar>(snackbarSideEffect)
                assertThat(snackbarSideEffect.type).isEqualTo(OTP_CREATED)
            }
        }

    @Test
    fun `should process otp scan result with manual creation chosen`() =
        runTest {
            viewModel = get { parametersOf(ShowSuggestedModel.DoNotShow) }

            viewModel.sideEffect.test {
                viewModel.onIntent(OtpQRScanReturned(otpCreated = false, otpManualCreationChosen = true))

                val sideEffect = awaitItem()
                assertIs<NavigateToCreateResourceForm>(sideEffect)
                assertThat(sideEffect.leadingContentType).isEqualTo(TOTP)
            }
        }

    @Test
    fun `should process resource form result with created resource`() =
        runTest {
            viewModel = get { parametersOf(ShowSuggestedModel.DoNotShow) }

            viewModel.sideEffect.test {
                viewModel.onIntent(
                    OtpIntent.ResourceFormReturned(
                        resourceCreated = true,
                        resourceEdited = false,
                        resourceName = "New Resource",
                    ),
                )

                assertIs<InitiateDataRefresh>(awaitItem())

                val snackbarSideEffect = awaitItem()
                assertIs<ShowSuccessSnackbar>(snackbarSideEffect)
                assertThat(snackbarSideEffect.type).isEqualTo(RESOURCE_CREATED)
                assertThat(snackbarSideEffect.message).isEqualTo("New Resource")
            }
        }

    @Test
    fun `should process resource form result with edited resource`() =
        runTest {
            viewModel = get { parametersOf(ShowSuggestedModel.DoNotShow) }

            viewModel.sideEffect.test {
                viewModel.onIntent(
                    OtpIntent.ResourceFormReturned(
                        resourceCreated = false,
                        resourceEdited = true,
                        resourceName = null,
                    ),
                )

                assertIs<InitiateDataRefresh>(awaitItem())

                val snackbarSideEffect = awaitItem()
                assertIs<ShowSuccessSnackbar>(snackbarSideEffect)
                assertThat(snackbarSideEffect.type).isEqualTo(RESOURCE_EDITED)
            }
        }

    @Test
    fun `should navigate to edit resource form when edit otp intent is received`() =
        runTest {
            viewModel = get { parametersOf(ShowSuggestedModel.DoNotShow) }

            viewModel.sideEffect.test {
                viewModel.onIntent(EditOtp(clickedOtp))

                val sideEffect = awaitItem()
                assertIs<NavigateToEditResourceForm>(sideEffect)
                assertThat(sideEffect.resourceId).isEqualTo(clickedOtp.resource.resourceId)
                assertThat(sideEffect.resourceName).isEqualTo(clickedOtp.resource.metadataJsonModel.name)
            }
        }

    @Test
    fun `should show delete confirmation dialog when delete otp intent is received`() =
        runTest {
            viewModel = get { parametersOf(ShowSuggestedModel.DoNotShow) }

            viewModel.onIntent(OtpIntent.DeleteOtp(clickedOtp))

            viewModel.viewState.test {
                assertThat(awaitItem().showDeleteTotpConfirmationDialog).isTrue()
                viewModel.onIntent(OtpIntent.CloseDeleteConfirmationDialog)
                assertThat(awaitItem().showDeleteTotpConfirmationDialog).isFalse()
            }
        }

    @Test
    fun `deleting totp from a shared combined resource should navigate to permissions confirmation`() =
        runTest {
            get<EditPermissionsConfirmationInteractor>().stub {
                on { shouldConfirmPermissions(combinedTotpResource.resourceId) } doReturn true
            }
            viewModel = get { parametersOf(ShowSuggestedModel.DoNotShow) }

            viewModel.sideEffect.test {
                viewModel.onIntent(OpenOtpMoreMenu(combinedTotpWrapper))
                viewModel.onIntent(ConfirmDeleteTotp)

                assertThat(awaitItem()).isEqualTo(
                    OtpSideEffect.NavigateToConfirmPermissions(combinedTotpResource.resourceId),
                )
            }
            verifyNoInteractions(get<ResourceUpdateActionsInteractorFactory>())
        }

    @Test
    fun `confirmed permissions should delete totp with the confirmed list`() =
        runTest {
            get<EditPermissionsConfirmationInteractor>().stub {
                on { shouldConfirmPermissions(combinedTotpResource.resourceId) } doReturn true
            }
            get<ResourceUpdateActionsInteractorFactory>().stub {
                on { create(any()) } doReturn get<ResourceUpdateActionsInteractor>()
            }
            get<ResourceUpdateActionsInteractor>().stub {
                on {
                    updateGenericResourceWithConfirmedPermissions(eq(UpdateAction.REMOVE_TOTP), any(), any(), any())
                } doReturn flowOf(ResourceUpdateActionResult.Success(combinedTotpResource.resourceId, "name"))
            }
            viewModel = get { parametersOf(ShowSuggestedModel.DoNotShow) }

            viewModel.sideEffect.test {
                viewModel.onIntent(OpenOtpMoreMenu(combinedTotpWrapper))
                viewModel.onIntent(ConfirmDeleteTotp)
                awaitItem()

                viewModel.onIntent(OtpIntent.ConfirmedPermissionsResult(emptyList()))

                assertThat(awaitItem()).isEqualTo(ShowSuccessSnackbar(SnackbarSuccessType.RESOURCE_DELETED))
                assertThat(awaitItem()).isEqualTo(InitiateDataRefresh)
            }
            verify(get<ResourceUpdateActionsInteractor>())
                .updateGenericResourceWithConfirmedPermissions(eq(UpdateAction.REMOVE_TOTP), any(), any(), any())
        }

    @Test
    fun `should show error when resource creation not possible`() =
        runTest {
            get<ResourceAccessInteractor>().stub {
                on { canCreateResource(anyOrNull()) } doReturn false
            }

            viewModel = get { parametersOf(ShowSuggestedModel.DoNotShow) }

            viewModel.sideEffect.test {
                viewModel.onIntent(CreateTotp)
                val stateAfterCreateTotp = awaitItem()
                assertIs<OtpSideEffect.ShowErrorSnackbar>(stateAfterCreateTotp)
                assertThat(stateAfterCreateTotp.type).isEqualTo(SnackbarErrorType.NO_SHARED_KEY_ACCESS)
            }
        }

    private companion object {
        private const val OTP_VALUE = "123456"

        private val totpFetchSuccess by lazy {
            SecretPropertyActionResult.Success(
                SecretPropertiesActionsInteractor.OTP_LABEL,
                isSecret = true,
                TotpSecret(
                    algorithm = "SHA1",
                    key = "JBSWY3DPEHPK3PXP",
                    digits = 6,
                    period = 30L,
                ),
            )
        }

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
                ResourceUiModel(
                    resourceId = "resId",
                    resourceTypeId = "resTypeId",
                    slug = "password-and-description",
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
                ResourceUiModel(
                    resourceId = "resId2",
                    resourceTypeId = "resTypeId",
                    slug = "password-and-description",
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

        private val combinedTotpResource by lazy {
            otpResources.first().copy(slug = "password-description-totp")
        }

        private val combinedTotpWrapper by lazy {
            OtpItemWrapper(
                combinedTotpResource,
                isVisible = false,
                isRefreshing = false,
                otpExpirySeconds = null,
                otpValue = null,
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
