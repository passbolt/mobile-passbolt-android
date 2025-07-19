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

package com.passbolt.mobile.android.feature.settings.accounts.keyinspector.keyinspectormoremenu

import com.passbolt.mobile.android.core.accounts.usecase.privatekey.GetSelectedUserPrivateKeyUseCase
import com.passbolt.mobile.android.feature.settings.screen.accounts.keyinspector.keyinspectormoremenu.KeyInspectorMoreMenuContract
import com.passbolt.mobile.android.gopenpgp.exception.OpenPgpError
import com.passbolt.mobile.android.gopenpgp.exception.OpenPgpResult
import org.junit.Rule
import org.junit.Test
import org.koin.core.logger.Level
import org.koin.test.KoinTest
import org.koin.test.KoinTestRule
import org.koin.test.inject
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class KeyInspectorMoreMenuPresenterTest : KoinTest {
    private val presenter: KeyInspectorMoreMenuContract.Presenter by inject()
    private val view = mock<KeyInspectorMoreMenuContract.View>()

    @get:Rule
    val koinTestRule =
        KoinTestRule.create {
            printLogger(Level.ERROR)
            modules(testKeyInspectorMoreMenuModule)
        }

    @Test
    fun `view should show share sheet for exporting private key`() {
        val mockPrivateKey = "PrivateKey"
        whenever(mockGetSelectedUserPrivateKeyUseCase.execute(Unit)).thenReturn(
            GetSelectedUserPrivateKeyUseCase.Output(mockPrivateKey),
        )

        presenter.attach(view)
        presenter.exportPrivateKeyClick()
        presenter.authenticationSucceeded()

        verify(view).showShareSheet(mockPrivateKey)
    }

    @Test
    fun `view should show share sheet for exporting public key`() {
        val mockPrivateKey = "PrivateKey"
        whenever(mockGetSelectedUserPrivateKeyUseCase.execute(Unit)).thenReturn(
            GetSelectedUserPrivateKeyUseCase.Output(mockPrivateKey),
        )
        val mockPublicKey = "PublicKey"
        mockOpenPgp.stub {
            onBlocking { mockOpenPgp.generatePublicKey(mockPrivateKey) }.thenReturn(
                OpenPgpResult.Result(mockPublicKey),
            )
        }

        presenter.attach(view)
        presenter.exportPublicKeyClick()
        presenter.authenticationSucceeded()

        verify(view).showShareSheet(mockPublicKey)
    }

    @Test
    fun `view should show error when public key cannot be generate`() {
        val mockPrivateKey = "PrivateKey"
        whenever(mockGetSelectedUserPrivateKeyUseCase.execute(Unit)).thenReturn(
            GetSelectedUserPrivateKeyUseCase.Output(mockPrivateKey),
        )
        val errorMessage = "errorMessage"
        mockOpenPgp.stub {
            onBlocking { mockOpenPgp.generatePublicKey(mockPrivateKey) }.thenReturn(
                OpenPgpResult.Error(OpenPgpError(errorMessage)),
            )
        }

        presenter.attach(view)
        presenter.exportPublicKeyClick()
        presenter.authenticationSucceeded()

        verify(view).showFailedToGeneratePublicKey(errorMessage)
    }
}
