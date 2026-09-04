package com.passbolt.mobile.android.feature.settings.debugsettings

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
import com.passbolt.mobile.android.core.logger.FileLoggingTree
import com.passbolt.mobile.android.domain.preferences.GlobalPreferencesUpdate
import com.passbolt.mobile.android.domain.preferences.PreferencesDefaults
import com.passbolt.mobile.android.domain.preferences.usecase.GetGlobalPreferencesUseCase
import com.passbolt.mobile.android.domain.preferences.usecase.UpdateGlobalPreferencesUseCase
import com.passbolt.mobile.android.feature.settings.screen.debuglogssettings.DebugLogsSettingsIntent.ToggleDebugLogs
import com.passbolt.mobile.android.feature.settings.screen.debuglogssettings.DebugLogsSettingsViewModel
import com.passbolt.mobile.android.ui.GlobalPreferencesUiModel
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
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalCoroutinesApi::class)
class DebugLogsSettingsViewModelTest : KoinTest {
    @get:Rule
    val koinTestRule =
        KoinTestRule.create {
            printLogger(Level.ERROR)
            modules(
                listOf(
                    module {
                        single { mock<GetGlobalPreferencesUseCase>() }
                        single { mock<UpdateGlobalPreferencesUseCase>() }
                        single { mock<FileLoggingTree>() }
                        factoryOf(::DebugLogsSettingsViewModel)
                    },
                ),
            )
        }

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var viewModel: DebugLogsSettingsViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state should be correct for disabled logs`() =
        runTest {
            val getGlobalPreferencesUseCase: GetGlobalPreferencesUseCase = get()
            whenever(getGlobalPreferencesUseCase.execute(Unit)) doReturn
                GlobalPreferencesUiModel(
                    areDebugLogsEnabled = false,
                    debugLogFileCreationDateTime = null,
                    isHideRootDialogEnabled = false,
                    isAuthRequiredOnEveryEntry = true,
                    debugLogLastAppVersion = null,
                    apiFetchPageSize = PreferencesDefaults.API_FETCH_PAGE_SIZE,
                    isApiFetchPageSizeManuallySet = false,
                    accessibilityPoliciesConsentGiven = true,
                    isCopyTotpOnAutofillEnabled = false,
                )

            viewModel = get()

            val state = viewModel.viewState.value

            assertThat(state.areDebugLogsEnabled).isFalse()
            assertThat(state.isAccessLogsEnabled).isFalse()
        }

    @Test
    fun `initial state should be correct for enabled logs`() =
        runTest {
            val getGlobalPreferencesUseCase: GetGlobalPreferencesUseCase = get()
            whenever(getGlobalPreferencesUseCase.execute(Unit)) doReturn
                GlobalPreferencesUiModel(
                    areDebugLogsEnabled = true,
                    debugLogFileCreationDateTime = null,
                    isHideRootDialogEnabled = false,
                    isAuthRequiredOnEveryEntry = true,
                    debugLogLastAppVersion = null,
                    apiFetchPageSize = PreferencesDefaults.API_FETCH_PAGE_SIZE,
                    isApiFetchPageSizeManuallySet = false,
                    accessibilityPoliciesConsentGiven = true,
                    isCopyTotpOnAutofillEnabled = false,
                )

            viewModel = get()

            val state = viewModel.viewState.value

            assertThat(state.areDebugLogsEnabled).isTrue()
            assertThat(state.isAccessLogsEnabled).isTrue()
        }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `toggleDebugLogsIntent should enable debug logs`() =
        runTest {
            val getGlobalPreferencesUseCase: GetGlobalPreferencesUseCase = get()
            whenever(getGlobalPreferencesUseCase.execute(Unit)) doReturn
                GlobalPreferencesUiModel(
                    areDebugLogsEnabled = false,
                    debugLogFileCreationDateTime = null,
                    isHideRootDialogEnabled = false,
                    isAuthRequiredOnEveryEntry = true,
                    debugLogLastAppVersion = null,
                    apiFetchPageSize = PreferencesDefaults.API_FETCH_PAGE_SIZE,
                    isApiFetchPageSizeManuallySet = false,
                    accessibilityPoliciesConsentGiven = true,
                    isCopyTotpOnAutofillEnabled = false,
                )

            viewModel =
                get<DebugLogsSettingsViewModel>()
                    .apply { onIntent(ToggleDebugLogs) }

            val state = viewModel.viewState.value

            assertThat(state.areDebugLogsEnabled).isTrue()
            assertThat(state.isAccessLogsEnabled).isTrue()
            argumentCaptor<GlobalPreferencesUpdate> {
                verify(get<UpdateGlobalPreferencesUseCase>()).execute(capture())
                assertThat(firstValue.areDebugLogsEnabled).isTrue()
            }
        }

    @Test
    fun `toggleDebugLogsIntent should disable debug logs`() =
        runTest {
            val getGlobalPreferencesUseCase: GetGlobalPreferencesUseCase = get()
            whenever(getGlobalPreferencesUseCase.execute(Unit)) doReturn
                GlobalPreferencesUiModel(
                    areDebugLogsEnabled = true,
                    debugLogFileCreationDateTime = null,
                    isHideRootDialogEnabled = false,
                    isAuthRequiredOnEveryEntry = true,
                    debugLogLastAppVersion = null,
                    apiFetchPageSize = PreferencesDefaults.API_FETCH_PAGE_SIZE,
                    isApiFetchPageSizeManuallySet = false,
                    accessibilityPoliciesConsentGiven = true,
                    isCopyTotpOnAutofillEnabled = false,
                )

            viewModel =
                get<DebugLogsSettingsViewModel>()
                    .apply { onIntent(ToggleDebugLogs) }

            val state = viewModel.viewState.value

            assertThat(state.areDebugLogsEnabled).isFalse()
            assertThat(state.isAccessLogsEnabled).isFalse()
            argumentCaptor<GlobalPreferencesUpdate> {
                verify(get<UpdateGlobalPreferencesUseCase>()).execute(capture())
                assertThat(firstValue.areDebugLogsEnabled).isFalse()
            }
        }
}
