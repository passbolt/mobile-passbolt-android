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

package com.passbolt.mobile.android.feature.transferaccounttodevice.summary

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.passbolt.mobile.android.commontest.TestCoroutineLaunchContext
import com.passbolt.mobile.android.core.mvp.coroutinecontext.CoroutineLaunchContext
import com.passbolt.mobile.android.feature.transferaccounttoanotherdevice.summary.TransferAccountSummaryIntent.GoBack
import com.passbolt.mobile.android.feature.transferaccounttoanotherdevice.summary.TransferAccountSummaryIntent.Initialize
import com.passbolt.mobile.android.feature.transferaccounttoanotherdevice.summary.TransferAccountSummaryIntent.PrimaryAction
import com.passbolt.mobile.android.feature.transferaccounttoanotherdevice.summary.TransferAccountSummaryIntent.TryAgain
import com.passbolt.mobile.android.feature.transferaccounttoanotherdevice.summary.TransferAccountSummarySideEffect.NavigateToMyAccount
import com.passbolt.mobile.android.feature.transferaccounttoanotherdevice.summary.TransferAccountSummarySideEffect.NavigateToTransferAccountStart
import com.passbolt.mobile.android.feature.transferaccounttoanotherdevice.summary.TransferAccountSummaryViewModel
import com.passbolt.mobile.android.ui.TransferAccountStatusType
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
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.koin.test.KoinTestRule
import org.koin.test.get
import kotlin.test.assertIs
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalCoroutinesApi::class)
class TransferAccountSummaryViewModelTest : KoinTest {
    @get:Rule
    val koinTestRule =
        KoinTestRule.create {
            printLogger(Level.ERROR)
            modules(
                listOf(
                    module {
                        singleOf(::TestCoroutineLaunchContext) bind CoroutineLaunchContext::class
                        factoryOf(::TransferAccountSummaryViewModel)
                    },
                ),
            )
        }

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var viewModel: TransferAccountSummaryViewModel

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
    fun `Initialize intent should update state with status type`() =
        runTest {
            viewModel = get()

            viewModel.viewState.test {
                assertThat(awaitItem().statusType).isNull()

                viewModel.onIntent(Initialize(TransferAccountStatusType.SUCCESS))

                assertThat(awaitItem().statusType).isEqualTo(TransferAccountStatusType.SUCCESS)
            }
        }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `PrimaryAction intent should emit NavigateToMyAccount side effect`() =
        runTest {
            viewModel = get()

            viewModel.sideEffect.test {
                viewModel.onIntent(PrimaryAction)
                assertIs<NavigateToMyAccount>(awaitItem())
            }
        }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `GoBack intent should emit NavigateToTransferAccountStart side effect`() =
        runTest {
            viewModel = get()

            viewModel.sideEffect.test {
                viewModel.onIntent(GoBack)
                assertIs<NavigateToTransferAccountStart>(awaitItem())
            }
        }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `TryAgain intent should emit NavigateToTransferAccountStart side effect`() =
        runTest {
            viewModel = get()

            viewModel.sideEffect.test {
                viewModel.onIntent(TryAgain)
                assertIs<NavigateToTransferAccountStart>(awaitItem())
            }
        }
}
