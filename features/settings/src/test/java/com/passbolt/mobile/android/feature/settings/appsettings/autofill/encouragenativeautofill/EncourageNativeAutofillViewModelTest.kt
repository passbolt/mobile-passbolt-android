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

package com.passbolt.mobile.android.feature.settings.appsettings.autofill.encouragenativeautofill

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.passbolt.mobile.android.core.autofill.AutofillInformationProvider
import com.passbolt.mobile.android.feature.settings.screen.appsettings.autofill.encouragenativeautofill.EncourageNativeAutofillIntent.Close
import com.passbolt.mobile.android.feature.settings.screen.appsettings.autofill.encouragenativeautofill.EncourageNativeAutofillIntent.DismissAutofillNotSupported
import com.passbolt.mobile.android.feature.settings.screen.appsettings.autofill.encouragenativeautofill.EncourageNativeAutofillIntent.EnableAutofillService
import com.passbolt.mobile.android.feature.settings.screen.appsettings.autofill.encouragenativeautofill.EncourageNativeAutofillIntent.SettingsResult
import com.passbolt.mobile.android.feature.settings.screen.appsettings.autofill.encouragenativeautofill.EncourageNativeAutofillIntent.Skip
import com.passbolt.mobile.android.feature.settings.screen.appsettings.autofill.encouragenativeautofill.EncourageNativeAutofillSideEffect.AutofillEnabled
import com.passbolt.mobile.android.feature.settings.screen.appsettings.autofill.encouragenativeautofill.EncourageNativeAutofillSideEffect.NavigateBack
import com.passbolt.mobile.android.feature.settings.screen.appsettings.autofill.encouragenativeautofill.EncourageNativeAutofillSideEffect.OpenAutofillSettings
import com.passbolt.mobile.android.feature.settings.screen.appsettings.autofill.encouragenativeautofill.EncourageNativeAutofillViewModel
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
import kotlin.test.assertIs
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalCoroutinesApi::class)
class EncourageNativeAutofillViewModelTest : KoinTest {
    @get:Rule
    val koinTestRule =
        KoinTestRule.create {
            printLogger(Level.ERROR)
            modules(
                listOf(
                    module {
                        single { mock<AutofillInformationProvider>() }
                        factoryOf(::EncourageNativeAutofillViewModel)
                    },
                ),
            )
        }

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var viewModel: EncourageNativeAutofillViewModel

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
    fun `should open autofill settings when autofill is supported`() =
        runTest {
            val autofillInformationProvider: AutofillInformationProvider = get()
            whenever(autofillInformationProvider.isAutofillServiceSupported()) doReturn true

            viewModel = get()

            viewModel.sideEffect.test {
                viewModel.onIntent(EnableAutofillService)

                assertIs<OpenAutofillSettings>(awaitItem())
            }
        }

    @Test
    fun `should show not supported dialog when autofill is not supported`() =
        runTest {
            val autofillInformationProvider: AutofillInformationProvider = get()
            whenever(autofillInformationProvider.isAutofillServiceSupported()) doReturn false

            viewModel = get()

            viewModel.viewState.test {
                assertThat(awaitItem().showAutofillNotSupported).isFalse()

                viewModel.onIntent(EnableAutofillService)

                assertThat(awaitItem().showAutofillNotSupported).isTrue()
            }
        }

    @Test
    fun `should dismiss not supported dialog`() =
        runTest {
            val autofillInformationProvider: AutofillInformationProvider = get()
            whenever(autofillInformationProvider.isAutofillServiceSupported()) doReturn false

            viewModel = get()

            viewModel.viewState.test {
                assertThat(awaitItem().showAutofillNotSupported).isFalse()

                viewModel.onIntent(EnableAutofillService)
                assertThat(awaitItem().showAutofillNotSupported).isTrue()

                viewModel.onIntent(DismissAutofillNotSupported)
                assertThat(awaitItem().showAutofillNotSupported).isFalse()
            }
        }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `should emit autofill enabled when passbolt autofill is set after settings result`() =
        runTest {
            val autofillInformationProvider: AutofillInformationProvider = get()
            whenever(autofillInformationProvider.isPassboltAutofillServiceSet()) doReturn true

            viewModel = get()

            viewModel.sideEffect.test {
                viewModel.onIntent(SettingsResult)

                assertIs<AutofillEnabled>(awaitItem())
            }
        }

    @Test
    fun `should not navigate back when passbolt autofill is not set after settings result`() =
        runTest {
            val autofillInformationProvider: AutofillInformationProvider = get()
            whenever(autofillInformationProvider.isPassboltAutofillServiceSet()) doReturn false

            viewModel = get()

            viewModel.sideEffect.test {
                viewModel.onIntent(SettingsResult)

                expectNoEvents()
            }
        }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `should navigate back on maybe later`() =
        runTest {
            viewModel = get()

            viewModel.sideEffect.test {
                viewModel.onIntent(Skip)

                assertIs<NavigateBack>(awaitItem())
            }
        }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `should navigate back on close`() =
        runTest {
            viewModel = get()

            viewModel.sideEffect.test {
                viewModel.onIntent(Close)

                assertIs<NavigateBack>(awaitItem())
            }
        }
}
