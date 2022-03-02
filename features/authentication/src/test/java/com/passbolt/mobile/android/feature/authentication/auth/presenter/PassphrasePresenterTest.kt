package com.passbolt.mobile.android.feature.authentication.auth.presenter

import android.security.keystore.KeyPermanentlyInvalidatedException
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever
import com.passbolt.mobile.android.core.navigation.ActivityIntents
import com.passbolt.mobile.android.feature.authentication.auth.AuthContract
import com.passbolt.mobile.android.feature.setup.enterpassphrase.VerifyPassphraseUseCase
import com.passbolt.mobile.android.storage.usecase.passphrase.CheckIfPassphraseFileExistsUseCase
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.koin.core.logger.Level
import org.koin.core.parameter.parametersOf
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

    private val presenter: AuthContract.Presenter by inject {
        parametersOf(ActivityIntents.AuthConfig.RefreshPassphrase)
    }
    private val mockView = mock<AuthContract.View>()

    @get:Rule
    val koinTestRule = KoinTestRule.create {
        printLogger(Level.ERROR)
        modules(testAuthModule)
    }

    @Before
    fun setup() {
        whenever(mockRootDetector.isDeviceRooted()).doReturn(false)
    }

    @After
    fun tearDown() {
        reset(mockVerifyPassphraseUseCase)
    }

    @Test
    fun `view should show wrong passphrase if passphrase is not correct`() {
        whenever(mockCheckIfPassphraseExistsUseCase.execute(anyOrNull()))
            .doReturn(CheckIfPassphraseFileExistsUseCase.Output(passphraseFileExists = false))
        mockVerifyPassphraseUseCase.stub {
            onBlocking { execute(any()) }.doReturn(VerifyPassphraseUseCase.Output(false))
        }

        presenter.argsRetrieved(ActivityIntents.AuthConfig.RefreshPassphrase, ACCOUNT)
        presenter.attach(mockView)
        verify(mockView).showAuthenticationReason(AuthContract.View.RefreshAuthReason.PASSPHRASE)
        presenter.signInClick("pass".toByteArray())

        verify(mockView).showTitle()
        verify(mockView).hideKeyboard()
        verify(mockView).showWrongPassphrase()
        verifyNoMoreInteractions(mockView)
    }

    @Test
    fun `view should show auth success if passphrase is correct`() {
        whenever(mockCheckIfPassphraseExistsUseCase.execute(anyOrNull()))
            .doReturn(CheckIfPassphraseFileExistsUseCase.Output(passphraseFileExists = false))
        mockVerifyPassphraseUseCase.stub {
            onBlocking { execute(any()) }.doReturn(VerifyPassphraseUseCase.Output(true))
        }

        presenter.argsRetrieved(ActivityIntents.AuthConfig.RefreshPassphrase, ACCOUNT)
        presenter.attach(mockView)
        presenter.signInClick("".toByteArray())

        verify(mockView).showTitle()
        verify(mockView).hideKeyboard()
        verify(mockView).clearPassphraseInput()
        verify(mockView).authSuccess()
        verify(mockView).showAuthenticationReason(AuthContract.View.RefreshAuthReason.PASSPHRASE)
        verifyNoMoreInteractions(mockView)
    }

    @Test
    fun `view should disable and enable sign in button if on passphrase empty flag change`() {
        whenever(mockCheckIfPassphraseExistsUseCase.execute(anyOrNull()))
            .doReturn(CheckIfPassphraseFileExistsUseCase.Output(passphraseFileExists = false))

        presenter.argsRetrieved(ActivityIntents.AuthConfig.RefreshPassphrase, ACCOUNT)
        presenter.attach(mockView)
        presenter.passphraseInputIsEmpty(true)
        verify(mockView).showTitle()
        verify(mockView).disableAuthButton()
        verify(mockView).showAuthenticationReason(AuthContract.View.RefreshAuthReason.PASSPHRASE)

        presenter.passphraseInputIsEmpty(false)
        verify(mockView).enableAuthButton()

        verifyNoMoreInteractions(mockView)
    }

    @Test
    fun `view should show account data on attach`() {
        whenever(mockCheckIfPassphraseExistsUseCase.execute(anyOrNull()))
            .doReturn(CheckIfPassphraseFileExistsUseCase.Output(passphraseFileExists = false))

        presenter.argsRetrieved(ActivityIntents.AuthConfig.RefreshPassphrase, ACCOUNT)
        presenter.attach(mockView)
        presenter.viewCreated()

        verify(mockView).showTitle()
        verify(mockView).showLabel(MOCK_LABEL)
        verify(mockView).showEmail(MOCK_ACCOUNT_DATA_EMAIL)
        verify(mockView).showAvatar(MOCK_ACCOUNT_DATA_AVATAR_URL)
        verify(mockView).showDomain(MOCK_ACCOUNT_DATA_URL)
        verify(mockView).showAuthenticationReason(AuthContract.View.RefreshAuthReason.PASSPHRASE)
        verifyNoMoreInteractions(mockView)
    }

    @Test
    fun `view should show biometric prompt when fingerprint configured`() {
        whenever(mockCheckIfPassphraseExistsUseCase.execute(anyOrNull()))
            .doReturn(CheckIfPassphraseFileExistsUseCase.Output(passphraseFileExists = true))
        whenever(mockFingerprintInformationProvider.hasBiometricSetUp()).thenReturn(true)

        presenter.argsRetrieved(ActivityIntents.AuthConfig.RefreshPassphrase, ACCOUNT)
        presenter.attach(mockView)

        verify(mockView).setBiometricAuthButtonVisible()
        verify(mockView).showBiometricPrompt(AuthContract.View.RefreshAuthReason.PASSPHRASE, mockCipher)
    }

    @Test
    fun `view should show auth reason when authentication is refreshed`() {
        whenever(mockCheckIfPassphraseExistsUseCase.execute(anyOrNull()))
            .doReturn(CheckIfPassphraseFileExistsUseCase.Output(passphraseFileExists = true))
        whenever(mockFingerprintInformationProvider.hasBiometricSetUp()).thenReturn(true)

        presenter.argsRetrieved(ActivityIntents.AuthConfig.RefreshPassphrase, ACCOUNT)
        presenter.attach(mockView)

        verify(mockView).showAuthenticationReason(AuthContract.View.RefreshAuthReason.PASSPHRASE)
        verify(mockView).showBiometricPrompt(AuthContract.View.RefreshAuthReason.PASSPHRASE, mockCipher)
    }

    @Test
    fun `view should hide biometric auth when key is invalidated`() {
        whenever(mockCheckIfPassphraseExistsUseCase.execute(anyOrNull()))
            .doReturn(CheckIfPassphraseFileExistsUseCase.Output(passphraseFileExists = true))
        whenever(mockFingerprintInformationProvider.hasBiometricSetUp()).thenReturn(true)
        whenever(mockBiometricCipher.getBiometricDecryptCipher(any())).thenThrow(KeyPermanentlyInvalidatedException())

        presenter.argsRetrieved(ActivityIntents.AuthConfig.RefreshPassphrase, ACCOUNT)
        presenter.attach(mockView)

        verify(mockView).setBiometricAuthButtonGone()
        verify(mockView).showFingerprintChangedError()
    }

    @Test
    fun `view should show root warning when detected`() {
        whenever(mockCheckIfPassphraseExistsUseCase.execute(anyOrNull()))
            .doReturn(CheckIfPassphraseFileExistsUseCase.Output(passphraseFileExists = true))
        whenever(mockFingerprintInformationProvider.hasBiometricSetUp()).thenReturn(true)
        whenever(mockRootDetector.isDeviceRooted()).doReturn(true)

        presenter.argsRetrieved(ActivityIntents.AuthConfig.RefreshPassphrase, ACCOUNT)
        presenter.attach(mockView)

        verify(mockView).showDeviceRootedDialog()
    }
}
