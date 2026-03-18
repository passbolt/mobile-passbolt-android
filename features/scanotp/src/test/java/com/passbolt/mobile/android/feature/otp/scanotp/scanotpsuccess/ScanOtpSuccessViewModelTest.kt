package com.passbolt.mobile.android.feature.otp.scanotp.scanotpsuccess

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.passbolt.mobile.android.core.resources.actions.ResourceCreateActionResult
import com.passbolt.mobile.android.core.resources.actions.ResourceUpdateActionResult
import com.passbolt.mobile.android.core.resources.usecase.GetDefaultCreateContentTypeUseCase
import com.passbolt.mobile.android.core.resourcetypes.graph.redesigned.UpdateAction
import com.passbolt.mobile.android.supportedresourceTypes.ContentType
import com.passbolt.mobile.android.ui.MetadataJsonModel
import com.passbolt.mobile.android.ui.MetadataTypeModel
import com.passbolt.mobile.android.ui.OtpParseResult
import com.passbolt.mobile.android.ui.ResourceModel
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
import org.koin.core.parameter.parametersOf
import org.koin.test.KoinTest
import org.koin.test.KoinTestRule
import org.koin.test.get
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import java.util.UUID
import kotlin.test.assertIs

@OptIn(ExperimentalCoroutinesApi::class)
class ScanOtpSuccessViewModelTest : KoinTest {
    @get:Rule
    val koinTestRule =
        KoinTestRule.create {
            printLogger(Level.ERROR)
            modules(testScanOtpSuccessModule)
        }

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `create standalone totp should create totp and navigate to otp list`() =
        runTest {
            mockGetDefaultCreateContentTypeUseCase.stub {
                onBlocking { execute(any()) }.doReturn(
                    GetDefaultCreateContentTypeUseCase.Output.CreationContentType(
                        ContentType.V5TotpStandalone,
                        MetadataTypeModel.V5,
                    ),
                )
            }
            val mockResourceId = UUID.randomUUID()
            val mockResourceName = "mockResourceName"
            mockResourceCreateActionsInteractor.stub {
                onBlocking {
                    createGenericResource(any(), anyOrNull(), any(), any())
                }.doReturn(flowOf(ResourceCreateActionResult.Success(mockResourceId.toString(), mockResourceName)))
            }

            val viewModel = get<ScanOtpSuccessViewModel> { parametersOf(mockScannedTotp, null) }

            viewModel.sideEffect.test {
                viewModel.onIntent(ScanOtpSuccessIntent.CreateStandaloneOtpClick)
                testDispatcher.scheduler.advanceUntilIdle()

                val sideEffect = awaitItem()
                assertIs<ScanOtpSuccessSideEffect.NavigateToOtpList>(sideEffect)
                assertThat(sideEffect.totp).isEqualTo(mockScannedTotp)
                assertThat(sideEffect.otpCreated).isTrue()
                assertThat(sideEffect.resourceId).isEqualTo(mockResourceId.toString())
            }
        }

    @Test
    fun `create standalone totp should show and hide progress`() =
        runTest {
            mockGetDefaultCreateContentTypeUseCase.stub {
                onBlocking { execute(any()) }.doReturn(
                    GetDefaultCreateContentTypeUseCase.Output.CreationContentType(
                        ContentType.V5TotpStandalone,
                        MetadataTypeModel.V5,
                    ),
                )
            }
            val mockResourceId = UUID.randomUUID()
            mockResourceCreateActionsInteractor.stub {
                onBlocking {
                    createGenericResource(any(), anyOrNull(), any(), any())
                }.doReturn(flowOf(ResourceCreateActionResult.Success(mockResourceId.toString(), "name")))
            }

            val viewModel = get<ScanOtpSuccessViewModel> { parametersOf(mockScannedTotp, null) }

            viewModel.viewState.test {
                val initialState = awaitItem()
                assertThat(initialState.showProgress).isFalse()

                viewModel.onIntent(ScanOtpSuccessIntent.CreateStandaloneOtpClick)

                val progressShown = awaitItem()
                assertThat(progressShown.showProgress).isTrue()

                testDispatcher.scheduler.advanceUntilIdle()

                val progressHidden = awaitItem()
                assertThat(progressHidden.showProgress).isFalse()
            }
        }

