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

package com.passbolt.mobile.android.feature.settings.accounts.keyinspector.keyinspectormoremenu

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.passbolt.mobile.android.commontest.TestCoroutineLaunchContext
import com.passbolt.mobile.android.core.accounts.usecase.privatekey.GetSelectedUserPrivateKeyUseCase
import com.passbolt.mobile.android.core.mvp.coroutinecontext.CoroutineLaunchContext
import com.passbolt.mobile.android.feature.settings.screen.accounts.keyinspector.keyinspectormoremenu.KeyInspectorBottomSheetIntent.ExportPrivateKey
import com.passbolt.mobile.android.feature.settings.screen.accounts.keyinspector.keyinspectormoremenu.KeyInspectorBottomSheetIntent.ExportPublicKey
import com.passbolt.mobile.android.feature.settings.screen.accounts.keyinspector.keyinspectormoremenu.KeyInspectorBottomSheetIntent.RefreshedPassphrase
import com.passbolt.mobile.android.feature.settings.screen.accounts.keyinspector.keyinspectormoremenu.KeyInspectorBottomSheetSideEffect.ConfirmPassphrase
import com.passbolt.mobile.android.feature.settings.screen.accounts.keyinspector.keyinspectormoremenu.KeyInspectorBottomSheetSideEffect.Dismiss
import com.passbolt.mobile.android.feature.settings.screen.accounts.keyinspector.keyinspectormoremenu.KeyInspectorBottomSheetSideEffect.ErrorSnackbarType.FAILED_TO_GENERATE_PUBLIC_KEY
import com.passbolt.mobile.android.feature.settings.screen.accounts.keyinspector.keyinspectormoremenu.KeyInspectorBottomSheetSideEffect.ShowErrorSnackbar
import com.passbolt.mobile.android.feature.settings.screen.accounts.keyinspector.keyinspectormoremenu.KeyInspectorBottomSheetSideEffect.ShowTextShareSheet
import com.passbolt.mobile.android.feature.settings.screen.accounts.keyinspector.keyinspectormoremenu.KeyInspectorBottomSheetViewModel
import com.passbolt.mobile.android.gopenpgp.OpenPgp
import com.passbolt.mobile.android.gopenpgp.exception.OpenPgpError
import com.passbolt.mobile.android.gopenpgp.exception.OpenPgpResult
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
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import kotlin.test.assertIs
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalCoroutinesApi::class)
class KeyInspectorBottomSheetViewModelTest : KoinTest {
    @get:Rule
    val koinTestRule =
        KoinTestRule.create {
            printLogger(Level.ERROR)
            modules(
                listOf(
                    module {
                        single { mock<GetSelectedUserPrivateKeyUseCase>() }
                        single { mock<OpenPgp>() }
                        singleOf(::TestCoroutineLaunchContext) bind CoroutineLaunchContext::class
                        factoryOf(::KeyInspectorBottomSheetViewModel)
                    },
                ),
            )
        }

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var viewModel: KeyInspectorBottomSheetViewModel

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
    fun `should show share sheet for exporting private key`() =
        runTest {
            val mockPrivateKey = "PrivateKey"
            val getSelectedUserPrivateKeyUseCase: GetSelectedUserPrivateKeyUseCase = get()
            whenever(getSelectedUserPrivateKeyUseCase.execute(Unit)) doReturn
                GetSelectedUserPrivateKeyUseCase.Output(mockPrivateKey)

            viewModel = get()
            viewModel.onIntent(ExportPrivateKey)

            viewModel.sideEffect.test {
                assertIs<ConfirmPassphrase>(expectItem())

                viewModel.onIntent(RefreshedPassphrase)
                assertIs<Dismiss>(expectItem())

                val shareEffect = expectItem()
                assertIs<ShowTextShareSheet>(shareEffect)
                assertThat(shareEffect.text).isEqualTo(mockPrivateKey)
            }
        }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `should show share sheet for exporting public key`() =
        runTest {
            val mockPrivateKey = "PrivateKey"
            val mockPublicKey = "PublicKey"
            val getSelectedUserPrivateKeyUseCase: GetSelectedUserPrivateKeyUseCase = get()
            whenever(getSelectedUserPrivateKeyUseCase.execute(Unit)) doReturn
                GetSelectedUserPrivateKeyUseCase.Output(mockPrivateKey)

            val mockOpenPgp = get<OpenPgp>()
            whenever(mockOpenPgp.generatePublicKey(mockPrivateKey)) doReturn
                OpenPgpResult.Result(mockPublicKey)

            viewModel = get()
            viewModel.onIntent(ExportPublicKey)

            viewModel.sideEffect.test {
                assertIs<ConfirmPassphrase>(expectItem())

                viewModel.onIntent(RefreshedPassphrase)
                assertIs<Dismiss>(expectItem())

                val shareEffect = expectItem()
                assertIs<ShowTextShareSheet>(shareEffect)
                assertThat(shareEffect.text).isEqualTo(mockPublicKey)
            }
        }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `should show error when public key cannot be generated`() =
        runTest {
            val mockPrivateKey = "PrivateKey"
            val errorMessage = "errorMessage"
            val getSelectedUserPrivateKeyUseCase: GetSelectedUserPrivateKeyUseCase = get()
            whenever(getSelectedUserPrivateKeyUseCase.execute(Unit)) doReturn
                GetSelectedUserPrivateKeyUseCase.Output(mockPrivateKey)

            val mockOpenPgp = get<OpenPgp>()
            whenever(mockOpenPgp.generatePublicKey(mockPrivateKey)) doReturn
                OpenPgpResult.Error(OpenPgpError(errorMessage))

            viewModel = get()
            viewModel.onIntent(ExportPublicKey)

            viewModel.sideEffect.test {
                assertIs<ConfirmPassphrase>(expectItem())

                viewModel.onIntent(RefreshedPassphrase)

                val errorEffect = expectItem()
                assertIs<ShowErrorSnackbar>(errorEffect)
                assertThat(errorEffect.type).isEqualTo(FAILED_TO_GENERATE_PUBLIC_KEY)
                assertThat(errorEffect.errorMessage).isEqualTo(errorMessage)
            }
        }
}
