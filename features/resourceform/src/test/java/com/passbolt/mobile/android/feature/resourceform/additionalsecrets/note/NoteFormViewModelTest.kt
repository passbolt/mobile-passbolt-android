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

package com.passbolt.mobile.android.feature.resourceform.additionalsecrets.note

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.passbolt.mobile.android.feature.resourceform.additionalsecrets.note.NoteFormIntent.ApplyChanges
import com.passbolt.mobile.android.feature.resourceform.additionalsecrets.note.NoteFormIntent.GoBack
import com.passbolt.mobile.android.feature.resourceform.additionalsecrets.note.NoteFormIntent.NoteTextChanged
import com.passbolt.mobile.android.feature.resourceform.additionalsecrets.note.NoteFormIntent.RemoveNote
import com.passbolt.mobile.android.feature.resourceform.additionalsecrets.note.NoteFormSideEffect.ApplyAndGoBack
import com.passbolt.mobile.android.feature.resourceform.additionalsecrets.note.NoteFormSideEffect.NavigateBack
import com.passbolt.mobile.android.ui.LeadingContentType.PASSWORD
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
import org.koin.core.parameter.parametersOf
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.koin.test.KoinTestRule
import org.koin.test.get
import kotlin.test.assertIs

@OptIn(ExperimentalCoroutinesApi::class)
class NoteFormViewModelTest : KoinTest {
    @get:Rule
    val koinTestRule =
        KoinTestRule.create {
            printLogger(Level.ERROR)
            modules(
                listOf(
                    module {
                        factory { params ->
                            NoteFormViewModel(
                                mode = params.get(),
                                note = params.get(),
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
    fun `constructor should set initial state with mode and note`() =
        runTest {
            val viewModel = get<NoteFormViewModel> { parametersOf(resourceFormMode, "mock note") }

            viewModel.viewState.test {
                val state = awaitItem()
                assertThat(state.resourceFormMode).isEqualTo(resourceFormMode)
                assertThat(state.note).isEqualTo("mock note")
            }
        }

    @Test
    fun `note changes should be applied`() =
        runTest {
            val viewModel = get<NoteFormViewModel> { parametersOf(resourceFormMode, "mock note") }
            viewModel.onIntent(NoteTextChanged("changed mock note"))

            viewModel.sideEffect.test {
                viewModel.onIntent(ApplyChanges)
                val sideEffect = awaitItem()
                assertIs<ApplyAndGoBack>(sideEffect)
                assertThat(sideEffect.note).isEqualTo("changed mock note")
            }
        }

    @Test
    fun `note validation should show error when note exceeds max length`() =
        runTest {
            val tooLongNote = "a".repeat(NoteFormViewModel.NOTE_MAX_LENGTH + 1)

            val viewModel = get<NoteFormViewModel> { parametersOf(resourceFormMode, "") }
            viewModel.onIntent(NoteTextChanged(tooLongNote))
            viewModel.onIntent(ApplyChanges)

            viewModel.viewState.test {
                val state = awaitItem()
                assertThat(state.noteValidationErrors).isNotEmpty()
            }
        }

    @Test
    fun `note validation should pass when note length is within limit`() =
        runTest {
            val validNote = "a".repeat(NoteFormViewModel.NOTE_MAX_LENGTH - 1)

            val viewModel = get<NoteFormViewModel> { parametersOf(resourceFormMode, "mock note") }
            viewModel.onIntent(NoteTextChanged(validNote))

            viewModel.sideEffect.test {
                viewModel.onIntent(ApplyChanges)
                val sideEffect = awaitItem()
                assertIs<ApplyAndGoBack>(sideEffect)
                assertThat(sideEffect.note).isEqualTo(validNote)
            }
        }

    @Test
    fun `go back should emit navigate up side effect`() =
        runTest {
            val viewModel = get<NoteFormViewModel> { parametersOf(resourceFormMode, "") }

            viewModel.sideEffect.test {
                viewModel.onIntent(GoBack)
                assertIs<NavigateBack>(awaitItem())
            }
        }

    @Test
    fun `remove note should emit apply and go back with null`() =
        runTest {
            val viewModel = get<NoteFormViewModel> { parametersOf(resourceFormMode, "mock note") }

            viewModel.sideEffect.test {
                viewModel.onIntent(RemoveNote)
                val sideEffect = awaitItem()
                assertIs<ApplyAndGoBack>(sideEffect)
                assertThat(sideEffect.note).isNull()
            }
        }

    private companion object {
        val resourceFormMode =
            ResourceFormMode.Create(
                leadingContentType = PASSWORD,
                parentFolderId = null,
            )
    }
}
