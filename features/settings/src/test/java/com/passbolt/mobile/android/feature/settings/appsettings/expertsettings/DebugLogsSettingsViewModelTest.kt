package com.passbolt.mobile.android.feature.settings.appsettings.expertsettings

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
import com.google.common.truth.Truth.assertThat
import com.passbolt.mobile.android.core.preferences.usecase.GetGlobalPreferencesUseCase
import com.passbolt.mobile.android.core.preferences.usecase.UpdateGlobalPreferencesUseCase
import com.passbolt.mobile.android.feature.settings.screen.appsettings.expertsettings.ExpertSettingsIntent.ToggleDeveloperMode
import com.passbolt.mobile.android.feature.settings.screen.appsettings.expertsettings.ExpertSettingsIntent.ToggleHideRootWarning
import com.passbolt.mobile.android.feature.settings.screen.appsettings.expertsettings.ExpertSettingsViewModel
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

@OptIn(ExperimentalCoroutinesApi::class)
class DebugLogsSettingsViewModelTest : KoinTest {
    @get:Rule
    val koinTestRule =
        KoinTestRule.create {
            printLogger(Level.ERROR)
            modules(
                listOf(
                    module {
                        single { mock<UpdateGlobalPreferencesUseCase>() }
                        single { mock<GetGlobalPreferencesUseCase>() }
                        factoryOf(::ExpertSettingsViewModel)
                    },
                ),
            )
        }

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var viewModel: ExpertSettingsViewModel

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
            val getGlobalPreferencesUseCase: GetGlobalPreferencesUseCase = get()
            whenever(getGlobalPreferencesUseCase.execute(Unit)) doReturn
                GetGlobalPreferencesUseCase.Output(
                    areDebugLogsEnabled = false,
                    debugLogFileCreationDateTime = null,
                    isDeveloperModeEnabled = false,
                    isHideRootDialogEnabled = false,
                )

            viewModel = get()

            val state = viewModel.viewState.value

            assertThat(state.isDeveloperModeChecked).isFalse()
            assertThat(state.isHideRootWarningChecked).isFalse()
            assertThat(state.isHideRootWarningEnabled).isFalse()
        }

    @Test
    fun `hide root dialog should be enabled after enabling developer mode`() =
        runTest {
            val getGlobalPreferencesUseCase: GetGlobalPreferencesUseCase = get()
            whenever(getGlobalPreferencesUseCase.execute(Unit)) doReturn
                GetGlobalPreferencesUseCase.Output(
                    areDebugLogsEnabled = false,
                    debugLogFileCreationDateTime = null,
                    isDeveloperModeEnabled = false,
                    isHideRootDialogEnabled = false,
                )

            viewModel = get()

            viewModel.onIntent(ToggleDeveloperMode)

            viewModel.viewState.value.apply {
                assertThat(isDeveloperModeChecked).isTrue()
                assertThat(isHideRootWarningEnabled).isTrue()
                assertThat(isHideRootWarningChecked).isFalse()
            }
        }

    @Test
    fun `hide root dialog should be disabled and unchecked after disabling developer mode`() =
        runTest {
            val getGlobalPreferencesUseCase: GetGlobalPreferencesUseCase = get()
            whenever(getGlobalPreferencesUseCase.execute(Unit)) doReturn
                GetGlobalPreferencesUseCase.Output(
                    areDebugLogsEnabled = false,
                    debugLogFileCreationDateTime = null,
                    isDeveloperModeEnabled = true,
                    isHideRootDialogEnabled = true,
                )

            viewModel = get()

            viewModel.onIntent(ToggleDeveloperMode)

            viewModel.viewState.value.apply {
                assertThat(isDeveloperModeChecked).isFalse()
                assertThat(isHideRootWarningEnabled).isFalse()
                assertThat(isHideRootWarningChecked).isFalse()
            }
        }

    @Test
    fun `hide root dialog should be enabled and checked after enabling developer mode and checking`() =
        runTest {
            val getGlobalPreferencesUseCase: GetGlobalPreferencesUseCase = get()
            whenever(getGlobalPreferencesUseCase.execute(Unit)) doReturn
                GetGlobalPreferencesUseCase.Output(
                    areDebugLogsEnabled = false,
                    debugLogFileCreationDateTime = null,
                    isDeveloperModeEnabled = false,
                    isHideRootDialogEnabled = false,
                )

            viewModel = get()

            viewModel.onIntent(ToggleDeveloperMode)
            viewModel.viewState.value.apply {
                assertThat(isDeveloperModeChecked).isTrue()
                assertThat(isHideRootWarningEnabled).isTrue()
                assertThat(isHideRootWarningChecked).isFalse()
            }

            viewModel.onIntent(ToggleHideRootWarning)
            viewModel.viewState.value.apply {
                assertThat(isDeveloperModeChecked).isTrue()
                assertThat(isHideRootWarningEnabled).isTrue()
                assertThat(isHideRootWarningChecked).isTrue()
            }
        }
}
