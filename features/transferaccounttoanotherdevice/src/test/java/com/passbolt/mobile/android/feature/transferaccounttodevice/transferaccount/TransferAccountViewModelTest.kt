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

package com.passbolt.mobile.android.feature.transferaccounttodevice.transferaccount

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.passbolt.mobile.android.commontest.TestCoroutineLaunchContext
import com.passbolt.mobile.android.core.authenticationcore.session.GetSessionUseCase
import com.passbolt.mobile.android.core.idlingresource.TransferAccountIdlingResource
import com.passbolt.mobile.android.core.mvp.authentication.SessionRefreshTrackingFlow
import com.passbolt.mobile.android.core.mvp.coroutinecontext.CoroutineLaunchContext
import com.passbolt.mobile.android.core.networking.NetworkResult
import com.passbolt.mobile.android.core.passphrasememorycache.PassphraseMemoryCache
import com.passbolt.mobile.android.feature.authentication.auth.usecase.GetSessionExpiryUseCase
import com.passbolt.mobile.android.feature.authentication.auth.usecase.GetSessionExpiryUseCase.Output.JwtWillExpire
import com.passbolt.mobile.android.feature.transferaccounttoanotherdevice.transferaccount.TransferAccountIntent.CancelTransfer
import com.passbolt.mobile.android.feature.transferaccounttoanotherdevice.transferaccount.TransferAccountIntent.ConfirmCancelTransfer
import com.passbolt.mobile.android.feature.transferaccounttoanotherdevice.transferaccount.TransferAccountIntent.DismissCancelDialog
import com.passbolt.mobile.android.feature.transferaccounttoanotherdevice.transferaccount.TransferAccountIntent.GoBack
import com.passbolt.mobile.android.feature.transferaccounttoanotherdevice.transferaccount.TransferAccountScreenSideEffect.ErrorSnackbarType.FAILED_TO_CREATE_TRANSFER
import com.passbolt.mobile.android.feature.transferaccounttoanotherdevice.transferaccount.TransferAccountScreenSideEffect.ErrorSnackbarType.FAILED_TO_FETCH_TRANSFER_DETAILS
import com.passbolt.mobile.android.feature.transferaccounttoanotherdevice.transferaccount.TransferAccountScreenSideEffect.ErrorSnackbarType.FAILED_TO_GENERATE_QR_DATA
import com.passbolt.mobile.android.feature.transferaccounttoanotherdevice.transferaccount.TransferAccountScreenSideEffect.ErrorSnackbarType.FAILED_TO_INITIALIZE_PARAMETERS
import com.passbolt.mobile.android.feature.transferaccounttoanotherdevice.transferaccount.TransferAccountScreenSideEffect.NavigateToResult
import com.passbolt.mobile.android.feature.transferaccounttoanotherdevice.transferaccount.TransferAccountScreenSideEffect.ShowErrorSnackbar
import com.passbolt.mobile.android.feature.transferaccounttoanotherdevice.transferaccount.TransferAccountViewModel
import com.passbolt.mobile.android.feature.transferaccounttoanotherdevice.transferaccount.data.CreateTransferInputParametersGenerator
import com.passbolt.mobile.android.feature.transferaccounttoanotherdevice.transferaccount.data.TransferQrCodesDataGenerator
import com.passbolt.mobile.android.feature.transferaccounttoanotherdevice.usecase.CreateTransferUseCase
import com.passbolt.mobile.android.feature.transferaccounttoanotherdevice.usecase.ViewTransferUseCase
import com.passbolt.mobile.android.ui.CreateTransferModel
import com.passbolt.mobile.android.ui.Status
import com.passbolt.mobile.android.ui.TransferAccountStatusType
import com.passbolt.mobile.android.ui.TransferModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
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
import org.mockito.kotlin.whenever
import java.net.UnknownHostException
import java.time.ZonedDateTime
import kotlin.test.assertIs
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalCoroutinesApi::class)
class TransferAccountViewModelTest : KoinTest {
    @get:Rule
    val koinTestRule =
        KoinTestRule.create {
            printLogger(Level.ERROR)
            modules(
                listOf(
                    module {
                        single { mock<CreateTransferInputParametersGenerator>() }
                        single { mock<TransferQrCodesDataGenerator>() }
                        single { mock<CreateTransferUseCase>() }
                        single { mock<ViewTransferUseCase>() }
                        single { mock<GetSessionUseCase>() }
                        single { mock<GetSessionExpiryUseCase>() }
                        single { mock<PassphraseMemoryCache>() }
                        singleOf(::TransferAccountIdlingResource)
                        singleOf(::TestCoroutineLaunchContext) bind CoroutineLaunchContext::class
                        factoryOf(::TransferAccountViewModel)
                        singleOf(::SessionRefreshTrackingFlow)
                    },
                ),
            )
        }

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var viewModel: TransferAccountViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        val passphraseMemoryCache: PassphraseMemoryCache = get()
        whenever(passphraseMemoryCache.getSessionDurationSeconds()) doReturn 5 * 60

