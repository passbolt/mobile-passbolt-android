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

package com.passbolt.mobile.android.feature.settings.appsettings

import com.google.common.truth.Truth
import com.passbolt.mobile.android.common.usecase.UserIdInput
import com.passbolt.mobile.android.core.accounts.usecase.selectedaccount.GetSelectedAccountUseCase
import com.passbolt.mobile.android.core.authenticationcore.passphrase.CheckIfPassphraseFileExistsUseCase
import com.passbolt.mobile.android.core.authenticationcore.passphrase.SavePassphraseUseCase
import com.passbolt.mobile.android.core.passphrasememorycache.PotentialPassphrase
import com.passbolt.mobile.android.feature.settings.screen.appsettings.AppSettingsContract
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.koin.core.logger.Level
import org.koin.test.KoinTest
import org.koin.test.KoinTestRule
import org.koin.test.inject
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever


class AppSettingsPresenterTest : KoinTest {

    private val presenter: AppSettingsContract.Presenter by inject()
    private val view = mock<AppSettingsContract.View>()

    @get:Rule
    val koinTestRule = KoinTestRule.create {
        printLogger(Level.ERROR)
        modules(testAppSettingsModule)
    }

    @Before
    fun setup() {
        whenever(checkIfPassphraseFileExistsUseCase.execute(anyOrNull())).thenReturn(
            CheckIfPassphraseFileExistsUseCase.Output(
                true
            )
        )
        whenever(getSelectedAccountUseCase.execute(anyOrNull())).thenReturn(
            GetSelectedAccountUseCase.Output("userId")
        )
    }

    @Test
    fun `disabling biometric should display confirmation dialog`() {
        presenter.attach(view)
        presenter.fingerprintSettingChanged(false)

        verify(view).showDisableFingerprintConfirmationDialog()
    }

    @Test
    fun `enabling biometric should navigate to authentication sign in if fingerprint is configured`() {
        whenever(fingerprintInformationProvider.hasBiometricSetUp()).doReturn(true)

        presenter.attach(view)
        presenter.fingerprintSettingChanged(true)

        verify(view).navigateToAuthGetPassphrase()
    }

    @Test
    fun `enabling biometric should show info when fingerprint is not configured`() {
        whenever(fingerprintInformationProvider.hasBiometricSetUp()).doReturn(false)

        presenter.attach(view)
        presenter.fingerprintSettingChanged(true)

        verify(view).showConfigureFingerprintFirst()
        verify(view).toggleFingerprintOff(true)
    }

    @Test
    fun `disabling fingerprint confirmed should toggle switch off`() {
        whenever(getSelectedAccountUseCase.execute(anyOrNull()))
            .thenReturn(GetSelectedAccountUseCase.Output("id"))

        presenter.attach(view)
        presenter.disableFingerprintConfirmed()

        verify(view).toggleFingerprintOff(eq(true))
        verify(removePassphraseUseCase).execute(eq(UserIdInput("id")))
    }

    @Test
    fun `disabling fingerprint canceled should toggle switch on`() {
        presenter.attach(view)
        presenter.disableFingerprintCanceled()

        verify(view).toggleFingerprintOn(eq(true))
    }

    @Test
    fun `re-enabling fingerprint should show proper ui`() {
        whenever(fingerprintInformationProvider.hasBiometricSetUp()).doReturn(true)
        whenever(checkIfPassphraseFileExistsUseCase.execute(anyOrNull())).thenReturn(
            CheckIfPassphraseFileExistsUseCase.Output(passphraseFileExists = false)
        )
        whenever(passphraseMemoryCache.get()).thenReturn(PotentialPassphrase.Passphrase(PASSPHRASE))

        presenter.attach(view)
        presenter.fingerprintSettingChanged(isEnabled = true)

        verify(view).navigateToAuthGetPassphrase()
        presenter.getPassphraseSucceeded() // user entered passphrase
        verify(view).showBiometricPrompt(mockCipher)
        presenter.biometricAuthSucceeded(mockCipher) // user touched fingerprint
        argumentCaptor<SavePassphraseUseCase.Input>().apply {
            verify(savePassphraseUseCase).execute(capture())
            Truth.assertThat(firstValue.passphrase).isEqualTo(PASSPHRASE)
        }
    }

    private companion object {
        private val PASSPHRASE = "passphrase".toByteArray()
        private const val URL = "https://www.passbolt.com"
    }
}
