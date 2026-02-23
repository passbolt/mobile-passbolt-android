package com.passbolt.mobile.android.feature.setup.transferdetails

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
import com.passbolt.mobile.android.core.qrscan.CameraInformationProvider
import com.passbolt.mobile.android.feature.setup.transferdetails.TransferDetailsIntent.DismissCameraPermissionRequiredDialog
import com.passbolt.mobile.android.feature.setup.transferdetails.TransferDetailsIntent.DismissCameraRequiredDialog
import com.passbolt.mobile.android.feature.setup.transferdetails.TransferDetailsIntent.GoBack
import com.passbolt.mobile.android.feature.setup.transferdetails.TransferDetailsIntent.GoToSettings
import com.passbolt.mobile.android.feature.setup.transferdetails.TransferDetailsIntent.GrantCameraPermission
import com.passbolt.mobile.android.feature.setup.transferdetails.TransferDetailsIntent.RejectCameraPermission
import com.passbolt.mobile.android.feature.setup.transferdetails.TransferDetailsIntent.StartQrCodeScanning
import com.passbolt.mobile.android.feature.setup.transferdetails.TransferDetailsSideEffect.NavigateBack
import com.passbolt.mobile.android.feature.setup.transferdetails.TransferDetailsSideEffect.NavigateToAppSettings
import com.passbolt.mobile.android.feature.setup.transferdetails.TransferDetailsSideEffect.NavigateToScanQr
import com.passbolt.mobile.android.feature.setup.transferdetails.TransferDetailsSideEffect.RequestCameraPermission
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
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.koin.test.KoinTestRule
import org.koin.test.get
import org.mockito.Mockito.mock
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.whenever
import kotlin.test.assertIs
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalCoroutinesApi::class)
class TransferDetailsViewModelTest : KoinTest {
    @get:Rule
    val koinTestRule =
        KoinTestRule.create {
            printLogger(Level.ERROR)
            modules(
                listOf(
                    module {
                        single { mock<CameraInformationProvider>() }
                        factoryOf(::TransferDetailsViewModel)
                    },
                ),
            )
        }

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var viewModel: TransferDetailsViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
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
                assertThat(state.showCameraRequiredDialog).isFalse()
                assertThat(state.showCameraPermissionRequiredDialog).isFalse()
            }
        }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `go back intent should emit navigate back side effect`() =
        runTest {
            viewModel = get()

            viewModel.sideEffect.test {
                viewModel.onIntent(GoBack)
                assertIs<NavigateBack>(awaitItem())
            }
        }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `start qr code scanning with camera available and permission granted should navigate to scan qr`() =
        runTest {
            val cameraInformationProvider: CameraInformationProvider = get()
            whenever(cameraInformationProvider.isCameraAvailable()) doReturn true
            whenever(cameraInformationProvider.isCameraPermissionGranted()) doReturn true

            viewModel = get()

            viewModel.sideEffect.test {
                viewModel.onIntent(StartQrCodeScanning)
                assertIs<NavigateToScanQr>(awaitItem())
            }
        }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `start qr code scanning with camera not available should show camera required dialog`() =
        runTest {
            val cameraInformationProvider: CameraInformationProvider = get()
            whenever(cameraInformationProvider.isCameraAvailable()) doReturn false

            viewModel = get()
            viewModel.onIntent(StartQrCodeScanning)

            viewModel.viewState.test {
                assertThat(awaitItem().showCameraRequiredDialog).isTrue()
            }
        }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `start qr code scanning with camera available but permission not granted should request camera permission`() =
        runTest {
            val cameraInformationProvider: CameraInformationProvider = get()
            whenever(cameraInformationProvider.isCameraAvailable()) doReturn true
            whenever(cameraInformationProvider.isCameraPermissionGranted()) doReturn false

            viewModel = get()

            viewModel.sideEffect.test {
                viewModel.onIntent(StartQrCodeScanning)
                assertIs<RequestCameraPermission>(awaitItem())
            }
        }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `reject camera permission should show camera permission required dialog`() =
        runTest {
            viewModel = get()
            viewModel.onIntent(RejectCameraPermission)

            viewModel.viewState.test {
                assertThat(awaitItem().showCameraPermissionRequiredDialog).isTrue()
            }
        }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `dismiss camera required dialog should hide camera required dialog`() =
        runTest {
            val cameraInformationProvider: CameraInformationProvider = get()
            whenever(cameraInformationProvider.isCameraAvailable()) doReturn false

            viewModel = get()
            viewModel.onIntent(StartQrCodeScanning)

            viewModel.viewState.drop(1).test {
                viewModel.onIntent(DismissCameraRequiredDialog)
                assertThat(awaitItem().showCameraRequiredDialog).isFalse()
            }
        }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `dismiss camera permission required dialog should hide camera permission required dialog`() =
        runTest {
            viewModel = get()
            viewModel.onIntent(RejectCameraPermission)

            viewModel.viewState.drop(1).test {
                viewModel.onIntent(DismissCameraPermissionRequiredDialog)
                assertThat(awaitItem().showCameraPermissionRequiredDialog).isFalse()
            }
        }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `go to settings intent should emit navigate to app settings side effect`() =
        runTest {
            viewModel = get()

            viewModel.sideEffect.test {
                viewModel.onIntent(GoToSettings)
                assertIs<NavigateToAppSettings>(awaitItem())
            }
        }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `grant camera permission with camera available and permission granted should navigate to scan qr`() =
        runTest {
            val cameraInformationProvider: CameraInformationProvider = get()
            whenever(cameraInformationProvider.isCameraAvailable()) doReturn true
            whenever(cameraInformationProvider.isCameraPermissionGranted()) doReturn true

            viewModel = get()

            viewModel.sideEffect.test {
                viewModel.onIntent(GrantCameraPermission)
                assertIs<NavigateToScanQr>(awaitItem())
            }
        }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `grant camera permission with camera not available should show camera required dialog`() =
        runTest {
            val cameraInformationProvider: CameraInformationProvider = get()
            whenever(cameraInformationProvider.isCameraAvailable()) doReturn false

            viewModel = get()
            viewModel.onIntent(GrantCameraPermission)

            viewModel.viewState.test {
                assertThat(awaitItem().showCameraRequiredDialog).isTrue()
            }
        }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `grant camera permission with camera available but permission not granted should request camera permission`() =
        runTest {
            val cameraInformationProvider: CameraInformationProvider = get()
            whenever(cameraInformationProvider.isCameraAvailable()) doReturn true
            whenever(cameraInformationProvider.isCameraPermissionGranted()) doReturn false

            viewModel = get()

            viewModel.sideEffect.test {
                viewModel.onIntent(GrantCameraPermission)
                assertIs<RequestCameraPermission>(awaitItem())
            }
        }
}
