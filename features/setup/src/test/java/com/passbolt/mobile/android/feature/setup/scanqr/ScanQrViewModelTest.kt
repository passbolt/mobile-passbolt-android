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

package com.passbolt.mobile.android.feature.setup.scanqr

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.passbolt.mobile.android.common.HttpsVerifier
import com.passbolt.mobile.android.common.UuidProvider
import com.passbolt.mobile.android.common.usecase.FetchFileAsStringUseCase
import com.passbolt.mobile.android.core.accounts.AccountKitParser
import com.passbolt.mobile.android.core.accounts.AccountsInteractor
import com.passbolt.mobile.android.core.accounts.AccountsInteractor.InjectAccountFailureType.ACCOUNT_ALREADY_LINKED
import com.passbolt.mobile.android.core.accounts.AccountsInteractor.InjectAccountFailureType.ERROR_NON_HTTPS_DOMAIN
import com.passbolt.mobile.android.core.accounts.AccountsInteractor.InjectAccountFailureType.ERROR_WHEN_SAVING_PRIVATE_KEY
import com.passbolt.mobile.android.core.accounts.usecase.accountdata.UpdateAccountDataUseCase
import com.passbolt.mobile.android.core.accounts.usecase.accounts.CheckAccountExistsUseCase
import com.passbolt.mobile.android.core.accounts.usecase.privatekey.SavePrivateKeyUseCase
import com.passbolt.mobile.android.core.accounts.usecase.selectedaccount.SaveCurrentApiUrlUseCase
import com.passbolt.mobile.android.core.navigation.AccountSetupDataModel
import com.passbolt.mobile.android.core.networking.NetworkResult.Failure.NetworkError
import com.passbolt.mobile.android.core.qrscan.analyzer.BarcodeScanResult
import com.passbolt.mobile.android.dto.response.qrcode.AccountKitPageDto
import com.passbolt.mobile.android.dto.response.qrcode.QrFirstPageDto
import com.passbolt.mobile.android.dto.response.qrcode.ReservedBytesDto
import com.passbolt.mobile.android.feature.setup.scanqr.ScanQrIntent.AccessLogs
import com.passbolt.mobile.android.feature.setup.scanqr.ScanQrIntent.ConfirmSetupLeave
import com.passbolt.mobile.android.feature.setup.scanqr.ScanQrIntent.DismissHelpMenu
import com.passbolt.mobile.android.feature.setup.scanqr.ScanQrIntent.DismissServerNotReachable
import com.passbolt.mobile.android.feature.setup.scanqr.ScanQrIntent.DismissSetupLeave
import com.passbolt.mobile.android.feature.setup.scanqr.ScanQrIntent.GoBack
import com.passbolt.mobile.android.feature.setup.scanqr.ScanQrIntent.ImportProfileManually
import com.passbolt.mobile.android.feature.setup.scanqr.ScanQrIntent.Initialize
import com.passbolt.mobile.android.feature.setup.scanqr.ScanQrIntent.OpenHelpMenu
import com.passbolt.mobile.android.feature.setup.scanqr.ScanQrIntent.SelectedAccountKit
import com.passbolt.mobile.android.feature.setup.scanqr.ScanQrIntent.StartCameraError
import com.passbolt.mobile.android.feature.setup.scanqr.ScanQrSideEffect.NavigateBack
import com.passbolt.mobile.android.feature.setup.scanqr.ScanQrSideEffect.NavigateToImportProfile
import com.passbolt.mobile.android.feature.setup.scanqr.ScanQrSideEffect.NavigateToLogs
import com.passbolt.mobile.android.feature.setup.scanqr.ScanQrSideEffect.NavigateToSummary
import com.passbolt.mobile.android.feature.setup.scanqr.ScanQrSideEffect.ShowToast
import com.passbolt.mobile.android.feature.setup.scanqr.ScanQrState.TooltipMessage.CAMERA_ERROR
import com.passbolt.mobile.android.feature.setup.scanqr.ScanQrState.TooltipMessage.CENTER_CAMERA_ON_BARCODE
import com.passbolt.mobile.android.feature.setup.scanqr.ScanQrState.TooltipMessage.KEEP_GOING
import com.passbolt.mobile.android.feature.setup.scanqr.ScanQrState.TooltipMessage.MULTIPLE_BARCODES
import com.passbolt.mobile.android.feature.setup.scanqr.ScanQrState.TooltipMessage.NOT_A_PASSBOLT_QR
import com.passbolt.mobile.android.feature.setup.scanqr.qrparser.ParseResult
import com.passbolt.mobile.android.feature.setup.scanqr.qrparser.ParseResult.FinishedWithSuccess
import com.passbolt.mobile.android.feature.setup.scanqr.qrparser.ParseResult.PassboltQr.FirstPage
import com.passbolt.mobile.android.feature.setup.scanqr.qrparser.ParseResult.UserResolvableError
import com.passbolt.mobile.android.feature.setup.scanqr.qrparser.ParseResult.UserResolvableError.ErrorType
import com.passbolt.mobile.android.feature.setup.scanqr.qrparser.ParseResult.UserResolvableError.ErrorType.NO_BARCODES_IN_RANGE
import com.passbolt.mobile.android.feature.setup.scanqr.qrparser.ScanQrParser
import com.passbolt.mobile.android.feature.setup.scanqr.usecase.UpdateTransferUseCase
import com.passbolt.mobile.android.ui.ResultStatus.AlreadyLinked
import com.passbolt.mobile.android.ui.ResultStatus.Failure
import com.passbolt.mobile.android.ui.ResultStatus.HttpNotSupported
import com.passbolt.mobile.android.ui.ResultStatus.NoNetwork
import com.passbolt.mobile.android.ui.ResultStatus.Success
import com.passbolt.mobile.android.ui.UpdateTransferModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
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
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.koin.test.KoinTestRule
import org.koin.test.get
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.UUID
import kotlin.test.assertIs
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalCoroutinesApi::class)
@Suppress("LargeClass")
class ScanQrViewModelTest : KoinTest {
    @get:Rule
    val koinTestRule =
        KoinTestRule.create {
            printLogger(Level.ERROR)
            modules(
                listOf(
                    module {
                        single { mock<UpdateTransferUseCase>() }
                        single { mockScanQrParser }
                        single { mock<UuidProvider>() }
                        single { mock<SavePrivateKeyUseCase>() }
                        single { mock<UpdateAccountDataUseCase>() }
                        single { mock<CheckAccountExistsUseCase>() }
                        single { mock<HttpsVerifier>() }
                        single { mock<SaveCurrentApiUrlUseCase>() }
                        single { mock<AccountsInteractor>() }
                        single { mock<AccountKitParser>() }
                        single { mock<FetchFileAsStringUseCase>() }
                        factoryOf(::ScanQrViewModel)
                    },
                ),
            )
        }

