package com.passbolt.mobile.android.feature.settings

import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockitokotlin2.anyOrNull
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.reset
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
import com.nhaarman.mockitokotlin2.whenever
import com.passbolt.mobile.android.feature.settings.screen.SettingsContract
import com.passbolt.mobile.android.storage.cache.passphrase.PotentialPassphrase
import com.passbolt.mobile.android.storage.usecase.input.UserIdInput
import com.passbolt.mobile.android.storage.usecase.passphrase.CheckIfPassphraseFileExistsUseCase
import com.passbolt.mobile.android.storage.usecase.passphrase.SavePassphraseUseCase
import com.passbolt.mobile.android.storage.usecase.selectedaccount.GetSelectedAccountUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.koin.core.logger.Level
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
@ExperimentalCoroutinesApi
class SettingsPresenterTest : KoinTest {

    private val presenter: SettingsContract.Presenter by inject()
    private val view = mock<SettingsContract.View>()

    @get:Rule
    val koinTestRule = KoinTestRule.create {
        printLogger(Level.ERROR)
        modules(testModule)
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
        presenter.attach(view)
        reset(view)
    }

    @Test
    fun `disabling biometric should display confirmation dialog`() {
        presenter.fingerprintSettingChanged(false)
        verify(view).showDisableFingerprintConfirmationDialog()
        verifyNoMoreInteractions(view)
    }

    @Test
    fun `enabling biometric should navigate to authentication sign in if fingerprint is configured`() {
        whenever(fingerprintInformationProvider.hasBiometricSetUp()).doReturn(true)
        presenter.fingerprintSettingChanged(true)
        verify(view).navigateToAuthGetPassphrase()
        verifyNoMoreInteractions(view)
    }

    @Test
    fun `enabling biometric should show info when fingerprint is not configured`() {
        whenever(fingerprintInformationProvider.hasBiometricSetUp()).doReturn(false)
        presenter.fingerprintSettingChanged(true)
        verify(view).showConfigureFingerprintFirst()
        verify(view).toggleFingerprintOff(true)
        verifyNoMoreInteractions(view)
    }

    @Test
    fun `privacy policy clicked should navigate to privacy policy website`() {
        // TODO use url from endpoint
        presenter.privacyPolicyClick()
        verify(view).openUrl("https://www.passbolt.com")
        verifyNoMoreInteractions(view)
    }

    @Test
    fun `terms clicked should navigate to terms website`() {
        // TODO use url from endpoint
        presenter.termsClick()
        verify(view).openUrl("https://www.passbolt.com")
        verifyNoMoreInteractions(view)
    }

    @Test
    fun `sign out clicked should open confirmation dialog`() {
        presenter.signOutClick()
        verify(view).showLogoutDialog()
        verifyNoMoreInteractions(view)
    }

    @Test
    fun `sign out confirmation clicked should navigate to sign in`() {
        presenter.logoutConfirmed()
        verify(view).navigateToSignInWithLogout()
        verifyNoMoreInteractions(view)
    }

    @Test
    fun `manage accounts clicked should navigate to accounts list`() {
        presenter.manageAccountsClick()
        verify(view).navigateToAccountListWithLogout()
        verifyNoMoreInteractions(view)
    }

    @Test
    fun `autofill setup success should display autofill enabled dialog`() {
        presenter.autofillSetupSuccessfully()
        verify(view).showAutofillEnabledDialog()
        verifyNoMoreInteractions(view)
    }

    @Test
    fun `disabling fingerprint confirmed should toggle switch off`() {
        whenever(getSelectedAccountUseCase.execute(anyOrNull()))
            .thenReturn(GetSelectedAccountUseCase.Output("id"))
        presenter.disableFingerprintConfirmed()
        verify(view).toggleFingerprintOff(eq(true))
        verify(removePassphraseUseCase).execute(eq(UserIdInput("id")))
        verifyNoMoreInteractions(view)
    }

    @Test
    fun `disabling fingerprint canceled should toggle switch on`() {
        presenter.disableFingerprintCanceled()
        verify(view).toggleFingerprintOn(eq(true))
        verifyNoMoreInteractions(view)
    }

    @Test
    fun `re-enabling fingerprint should show proper ui`() {
        whenever(fingerprintInformationProvider.hasBiometricSetUp()).doReturn(true)
        whenever(checkIfPassphraseFileExistsUseCase.execute(anyOrNull())).thenReturn(
            CheckIfPassphraseFileExistsUseCase.Output(passphraseFileExists = false)
        )
        whenever(passphraseMemoryCache.get()).thenReturn(PotentialPassphrase.Passphrase(PASSPHRASE))

        presenter.fingerprintSettingChanged(isEnabled = true)

        verify(view).navigateToAuthGetPassphrase()
        presenter.getPassphraseSucceeded() // user entered passphrase
        verify(view).showBiometricPrompt(mockCipher)
        presenter.biometricAuthSucceeded(mockCipher) // user touched fingerprint
        argumentCaptor<SavePassphraseUseCase.Input>().apply {
            verify(savePassphraseUseCase).execute(capture())
            assertThat(firstValue.passphrase).isEqualTo(PASSPHRASE)
        }
        verifyNoMoreInteractions(view)
    }

    private companion object {
        private val PASSPHRASE = "passphrase".toByteArray()
    }
}
