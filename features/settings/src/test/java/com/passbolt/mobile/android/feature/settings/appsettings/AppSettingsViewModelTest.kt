package com.passbolt.mobile.android.feature.settings.appsettings

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.passbolt.mobile.android.common.BiometricInformationProvider
import com.passbolt.mobile.android.common.autofill.DetectAutofillConflict
import com.passbolt.mobile.android.common.usecase.UserIdInput
import com.passbolt.mobile.android.core.accounts.usecase.biometrickey.SaveBiometricKeyIvUseCase
import com.passbolt.mobile.android.core.accounts.usecase.selectedaccount.GetSelectedAccountUseCase
import com.passbolt.mobile.android.core.authenticationcore.passphrase.CheckIfPassphraseFileExistsUseCase
import com.passbolt.mobile.android.core.authenticationcore.passphrase.RemovePassphraseUseCase
import com.passbolt.mobile.android.core.authenticationcore.passphrase.SavePassphraseUseCase
import com.passbolt.mobile.android.core.passphrasememorycache.PassphraseMemoryCache
import com.passbolt.mobile.android.core.passphrasememorycache.PotentialPassphrase.KeyStatus.VALID
import com.passbolt.mobile.android.core.passphrasememorycache.PotentialPassphrase.Passphrase
import com.passbolt.mobile.android.core.passphrasememorycache.PotentialPassphrase.PassphraseNotPresent
import com.passbolt.mobile.android.encryptedstorage.biometric.BiometricCipher
import com.passbolt.mobile.android.feature.authentication.auth.usecase.BiometryInteractor
import com.passbolt.mobile.android.feature.settings.screen.appsettings.AppSettingsIntent
import com.passbolt.mobile.android.feature.settings.screen.appsettings.AppSettingsIntent.ConfigureBiometric
import com.passbolt.mobile.android.feature.settings.screen.appsettings.AppSettingsIntent.ConfirmDisableBiometric
import com.passbolt.mobile.android.feature.settings.screen.appsettings.AppSettingsSideEffect
import com.passbolt.mobile.android.feature.settings.screen.appsettings.AppSettingsSideEffect.LaunchBiometricPrompt
import com.passbolt.mobile.android.feature.settings.screen.appsettings.AppSettingsSideEffect.NavigateToSystemSettings
import com.passbolt.mobile.android.feature.settings.screen.appsettings.AppSettingsViewModel
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
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.UUID
import javax.crypto.Cipher
import kotlin.time.ExperimentalTime

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

