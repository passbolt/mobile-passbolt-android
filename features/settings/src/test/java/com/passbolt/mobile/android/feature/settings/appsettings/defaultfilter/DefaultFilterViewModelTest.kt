package com.passbolt.mobile.android.feature.settings.appsettings.defaultfilter

import com.google.common.truth.Truth.assertThat
import com.passbolt.mobile.android.core.preferences.usecase.GetHomeDisplayViewPrefsUseCase
import com.passbolt.mobile.android.core.preferences.usecase.HomeDisplayViewPrefsValidator
import com.passbolt.mobile.android.core.preferences.usecase.UpdateHomeDisplayViewPrefsUseCase
import com.passbolt.mobile.android.entity.home.HomeDisplayView.ALL_ITEMS
import com.passbolt.mobile.android.feature.settings.screen.appsettings.defaultfilter.DefaultFilterIntent.SelectDefaultFilter
import com.passbolt.mobile.android.feature.settings.screen.appsettings.defaultfilter.DefaultFilterViewModel
import com.passbolt.mobile.android.ui.DefaultFilterModel
import com.passbolt.mobile.android.ui.DefaultFilterModel.EXPIRY
import com.passbolt.mobile.android.ui.DefaultFilterModel.FAVOURITES
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
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

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultFilterViewModelTest : KoinTest {
    @get:Rule
    val koinTestRule =
        KoinTestRule.create {
            printLogger(Level.ERROR)
            modules(
                listOf(
                    module {
                        single { mock<UpdateHomeDisplayViewPrefsUseCase>() }
                        single { mock<HomeDisplayViewPrefsValidator>() }
                        single { mock<GetHomeDisplayViewPrefsUseCase>() }
                        factoryOf(::DefaultFilterViewModel)
                    },
                ),
            )
        }

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var viewModel: DefaultFilterViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `should show validated filter list and selected filter initially`() {
        val homeDisplayViewPrefsValidator: HomeDisplayViewPrefsValidator = get()
        whenever(homeDisplayViewPrefsValidator.validatedDefaultFiltersList()) doReturn DefaultFilterModel.entries
        val getHomeDisplayViewPrefsUseCase: GetHomeDisplayViewPrefsUseCase = get()
        whenever(getHomeDisplayViewPrefsUseCase.execute(Unit)) doReturn
            GetHomeDisplayViewPrefsUseCase.Output(
                lastUsedHomeView = ALL_ITEMS,
                userSetHomeView = EXPIRY,
            )

        viewModel = get()

        assertThat(viewModel.viewState.value.allFilters).containsExactlyElementsIn(DefaultFilterModel.entries)
        assertThat(viewModel.viewState.value.selectedFilter).isEqualTo(EXPIRY)
    }

    @Test
    fun `selected filter should be updated`() {
        val homeDisplayViewPrefsValidator: HomeDisplayViewPrefsValidator = get()
        whenever(homeDisplayViewPrefsValidator.validatedDefaultFiltersList()) doReturn DefaultFilterModel.entries
        val getHomeDisplayViewPrefsUseCase: GetHomeDisplayViewPrefsUseCase = get()
        whenever(getHomeDisplayViewPrefsUseCase.execute(Unit)) doReturn
            GetHomeDisplayViewPrefsUseCase.Output(
                lastUsedHomeView = ALL_ITEMS,
                userSetHomeView = EXPIRY,
            )

        viewModel = get()
        viewModel.onIntent(SelectDefaultFilter(FAVOURITES))

        assertThat(viewModel.viewState.value.allFilters).containsExactlyElementsIn(DefaultFilterModel.entries)
        assertThat(viewModel.viewState.value.selectedFilter).isEqualTo(FAVOURITES)
        argumentCaptor<UpdateHomeDisplayViewPrefsUseCase.Input> {
            verify(get<UpdateHomeDisplayViewPrefsUseCase>()).execute(capture())
            assertThat(firstValue.userSetHomeView).isEqualTo(FAVOURITES)
        }
    }
}
