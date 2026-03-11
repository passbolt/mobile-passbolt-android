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

package com.passbolt.mobile.android.feature.settings.appsettings.autofill.encourageaccessibility

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.passbolt.mobile.android.core.autofill.AutofillInformationProvider
import com.passbolt.mobile.android.feature.settings.screen.appsettings.autofill.encourageaccessibility.EncourageAccessibilityIntent.Close
import com.passbolt.mobile.android.feature.settings.screen.appsettings.autofill.encourageaccessibility.EncourageAccessibilityIntent.ConsentToEnableAccessibility
import com.passbolt.mobile.android.feature.settings.screen.appsettings.autofill.encourageaccessibility.EncourageAccessibilityIntent.DismissEnableAccessibilityConsent
import com.passbolt.mobile.android.feature.settings.screen.appsettings.autofill.encourageaccessibility.EncourageAccessibilityIntent.EnableAccessibilityService
import com.passbolt.mobile.android.feature.settings.screen.appsettings.autofill.encourageaccessibility.EncourageAccessibilityIntent.GrantOverlayPermission
import com.passbolt.mobile.android.feature.settings.screen.appsettings.autofill.encourageaccessibility.EncourageAccessibilityIntent.RefreshState
import com.passbolt.mobile.android.feature.settings.screen.appsettings.autofill.encourageaccessibility.EncourageAccessibilitySideEffect.NavigateBack
import com.passbolt.mobile.android.feature.settings.screen.appsettings.autofill.encourageaccessibility.EncourageAccessibilitySideEffect.OpenAccessibilitySettings
import com.passbolt.mobile.android.feature.settings.screen.appsettings.autofill.encourageaccessibility.EncourageAccessibilitySideEffect.OpenOverlaySettings
import com.passbolt.mobile.android.feature.settings.screen.appsettings.autofill.encourageaccessibility.EncourageAccessibilityViewModel
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
class EncourageAccessibilityViewModelTest : KoinTest {
    @get:Rule
    val koinTestRule =
        KoinTestRule.create {
            printLogger(Level.ERROR)
            modules(
                listOf(
                    module {
                        single { mock<AutofillInformationProvider>() }
                        factoryOf(::EncourageAccessibilityViewModel)
                    },
                ),
            )
        }

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var viewModel: EncourageAccessibilityViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `should refresh state on init`() =
        runTest {
            val autofillInformationProvider: AutofillInformationProvider = get()
            whenever(autofillInformationProvider.isAccessibilityServiceEnabled()) doReturn true
            whenever(autofillInformationProvider.isAccessibilityOverlayEnabled()) doReturn true

            viewModel = get()

            viewModel.viewState.test {
                val state = awaitItem()
                assertThat(state.isAccessibilityServiceEnabled).isTrue()
                assertThat(state.isOverlayPermissionGranted).isTrue()
            }
        }

    @Test
    fun `should update state on refresh`() =
        runTest {
            val autofillInformationProvider: AutofillInformationProvider = get()
            whenever(autofillInformationProvider.isAccessibilityServiceEnabled()) doReturn false
            whenever(autofillInformationProvider.isAccessibilityOverlayEnabled()) doReturn false

            viewModel = get()

            viewModel.viewState.test {
                val initial = awaitItem()
                assertThat(initial.isAccessibilityServiceEnabled).isFalse()
                assertThat(initial.isOverlayPermissionGranted).isFalse()

                whenever(autofillInformationProvider.isAccessibilityServiceEnabled()) doReturn true
                whenever(autofillInformationProvider.isAccessibilityOverlayEnabled()) doReturn true
                viewModel.onIntent(RefreshState)

                val updated = awaitItem()
                assertThat(updated.isAccessibilityServiceEnabled).isTrue()
                assertThat(updated.isOverlayPermissionGranted).isTrue()
            }
        }

    @Test
    fun `should show consent dialog when service is not enabled and service click`() =
        runTest {
            val autofillInformationProvider: AutofillInformationProvider = get()
            whenever(autofillInformationProvider.isAccessibilityServiceEnabled()) doReturn false
            whenever(autofillInformationProvider.isAccessibilityOverlayEnabled()) doReturn false

            viewModel = get()

            viewModel.viewState.test {
                assertThat(awaitItem().showAccessibilityConsent).isFalse()

                viewModel.onIntent(EnableAccessibilityService)

                assertThat(awaitItem().showAccessibilityConsent).isTrue()
            }
        }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `should open accessibility settings when service is already enabled and service click`() =
        runTest {
            val autofillInformationProvider: AutofillInformationProvider = get()
            whenever(autofillInformationProvider.isAccessibilityServiceEnabled()) doReturn true
            whenever(autofillInformationProvider.isAccessibilityOverlayEnabled()) doReturn false

            viewModel = get()

            viewModel.sideEffect.test {
                viewModel.onIntent(EnableAccessibilityService)

                assertIs<OpenAccessibilitySettings>(awaitItem())
            }
        }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `should open overlay settings on overlay click`() =
        runTest {
            val autofillInformationProvider: AutofillInformationProvider = get()
            whenever(autofillInformationProvider.isAccessibilityServiceEnabled()) doReturn false
            whenever(autofillInformationProvider.isAccessibilityOverlayEnabled()) doReturn false

            viewModel = get()

            viewModel.sideEffect.test {
                viewModel.onIntent(GrantOverlayPermission)

                assertIs<OpenOverlaySettings>(awaitItem())
            }
        }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `should navigate back on close`() =
        runTest {
            val autofillInformationProvider: AutofillInformationProvider = get()
            whenever(autofillInformationProvider.isAccessibilityServiceEnabled()) doReturn false
            whenever(autofillInformationProvider.isAccessibilityOverlayEnabled()) doReturn false

            viewModel = get()

            viewModel.sideEffect.test {
                viewModel.onIntent(Close)

                assertIs<NavigateBack>(awaitItem())
            }
        }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `should dismiss consent dialog and open accessibility settings on consent given`() =
        runTest {
            val autofillInformationProvider: AutofillInformationProvider = get()
            whenever(autofillInformationProvider.isAccessibilityServiceEnabled()) doReturn false
            whenever(autofillInformationProvider.isAccessibilityOverlayEnabled()) doReturn false

            viewModel = get()

            viewModel.viewState.test {
                assertThat(awaitItem().showAccessibilityConsent).isFalse()

                viewModel.onIntent(EnableAccessibilityService)
                assertThat(awaitItem().showAccessibilityConsent).isTrue()

                viewModel.sideEffect.test {
                    viewModel.onIntent(ConsentToEnableAccessibility)
                    assertIs<OpenAccessibilitySettings>(awaitItem())
                }

                assertThat(awaitItem().showAccessibilityConsent).isFalse()
            }
        }

    @Test
    fun `should dismiss consent dialog on dismiss consent`() =
        runTest {
            val autofillInformationProvider: AutofillInformationProvider = get()
            whenever(autofillInformationProvider.isAccessibilityServiceEnabled()) doReturn false
            whenever(autofillInformationProvider.isAccessibilityOverlayEnabled()) doReturn false

            viewModel = get()

            viewModel.viewState.test {
                assertThat(awaitItem().showAccessibilityConsent).isFalse()

                viewModel.onIntent(EnableAccessibilityService)
                assertThat(awaitItem().showAccessibilityConsent).isTrue()

                viewModel.onIntent(DismissEnableAccessibilityConsent)
                assertThat(awaitItem().showAccessibilityConsent).isFalse()
            }
        }
}
