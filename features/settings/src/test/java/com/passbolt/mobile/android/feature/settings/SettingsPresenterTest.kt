package com.passbolt.mobile.android.feature.settings

import com.google.common.truth.Truth.assertThat
import com.passbolt.mobile.android.feature.settings.screen.SettingsContract
import com.passbolt.mobile.android.entity.featureflags.FeatureFlagsModel
import com.passbolt.mobile.android.storage.usecase.featureflags.GetFeatureFlagsUseCase
import com.passbolt.mobile.android.storage.cache.passphrase.PotentialPassphrase
import com.passbolt.mobile.android.storage.usecase.input.UserIdInput
import com.passbolt.mobile.android.storage.usecase.passphrase.CheckIfPassphraseFileExistsUseCase
import com.passbolt.mobile.android.storage.usecase.passphrase.SavePassphraseUseCase
import com.passbolt.mobile.android.storage.usecase.preferences.GetGlobalPreferencesUseCase
import com.passbolt.mobile.android.storage.usecase.selectedaccount.GetSelectedAccountUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
import org.mockito.kotlin.reset
import org.mockito.kotlin.stub
import org.mockito.kotlin.times
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
        getFeatureFlagsUseCase.stub {
            onBlocking { execute(Unit) }.doReturn(
                GetFeatureFlagsUseCase.Output(
                    FeatureFlagsModel(
                        URL,
                        URL,
                        isPreviewPasswordAvailable = true,
                        areFoldersAvailable = false,
                        areTagsAvailable = false
                    )
                )
            )
        }
        whenever(mockGetGlobalPreferencesUseCase.execute(Unit)).thenReturn(
            GetGlobalPreferencesUseCase.Output(areDebugLogsEnabled = false)
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
    fun `privacy policy clicked should navigate to privacy policy website`() {
        presenter.attach(view)
        presenter.privacyPolicyClick()

        verify(view).openUrl(URL)
    }

    @Test
    fun `privacy policy and terms should hide if urls not provided`() {
        getFeatureFlagsUseCase.stub {
            onBlocking { execute(Unit) }.doReturn(
                GetFeatureFlagsUseCase.Output(
                    FeatureFlagsModel(
                        null,
                        null,
                        isPreviewPasswordAvailable = true,
                        areFoldersAvailable = false,
                        areTagsAvailable = false
                    )
                )
            )
        }

        presenter.attach(view)

        verify(view).hidePrivacyPolicyButton()
        verify(view).hideTermsAndConditionsButton()
    }

    @Test
    fun `terms clicked should navigate to terms website`() {
        presenter.attach(view)
        presenter.termsClick()

        verify(view).openUrl(URL)
    }

    @Test
    fun `sign out clicked should open confirmation dialog`() {
        presenter.attach(view)
        presenter.signOutClick()

        verify(view).showLogoutDialog()
    }

    @Test
    fun `sign out confirmation clicked should navigate to sign in`() {
        presenter.attach(view)
        presenter.logoutConfirmed()

        verify(view).showProgress()
        verify(view).hideProgress()
        verify(view).navigateToSignInWithLogout()
    }

    @Test
    fun `manage accounts clicked should navigate to accounts list`() {
        presenter.attach(view)
        presenter.manageAccountsClick()

        verify(view).navigateToManageAccounts()
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

        verify(view, times(2)).toggleFingerprintOn(eq(true))
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
            assertThat(firstValue.passphrase).isEqualTo(PASSPHRASE)
        }
    }

    @Test
    fun `refreshing feature flags should cause UI refresh`() {
        val featureFlags = FeatureFlagsModel(
            null,
            null,
            isPreviewPasswordAvailable = true,
            areFoldersAvailable = false,
            areTagsAvailable = false
        )
        getFeatureFlagsUseCase.stub {
            onBlocking { execute(Unit) }.doReturn(
                GetFeatureFlagsUseCase.Output(featureFlags)
            )
        }

        presenter.attach(view)

        verify(view).hidePrivacyPolicyButton()
        verify(view).hideTermsAndConditionsButton()

        getFeatureFlagsUseCase.apply {
            reset(this)
            stub {
                onBlocking { execute(Unit) }.doReturn(
                    GetFeatureFlagsUseCase.Output(
                        featureFlags.copy(privacyPolicyUrl = URL, termsAndConditionsUrl = URL)
                    )
                )
            }
        }
        presenter.viewResumed()

        verify(view).showPrivacyPolicyButton()
        verify(view).showTermsAndConditionsButton()
    }

    private companion object {
        private val PASSPHRASE = "passphrase".toByteArray()
        private const val URL = "https://www.passbolt.com"
    }
}
