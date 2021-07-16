package com.passbolt.mobile.android.feature.authentication.auth.presenter

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.reset
import com.nhaarman.mockitokotlin2.stub
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
import com.nhaarman.mockitokotlin2.whenever
import com.passbolt.mobile.android.core.navigation.AuthenticationType
import com.passbolt.mobile.android.feature.authentication.auth.AuthContract
import com.passbolt.mobile.android.feature.setup.enterpassphrase.VerifyPassphraseUseCase
import com.passbolt.mobile.android.storage.usecase.passphrase.CheckIfPassphraseFileExistsUseCase
import com.passbolt.mobile.android.storage.usecase.privatekey.GetSelectedUserPrivateKeyUseCase
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.koin.core.logger.Level
import org.koin.core.qualifier.named
import org.koin.test.KoinTest
import org.koin.test.KoinTestRule
import org.koin.test.inject

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

class PassphrasePresenterTest : KoinTest {

    private val presenter: AuthContract.Presenter by inject(named(AuthenticationType.Passphrase.javaClass.simpleName))
    private val mockView = mock<AuthContract.View>()

    @get:Rule
    val koinTestRule = KoinTestRule.create {
        printLogger(Level.ERROR)
        modules(testAuthModule)
    }

    @After
    fun tearDown() {
        reset(mockVerifyPassphraseUseCase)
    }

    @Test
    fun `view should show wrong passphrase if passphrase is not correct`() {
        whenever(mockGetSelectedPrivateKeyUseCase.execute(Unit))
            .doReturn(GetSelectedUserPrivateKeyUseCase.Output("privateKey"))
        whenever(mockCheckIfPassphraseExistsUseCase.execute(Unit))
            .doReturn(CheckIfPassphraseFileExistsUseCase.Output(passphraseFileExists = false))
        mockVerifyPassphraseUseCase.stub {
            onBlocking { execute(any()) }.doReturn(VerifyPassphraseUseCase.Output(false))
        }

        presenter.attach(mockView)
        presenter.signInClick("pass".toByteArray())

        verify(mockView).showTitle()
        verify(mockView).hideKeyboard()
        verify(mockView).showWrongPassphrase()
        verifyNoMoreInteractions(mockView)
    }

    @Test
    fun `view should show auth success if passphrase is correct`() {
        whenever(mockGetSelectedPrivateKeyUseCase.execute(Unit))
            .doReturn(GetSelectedUserPrivateKeyUseCase.Output("privateKey"))
        whenever(mockCheckIfPassphraseExistsUseCase.execute(Unit))
            .doReturn(CheckIfPassphraseFileExistsUseCase.Output(passphraseFileExists = false))
        mockVerifyPassphraseUseCase.stub {
            onBlocking { execute(any()) }.doReturn(VerifyPassphraseUseCase.Output(true))
        }

        presenter.attach(mockView)
        presenter.argsRetrieved(ACCOUNT)
        presenter.signInClick("".toByteArray())

        verify(mockView).showTitle()
        verify(mockView).hideKeyboard()
        verify(mockView).authSuccess()
        verifyNoMoreInteractions(mockView)
    }

    @Test
    fun `view should disable and enable sign in button if on passphrase empty flag change`() {
        whenever(mockCheckIfPassphraseExistsUseCase.execute(Unit))
            .doReturn(CheckIfPassphraseFileExistsUseCase.Output(passphraseFileExists = false))

        presenter.attach(mockView)
        presenter.passphraseInputIsEmpty(true)
        verify(mockView).showTitle()
        verify(mockView).disableAuthButton()

        presenter.passphraseInputIsEmpty(false)
        verify(mockView).enableAuthButton()

        verifyNoMoreInteractions(mockView)
    }

    @Test
    fun `view should show account data on attach`() {
        whenever(mockCheckIfPassphraseExistsUseCase.execute(Unit))
            .doReturn(CheckIfPassphraseFileExistsUseCase.Output(passphraseFileExists = false))

        presenter.attach(mockView)
        presenter.argsRetrieved(ACCOUNT)
        presenter.viewCreated(true)

        verify(mockView).showTitle()
        verify(mockView).showName("$MOCK_ACCOUNT_DATA_FIRST_NAME $MOCK_ACCOUNT_DATA_LAST_NAME")
        verify(mockView).showEmail(MOCK_ACCOUNT_DATA_EMAIL)
        verify(mockView).showAvatar(MOCK_ACCOUNT_DATA_AVATAR_URL)
        verify(mockView).showDomain(MOCK_ACCOUNT_DATA_URL)
        verifyNoMoreInteractions(mockView)
    }

    @Test
    fun `view should show biometric prompt when fingerprint configured`() {
        whenever(mockCheckIfPassphraseExistsUseCase.execute(Unit))
            .doReturn(CheckIfPassphraseFileExistsUseCase.Output(passphraseFileExists = true))
        whenever(mockFingerprintInformationProvider.hasBiometricSetUp()).thenReturn(true)

        presenter.attach(mockView)
        presenter.argsRetrieved(ACCOUNT)

        verify(mockView).setBiometricAuthButtonVisible()
        verify(mockView).showBiometricPrompt()
    }
}
