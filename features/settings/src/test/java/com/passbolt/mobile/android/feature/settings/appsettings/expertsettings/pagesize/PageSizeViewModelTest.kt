package com.passbolt.mobile.android.feature.settings.appsettings.expertsettings.pagesize

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
import com.passbolt.mobile.android.domain.preferences.GlobalPreferencesUpdate
import com.passbolt.mobile.android.domain.preferences.PreferencesDefaults
import com.passbolt.mobile.android.domain.preferences.usecase.GetAutomaticPageSizeUseCase
import com.passbolt.mobile.android.domain.preferences.usecase.GetGlobalPreferencesUseCase
import com.passbolt.mobile.android.domain.preferences.usecase.UpdateGlobalPreferencesUseCase
import com.passbolt.mobile.android.feature.settings.screen.appsettings.expertsettings.pagesize.PageSizeIntent.GoBack
import com.passbolt.mobile.android.feature.settings.screen.appsettings.expertsettings.pagesize.PageSizeIntent.PageSizeChanged
import com.passbolt.mobile.android.feature.settings.screen.appsettings.expertsettings.pagesize.PageSizeIntent.RestoreDefaultsClick
import com.passbolt.mobile.android.feature.settings.screen.appsettings.expertsettings.pagesize.PageSizeIntent.SaveClick
import com.passbolt.mobile.android.feature.settings.screen.appsettings.expertsettings.pagesize.PageSizeSideEffect.NavigateBack
import com.passbolt.mobile.android.feature.settings.screen.appsettings.expertsettings.pagesize.PageSizeViewModel
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
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.koin.test.KoinTestRule
import org.koin.test.get
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class PageSizeViewModelTest : KoinTest {
    @get:Rule
    val koinTestRule =
        KoinTestRule.create {
            printLogger(Level.ERROR)
            modules(
                listOf(
                    module {
                        single { mock<GetGlobalPreferencesUseCase>() }
                        single { mock<UpdateGlobalPreferencesUseCase>() }
                        single { mock<GetAutomaticPageSizeUseCase>() }
                        factory { PageSizeViewModel(get(), get(), get()) }
                    },
                ),
            )
        }

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var viewModel: PageSizeViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun stubPreferences(pageSize: Int = PreferencesDefaults.API_FETCH_PAGE_SIZE) {
        val getGlobalPreferencesUseCase: GetGlobalPreferencesUseCase = get()
        whenever(getGlobalPreferencesUseCase.execute(Unit)) doReturn
            GlobalPreferencesUiModel(
                areDebugLogsEnabled = false,
                debugLogFileCreationDateTime = null,
                isHideRootDialogEnabled = true,
                isAuthRequiredOnEveryEntry = true,
                debugLogLastAppVersion = null,
                apiFetchPageSize = pageSize,
                isApiFetchPageSizeManuallySet = false,
                accessibilityPoliciesConsentGiven = true,
                isCopyTotpOnAutofillEnabled = false,
            )
        val getAutomaticPageSizeUseCase: GetAutomaticPageSizeUseCase = get()
        whenever(getAutomaticPageSizeUseCase.execute(Unit)) doReturn
            GetAutomaticPageSizeUseCase.Output(defaultPageSize = 3_000, recommendedLimit = 5_000)
    }

    @Test
    fun `initial state should reflect stored value and automatic page sizes`() =
        runTest {
            stubPreferences()
            viewModel = get()

            val state = viewModel.viewState.value

            assertThat(state.selectedIndex).isEqualTo(3)
            assertThat(state.savedIndex).isEqualTo(3)
            assertThat(state.automaticDefaultIndex).isEqualTo(4)
            assertThat(state.recommendedLimitIndex).isEqualTo(5)
            assertThat(state.hasUnsavedChange).isFalse()
            assertThat(state.isOverRecommendedLimit).isFalse()
        }

    @Test
    fun `initial state should not persist anything`() =
        runTest {
            stubPreferences()
            viewModel = get()

            val updateGlobalPreferencesUseCase: UpdateGlobalPreferencesUseCase = get()
            verify(updateGlobalPreferencesUseCase, never()).execute(any())
        }

    @Test
    fun `stored value outside allowed sizes should fall back to the first index`() =
        runTest {
            stubPreferences(pageSize = 100_000)
            viewModel = get()

            assertThat(viewModel.viewState.value.selectedIndex).isEqualTo(0)
        }

    @Test
    fun `changing page size should update state without persisting`() =
        runTest {
            stubPreferences()
            viewModel = get()

            viewModel.onIntent(PageSizeChanged(sliderIndex = 6))

            val state = viewModel.viewState.value
            assertThat(state.selectedIndex).isEqualTo(6)
            assertThat(state.hasUnsavedChange).isTrue()

            val updateGlobalPreferencesUseCase: UpdateGlobalPreferencesUseCase = get()
            verify(updateGlobalPreferencesUseCase, never()).execute(any())
        }

    @Test
    fun `values above the recommended limit should be flagged as over the limit`() =
        runTest {
            stubPreferences()
            viewModel = get()

            viewModel.onIntent(PageSizeChanged(sliderIndex = 6))
            assertThat(viewModel.viewState.value.isOverRecommendedLimit).isTrue()

            viewModel.onIntent(PageSizeChanged(sliderIndex = 5))
            assertThat(viewModel.viewState.value.isOverRecommendedLimit).isFalse()
        }

    @Test
    fun `save should persist the selected page size as a manual choice`() =
        runTest {
            stubPreferences()
            viewModel = get()

            viewModel.onIntent(PageSizeChanged(sliderIndex = 6))
            viewModel.onIntent(SaveClick)

            val updateGlobalPreferencesUseCase: UpdateGlobalPreferencesUseCase = get()
            argumentCaptor<GlobalPreferencesUpdate> {
                verify(updateGlobalPreferencesUseCase).execute(capture())
                assertThat(firstValue.apiFetchPageSize).isEqualTo(10_000)
                assertThat(firstValue.isApiFetchPageSizeManuallySet).isTrue()
            }

            val state = viewModel.viewState.value
            assertThat(state.savedIndex).isEqualTo(6)
            assertThat(state.hasUnsavedChange).isFalse()
        }

    @Test
    fun `restore defaults should persist the automatic value and reset the slider`() =
        runTest {
            stubPreferences()
            viewModel = get()

            viewModel.onIntent(PageSizeChanged(sliderIndex = 6))
            viewModel.onIntent(RestoreDefaultsClick)

            val updateGlobalPreferencesUseCase: UpdateGlobalPreferencesUseCase = get()
            argumentCaptor<GlobalPreferencesUpdate> {
                verify(updateGlobalPreferencesUseCase).execute(capture())
                assertThat(firstValue.apiFetchPageSize).isEqualTo(3_000)
                assertThat(firstValue.isApiFetchPageSizeManuallySet).isFalse()
            }

            val state = viewModel.viewState.value
            assertThat(state.selectedIndex).isEqualTo(4)
            assertThat(state.savedIndex).isEqualTo(4)
            assertThat(state.hasUnsavedChange).isFalse()
        }

    @Test
    fun `go back intent should emit navigate back side effect`() =
        runTest {
            stubPreferences()
            viewModel = get()

            viewModel.sideEffect.test {
                viewModel.onIntent(GoBack)
                assertThat(awaitItem()).isEqualTo(NavigateBack)
            }
        }
}