        val getSessionExpiryUseCase: GetSessionExpiryUseCase = get()
        whenever(getSessionExpiryUseCase.execute(Unit)) doReturn JwtWillExpire(ZonedDateTime.now().plusMinutes(5))

        val getSessionUseCase: GetSessionUseCase = get()
        whenever(getSessionUseCase.execute(any())) doReturn
            GetSessionUseCase.Output(
                accessToken = "test-access-token",
                refreshToken = "test-refresh-token",
                mfaToken = null,
            )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `initialization should show error when parameters generator fails`() =
        runTest {
            val parametersGenerator: CreateTransferInputParametersGenerator = get()
            parametersGenerator.stub {
                onBlocking { calculateCreateTransferParameters() } doReturn CreateTransferInputParametersGenerator.Output.Error
            }

            viewModel = get()

            viewModel.sideEffect.test {
                val effect = awaitItem()
                assertThat(effect).isInstanceOf(ShowErrorSnackbar::class.java)
                assertThat((effect as ShowErrorSnackbar).type).isEqualTo(FAILED_TO_INITIALIZE_PARAMETERS)
            }
        }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `initialization should show error when create transfer fails`() =
        runTest {
            val errorMessage = "Network error"
            val parametersGenerator: CreateTransferInputParametersGenerator = get()
            parametersGenerator.stub {
                onBlocking { calculateCreateTransferParameters() } doReturn
                    CreateTransferInputParametersGenerator.Output.Parameters(
                        keyJson = TEST_KEY_JSON,
                        totalPagesCount = 3,
                        pagesDataHash = TEST_HASH,
                    )
            }

            val createTransferUseCase: CreateTransferUseCase = get()
            createTransferUseCase.stub {
                onBlocking { execute(any()) } doReturn
                    CreateTransferUseCase.Output.Failure(
                        NetworkResult.Failure.NetworkError(
                            UnknownHostException(),
                            errorMessage,
                        ),
                    )
            }

            viewModel = get()

            viewModel.sideEffect.test {
                val networkingErrorEffect = awaitItem()
                assertThat(networkingErrorEffect).isInstanceOf(ShowErrorSnackbar::class.java)
                assertThat((networkingErrorEffect as ShowErrorSnackbar).type).isEqualTo(FAILED_TO_CREATE_TRANSFER)
                assertThat(networkingErrorEffect.errorMessage).isEqualTo(errorMessage)
            }
        }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `initialization should show error when QR data generation fails`() =
        runTest {
            val parametersGenerator: CreateTransferInputParametersGenerator = get()
            parametersGenerator.stub {
                onBlocking { calculateCreateTransferParameters() } doReturn
                    CreateTransferInputParametersGenerator.Output.Parameters(
                        keyJson = TEST_KEY_JSON,
                        totalPagesCount = 3,
                        pagesDataHash = TEST_HASH,
                    )
            }

            val createTransferUseCase: CreateTransferUseCase = get()
            createTransferUseCase.stub {
                onBlocking { execute(any()) } doReturn CreateTransferUseCase.Output.Success(TEST_CREATE_TRANSFER_MODEL)
            }

            val qrDataGenerator: TransferQrCodesDataGenerator = get()
            qrDataGenerator.stub {
                onBlocking { generateQrCodesDataPages(any()) } doReturn TransferQrCodesDataGenerator.Output.Error
            }

            viewModel = get()

            viewModel.sideEffect.test {
                val effect = awaitItem()
                assertThat(effect).isInstanceOf(ShowErrorSnackbar::class.java)
                assertThat((effect as ShowErrorSnackbar).type).isEqualTo(FAILED_TO_GENERATE_QR_DATA)
            }
        }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `successful initialization should update view state with QR code content`() =
        runTest {
            val parametersGenerator: CreateTransferInputParametersGenerator = get()
            parametersGenerator.stub {
                onBlocking { calculateCreateTransferParameters() } doReturn
                    CreateTransferInputParametersGenerator.Output.Parameters(
                        keyJson = TEST_KEY_JSON,
                        totalPagesCount = 3,
                        pagesDataHash = TEST_HASH,
                    )
            }

            val createTransferUseCase: CreateTransferUseCase = get()
            createTransferUseCase.stub {
                onBlocking { execute(any()) } doReturn CreateTransferUseCase.Output.Success(TEST_CREATE_TRANSFER_MODEL)
            }

            val qrPages = listOf("qr-page-0", "qr-page-1", "qr-page-2")
            val qrDataGenerator: TransferQrCodesDataGenerator = get()
            qrDataGenerator.stub {
                onBlocking { generateQrCodesDataPages(any()) } doReturn TransferQrCodesDataGenerator.Output.QrPages(qrPages)
            }

            val viewTransferUseCase: ViewTransferUseCase = get()
            viewTransferUseCase.stub {
                onBlocking { execute(any()) } doReturn
                    ViewTransferUseCase.Output.Success(
                        TEST_TRANSFER_MODEL.copy(currentPage = 0),
                    )
            }

            viewModel = get()

            viewModel.viewState.test {
                val state = awaitItem()
                assertThat(state.qrCodeContent).isEqualTo("qr-page-0")
                assertThat(state.totalPages).isEqualTo(3)
                assertThat(state.currentPage).isEqualTo(0)
                assertThat(state.showProgress).isFalse()
            }

            viewModel.cancelPollingForTests()
        }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `transfer polling should update current page when server returns new page`() =
        runTest {
            setupSuccessfulInitialization()

            val viewTransferUseCase: ViewTransferUseCase = get()
            viewTransferUseCase.stub {
                onBlocking { execute(any()) } doReturn
                    ViewTransferUseCase.Output.Success(
                        TEST_TRANSFER_MODEL.copy(currentPage = 1),
                    )
            }

            viewModel = get()

            advanceTimeBy(TransferAccountViewModel.GET_TRANSFER_LOOP_INTERVAL_DELAY_MILLIS + 100)

            viewModel.viewState.test {
                val state = awaitItem()
                assertThat(state.currentPage).isEqualTo(1)
                assertThat(state.qrCodeContent).isEqualTo("qr-page-1")
            }

            viewModel.cancelPollingForTests()
        }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `transfer polling should show error when fetch transfer details fails`() =
        runTest {
            setupSuccessfulInitialization()

            val errorMessage = "Failed to fetch transfer"
            val viewTransferUseCase: ViewTransferUseCase = get()
            viewTransferUseCase.stub {
                onBlocking { execute(any()) } doReturn
                    ViewTransferUseCase.Output.Failure(
                        NetworkResult.Failure.NetworkError(
                            UnknownHostException(),
                            errorMessage,
                        ),
                    )
            }

            viewModel = get()

            advanceTimeBy(TransferAccountViewModel.GET_TRANSFER_LOOP_INTERVAL_DELAY_MILLIS + 100)

            viewModel.sideEffect.test {
                val effect = awaitItem()
                assertThat(effect).isInstanceOf(ShowErrorSnackbar::class.java)
                assertThat((effect as ShowErrorSnackbar).type).isEqualTo(FAILED_TO_FETCH_TRANSFER_DETAILS)
                assertThat(effect.errorMessage).isEqualTo(errorMessage)
            }

            viewModel.cancelPollingForTests()
        }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `transfer polling should navigate to success when transfer completes`() =
        runTest {
            setupSuccessfulInitialization()

            val viewTransferUseCase: ViewTransferUseCase = get()
            viewTransferUseCase.stub {
                onBlocking { execute(any()) }
                    .doReturn(ViewTransferUseCase.Output.Success(TEST_TRANSFER_MODEL.copy(currentPage = 0)))
                    .doReturn(ViewTransferUseCase.Output.Success(TEST_TRANSFER_MODEL.copy(currentPage = 1)))
                    .doReturn(
                        ViewTransferUseCase.Output.Success(
                            TEST_TRANSFER_MODEL.copy(
                                currentPage = 2,
                                status = Status.COMPLETE,
                            ),
                        ),
                    )
            }

            viewModel = get()

            advanceTimeBy(10 * TransferAccountViewModel.GET_TRANSFER_LOOP_INTERVAL_DELAY_MILLIS + 100)

            viewModel.sideEffect.test {
                val effect = awaitItem()
                assertThat(effect).isInstanceOf(NavigateToResult::class.java)
                assertThat((effect as NavigateToResult).statusType).isEqualTo(TransferAccountStatusType.SUCCESS)
            }
        }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `transfer polling should navigate to failure when transfer errors`() =
        runTest {
            setupSuccessfulInitialization()

            val viewTransferUseCase: ViewTransferUseCase = get()
            viewTransferUseCase.stub {
                onBlocking { execute(any()) } doReturn
                    ViewTransferUseCase.Output.Success(
                        TEST_TRANSFER_MODEL.copy(status = Status.ERROR),
                    )
            }

            viewModel = get()

            advanceTimeBy(TransferAccountViewModel.GET_TRANSFER_LOOP_INTERVAL_DELAY_MILLIS + 100)

            viewModel.sideEffect.test {
                val effect = awaitItem()
                assertThat(effect).isInstanceOf(NavigateToResult::class.java)
                assertThat((effect as NavigateToResult).statusType).isEqualTo(TransferAccountStatusType.FAILURE)
            }
        }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `GoBack intent should show cancel dialog`() =
        runTest {
            setupSuccessfulInitialization()

            viewModel = get()

            viewModel.viewState.test {
                val initialState = awaitItem()
                assertThat(initialState.showCancelDialog).isFalse()

                viewModel.onIntent(GoBack)

                val updatedState = awaitItem()
                assertThat(updatedState.showCancelDialog).isTrue()
            }

            viewModel.cancelPollingForTests()
        }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `CancelTransfer intent should show cancel dialog`() =
        runTest {
            setupSuccessfulInitialization()

            viewModel = get()

            viewModel.viewState.test {
                val initialState = awaitItem()
                assertThat(initialState.showCancelDialog).isFalse()

                viewModel.onIntent(CancelTransfer)

                val updatedState = awaitItem()
                assertThat(updatedState.showCancelDialog).isTrue()
            }

            viewModel.cancelPollingForTests()
        }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `DismissCancelDialog intent should hide cancel dialog`() =
        runTest {
            setupSuccessfulInitialization()

            viewModel = get()

            viewModel.viewState.test {
                awaitItem()

                viewModel.onIntent(CancelTransfer)
                val showDialogState = awaitItem()
                assertThat(showDialogState.showCancelDialog).isTrue()

                viewModel.onIntent(DismissCancelDialog)
                val hideDialogState = awaitItem()
                assertThat(hideDialogState.showCancelDialog).isFalse()
            }

            viewModel.cancelPollingForTests()
        }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `ConfirmCancelTransfer intent should navigate to canceled result`() =
        runTest {
            setupSuccessfulInitialization()

            viewModel = get()

            viewModel.sideEffect.test {
                viewModel.onIntent(ConfirmCancelTransfer)

                val effect = awaitItem()
                assertIs<NavigateToResult>(effect)
                assertThat(effect.statusType).isEqualTo(TransferAccountStatusType.CANCELED)
            }
        }

