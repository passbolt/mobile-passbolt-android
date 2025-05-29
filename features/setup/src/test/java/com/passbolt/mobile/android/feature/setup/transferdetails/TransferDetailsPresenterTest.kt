package com.passbolt.mobile.android.feature.setup.transferdetails

import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.koin.core.logger.Level
import org.koin.test.KoinTest
import org.koin.test.KoinTestRule
import org.koin.test.inject
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

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
class TransferDetailsPresenterTest : KoinTest {
    private val presenter: TransferDetailsPresenter by inject()
    private var view: TransferDetailsContract.View = mock()

    @get:Rule
    val koinTestRule =
        KoinTestRule.create {
            printLogger(Level.ERROR)
            modules(transferDetailsModule)
        }

    @Before
    fun setUp() {
        presenter.attach(view)
    }

    @Test
    fun `click scan qr code button should open next screen`() {
        whenever(cameraInformationProvider.isCameraAvailable()).thenReturn(true)
        whenever(cameraInformationProvider.isCameraPermissionGranted()).thenReturn(true)
        presenter.scanQrCodesButtonClick()
        verify(view).navigateToScanQr()
    }

    @Test
    fun `click scan qr code button when camera is not available should display proper dialog `() {
        whenever(cameraInformationProvider.isCameraAvailable()).thenReturn(false)
        whenever(cameraInformationProvider.isCameraPermissionGranted()).thenReturn(true)
        presenter.scanQrCodesButtonClick()
        verify(view).showCameraRequiredDialog()
    }

    @Test
    fun `click scan qr code button when permissions is not granted should display proper dialog `() {
        whenever(cameraInformationProvider.isCameraAvailable()).thenReturn(true)
        whenever(cameraInformationProvider.isCameraPermissionGranted()).thenReturn(false)
        presenter.scanQrCodesButtonClick()
        verify(view).requestCameraPermission()
    }

    @Test
    fun `rejecting camera permissions should display information dialog`() {
        presenter.permissionRejectedClick()
        verify(view).showCameraPermissionRequiredDialog()
    }

    @Test
    fun `click settings button should open settings screen`() {
        presenter.settingsButtonClick()
        verify(view).navigateToAppSettings()
    }
}