@OptIn(ExperimentalCoroutinesApi::class)
class AppSettingsViewModelTest : KoinTest {
    @get:Rule
    val koinTestRule =
        KoinTestRule.create {
            printLogger(Level.ERROR)
            modules(
                listOf(
                    module {
                        single { mock<CheckIfPassphraseFileExistsUseCase>() }
                        single { mock<GetSelectedAccountUseCase>() }
                        single { mock<BiometricInformationProvider>() }
                        single { mock<PassphraseMemoryCache>() }
                        single { mock<RemovePassphraseUseCase>() }
                        single { mock<BiometricCipher>() }
                        single { mock<BiometryInteractor>() }
                        single { mock<SavePassphraseUseCase>() }
                        single { mock<SaveBiometricKeyIvUseCase>() }
                        single { mock<DetectAutofillConflict>() }
                        factoryOf(::AppSettingsViewModel)
                    },
                ),
            )
        }

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var viewModel: AppSettingsViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        val getSelectedAccountUseCase: GetSelectedAccountUseCase = get()
        whenever(getSelectedAccountUseCase.execute(Unit)) doReturn
            GetSelectedAccountUseCase.Output(
                selectedAccount = SELECTED_ACCOUNT_ID,
            )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial biometric state should be correct for disabled biometric`() =
        runTest {
            val checkIfPassphraseFileExistsUseCase: CheckIfPassphraseFileExistsUseCase = get()
            whenever(checkIfPassphraseFileExistsUseCase.execute(any())) doReturn
                CheckIfPassphraseFileExistsUseCase.Output(
                    passphraseFileExists = false,
                )

            viewModel = get()

            assertThat(viewModel.viewState.value.isBiometricEnabled).isFalse()
        }

    @Test
    fun `initial biometric state should be correct for enabled biometric`() =
        runTest {
            val checkIfPassphraseFileExistsUseCase: CheckIfPassphraseFileExistsUseCase = get()
            whenever(checkIfPassphraseFileExistsUseCase.execute(any())) doReturn
                CheckIfPassphraseFileExistsUseCase.Output(
                    passphraseFileExists = true,
                )

            viewModel = get()

            assertThat(viewModel.viewState.value.isBiometricEnabled).isTrue()
        }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `enabling biometric should show info and navigate to settings when biometric is not configured`() =
        runTest {
            val checkIfPassphraseFileExistsUseCase: CheckIfPassphraseFileExistsUseCase = get()
            whenever(checkIfPassphraseFileExistsUseCase.execute(any())) doReturn
                CheckIfPassphraseFileExistsUseCase.Output(
                    passphraseFileExists = false,
                )
            val biometricInformationProvider: BiometricInformationProvider = get()
            whenever(biometricInformationProvider.hasBiometricSetUp()) doReturn false

            viewModel = get()
            viewModel.onIntent(AppSettingsIntent.ToggleBiometric)

            viewModel.viewState.test {
                assertThat(awaitItem().isConfigureBiometricDialogVisible).isTrue()

                viewModel.onIntent(ConfigureBiometric)
                assertThat(awaitItem().isConfigureBiometricDialogVisible).isFalse()

                viewModel.sideEffect.test {
                    assertThat(awaitItem()).isInstanceOf(NavigateToSystemSettings::class.java)
                }
            }
        }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `disable biometric should confirm and disable biometry`() =
        runTest {
            val checkIfPassphraseFileExistsUseCase: CheckIfPassphraseFileExistsUseCase = get()
            whenever(checkIfPassphraseFileExistsUseCase.execute(any())) doReturn
                CheckIfPassphraseFileExistsUseCase.Output(
                    passphraseFileExists = true,
                )

            viewModel = get()
            viewModel.onIntent(AppSettingsIntent.ToggleBiometric)

            viewModel.viewState.test {
                assertThat(awaitItem().isDisableBiometricDialogVisible).isTrue()

                viewModel.onIntent(ConfirmDisableBiometric)

                val userIdInput = UserIdInput(SELECTED_ACCOUNT_ID)
                verify(get<RemovePassphraseUseCase>()).execute(userIdInput)

                val stateAfterDisabling = awaitItem()
                assertThat(stateAfterDisabling.isDisableBiometricDialogVisible).isFalse()
                assertThat(stateAfterDisabling.isBiometricEnabled).isFalse()
            }
        }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `enable biometric should ask for auth and enable biometry`() =
        runTest {
            val checkIfPassphraseFileExistsUseCase: CheckIfPassphraseFileExistsUseCase = get()
            whenever(checkIfPassphraseFileExistsUseCase.execute(any())) doReturn
                CheckIfPassphraseFileExistsUseCase.Output(
                    passphraseFileExists = false,
                )
            val biometricInformationProvider: BiometricInformationProvider = get()
            whenever(biometricInformationProvider.hasBiometricSetUp()) doReturn true
            val passphraseMemoryCache: PassphraseMemoryCache = get()
            whenever(passphraseMemoryCache.hasPassphrase()) doReturn true
            whenever(passphraseMemoryCache.get()) doReturn Passphrase(PASSPHRASE)
            val biometricCipher: BiometricCipher = get()
            whenever(biometricCipher.getBiometricEncryptCipher()) doReturn mock<Cipher>()

            viewModel = get()
            viewModel.onIntent(AppSettingsIntent.ToggleBiometric)

            viewModel.viewState.drop(1).test {
                viewModel.sideEffect.test {
                    assertThat(awaitItem()).isInstanceOf(LaunchBiometricPrompt::class.java)
                }
                val authenticatedCipher = mock<Cipher>()
                whenever(authenticatedCipher.iv) doReturn ByteArray(256)
                viewModel.onIntent(AppSettingsIntent.FinalizedBiometricAuth(authenticatedCipher))

                argumentCaptor<SavePassphraseUseCase.Input> {
                    verify(get<SavePassphraseUseCase>()).execute(capture())
                    assertThat(firstValue.passphrase).isEqualTo(PASSPHRASE)
                }
                verify(get<SaveBiometricKeyIvUseCase>()).execute(any())

                assertThat(awaitItem().isBiometricEnabled).isTrue()
            }
        }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `enable biometric should ask for auth and passphrase if missing and enable biometry`() =
        runTest {
            val checkIfPassphraseFileExistsUseCase: CheckIfPassphraseFileExistsUseCase = get()
            whenever(checkIfPassphraseFileExistsUseCase.execute(any())) doReturn
                CheckIfPassphraseFileExistsUseCase.Output(
                    passphraseFileExists = false,
                )
            val biometricInformationProvider: BiometricInformationProvider = get()
            whenever(biometricInformationProvider.hasBiometricSetUp()) doReturn true
            val passphraseMemoryCache: PassphraseMemoryCache = get()
            whenever(passphraseMemoryCache.hasPassphrase()) doReturn false
            whenever(passphraseMemoryCache.get()) doReturn PassphraseNotPresent(keyStatus = VALID)
            val biometricCipher: BiometricCipher = get()
            whenever(biometricCipher.getBiometricEncryptCipher()) doReturn mock<Cipher>()

            viewModel = get()
            viewModel.onIntent(AppSettingsIntent.ToggleBiometric)

            viewModel.viewState.drop(1).test {
                viewModel.sideEffect.test {
                    assertThat(awaitItem()).isInstanceOf(AppSettingsSideEffect.NavigateToGetPassphrase::class.java)
                }
                whenever(passphraseMemoryCache.hasPassphrase()) doReturn true
                whenever(passphraseMemoryCache.get()) doReturn Passphrase(PASSPHRASE)
                viewModel.onIntent(AppSettingsIntent.RefreshedPassphrase)

                val authenticatedCipher = mock<Cipher>()
                whenever(authenticatedCipher.iv) doReturn ByteArray(256)
                viewModel.onIntent(AppSettingsIntent.FinalizedBiometricAuth(authenticatedCipher))

                argumentCaptor<SavePassphraseUseCase.Input> {
                    verify(get<SavePassphraseUseCase>()).execute(capture())
                    assertThat(firstValue.passphrase).isEqualTo(PASSPHRASE)
                }
                verify(get<SaveBiometricKeyIvUseCase>()).execute(any())

                assertThat(awaitItem().isBiometricEnabled).isTrue()
            }
        }

    @Test
    fun `should detect autofill conflict when detector returns true on initialize`() =
        runTest {
            val checkIfPassphraseFileExistsUseCase: CheckIfPassphraseFileExistsUseCase = get()
            whenever(checkIfPassphraseFileExistsUseCase.execute(any())) doReturn
                CheckIfPassphraseFileExistsUseCase.Output(
                    passphraseFileExists = false,
                )
            val detectAutofillConflict: DetectAutofillConflict = get()
            whenever(detectAutofillConflict.invoke()) doReturn true

            viewModel = get()
            viewModel.onIntent(AppSettingsIntent.Initialize)

            assertThat(viewModel.viewState.value.isAutofillConflictDetected).isTrue()
        }

    @Test
    fun `should not detect autofill conflict when detector returns false on initialize`() =
        runTest {
            val checkIfPassphraseFileExistsUseCase: CheckIfPassphraseFileExistsUseCase = get()
            whenever(checkIfPassphraseFileExistsUseCase.execute(any())) doReturn
                CheckIfPassphraseFileExistsUseCase.Output(
                    passphraseFileExists = false,
                )
            val detectAutofillConflict: DetectAutofillConflict = get()
            whenever(detectAutofillConflict.invoke()) doReturn false

            viewModel = get()
            viewModel.onIntent(AppSettingsIntent.Initialize)

            assertThat(viewModel.viewState.value.isAutofillConflictDetected).isFalse()
        }

    @Test
    fun `initial state should have autofill conflict as false before initialize`() =
        runTest {
            val checkIfPassphraseFileExistsUseCase: CheckIfPassphraseFileExistsUseCase = get()
            whenever(checkIfPassphraseFileExistsUseCase.execute(any())) doReturn
                CheckIfPassphraseFileExistsUseCase.Output(
                    passphraseFileExists = false,
                )

            viewModel = get()

            assertThat(viewModel.viewState.value.isAutofillConflictDetected).isFalse()
        }

    private companion object {
        private val SELECTED_ACCOUNT_ID = UUID.randomUUID().toString()
        private val PASSPHRASE = "passphrase".toByteArray()
    }
}