    private fun setupSuccessfulInitialization() {
        val parametersGenerator: CreateTransferInputParametersGenerator = get()
        parametersGenerator.stub {
            onBlocking { calculateCreateTransferParameters() } doReturn
                CreateTransferInputParametersGenerator.Output.Parameters(
                    keyJson = TEST_KEY_JSON,
                    totalPagesCount = 3,
                    pagesDataHash = TEST_HASH,
                )
        }

        val createTransferUseCase: CreateTransferUseCase = get()
        createTransferUseCase.stub {
            onBlocking { execute(any()) } doReturn CreateTransferUseCase.Output.Success(TEST_CREATE_TRANSFER_MODEL)
        }

        val qrPages = listOf("qr-page-0", "qr-page-1", "qr-page-2")
        val qrDataGenerator: TransferQrCodesDataGenerator = get()
        qrDataGenerator.stub {
            onBlocking { generateQrCodesDataPages(any()) } doReturn TransferQrCodesDataGenerator.Output.QrPages(qrPages)
        }

        val viewTransferUseCase: ViewTransferUseCase = get()
        viewTransferUseCase.stub {
            onBlocking { execute(any()) } doReturn ViewTransferUseCase.Output.Success(TEST_TRANSFER_MODEL)
        }
    }

    private companion object {
        private const val TEST_KEY_JSON = """{"some":"json"}"""
        private const val TEST_HASH = "test-hash-value"
        private const val TEST_TRANSFER_ID = "transfer-123"
        private const val TEST_AUTH_TOKEN = "auth-token-123"

        private val TEST_CREATE_TRANSFER_MODEL =
            CreateTransferModel(
                id = TEST_TRANSFER_ID,
                status = Status.START,
                currentPage = 0,
                totalPages = 3,
                hash = TEST_HASH,
                authenticationToken = TEST_AUTH_TOKEN,
            )

        private val TEST_TRANSFER_MODEL =
            TransferModel(
                id = TEST_TRANSFER_ID,
                status = Status.IN_PROGRESS,
                currentPage = 0,
                totalPages = 3,
                hash = TEST_HASH,
            )
    }
}