    private val testDispatcher = StandardTestDispatcher()
    private val mockScanQrParser = mock<ScanQrParser>()
    private val barcodeScanFlow = MutableStateFlow<BarcodeScanResult>(BarcodeScanResult.NoBarcodeInRange)
    private val parseResultFlow = MutableSharedFlow<ParseResult>()

    private lateinit var viewModel: ScanQrViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        whenever(mockScanQrParser.parseResultFlow) doReturn parseResultFlow
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `initial state should have default values`() =
        runTest {
            viewModel = get()

            viewModel.viewState.test {
                val state = awaitItem()
                assertThat(state.showSetupLeaveConfirmationDialog).isFalse()
                assertThat(state.showHelpMenu).isFalse()
                assertThat(state.showServerNotReachableDialog).isFalse()
                assertThat(state.showProgress).isFalse()
                assertThat(state.totalPages).isEqualTo(0)
                assertThat(state.currentPage).isEqualTo(0)
                assertThat(state.tooltipMessage).isEqualTo(CENTER_CAMERA_ON_BARCODE)
            }
        }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `go back intent should show exit confirmation dialog`() =
        runTest {
            viewModel = get()
            viewModel.onIntent(GoBack)

            viewModel.viewState.test {
                assertThat(awaitItem().showSetupLeaveConfirmationDialog).isTrue()
            }
        }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `confirm setup leave should emit navigate back side effect`() =
        runTest {
            viewModel = get()

            viewModel.sideEffect.test {
                viewModel.onIntent(ConfirmSetupLeave)
                assertIs<NavigateBack>(awaitItem())
            }
        }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `dismiss setup leave should hide exit confirmation dialog`() =
        runTest {
            viewModel = get()
            viewModel.onIntent(GoBack)

            viewModel.viewState.drop(1).test {
                viewModel.onIntent(DismissSetupLeave)
                assertThat(awaitItem().showSetupLeaveConfirmationDialog).isFalse()
            }
        }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `open help menu should show help menu`() =
        runTest {
            viewModel = get()
            viewModel.onIntent(OpenHelpMenu)

            viewModel.viewState.test {
                assertThat(awaitItem().showHelpMenu).isTrue()
            }
        }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `dismiss help menu should hide help menu`() =
        runTest {
            viewModel = get()
            viewModel.onIntent(OpenHelpMenu)
            viewModel.onIntent(DismissHelpMenu)

            viewModel.viewState.test {
                assertThat(awaitItem().showHelpMenu).isFalse()
            }
        }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `import profile manually should emit navigate to import profile`() =
        runTest {
            viewModel = get()

            viewModel.sideEffect.test {
                viewModel.onIntent(ImportProfileManually)
                assertIs<NavigateToImportProfile>(awaitItem())
            }
        }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `access logs should emit navigate to logs`() =
        runTest {
            viewModel = get()

            viewModel.sideEffect.test {
                viewModel.onIntent(AccessLogs)
                assertIs<NavigateToLogs>(awaitItem())
            }
        }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `start camera error should show camera error tooltip`() =
        runTest {
            viewModel = get()
            val exception = Exception("Camera error")

            viewModel.viewState.drop(1).test {
                viewModel.onIntent(StartCameraError(exception))
                assertThat(awaitItem().tooltipMessage).isEqualTo(CAMERA_ERROR)
            }
        }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `dismiss server not reachable should hide server not reachable dialog`() =
        runTest {
            viewModel = get()
            viewModel.onIntent(DismissServerNotReachable)

            viewModel.viewState.test {
                assertThat(awaitItem().showServerNotReachableDialog).isFalse()
            }
        }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `initialize with barcode scan flow should start parsing`() =
        runTest {
            viewModel = get()
            viewModel.onIntent(Initialize(barcodeScanFlow = barcodeScanFlow, accountSetupDataModel = null))

            testScheduler.advanceUntilIdle()

            verify(mockScanQrParser).startParsing(barcodeScanFlow)
        }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `user resolvable error multiple barcodes should show multiple barcodes tooltip`() =
        runTest {
            viewModel = get()
            viewModel.onIntent(Initialize(barcodeScanFlow = barcodeScanFlow, accountSetupDataModel = null))
            testScheduler.advanceUntilIdle()

            viewModel.viewState.drop(1).test {
                parseResultFlow.emit(UserResolvableError(ErrorType.MULTIPLE_BARCODES))

                assertThat(awaitItem().tooltipMessage).isEqualTo(MULTIPLE_BARCODES)
            }
        }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `user resolvable error no barcodes in range should show center camera tooltip`() =
        runTest {
            viewModel = get()
            viewModel.onIntent(Initialize(barcodeScanFlow = barcodeScanFlow, accountSetupDataModel = null))
            testScheduler.advanceUntilIdle()

            viewModel.viewState.test {
                parseResultFlow.emit(UserResolvableError(NO_BARCODES_IN_RANGE))

                assertThat(awaitItem().tooltipMessage).isEqualTo(CENTER_CAMERA_ON_BARCODE)
            }
        }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `user resolvable error not a passbolt qr should show not a passbolt qr tooltip`() =
        runTest {
            viewModel = get()
            viewModel.onIntent(Initialize(barcodeScanFlow = barcodeScanFlow, accountSetupDataModel = null))
            testScheduler.advanceUntilIdle()

            viewModel.viewState.drop(1).test {
                parseResultFlow.emit(UserResolvableError(ErrorType.NOT_A_PASSBOLT_QR))

                assertThat(awaitItem().tooltipMessage).isEqualTo(NOT_A_PASSBOLT_QR)
            }
        }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `first page scan with new account should initialize progress and show keep going`() =
        runTest {
            val uuidProvider: UuidProvider = get()
            val checkAccountExistsUseCase: CheckAccountExistsUseCase = get()
            val httpsVerifier: HttpsVerifier = get()
            val updateTransferUseCase: UpdateTransferUseCase = get()

            whenever(uuidProvider.get()) doReturn NEW_USER_ID
            whenever(checkAccountExistsUseCase.execute(any())) doReturn CheckAccountExistsUseCase.Output(false)
            whenever(httpsVerifier.isHttps(any())) doReturn true
            whenever(updateTransferUseCase.execute(any())) doReturn
                UpdateTransferUseCase.Output.Success(
                    UpdateTransferModel(TEST_TRANSFER_ID.toString(), null, null, null, null),
                )

            viewModel = get()
            viewModel.onIntent(Initialize(barcodeScanFlow = barcodeScanFlow, accountSetupDataModel = null))
            testScheduler.advanceUntilIdle()

            viewModel.viewState.drop(1).test {
                parseResultFlow.emit(FirstPage(FIRST_PAGE_RESERVED_BYTES_DTO, FIRST_PAGE_CONTENT))

                val state = awaitItem()
                assertThat(state.totalPages).isEqualTo(TOTAL_PAGES)
                assertThat(state.tooltipMessage).isEqualTo(KEEP_GOING)

                cancelAndIgnoreRemainingEvents()
            }
        }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `first page scan with non https domain should navigate to summary with http not supported`() =
        runTest {
            val uuidProvider: UuidProvider = get()
            val checkAccountExistsUseCase: CheckAccountExistsUseCase = get()
            val httpsVerifier: HttpsVerifier = get()

            whenever(uuidProvider.get()) doReturn NEW_USER_ID
            whenever(checkAccountExistsUseCase.execute(any())) doReturn CheckAccountExistsUseCase.Output(false)
            whenever(httpsVerifier.isHttps(any())) doReturn false

            viewModel = get()
            viewModel.onIntent(Initialize(barcodeScanFlow = barcodeScanFlow, accountSetupDataModel = null))
            testScheduler.advanceUntilIdle()

            viewModel.sideEffect.test {
                parseResultFlow.emit(FirstPage(FIRST_PAGE_RESERVED_BYTES_DTO, FIRST_PAGE_CONTENT))

                val effect = awaitItem()
                assertIs<NavigateToSummary>(effect)
                assertIs<HttpNotSupported>(effect.status)
            }
        }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `first page scan with existing account should navigate to summary with already linked`() =
        runTest {
            val uuidProvider: UuidProvider = get()
            val checkAccountExistsUseCase: CheckAccountExistsUseCase = get()
            val httpsVerifier: HttpsVerifier = get()
            val updateTransferUseCase: UpdateTransferUseCase = get()

            whenever(uuidProvider.get()) doReturn NEW_USER_ID
            whenever(checkAccountExistsUseCase.execute(any())) doReturn
                CheckAccountExistsUseCase.Output(
                    true,
                    TEST_USER_ID.toString(),
                )
            whenever(httpsVerifier.isHttps(any())) doReturn true
            whenever(updateTransferUseCase.execute(any())) doReturn
                UpdateTransferUseCase.Output.Success(
                    UpdateTransferModel(TEST_TRANSFER_ID.toString(), null, null, null, null),
                )

            viewModel = get()
            viewModel.onIntent(Initialize(barcodeScanFlow = barcodeScanFlow, accountSetupDataModel = null))
            testScheduler.advanceUntilIdle()

            viewModel.sideEffect.test {
                parseResultFlow.emit(FirstPage(FIRST_PAGE_RESERVED_BYTES_DTO, FIRST_PAGE_CONTENT))

                val effect = awaitItem()
                assertIs<NavigateToSummary>(effect)
                assertIs<AlreadyLinked>(effect.status)
            }
        }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `subsequent page scan should show keep going tooltip`() =
        runTest {
            val uuidProvider: UuidProvider = get()
            val checkAccountExistsUseCase: CheckAccountExistsUseCase = get()
            val httpsVerifier: HttpsVerifier = get()
            val updateTransferUseCase: UpdateTransferUseCase = get()

            whenever(uuidProvider.get()) doReturn NEW_USER_ID
            whenever(checkAccountExistsUseCase.execute(any())) doReturn CheckAccountExistsUseCase.Output(false)
            whenever(httpsVerifier.isHttps(any())) doReturn true
            whenever(updateTransferUseCase.execute(any())) doReturn
                UpdateTransferUseCase.Output.Success(
                    UpdateTransferModel(TEST_TRANSFER_ID.toString(), null, null, null, null),
                )

            viewModel = get()
            viewModel.onIntent(Initialize(barcodeScanFlow = barcodeScanFlow, accountSetupDataModel = null))
            testScheduler.advanceUntilIdle()

            viewModel.viewState.drop(1).test {
                parseResultFlow.emit(FirstPage(FIRST_PAGE_RESERVED_BYTES_DTO, FIRST_PAGE_CONTENT))
                skipItems(1) // first page state

                parseResultFlow.emit(ParseResult.PassboltQr.SubsequentPage(SUBSEQUENT_PAGE_RESERVED_BYTES_DTO, SUBSEQUENT_PAGE_CONTENT))

                assertThat(awaitItem().tooltipMessage).isEqualTo(KEEP_GOING)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `finished with success should save private key and navigate to summary with success`() =
        runTest {
            val uuidProvider: UuidProvider = get()
            val checkAccountExistsUseCase: CheckAccountExistsUseCase = get()
            val httpsVerifier: HttpsVerifier = get()
            val updateTransferUseCase: UpdateTransferUseCase = get()
            val savePrivateKeyUseCase: SavePrivateKeyUseCase = get()

            whenever(uuidProvider.get()) doReturn NEW_USER_ID
            whenever(checkAccountExistsUseCase.execute(any())) doReturn CheckAccountExistsUseCase.Output(false)
            whenever(httpsVerifier.isHttps(any())) doReturn true
            whenever(updateTransferUseCase.execute(any())) doReturn
                UpdateTransferUseCase.Output.Success(
                    UpdateTransferModel(TEST_TRANSFER_ID.toString(), null, null, null, null),
                )
            whenever(savePrivateKeyUseCase.execute(any())) doReturn SavePrivateKeyUseCase.Output.Success

            viewModel = get()
            viewModel.onIntent(Initialize(barcodeScanFlow = barcodeScanFlow, accountSetupDataModel = null))
            testScheduler.advanceUntilIdle()

            viewModel.sideEffect.test {
                parseResultFlow.emit(FirstPage(FIRST_PAGE_RESERVED_BYTES_DTO, FIRST_PAGE_CONTENT))
                parseResultFlow.emit(FinishedWithSuccess(ARMORED_KEY))

                val effect = awaitItem()
                assertIs<NavigateToSummary>(effect)
                assertIs<Success>(effect.status)
            }

            verify(savePrivateKeyUseCase).execute(any())
        }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `finished with success but save private key fails should navigate to summary with failure`() =
        runTest {
            val uuidProvider: UuidProvider = get()
            val checkAccountExistsUseCase: CheckAccountExistsUseCase = get()
            val httpsVerifier: HttpsVerifier = get()
            val updateTransferUseCase: UpdateTransferUseCase = get()
            val savePrivateKeyUseCase: SavePrivateKeyUseCase = get()

            whenever(uuidProvider.get()) doReturn NEW_USER_ID
            whenever(checkAccountExistsUseCase.execute(any())) doReturn CheckAccountExistsUseCase.Output(false)
            whenever(httpsVerifier.isHttps(any())) doReturn true
            whenever(updateTransferUseCase.execute(any())) doReturn
                UpdateTransferUseCase.Output.Success(
                    UpdateTransferModel(TEST_TRANSFER_ID.toString(), null, null, null, null),
                )
            whenever(savePrivateKeyUseCase.execute(any())) doReturn SavePrivateKeyUseCase.Output.Failure

            viewModel = get()
            viewModel.onIntent(Initialize(barcodeScanFlow = barcodeScanFlow, accountSetupDataModel = null))
            testScheduler.advanceUntilIdle()

            viewModel.sideEffect.test {
                parseResultFlow.emit(FirstPage(FIRST_PAGE_RESERVED_BYTES_DTO, FIRST_PAGE_CONTENT))
                parseResultFlow.emit(FinishedWithSuccess(ARMORED_KEY))

                val effect = awaitItem()
                assertIs<NavigateToSummary>(effect)
                assertIs<Failure>(effect.status)

                cancelAndIgnoreRemainingEvents()
            }
        }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `parse failure should navigate to summary with failure`() =
        runTest {
            viewModel = get()
            viewModel.onIntent(Initialize(barcodeScanFlow = barcodeScanFlow, accountSetupDataModel = null))
            testScheduler.advanceUntilIdle()

            viewModel.sideEffect.test {
                parseResultFlow.emit(ParseResult.Failure(Exception("Parse error")))

                val effect = awaitItem()
                assertIs<NavigateToSummary>(effect)
                assertIs<Failure>(effect.status)
            }
        }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `update transfer failure with server not reachable should show server not reachable dialog`() =
        runTest {
            val uuidProvider: UuidProvider = get()
            val checkAccountExistsUseCase: CheckAccountExistsUseCase = get()
            val httpsVerifier: HttpsVerifier = get()
            val updateTransferUseCase: UpdateTransferUseCase = get()

            whenever(uuidProvider.get()) doReturn NEW_USER_ID
            whenever(checkAccountExistsUseCase.execute(any())) doReturn CheckAccountExistsUseCase.Output(false)
            whenever(httpsVerifier.isHttps(any())) doReturn true
            whenever(updateTransferUseCase.execute(any())) doReturn
                UpdateTransferUseCase.Output.Failure(
                    NetworkError(
                        exception = SocketTimeoutException("Server not reachable"),
                        headerMessage = "Server not reachable",
                    ),
                )

            viewModel = get()
            viewModel.onIntent(Initialize(barcodeScanFlow = barcodeScanFlow, accountSetupDataModel = null))
            testScheduler.advanceUntilIdle()

            viewModel.viewState.drop(2).test {
                parseResultFlow.emit(FirstPage(FIRST_PAGE_RESERVED_BYTES_DTO, FIRST_PAGE_CONTENT))

                val state = awaitItem()
                assertThat(state.showServerNotReachableDialog).isTrue()
            }
        }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `update transfer failure with no network should navigate to summary with no network`() =
        runTest {
            val uuidProvider: UuidProvider = get()
            val checkAccountExistsUseCase: CheckAccountExistsUseCase = get()
            val httpsVerifier: HttpsVerifier = get()
            val updateTransferUseCase: UpdateTransferUseCase = get()

            whenever(uuidProvider.get()) doReturn NEW_USER_ID
            whenever(checkAccountExistsUseCase.execute(any())) doReturn CheckAccountExistsUseCase.Output(false)
            whenever(httpsVerifier.isHttps(any())) doReturn true
            whenever(updateTransferUseCase.execute(any())) doReturn
                UpdateTransferUseCase.Output.Failure(
                    NetworkError(
                        exception = UnknownHostException("No network"),
                        headerMessage = "No network",
                    ),
                )

            viewModel = get()
            viewModel.onIntent(Initialize(barcodeScanFlow = barcodeScanFlow, accountSetupDataModel = null))
            testScheduler.advanceUntilIdle()

            viewModel.sideEffect.test {
                parseResultFlow.emit(FirstPage(FIRST_PAGE_RESERVED_BYTES_DTO, FIRST_PAGE_CONTENT))

                val effect = awaitItem()
                assertIs<NavigateToSummary>(effect)
                assertIs<NoNetwork>(effect.status)
            }
        }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `update transfer failure with other error should show toast`() =
        runTest {
            val uuidProvider: UuidProvider = get()
            val checkAccountExistsUseCase: CheckAccountExistsUseCase = get()
            val httpsVerifier: HttpsVerifier = get()
            val updateTransferUseCase: UpdateTransferUseCase = get()

            whenever(uuidProvider.get()) doReturn NEW_USER_ID
            whenever(checkAccountExistsUseCase.execute(any())) doReturn CheckAccountExistsUseCase.Output(false)
            whenever(httpsVerifier.isHttps(any())) doReturn true
            whenever(updateTransferUseCase.execute(any())) doReturn
                UpdateTransferUseCase.Output.Failure(
                    NetworkError(
                        exception = Exception("Unknown error"),
                        headerMessage = "Unknown error",
                    ),
                )

            viewModel = get()
            viewModel.onIntent(Initialize(barcodeScanFlow = barcodeScanFlow, accountSetupDataModel = null))
            testScheduler.advanceUntilIdle()

            viewModel.sideEffect.test {
                parseResultFlow.emit(FirstPage(FIRST_PAGE_RESERVED_BYTES_DTO, FIRST_PAGE_CONTENT))

                val effect = awaitItem()
                assertIs<ShowToast>(effect)
                assertThat(effect.type).isEqualTo(ToastType.UPDATE_TRANSFER_ERROR)
            }
        }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `initialize with predefined account data should inject account successfully`() =
        runTest {
            val accountsInteractor: AccountsInteractor = get()

            val onSuccessCaptor = argumentCaptor<(String) -> Unit>()
            val onFailureCaptor = argumentCaptor<(AccountsInteractor.InjectAccountFailureType) -> Unit>()

            viewModel = get()

            viewModel.sideEffect.test {
                viewModel.onIntent(Initialize(barcodeScanFlow = barcodeScanFlow, accountSetupDataModel = ACCOUNT_SETUP_DATA))
                testScheduler.advanceUntilIdle()

                verify(accountsInteractor).injectPredefinedAccountData(
                    eq(ACCOUNT_SETUP_DATA),
                    onSuccess = onSuccessCaptor.capture(),
                    onFailure = onFailureCaptor.capture(),
                )

                onSuccessCaptor.firstValue.invoke(NEW_USER_ID)

                val effect = awaitItem()
                assertIs<NavigateToSummary>(effect)
                assertIs<Success>(effect.status)
            }

            verify(mockScanQrParser, never()).startParsing(any())
        }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `initialize with predefined account data account already linked should navigate with already linked status`() =
        runTest {
            val accountsInteractor: AccountsInteractor = get()

            val onSuccessCaptor = argumentCaptor<(String) -> Unit>()
            val onFailureCaptor = argumentCaptor<(AccountsInteractor.InjectAccountFailureType) -> Unit>()

            viewModel = get()

            viewModel.sideEffect.test {
                viewModel.onIntent(Initialize(barcodeScanFlow = barcodeScanFlow, accountSetupDataModel = ACCOUNT_SETUP_DATA))
                testScheduler.advanceUntilIdle()

                verify(accountsInteractor).injectPredefinedAccountData(
                    eq(ACCOUNT_SETUP_DATA),
                    onSuccess = onSuccessCaptor.capture(),
                    onFailure = onFailureCaptor.capture(),
                )

                onFailureCaptor.firstValue.invoke(ACCOUNT_ALREADY_LINKED)

                val effect = awaitItem()
                assertIs<NavigateToSummary>(effect)
                assertIs<AlreadyLinked>(effect.status)
            }
        }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `initialize with predefined account data non https domain should navigate with http not supported status`() =
        runTest {
            val accountsInteractor: AccountsInteractor = get()

            val onSuccessCaptor = argumentCaptor<(String) -> Unit>()
            val onFailureCaptor = argumentCaptor<(AccountsInteractor.InjectAccountFailureType) -> Unit>()

            viewModel = get()

            viewModel.sideEffect.test {
                viewModel.onIntent(Initialize(barcodeScanFlow = barcodeScanFlow, accountSetupDataModel = ACCOUNT_SETUP_DATA))
                testScheduler.advanceUntilIdle()

                verify(accountsInteractor).injectPredefinedAccountData(
                    eq(ACCOUNT_SETUP_DATA),
                    onSuccess = onSuccessCaptor.capture(),
                    onFailure = onFailureCaptor.capture(),
                )

                onFailureCaptor.firstValue.invoke(ERROR_NON_HTTPS_DOMAIN)

                val effect = awaitItem()
                assertIs<NavigateToSummary>(effect)
                assertIs<HttpNotSupported>(effect.status)
            }
        }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `initialize with predefined account data private key error should navigate with failure status`() =
        runTest {
            val accountsInteractor: AccountsInteractor = get()

            val onSuccessCaptor = argumentCaptor<(String) -> Unit>()
            val onFailureCaptor = argumentCaptor<(AccountsInteractor.InjectAccountFailureType) -> Unit>()

            viewModel = get()

            viewModel.sideEffect.test {
                viewModel.onIntent(Initialize(barcodeScanFlow = barcodeScanFlow, accountSetupDataModel = ACCOUNT_SETUP_DATA))
                testScheduler.advanceUntilIdle()

                verify(accountsInteractor).injectPredefinedAccountData(
                    eq(ACCOUNT_SETUP_DATA),
                    onSuccess = onSuccessCaptor.capture(),
                    onFailure = onFailureCaptor.capture(),
                )

                onFailureCaptor.firstValue.invoke(ERROR_WHEN_SAVING_PRIVATE_KEY)

                val effect = awaitItem()
                assertIs<NavigateToSummary>(effect)
                assertIs<Failure>(effect.status)
            }
        }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `selected account kit with valid data should parse and inject account`() =
        runTest {
            val accountKitParser: AccountKitParser = get()
            val accountsInteractor: AccountsInteractor = get()

            val onSuccessParseCaptor = argumentCaptor<(AccountSetupDataModel) -> Unit>()
            val onFailureParseCaptor = argumentCaptor<(String) -> Unit>()
            val onSuccessInjectCaptor = argumentCaptor<(String) -> Unit>()
            val onFailureInjectCaptor = argumentCaptor<(AccountsInteractor.InjectAccountFailureType) -> Unit>()

            viewModel = get()

            viewModel.sideEffect.test {
                viewModel.onIntent(SelectedAccountKit(ACCOUNT_KIT_STRING))
                testScheduler.advanceUntilIdle()

                verify(accountKitParser).parseAndVerify(
                    eq(ACCOUNT_KIT_STRING),
                    onSuccess = onSuccessParseCaptor.capture(),
                    onFailure = onFailureParseCaptor.capture(),
                )

                onSuccessParseCaptor.firstValue.invoke(ACCOUNT_SETUP_DATA)

                verify(accountsInteractor).injectPredefinedAccountData(
                    eq(ACCOUNT_SETUP_DATA),
                    onSuccess = onSuccessInjectCaptor.capture(),
                    onFailure = onFailureInjectCaptor.capture(),
                )

                onSuccessInjectCaptor.firstValue.invoke(NEW_USER_ID)

                val effect = awaitItem()
                assertIs<NavigateToSummary>(effect)
                assertIs<Success>(effect.status)
            }
        }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `selected account kit with parse failure should navigate to summary with failure`() =
        runTest {
            val accountKitParser: AccountKitParser = get()

            val onSuccessParseCaptor = argumentCaptor<(AccountSetupDataModel) -> Unit>()
            val onFailureParseCaptor = argumentCaptor<(String) -> Unit>()

            viewModel = get()

            viewModel.sideEffect.test {
                viewModel.onIntent(SelectedAccountKit(ACCOUNT_KIT_STRING))
                testScheduler.advanceUntilIdle()

                verify(accountKitParser).parseAndVerify(
                    eq(ACCOUNT_KIT_STRING),
                    onSuccess = onSuccessParseCaptor.capture(),
                    onFailure = onFailureParseCaptor.capture(),
                )

                onFailureParseCaptor.firstValue.invoke("Parse error")

                val effect = awaitItem()
                assertIs<NavigateToSummary>(effect)
                assertIs<Failure>(effect.status)
            }
        }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `account kit page with valid url should fetch and inject account`() =
        runTest {
            val fetchFileAsStringUseCase: FetchFileAsStringUseCase = get()
            val accountKitParser: AccountKitParser = get()
            val accountsInteractor: AccountsInteractor = get()

            whenever(fetchFileAsStringUseCase.execute(any())) doReturn
                FetchFileAsStringUseCase.Output.Success(
                    ACCOUNT_KIT_STRING,
                )

            val onSuccessParseCaptor = argumentCaptor<(AccountSetupDataModel) -> Unit>()
            val onFailureParseCaptor = argumentCaptor<(String) -> Unit>()
            val onSuccessInjectCaptor = argumentCaptor<(String) -> Unit>()
            val onFailureInjectCaptor = argumentCaptor<(AccountsInteractor.InjectAccountFailureType) -> Unit>()

            viewModel = get()
            viewModel.onIntent(Initialize(barcodeScanFlow = barcodeScanFlow, accountSetupDataModel = null))
            testScheduler.advanceUntilIdle()

            viewModel.sideEffect.test {
                parseResultFlow.emit(ParseResult.PassboltQr.AccountKitPage(ACCOUNT_KIT_PAGE_RESERVED_BYTES_DTO, ACCOUNT_KIT_CONTENT))

                verify(fetchFileAsStringUseCase).execute(any())
                verify(accountKitParser).parseAndVerify(
                    eq(ACCOUNT_KIT_STRING),
                    onSuccess = onSuccessParseCaptor.capture(),
                    onFailure = onFailureParseCaptor.capture(),
                )

                onSuccessParseCaptor.firstValue.invoke(ACCOUNT_SETUP_DATA)

                verify(accountsInteractor).injectPredefinedAccountData(
                    eq(ACCOUNT_SETUP_DATA),
                    onSuccess = onSuccessInjectCaptor.capture(),
                    onFailure = onFailureInjectCaptor.capture(),
                )

                onSuccessInjectCaptor.firstValue.invoke(NEW_USER_ID)

                val effect = awaitItem()
                assertIs<NavigateToSummary>(effect)
                assertIs<Success>(effect.status)
            }
        }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `account kit page with fetch failure should navigate to summary with failure`() =
        runTest {
            val fetchFileAsStringUseCase: FetchFileAsStringUseCase = get()

            whenever(fetchFileAsStringUseCase.execute(any())) doReturn FetchFileAsStringUseCase.Output.Failure

            viewModel = get()
            viewModel.onIntent(Initialize(barcodeScanFlow = barcodeScanFlow, accountSetupDataModel = null))
            testScheduler.advanceUntilIdle()

            viewModel.sideEffect.test {
                parseResultFlow.emit(ParseResult.PassboltQr.AccountKitPage(ACCOUNT_KIT_PAGE_RESERVED_BYTES_DTO, ACCOUNT_KIT_CONTENT))

                val effect = awaitItem()
                assertIs<NavigateToSummary>(effect)
                assertIs<Failure>(effect.status)
            }
        }

    private companion object {
        private const val TOTAL_PAGES = 10
        private const val NEW_USER_ID = "new_user_id_123"
        private const val TEST_DOMAIN = "https://test.passbolt.com"
        private const val ARMORED_KEY = "-----BEGIN PGP PRIVATE KEY BLOCK-----"
        private const val ACCOUNT_KIT_STRING = "account_kit_json_data"

        private val TEST_TRANSFER_ID = UUID.randomUUID()
        private val TEST_USER_ID = UUID.randomUUID()

        private val FIRST_PAGE_RESERVED_BYTES_DTO = ReservedBytesDto(1, 0)
        private val FIRST_PAGE_CONTENT =
            QrFirstPageDto(
                TEST_TRANSFER_ID,
                TEST_USER_ID,
                TOTAL_PAGES,
                "test_auth_token",
                "test_hash",
                TEST_DOMAIN,
            )

        private val SUBSEQUENT_PAGE_RESERVED_BYTES_DTO = ReservedBytesDto(1, 1)
        private val SUBSEQUENT_PAGE_CONTENT = "subsequent_page_data".toByteArray()

        private val ACCOUNT_KIT_PAGE_RESERVED_BYTES_DTO = ReservedBytesDto(1, 0)
        private val ACCOUNT_KIT_CONTENT = AccountKitPageDto("https://test.passbolt.com/account-kit.json")

        private val ACCOUNT_SETUP_DATA =
            AccountSetupDataModel(
                serverUserId = "server_user_id",
                domain = TEST_DOMAIN,
                userName = "john.doe@passbolt.com",
                firstName = "John",
                lastName = "Doe",
                avatarUrl = "https://test.passbolt.com/avatar.jpg",
                keyFingerprint = "fingerprint",
                armoredKey = ARMORED_KEY,
            )
    }
}
