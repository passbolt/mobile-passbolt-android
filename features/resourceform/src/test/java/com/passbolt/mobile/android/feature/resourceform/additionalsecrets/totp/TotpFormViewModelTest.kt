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

package com.passbolt.mobile.android.feature.resourceform.additionalsecrets.totp

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.passbolt.mobile.android.feature.resourceform.additionalsecrets.totp.TotpFormIntent.AdvancedSettingsChanged
import com.passbolt.mobile.android.feature.resourceform.additionalsecrets.totp.TotpFormIntent.ApplyChanges
import com.passbolt.mobile.android.feature.resourceform.additionalsecrets.totp.TotpFormIntent.GoBack
import com.passbolt.mobile.android.feature.resourceform.additionalsecrets.totp.TotpFormIntent.IssuerChanged
import com.passbolt.mobile.android.feature.resourceform.additionalsecrets.totp.TotpFormIntent.RemoveTotp
import com.passbolt.mobile.android.feature.resourceform.additionalsecrets.totp.TotpFormIntent.SecretChanged
import com.passbolt.mobile.android.feature.resourceform.additionalsecrets.totp.TotpFormIntent.TotpScanned
import com.passbolt.mobile.android.feature.resourceform.additionalsecrets.totp.TotpFormSideEffect.ApplyAndGoBack
import com.passbolt.mobile.android.feature.resourceform.additionalsecrets.totp.TotpFormSideEffect.NavigateBack
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
class TotpFormViewModelTest : KoinTest {
    @get:Rule
    val koinTestRule =
        KoinTestRule.create {
            printLogger(Level.ERROR)
            modules(
                listOf(
                    module {
                        factory { params ->
                            TotpFormViewModel(
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
    fun `constructor should set initial state with mode and totp fields`() =
        runTest {
            val viewModel = get<TotpFormViewModel> { parametersOf(resourceFormMode, totp) }

            viewModel.viewState.test {
                val state = awaitItem()
                assertThat(state.resourceFormMode).isEqualTo(resourceFormMode)
                assertThat(state.secret).isEqualTo(MOCK_SECRET)
                assertThat(state.issuer).isEqualTo(MOCK_ISSUER)
                assertThat(state.expiry).isEqualTo(MOCK_EXPIRY)
                assertThat(state.length).isEqualTo(MOCK_LENGTH)
                assertThat(state.algorithm).isEqualTo(MOCK_ALGORITHM)
            }
        }

    @Test
    fun `secret and issuer changes should be applied`() =
        runTest {
            val changedSecret = "AAAAAAAA"
            val changedIssuer = "changed issuer"

            val viewModel = get<TotpFormViewModel> { parametersOf(resourceFormMode, totp) }
            viewModel.onIntent(SecretChanged(changedSecret))
            viewModel.onIntent(IssuerChanged(changedIssuer))

            viewModel.sideEffect.test {
                viewModel.onIntent(ApplyChanges)
                val sideEffect = awaitItem()
                assertIs<ApplyAndGoBack>(sideEffect)
                assertThat(sideEffect.totpUiModel!!.secret).isEqualTo(changedSecret)
                assertThat(sideEffect.totpUiModel!!.issuer).isEqualTo(changedIssuer)
            }
        }

    @Test
    fun `advanced settings changes should be applied`() =
        runTest {
            val changedExpiry = "31"
            val changedLength = "7"
            val changedAlgorithm = OtpParseResult.OtpQr.Algorithm.SHA256.name
            val changedAdvancedSettings =
                totp.copy(
                    expiry = changedExpiry,
                    length = changedLength,
                    algorithm = changedAlgorithm,
                )

            val viewModel = get<TotpFormViewModel> { parametersOf(resourceFormMode, totp) }
            viewModel.onIntent(AdvancedSettingsChanged(changedAdvancedSettings))
            viewModel.onIntent(SecretChanged("AAAAAAAA"))

            viewModel.sideEffect.test {
                viewModel.onIntent(ApplyChanges)
                val sideEffect = awaitItem()
                assertIs<ApplyAndGoBack>(sideEffect)
                assertThat(sideEffect.totpUiModel!!.expiry).isEqualTo(changedExpiry)
                assertThat(sideEffect.totpUiModel!!.length).isEqualTo(changedLength)
                assertThat(sideEffect.totpUiModel!!.algorithm).isEqualTo(changedAlgorithm)
            }
        }

    @Test
    fun `scan totp should apply changes`() =
        runTest {
            val scannedTotp =
                OtpParseResult.OtpQr.TotpQr(
                    label = "label",
                    secret = "AAAAAAAA",
                    issuer = "issuer",
                    algorithm = OtpParseResult.OtpQr.Algorithm.SHA1,
                    digits = 6,
                    period = 30,
                )

            val viewModel = get<TotpFormViewModel> { parametersOf(resourceFormMode, totp) }
            viewModel.onIntent(TotpScanned(isManualCreationChosen = false, scannedTotp))

            viewModel.sideEffect.test {
                viewModel.onIntent(ApplyChanges)
                val sideEffect = awaitItem()
                assertIs<ApplyAndGoBack>(sideEffect)
                assertThat(sideEffect.totpUiModel!!.secret).isEqualTo(scannedTotp.secret)
                assertThat(sideEffect.totpUiModel!!.issuer).isEqualTo(scannedTotp.issuer)
                assertThat(sideEffect.totpUiModel!!.algorithm).isEqualTo(scannedTotp.algorithm.name)
                assertThat(sideEffect.totpUiModel!!.length).isEqualTo(scannedTotp.digits.toString())
                assertThat(sideEffect.totpUiModel!!.expiry).isEqualTo(scannedTotp.period.toString())
            }
        }

    @Test
    fun `validation should show error when secret is empty`() =
        runTest {
            val viewModel = get<TotpFormViewModel> { parametersOf(resourceFormMode, totp) }
            viewModel.onIntent(SecretChanged(""))
            viewModel.onIntent(ApplyChanges)

            viewModel.viewState.test {
                val state = awaitItem()
                assertThat(state.secretValidationErrors).isNotEmpty()
            }
        }

    @Test
    fun `validation should show error when secret is not base32`() =
        runTest {
            val viewModel = get<TotpFormViewModel> { parametersOf(resourceFormMode, totp) }
            viewModel.onIntent(SecretChanged("invalid!@#"))
            viewModel.onIntent(ApplyChanges)

            viewModel.viewState.test {
                val state = awaitItem()
                assertThat(state.secretValidationErrors).isNotEmpty()
            }
        }

    @Test
    fun `go back should emit navigate back side effect`() =
        runTest {
            val viewModel = get<TotpFormViewModel> { parametersOf(resourceFormMode, totp) }

            viewModel.sideEffect.test {
                viewModel.onIntent(GoBack)
                assertIs<NavigateBack>(awaitItem())
            }
        }

    @Test
    fun `remove totp should emit apply and go back with null`() =
        runTest {
            val viewModel = get<TotpFormViewModel> { parametersOf(resourceFormMode, totp) }

            viewModel.sideEffect.test {
                viewModel.onIntent(RemoveTotp)
                val sideEffect = awaitItem()
                assertIs<ApplyAndGoBack>(sideEffect)
                assertThat(sideEffect.totpUiModel).isNull()
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
