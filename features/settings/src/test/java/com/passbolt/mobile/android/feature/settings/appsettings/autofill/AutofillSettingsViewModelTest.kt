package com.passbolt.mobile.android.feature.settings.appsettings.autofill

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
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.passbolt.mobile.android.feature.autofill.informationprovider.AutofillInformationProvider
import com.passbolt.mobile.android.feature.autofill.informationprovider.AutofillInformationProvider.ChromeNativeAutofillStatus.DISABLED
import com.passbolt.mobile.android.feature.autofill.informationprovider.AutofillInformationProvider.ChromeNativeAutofillStatus.NOT_SUPPORTED
import com.passbolt.mobile.android.feature.settings.screen.appsettings.autofill.AutofillScreenSideEffect.NavigateToChromeNativeAutofill
import com.passbolt.mobile.android.feature.settings.screen.appsettings.autofill.AutofillScreenSideEffect.NavigateToEncourageAccessibilityAutofill
import com.passbolt.mobile.android.feature.settings.screen.appsettings.autofill.AutofillScreenSideEffect.NavigateToEncourageNativeAutofill
import com.passbolt.mobile.android.feature.settings.screen.appsettings.autofill.AutofillScreenSideEffect.NavigateToNativeAutofillEnabled
import com.passbolt.mobile.android.feature.settings.screen.appsettings.autofill.AutofillSettingsIntent.ToggleAccessibilityAutofill
import com.passbolt.mobile.android.feature.settings.screen.appsettings.autofill.AutofillSettingsIntent.ToggleChromeNativeAutofill
import com.passbolt.mobile.android.feature.settings.screen.appsettings.autofill.AutofillSettingsIntent.ToggleNativeAutofill
import com.passbolt.mobile.android.feature.settings.screen.appsettings.autofill.AutofillSettingsIntent.UpdateAutofillState
import com.passbolt.mobile.android.feature.settings.screen.appsettings.autofill.AutofillSettingsViewModel
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
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.whenever
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalCoroutinesApi::class)
class AutofillSettingsViewModelTest : KoinTest {
    @get:Rule
    val koinTestRule =
        KoinTestRule.create {
            printLogger(Level.ERROR)
            modules(
                listOf(
                    module {
                        single { mock<AutofillInformationProvider>() }
                        factoryOf(::AutofillSettingsViewModel)
                    },
                ),
            )
        }

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var viewModel: AutofillSettingsViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state should be correct for disabled settings`() =
        runTest {
            val autofillInformationProvider: AutofillInformationProvider = get()
            whenever(autofillInformationProvider.isAutofillServiceSupported()) doReturn false
            whenever(autofillInformationProvider.isPassboltAutofillServiceSet()) doReturn false
            whenever(autofillInformationProvider.isAccessibilityAutofillSetup()) doReturn false
            whenever(autofillInformationProvider.getChromeNativeAutofillStatus()) doReturn DISABLED

            viewModel = get()

            val state = viewModel.viewState.value

            assertThat(state.isNativeAutofillChecked).isFalse()
            assertThat(state.isAccessibilityAutofillChecked).isFalse()
            assertThat(state.isChromeNativeAutofillEnabled).isTrue()
            assertThat(state.isChromeNativeAutofillChecked).isFalse()
        }

    @Test
    fun `chrome native autofill should be disabled if not supported`() =
        runTest {
            val autofillInformationProvider: AutofillInformationProvider = get()
            whenever(autofillInformationProvider.isAutofillServiceSupported()) doReturn false
            whenever(autofillInformationProvider.isPassboltAutofillServiceSet()) doReturn false
            whenever(autofillInformationProvider.isAccessibilityAutofillSetup()) doReturn false
            whenever(autofillInformationProvider.getChromeNativeAutofillStatus()) doReturn NOT_SUPPORTED

            viewModel = get()

            val state = viewModel.viewState.value

            assertThat(state.isNativeAutofillChecked).isFalse()
            assertThat(state.isAccessibilityAutofillChecked).isFalse()
            assertThat(state.isChromeNativeAutofillEnabled).isFalse()
            assertThat(state.isChromeNativeAutofillChecked).isFalse()
        }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `when native autofill enabled a click should show enabled dialog`() =
        runTest {
            val autofillInformationProvider: AutofillInformationProvider = get()
            whenever(autofillInformationProvider.isAutofillServiceSupported()) doReturn true
            whenever(autofillInformationProvider.isPassboltAutofillServiceSet()) doReturn true
            whenever(autofillInformationProvider.isAccessibilityAutofillSetup()) doReturn false
            whenever(autofillInformationProvider.getChromeNativeAutofillStatus()) doReturn NOT_SUPPORTED

            viewModel = get()

            val state = viewModel.viewState.value

            assertThat(state.isNativeAutofillChecked).isTrue()

            viewModel.sideEffect.test {
                viewModel.onIntent(ToggleNativeAutofill)

                assertThat(expectItem()).isEqualTo(NavigateToNativeAutofillEnabled)
            }
        }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `when native autofill disabled a click should show encourage dialog and refresh state after enabling`() =
        runTest {
            val autofillInformationProvider: AutofillInformationProvider = get()
            whenever(autofillInformationProvider.isAutofillServiceSupported()) doReturn true
            whenever(autofillInformationProvider.isPassboltAutofillServiceSet()) doReturn false
            whenever(autofillInformationProvider.isAccessibilityAutofillSetup()) doReturn false
            whenever(autofillInformationProvider.getChromeNativeAutofillStatus()) doReturn NOT_SUPPORTED

            viewModel = get()

            val state = viewModel.viewState.value

            assertThat(state.isNativeAutofillChecked).isFalse()

            viewModel.sideEffect.test {
                viewModel.onIntent(ToggleNativeAutofill)

                assertThat(expectItem()).isEqualTo(NavigateToEncourageNativeAutofill)

                whenever(autofillInformationProvider.isPassboltAutofillServiceSet()) doReturn true
                viewModel.onIntent(UpdateAutofillState)

                assertThat(viewModel.viewState.value.isNativeAutofillChecked).isTrue()
            }
        }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `when chrome native autofill disabled a click should trigger chrome settings deeplink`() =
        runTest {
            val autofillInformationProvider: AutofillInformationProvider = get()
            whenever(autofillInformationProvider.isAutofillServiceSupported()) doReturn true
            whenever(autofillInformationProvider.isPassboltAutofillServiceSet()) doReturn true
            whenever(autofillInformationProvider.isAccessibilityAutofillSetup()) doReturn false
            whenever(autofillInformationProvider.getChromeNativeAutofillStatus()) doReturn DISABLED

            viewModel = get()

            val state = viewModel.viewState.value

            assertThat(state.isChromeNativeAutofillChecked).isFalse()
            assertThat(state.isChromeNativeAutofillEnabled).isTrue()

            viewModel.sideEffect.test {
                viewModel.onIntent(ToggleChromeNativeAutofill)

                assertThat(expectItem()).isEqualTo(NavigateToChromeNativeAutofill)
            }
        }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `when native autofill disabled a click should trigger encourage dialog and enable after change`() =
        runTest {
            val autofillInformationProvider: AutofillInformationProvider = get()
            whenever(autofillInformationProvider.isAutofillServiceSupported()) doReturn true
            whenever(autofillInformationProvider.isPassboltAutofillServiceSet()) doReturn true
            whenever(autofillInformationProvider.getChromeNativeAutofillStatus()) doReturn DISABLED
            whenever(autofillInformationProvider.isAccessibilityAutofillSetup()) doReturn false

            viewModel = get()

            val state = viewModel.viewState.value

            assertThat(state.isAccessibilityAutofillChecked).isFalse()

            viewModel.sideEffect.test {
                viewModel.onIntent(ToggleAccessibilityAutofill)

                assertThat(expectItem()).isEqualTo(NavigateToEncourageAccessibilityAutofill)

                whenever(autofillInformationProvider.isAccessibilityAutofillSetup()) doReturn true
                assertThat(viewModel.viewState.value.isAccessibilityAutofillChecked).isFalse()
            }
        }
}
