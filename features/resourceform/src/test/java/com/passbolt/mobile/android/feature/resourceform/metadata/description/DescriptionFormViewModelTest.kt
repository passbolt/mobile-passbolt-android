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

package com.passbolt.mobile.android.feature.resourceform.metadata.description

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.passbolt.mobile.android.feature.resourceform.metadata.description.DescriptionFormIntent.ApplyChanges
import com.passbolt.mobile.android.feature.resourceform.metadata.description.DescriptionFormIntent.DescriptionChanged
import com.passbolt.mobile.android.feature.resourceform.metadata.description.DescriptionFormIntent.GoBack
import com.passbolt.mobile.android.feature.resourceform.metadata.description.DescriptionFormSideEffect.ApplyAndGoBack
import com.passbolt.mobile.android.feature.resourceform.metadata.description.DescriptionFormSideEffect.NavigateBack
import com.passbolt.mobile.android.ui.LeadingContentType
import com.passbolt.mobile.android.ui.ResourceFormMode
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
import kotlin.test.assertIs

@OptIn(ExperimentalCoroutinesApi::class)
class DescriptionFormViewModelTest : KoinTest {
    @get:Rule
    val koinTestRule =
        KoinTestRule.create {
            printLogger(Level.ERROR)
            modules(
                listOf(
                    module {
                        single<ResourceFormMode> { createMode }
                        single { DESCRIPTION }
                        factory { DescriptionFormViewModel(get(), get()) }
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
    fun `view model should initialize state with provided constructor args`() =
        runTest {
            val viewModel = get<DescriptionFormViewModel>()

            viewModel.viewState.test {
                val state = awaitItem()
                assertThat(state.resourceFormMode).isEqualTo(createMode)
                assertThat(state.metadataDescription).isEqualTo(DESCRIPTION)
            }
        }

    @Test
    fun `description changes should update state`() =
        runTest {
            val changedDescription = "changed description"
            val viewModel = get<DescriptionFormViewModel>()

            viewModel.onIntent(DescriptionChanged(changedDescription))

            viewModel.viewState.test {
                val state = awaitItem()
                assertThat(state.metadataDescription).isEqualTo(changedDescription)
            }
        }

    @Test
    fun `apply changes should emit apply and go back side effect with current description`() =
        runTest {
            val changedDescription = "changed description"
            val viewModel = get<DescriptionFormViewModel>()
            viewModel.onIntent(DescriptionChanged(changedDescription))

            viewModel.sideEffect.test {
                viewModel.onIntent(ApplyChanges)

                val sideEffect = awaitItem()
                assertIs<ApplyAndGoBack>(sideEffect)
                assertThat(sideEffect.metadataDescription).isEqualTo(changedDescription)
            }
        }

    @Test
    fun `go back should emit navigate up side effect`() =
        runTest {
            val viewModel = get<DescriptionFormViewModel>()

            viewModel.sideEffect.test {
                viewModel.onIntent(GoBack)
                assertIs<NavigateBack>(awaitItem())
            }
        }

    private companion object {
        const val DESCRIPTION = "mock description"
        val createMode =
            ResourceFormMode.Create(
                leadingContentType = LeadingContentType.PASSWORD,
                parentFolderId = null,
            )
    }
}