    @Test
    fun `link to resource click should navigate to resource picker`() =
        runTest {
            val viewModel = get<ScanOtpSuccessViewModel> { parametersOf(mockScannedTotp, null) }

            viewModel.sideEffect.test {
                viewModel.onIntent(ScanOtpSuccessIntent.LinkToResourceClick)

                val sideEffect = awaitItem()
                assertIs<ScanOtpSuccessSideEffect.NavigateToResourcePicker>(sideEffect)
                assertThat(sideEffect.suggestedUri).isEqualTo(mockScannedTotp.issuer)
            }
        }

    @Test
    fun `link totp to linked resource should update and navigate to otp list`() =
        runTest {
            val mockResourceId = UUID.randomUUID()
            val mockResourceTypeId = UUID.randomUUID()
            val mockResourceName = "mockResourceName"
            val mockMetadataJsonModel =
                mock<MetadataJsonModel> {
                    on { name } doReturn mockResourceName
                }
            val mockLinkResourceModel =
                mock<ResourceModel> {
                    on { resourceTypeId } doReturn mockResourceTypeId.toString()
                    on { metadataJsonModel } doReturn mockMetadataJsonModel
                }

            mockResourceUpdateActionsInteractor.stub {
                onBlocking {
                    updateGenericResource(eq(UpdateAction.ADD_TOTP), any(), any())
                }.doReturn(flowOf(ResourceUpdateActionResult.Success(mockResourceId.toString(), mockResourceName)))
            }
            mockIdToSlugMappingProvider.stub {
                onBlocking { provideMappingForSelectedAccount() }.doReturn(
                    mapOf(mockResourceTypeId to ContentType.V5DefaultWithTotp.slug),
                )
            }

            val viewModel = get<ScanOtpSuccessViewModel> { parametersOf(mockScannedTotp, null) }

            viewModel.sideEffect.test {
                viewModel.onIntent(
                    ScanOtpSuccessIntent.LinkedResourceReceived(mockLinkResourceModel),
                )
                testDispatcher.scheduler.advanceUntilIdle()

                val sideEffect = awaitItem()
                assertIs<ScanOtpSuccessSideEffect.NavigateToOtpList>(sideEffect)
                assertThat(sideEffect.totp).isEqualTo(mockScannedTotp)
                assertThat(sideEffect.otpCreated).isTrue()
                assertThat(sideEffect.resourceId).isEqualTo(mockResourceId.toString())
            }
        }

    @Test
    fun `link totp to password resource should update and navigate to otp list`() =
        runTest {
            val mockResourceId = UUID.randomUUID()
            val mockResourceTypeId = UUID.randomUUID()
            val mockResourceName = "mockResourceName"
            val mockMetadataJsonModel =
                mock<MetadataJsonModel> {
                    on { name } doReturn mockResourceName
                }
            val mockLinkResourceModel =
                mock<ResourceModel> {
                    on { resourceTypeId } doReturn mockResourceTypeId.toString()
                    on { metadataJsonModel } doReturn mockMetadataJsonModel
                }

            mockResourceUpdateActionsInteractor.stub {
                onBlocking {
                    updateGenericResource(eq(UpdateAction.ADD_TOTP), any(), any())
                }.doReturn(flowOf(ResourceUpdateActionResult.Success(mockResourceId.toString(), mockResourceName)))
            }
            mockIdToSlugMappingProvider.stub {
                onBlocking { provideMappingForSelectedAccount() }.doReturn(
                    mapOf(mockResourceTypeId to ContentType.V5Default.slug),
                )
            }

            val viewModel = get<ScanOtpSuccessViewModel> { parametersOf(mockScannedTotp, null) }

            viewModel.sideEffect.test {
                viewModel.onIntent(
                    ScanOtpSuccessIntent.LinkedResourceReceived(mockLinkResourceModel),
                )
                testDispatcher.scheduler.advanceUntilIdle()

                val sideEffect = awaitItem()
                assertIs<ScanOtpSuccessSideEffect.NavigateToOtpList>(sideEffect)
                assertThat(sideEffect.totp).isEqualTo(mockScannedTotp)
                assertThat(sideEffect.otpCreated).isTrue()
                assertThat(sideEffect.resourceId).isEqualTo(mockResourceId.toString())
            }
        }

    private companion object {
        val mockScannedTotp =
            OtpParseResult.OtpQr.TotpQr(
                label = "label",
                secret = "secret",
                issuer = "issuer",
                algorithm = OtpParseResult.OtpQr.Algorithm.SHA512,
                digits = 6,
                period = 30,
            )
    }
}
