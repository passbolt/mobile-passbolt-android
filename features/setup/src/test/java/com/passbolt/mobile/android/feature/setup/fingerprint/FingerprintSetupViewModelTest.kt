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

package com.passbolt.mobile.android.feature.setup.fingerprint

import android.security.keystore.KeyPermanentlyInvalidatedException
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.passbolt.mobile.android.common.FingerprintInformationProvider
import com.passbolt.mobile.android.core.accounts.usecase.biometrickey.SaveBiometricKeyIvUseCase
import com.passbolt.mobile.android.core.authenticationcore.passphrase.SavePassphraseUseCase
import com.passbolt.mobile.android.core.passphrasememorycache.PassphraseMemoryCache
import com.passbolt.mobile.android.core.passphrasememorycache.PotentialPassphrase
import com.passbolt.mobile.android.encryptedstorage.biometric.BiometricCipher
import com.passbolt.mobile.android.feature.authentication.auth.usecase.BiometryInteractor
import com.passbolt.mobile.android.feature.autofill.informationprovider.AutofillInformationProvider
import com.passbolt.mobile.android.feature.setup.fingerprint.FingerprintSetupIntent.AuthenticationSuccess
import com.passbolt.mobile.android.feature.setup.fingerprint.FingerprintSetupIntent.AutofillSetupSuccess
import com.passbolt.mobile.android.feature.setup.fingerprint.FingerprintSetupIntent.BiometricAuthenticationCancel
import com.passbolt.mobile.android.feature.setup.fingerprint.FingerprintSetupIntent.BiometricAuthenticationError
import com.passbolt.mobile.android.feature.setup.fingerprint.FingerprintSetupIntent.BiometricAuthenticationSuccess
import com.passbolt.mobile.android.feature.setup.fingerprint.FingerprintSetupIntent.ConfirmKeyPermanentlyInvalidated
import com.passbolt.mobile.android.feature.setup.fingerprint.FingerprintSetupIntent.DismissKeyPermanentlyInvalidated
import com.passbolt.mobile.android.feature.setup.fingerprint.FingerprintSetupIntent.GoToApp
import com.passbolt.mobile.android.feature.setup.fingerprint.FingerprintSetupIntent.KeyPermanentlyInvalidated
import com.passbolt.mobile.android.feature.setup.fingerprint.FingerprintSetupIntent.MaybeLater
import com.passbolt.mobile.android.feature.setup.fingerprint.FingerprintSetupIntent.ResumeView
import com.passbolt.mobile.android.feature.setup.fingerprint.FingerprintSetupIntent.SetupAutofillLater
import com.passbolt.mobile.android.feature.setup.fingerprint.FingerprintSetupIntent.UseFingerprint
import com.passbolt.mobile.android.feature.setup.fingerprint.FingerprintSetupSideEffect.NavigateToAppSystemSettings
import com.passbolt.mobile.android.feature.setup.fingerprint.FingerprintSetupSideEffect.NavigateToHome
import com.passbolt.mobile.android.feature.setup.fingerprint.FingerprintSetupSideEffect.ShowBiometricPrompt
import com.passbolt.mobile.android.feature.setup.fingerprint.FingerprintSetupSideEffect.ShowEncourageAutofillDialog
import com.passbolt.mobile.android.feature.setup.fingerprint.FingerprintSetupSideEffect.ShowErrorSnackbar
import com.passbolt.mobile.android.feature.setup.fingerprint.FingerprintSetupSideEffect.StartAuthActivity
import com.passbolt.mobile.android.ui.BiometricAuthError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import javax.crypto.Cipher
import kotlin.test.assertIs
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalCoroutinesApi::class)
class FingerprintSetupViewModelTest : KoinTest {
    @get:Rule
    val koinTestRule =
        KoinTestRule.create {
            printLogger(Level.ERROR)
            modules(
                listOf(
                    module {
                        single { mock<FingerprintInformationProvider>() }
                        single { mock<AutofillInformationProvider>() }
                        single { mock<PassphraseMemoryCache>() }
                        single { mock<SavePassphraseUseCase>() }
                        single { mock<BiometricCipher>() }
                        single { mock<SaveBiometricKeyIvUseCase>() }
                        single { mock<BiometryInteractor>() }
                        factoryOf(::FingerprintSetupViewModel)
                    },
                ),
            )
        }

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var viewModel: FingerprintSetupViewModel

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
                assertThat(state.hasBiometricSetup).isFalse()
                assertThat(state.showKeyChangesDetected).isFalse()
            }
        }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `resume view with biometric setup should update state`() =
        runTest {
            val fingerprintInformationProvider: FingerprintInformationProvider = get()
            whenever(fingerprintInformationProvider.hasBiometricSetUp()) doReturn true

            viewModel = get()
            viewModel.onIntent(ResumeView)

            viewModel.viewState.test {
                val state = awaitItem()
                assertThat(state.hasBiometricSetup).isTrue()
            }
        }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `resume view without biometric setup should update state`() =
        runTest {
            val fingerprintInformationProvider: FingerprintInformationProvider = get()
            whenever(fingerprintInformationProvider.hasBiometricSetUp()) doReturn false

            viewModel = get()
            viewModel.onIntent(ResumeView)

            viewModel.viewState.test {
                val state = awaitItem()
                assertThat(state.hasBiometricSetup).isFalse()
            }
        }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `use fingerprint when biometric is set up should show biometric prompt`() =
        runTest {
            val fingerprintInformationProvider: FingerprintInformationProvider = get()
            val biometricCipher: BiometricCipher = get()
            val mockCipher = mock<Cipher>()
            whenever(fingerprintInformationProvider.hasBiometricSetUp()) doReturn true
            whenever(biometricCipher.getBiometricEncryptCipher()) doReturn mockCipher

            viewModel = get()

            viewModel.sideEffect.test {
                viewModel.onIntent(UseFingerprint)

                val effect = awaitItem()
                assertIs<ShowBiometricPrompt>(effect)
                assertThat(effect.cipher).isEqualTo(mockCipher)
            }
        }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `use fingerprint when biometric is not set up should navigate to system settings`() =
        runTest {
            val fingerprintInformationProvider: FingerprintInformationProvider = get()
            whenever(fingerprintInformationProvider.hasBiometricSetUp()) doReturn false

            viewModel = get()

            viewModel.sideEffect.test {
                viewModel.onIntent(UseFingerprint)

                val effect = awaitItem()
                assertIs<NavigateToAppSystemSettings>(effect)
            }
        }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `maybe later with passphrase and autofill not set should show autofill dialog`() =
        runTest {
            val passphraseMemoryCache: PassphraseMemoryCache = get()
            val autofillInformationProvider: AutofillInformationProvider = get()
            whenever(passphraseMemoryCache.get()) doReturn PotentialPassphrase.Passphrase(TEST_PASSPHRASE)
            whenever(autofillInformationProvider.isAutofillServiceSupported()) doReturn true
            whenever(autofillInformationProvider.isPassboltAutofillServiceSet()) doReturn false

            viewModel = get()

            viewModel.sideEffect.test {
                viewModel.onIntent(MaybeLater)

                val effect = awaitItem()
                assertIs<ShowEncourageAutofillDialog>(effect)
            }
        }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `maybe later with passphrase and autofill already set should navigate to home`() =
        runTest {
            val passphraseMemoryCache: PassphraseMemoryCache = get()
            val autofillInformationProvider: AutofillInformationProvider = get()
            whenever(passphraseMemoryCache.get()) doReturn PotentialPassphrase.Passphrase(TEST_PASSPHRASE)
            whenever(autofillInformationProvider.isAutofillServiceSupported()) doReturn true
            whenever(autofillInformationProvider.isPassboltAutofillServiceSet()) doReturn true

            viewModel = get()

            viewModel.sideEffect.test {
                viewModel.onIntent(MaybeLater)

                val effect = awaitItem()
                assertIs<NavigateToHome>(effect)
            }
        }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `maybe later with passphrase and autofill not supported should navigate to home`() =
        runTest {
            val passphraseMemoryCache: PassphraseMemoryCache = get()
            val autofillInformationProvider: AutofillInformationProvider = get()
            whenever(passphraseMemoryCache.get()) doReturn PotentialPassphrase.Passphrase(TEST_PASSPHRASE)
            whenever(autofillInformationProvider.isAutofillServiceSupported()) doReturn false

            viewModel = get()

            viewModel.sideEffect.test {
                viewModel.onIntent(MaybeLater)

                val effect = awaitItem()
                assertIs<NavigateToHome>(effect)
            }
        }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `maybe later without passphrase should start auth activity`() =
        runTest {
            val passphraseMemoryCache: PassphraseMemoryCache = get()
            whenever(passphraseMemoryCache.get()) doReturn PotentialPassphrase.PassphraseNotPresent()

            viewModel = get()

            viewModel.sideEffect.test {
                viewModel.onIntent(MaybeLater)

                val effect = awaitItem()
                assertIs<StartAuthActivity>(effect)
            }
        }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `key permanently invalidated should disable biometry and show key changes detected`() =
        runTest {
            val biometryInteractor: BiometryInteractor = get()
            viewModel = get()
            val exception = KeyPermanentlyInvalidatedException("Key invalidated")

            viewModel.viewState.test {
                val initialState = awaitItem()
                assertThat(initialState.showKeyChangesDetected).isFalse()

                viewModel.onIntent(KeyPermanentlyInvalidated(exception))

                val updatedState = awaitItem()
                assertThat(updatedState.showKeyChangesDetected).isTrue()
            }

            verify(biometryInteractor).disableBiometry()
        }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `dismiss key permanently invalidated should hide key changes detected dialog`() =
        runTest {
            viewModel = get()
            val exception = KeyPermanentlyInvalidatedException("Key invalidated")

            viewModel.onIntent(KeyPermanentlyInvalidated(exception))

            viewModel.viewState.test {
                val stateWithDialog = awaitItem()
                assertThat(stateWithDialog.showKeyChangesDetected).isTrue()

                viewModel.onIntent(DismissKeyPermanentlyInvalidated)

                val stateWithoutDialog = awaitItem()
                assertThat(stateWithoutDialog.showKeyChangesDetected).isFalse()
            }
        }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `confirm key permanently invalidated should start auth activity`() =
        runTest {
            viewModel = get()

            viewModel.sideEffect.test {
                viewModel.onIntent(ConfirmKeyPermanentlyInvalidated)

                val effect = awaitItem()
                assertIs<StartAuthActivity>(effect)
            }
        }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `setup autofill later should navigate to home`() =
        runTest {
            viewModel = get()

            viewModel.sideEffect.test {
                viewModel.onIntent(SetupAutofillLater)

                val effect = awaitItem()
                assertIs<NavigateToHome>(effect)
            }
        }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `go to app should navigate to home`() =
        runTest {
            viewModel = get()

            viewModel.sideEffect.test {
                viewModel.onIntent(GoToApp)

                val effect = awaitItem()
                assertIs<NavigateToHome>(effect)
            }
        }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `authentication success should show biometric prompt`() =
        runTest {
            val biometricCipher: BiometricCipher = get()
            val mockCipher = mock<Cipher>()
            whenever(biometricCipher.getBiometricEncryptCipher()) doReturn mockCipher

            viewModel = get()

            viewModel.sideEffect.test {
                viewModel.onIntent(AuthenticationSuccess)

                val effect = awaitItem()
                assertIs<ShowBiometricPrompt>(effect)
                assertThat(effect.cipher).isEqualTo(mockCipher)
            }
        }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `biometric authentication success with cipher should save passphrase and show autofill dialog`() =
        runTest {
            val passphraseMemoryCache: PassphraseMemoryCache = get()
            val autofillInformationProvider: AutofillInformationProvider = get()
            val savePassphraseUseCase: SavePassphraseUseCase = get()
            val saveBiometricKeyIvUseCase: SaveBiometricKeyIvUseCase = get()

            val mockAuthenticatedCipher = mock<Cipher>()
            whenever(mockAuthenticatedCipher.iv) doReturn TEST_AUTHENTICATED_IV

            whenever(passphraseMemoryCache.get()) doReturn PotentialPassphrase.Passphrase(TEST_PASSPHRASE)
            whenever(autofillInformationProvider.isAutofillServiceSupported()) doReturn true
            whenever(autofillInformationProvider.isPassboltAutofillServiceSet()) doReturn false

            viewModel = get()

            viewModel.sideEffect.test {
                viewModel.onIntent(BiometricAuthenticationSuccess(mockAuthenticatedCipher))

                val effect = awaitItem()
                assertIs<ShowEncourageAutofillDialog>(effect)
            }

            verify(savePassphraseUseCase).execute(any())
            verify(saveBiometricKeyIvUseCase).execute(any())
        }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `biometric authentication success with cipher and autofill set should navigate to home`() =
        runTest {
            val passphraseMemoryCache: PassphraseMemoryCache = get()
            val autofillInformationProvider: AutofillInformationProvider = get()
            val savePassphraseUseCase: SavePassphraseUseCase = get()
            val saveBiometricKeyIvUseCase: SaveBiometricKeyIvUseCase = get()

            val mockAuthenticatedCipher = mock<Cipher>()
            whenever(mockAuthenticatedCipher.iv) doReturn TEST_AUTHENTICATED_IV

            whenever(passphraseMemoryCache.get()) doReturn PotentialPassphrase.Passphrase(TEST_PASSPHRASE)
            whenever(autofillInformationProvider.isAutofillServiceSupported()) doReturn true
            whenever(autofillInformationProvider.isPassboltAutofillServiceSet()) doReturn true

            viewModel = get()

            viewModel.sideEffect.test {
                viewModel.onIntent(BiometricAuthenticationSuccess(mockAuthenticatedCipher))

                val effect = awaitItem()
                assertIs<NavigateToHome>(effect)
            }

            verify(savePassphraseUseCase).execute(any())
            verify(saveBiometricKeyIvUseCase).execute(any())
        }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `biometric authentication success without cipher should show autofill dialog`() =
        runTest {
            val passphraseMemoryCache: PassphraseMemoryCache = get()
            val autofillInformationProvider: AutofillInformationProvider = get()
            whenever(passphraseMemoryCache.get()) doReturn PotentialPassphrase.Passphrase(TEST_PASSPHRASE)
            whenever(autofillInformationProvider.isAutofillServiceSupported()) doReturn true
            whenever(autofillInformationProvider.isPassboltAutofillServiceSet()) doReturn false

            viewModel = get()

            viewModel.sideEffect.test {
                viewModel.onIntent(BiometricAuthenticationSuccess(null))

                val effect = awaitItem()
                assertIs<ShowEncourageAutofillDialog>(effect)
            }
        }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `biometric authentication success without passphrase should start auth activity`() =
        runTest {
            val passphraseMemoryCache: PassphraseMemoryCache = get()
            val mockAuthenticatedCipher = mock<Cipher>()
            whenever(mockAuthenticatedCipher.iv) doReturn TEST_AUTHENTICATED_IV
            whenever(passphraseMemoryCache.get()) doReturn PotentialPassphrase.PassphraseNotPresent()

            viewModel = get()

            viewModel.sideEffect.test {
                viewModel.onIntent(BiometricAuthenticationSuccess(mockAuthenticatedCipher))

                val effect = awaitItem()
                assertIs<StartAuthActivity>(effect)
            }
        }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `biometric authentication cancel should not emit any side effects`() =
        runTest {
            viewModel = get()

            viewModel.sideEffect.test {
                viewModel.onIntent(BiometricAuthenticationCancel)

                expectNoEvents()
            }
        }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `biometric authentication error lockout should show error snackbar`() =
        runTest {
            viewModel = get()

            viewModel.sideEffect.test {
                viewModel.onIntent(BiometricAuthenticationError(BiometricAuthError.ERROR_LOCKOUT))

                val effect = awaitItem()
                assertIs<ShowErrorSnackbar>(effect)
                assertThat(effect.errorType).isEqualTo(SnackbarErrorType.AUTHENTICATION_LOCKOUT)
            }
        }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `biometric authentication error lockout permanent should show error snackbar`() =
        runTest {
            viewModel = get()

            viewModel.sideEffect.test {
                viewModel.onIntent(BiometricAuthenticationError(BiometricAuthError.ERROR_LOCKOUT_PERMANENT))

                val effect = awaitItem()
                assertIs<ShowErrorSnackbar>(effect)
                assertThat(effect.errorType).isEqualTo(SnackbarErrorType.AUTHENTICATION_LOCKOUT_PERMANENT)
            }
        }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `biometric authentication error generic should show error snackbar`() =
        runTest {
            viewModel = get()

            viewModel.sideEffect.test {
                viewModel.onIntent(BiometricAuthenticationError(BiometricAuthError.GENERIC))

                val effect = awaitItem()
                assertIs<ShowErrorSnackbar>(effect)
                assertThat(effect.errorType).isEqualTo(SnackbarErrorType.AUTHENTICATION_GENERIC)
            }
        }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `autofill setup success should navigate to home`() =
        runTest {
            viewModel = get()

            viewModel.sideEffect.test {
                viewModel.onIntent(AutofillSetupSuccess)

                val effect = awaitItem()
                assertIs<NavigateToHome>(effect)
            }
        }

    companion object {
        private val TEST_PASSPHRASE = "testPassphrase123".toByteArray()
        private val TEST_AUTHENTICATED_IV = ByteArray(16) { (it * 2).toByte() }
    }
}
