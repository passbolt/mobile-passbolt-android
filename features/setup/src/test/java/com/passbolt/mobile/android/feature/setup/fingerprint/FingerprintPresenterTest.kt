package com.passbolt.mobile.android.feature.setup.fingerprint

import com.passbolt.mobile.android.core.passphrasememorycache.PotentialPassphrase
import com.passbolt.mobile.android.feature.setup.di.testModule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.koin.core.logger.Level
import org.koin.test.KoinTest
import org.koin.test.KoinTestRule
import org.koin.test.inject
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
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
@ExperimentalCoroutinesApi
class FingerprintPresenterTest : KoinTest {

    private val presenter: FingerprintContract.Presenter by inject()
    private var view: FingerprintContract.View = mock()

    @get:Rule
    val koinTestRule = KoinTestRule.create {
        printLogger(Level.ERROR)
        modules(testModule, fingerprintModule)
    }

    @Before
    fun setUp() {
        presenter.attach(view)
    }

    @Test
    fun `click use fingerprint when no biometrics is set up should open settings`() {
        whenever(fingerprintInformationProvider.hasBiometricSetUp()).thenReturn(false)
        presenter.useFingerprintClick()
        verify(view).navigateToSystemSettings()
        verifyNoMoreInteractions(view)
    }

    @Test
    fun `when no biometrics should be displayed configure fingerprint ui`() {
        whenever(fingerprintInformationProvider.hasBiometricSetUp()).thenReturn(false)
        presenter.resume()
        verify(view).showConfigureFingerprint()
        verifyNoMoreInteractions(view)
    }

    @Test
    fun `when biometrics is set up should be displayed use fingerprint ui`() {
        whenever(fingerprintInformationProvider.hasBiometricSetUp()).thenReturn(true)
        presenter.resume()
        verify(view).showUseFingerprint()
        verifyNoMoreInteractions(view)
    }

    @Test
    fun `when biometrics auth is a success and cache has passphrase encourage autofill should show`() {
        whenever(fingerprintInformationProvider.hasBiometricSetUp()).thenReturn(true)
        whenever(autofillInformationProvider.isAutofillServiceSupported()).thenReturn(true)
        whenever(autofillInformationProvider.isPassboltAutofillServiceSet()).thenReturn(false)
        whenever(passphraseMemoryCache.get()).thenReturn(
            PotentialPassphrase.Passphrase("passphrase".toByteArray())
        )

        presenter.authenticationSucceeded()

        verify(view).showEncourageAutofillDialog()
        verifyNoMoreInteractions(view)
    }

    @Test
    fun `when biometrics auth is a success and cache passphrase expired should authenticate`() {
        whenever(fingerprintInformationProvider.hasBiometricSetUp()).thenReturn(true)
        whenever(passphraseMemoryCache.get()).thenReturn(
            PotentialPassphrase.PassphraseNotPresent()
        )

        presenter.authenticationSucceeded()

        verify(view).startAuthActivity()
        verifyNoMoreInteractions(view)
    }

    @Test
    fun `when autofill service is not already set up should open autofill setup screen`() {
        whenever(fingerprintInformationProvider.hasBiometricSetUp()).thenReturn(true)
        whenever(autofillInformationProvider.isAutofillServiceSupported()).thenReturn(true)
        whenever(autofillInformationProvider.isPassboltAutofillServiceSet()).thenReturn(false)
        whenever(passphraseMemoryCache.get()).thenReturn(
            PotentialPassphrase.Passphrase("passphrase".toByteArray())
        )

        presenter.maybeLaterClick()

        verify(view).showEncourageAutofillDialog()
        verifyNoMoreInteractions(view)
    }

    @Test
    fun `when autofill service is already set up should navigate to home directly`() {
        whenever(fingerprintInformationProvider.hasBiometricSetUp()).thenReturn(true)
        whenever(autofillInformationProvider.isAutofillServiceSupported()).thenReturn(true)
        whenever(autofillInformationProvider.isPassboltAutofillServiceSet()).thenReturn(true)
        whenever(passphraseMemoryCache.get()).thenReturn(
            PotentialPassphrase.Passphrase("passphrase".toByteArray())
        )

        presenter.maybeLaterClick()

        verify(view).navigateToHome()
        verifyNoMoreInteractions(view)
    }

    @Test
    fun `when autofill service is already set up and chrome service is available view should show chrome autofill encouragement`() {
        whenever(fingerprintInformationProvider.hasBiometricSetUp()).thenReturn(true)
        whenever(autofillInformationProvider.isAutofillServiceSupported()).thenReturn(true)
        whenever(autofillInformationProvider.isPassboltAutofillServiceSet()).thenReturn(true)
        whenever(mockEncouragementsInteractor.shouldShowChromeNativeAutofillEncouragement()).thenReturn(true)
        whenever(passphraseMemoryCache.get()).thenReturn(
            PotentialPassphrase.Passphrase("passphrase".toByteArray())
        )

        presenter.autofillSetupSuccess()

        verify(view).showEncourageChromeNativeAutofillDialog()
    }
}

