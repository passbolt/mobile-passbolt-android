/**
 * Passbolt - Open source password manager for teams
 * Copyright (c) 2026 Passbolt SA
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

package com.passbolt.mobile.android.feature.resourceform.additionalsecrets.totp.advanced

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.passbolt.mobile.android.feature.resourceform.additionalsecrets.totp.advanced.TotpAdvancedSettingsFormIntent.AlgorithmChanged
import com.passbolt.mobile.android.feature.resourceform.additionalsecrets.totp.advanced.TotpAdvancedSettingsFormIntent.ApplyChanges
import com.passbolt.mobile.android.feature.resourceform.additionalsecrets.totp.advanced.TotpAdvancedSettingsFormIntent.DigitChanged
import com.passbolt.mobile.android.feature.resourceform.additionalsecrets.totp.advanced.TotpAdvancedSettingsFormIntent.GoBack
import com.passbolt.mobile.android.feature.resourceform.additionalsecrets.totp.advanced.TotpAdvancedSettingsFormIntent.PeriodChanged
import com.passbolt.mobile.android.feature.resourceform.additionalsecrets.totp.advanced.TotpAdvancedSettingsFormSideEffect.ApplyAndGoBack
import com.passbolt.mobile.android.feature.resourceform.additionalsecrets.totp.advanced.TotpAdvancedSettingsFormSideEffect.NavigateBack
import com.passbolt.mobile.android.ui.LeadingContentType
import com.passbolt.mobile.android.ui.OtpParseResult
import com.passbolt.mobile.android.ui.ResourceFormMode
import com.passbolt.mobile.android.ui.TotpUiModel
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
import org.koin.core.parameter.parametersOf
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.koin.test.KoinTestRule
import org.koin.test.get
import kotlin.test.assertIs

@OptIn(ExperimentalCoroutinesApi::class)
class TotpAdvancedSettingsFormViewModelTest : KoinTest {
    @get:Rule
    val koinTestRule =
        KoinTestRule.create {
            printLogger(Level.ERROR)
            modules(
                listOf(
                    module {
                        factory { params ->
                            TotpAdvancedSettingsFormViewModel(
                                mode = params.get(),
                                totpUiModel = params.get(),
                            )
                        }
                    },
                ),
            )
        }

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `constructor should set initial state with mode and advanced settings`() =
        runTest {
            val viewModel = get<TotpAdvancedSettingsFormViewModel> { parametersOf(resourceFormMode, totp) }

            viewModel.viewState.test {
                val state = awaitItem()
                assertThat(state.resourceFormMode).isEqualTo(resourceFormMode)
                assertThat(state.expiry).isEqualTo(MOCK_EXPIRY)
                assertThat(state.length).isEqualTo(MOCK_LENGTH)
                assertThat(state.algorithm).isEqualTo(MOCK_ALGORITHM)
            }
        }

    @Test
    fun `advanced settings changes should be applied`() =
        runTest {
            val changedExpiry = "31"
            val changedLength = "7"
            val changedAlgorithm = OtpParseResult.OtpQr.Algorithm.SHA256.name

            val viewModel = get<TotpAdvancedSettingsFormViewModel> { parametersOf(resourceFormMode, totp) }
            viewModel.onIntent(PeriodChanged(changedExpiry))
            viewModel.onIntent(DigitChanged(changedLength))
            viewModel.onIntent(AlgorithmChanged(changedAlgorithm))

            viewModel.sideEffect.test {
                viewModel.onIntent(ApplyChanges)
                val sideEffect = awaitItem()
                assertIs<ApplyAndGoBack>(sideEffect)
                assertThat(sideEffect.totpModel.expiry).isEqualTo(changedExpiry)
                assertThat(sideEffect.totpModel.length).isEqualTo(changedLength)
                assertThat(sideEffect.totpModel.algorithm).isEqualTo(changedAlgorithm)
            }
        }

    @Test
    fun `validation should show error when period is not a positive integer`() =
        runTest {
            val viewModel = get<TotpAdvancedSettingsFormViewModel> { parametersOf(resourceFormMode, totp) }
            viewModel.onIntent(PeriodChanged("invalid"))
            viewModel.onIntent(ApplyChanges)

            viewModel.viewState.test {
                val state = awaitItem()
                assertThat(state.periodValidationErrors).isNotEmpty()
            }
        }

    @Test
    fun `go back should emit navigate back side effect`() =
        runTest {
            val viewModel = get<TotpAdvancedSettingsFormViewModel> { parametersOf(resourceFormMode, totp) }

            viewModel.sideEffect.test {
                viewModel.onIntent(GoBack)
                assertIs<NavigateBack>(awaitItem())
            }
        }

    private companion object {
        private const val MOCK_SECRET = "AAAAAAAA"
        private const val MOCK_ISSUER = "mock issuer"
        private const val MOCK_EXPIRY = "30"
        private const val MOCK_LENGTH = "6"
        private val MOCK_ALGORITHM = OtpParseResult.OtpQr.Algorithm.SHA1.name

        val resourceFormMode =
            ResourceFormMode.Create(
                leadingContentType = LeadingContentType.TOTP,
                parentFolderId = null,
            )

        private val totp =
            TotpUiModel(
                secret = MOCK_SECRET,
                issuer = MOCK_ISSUER,
                expiry = MOCK_EXPIRY,
                length = MOCK_LENGTH,
                algorithm = MOCK_ALGORITHM,
            )
    }
}
