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

package com.passbolt.mobile.android.feature.resourceform.metadata.additionaluris

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.passbolt.mobile.android.common.UuidProvider
import com.passbolt.mobile.android.feature.resourceform.metadata.additionaluris.AdditionalUrisFormIntent.AddAdditionalUri
import com.passbolt.mobile.android.feature.resourceform.metadata.additionaluris.AdditionalUrisFormIntent.AdditionalUriChanged
import com.passbolt.mobile.android.feature.resourceform.metadata.additionaluris.AdditionalUrisFormIntent.ApplyChanges
import com.passbolt.mobile.android.feature.resourceform.metadata.additionaluris.AdditionalUrisFormIntent.GoBack
import com.passbolt.mobile.android.feature.resourceform.metadata.additionaluris.AdditionalUrisFormIntent.Initialize
import com.passbolt.mobile.android.feature.resourceform.metadata.additionaluris.AdditionalUrisFormIntent.MainUriChanged
import com.passbolt.mobile.android.feature.resourceform.metadata.additionaluris.AdditionalUrisFormIntent.RemoveAdditionalUri
import com.passbolt.mobile.android.feature.resourceform.metadata.additionaluris.AdditionalUrisFormSideEffect.ApplyAndGoBack
import com.passbolt.mobile.android.feature.resourceform.metadata.additionaluris.AdditionalUrisFormSideEffect.NavigateUp
import com.passbolt.mobile.android.feature.resourceform.metadata.additionaluris.AdditionalUrisFormSideEffect.ShowErrorSnackbar
import com.passbolt.mobile.android.feature.resourceform.metadata.additionaluris.AdditionalUrisLimitChecker.Companion.MAX_ADDITIONAL_URIS
import com.passbolt.mobile.android.ui.AdditionalUrisUiModel
import com.passbolt.mobile.android.ui.LeadingContentType
import com.passbolt.mobile.android.ui.ResourceFormMode.Create
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
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.util.UUID
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalCoroutinesApi::class)
class AdditionalUrisFormViewModelTest : KoinTest {
    @get:Rule
    val koinTestRule =
        KoinTestRule.create {
            printLogger(Level.ERROR)
            modules(
                listOf(
                    module {
                        single { mock<UuidProvider>() }
                        single { mock<FormValidator<AdditionalUrisValidationInput>>() }
                        single { mock<LimitChecker>() }
                        factory { AdditionalUrisFormViewModel(get(), get(), get()) }
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

    @OptIn(ExperimentalTime::class)
    @Test
    fun `initial state should have empty values`() =
        runTest {
            val viewModel = get<AdditionalUrisFormViewModel>()

            viewModel.viewState.test {
                val state = awaitItem()
                assertThat(state.resourceFormMode).isNull()
                assertThat(state.mainUri).isEmpty()
                assertThat(state.mainUriError).isNull()
                assertThat(state.additionalUris).isEmpty()
            }
        }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `initialize should update state with provided model values`() =
        runTest {
            val uuid1 = UUID.randomUUID()
            val uuid2 = UUID.randomUUID()

            val uuidProvider = get<UuidProvider>()

            whenever(uuidProvider.get()).thenReturn(uuid1.toString(), uuid2.toString())

            val viewModel = get<AdditionalUrisFormViewModel>()
            viewModel.onIntent(
                Initialize(
                    mode = resourceFormMode,
                    additionalUris = additionalUrisModel,
                ),
            )

            viewModel.viewState.test {
                val state = awaitItem()
                assertThat(state.resourceFormMode).isEqualTo(resourceFormMode)
                assertThat(state.mainUri).isEqualTo(MAIN_URI)
                assertThat(state.additionalUris).hasSize(2)
                assertThat(state.additionalUris[uuid1]?.uri).isEqualTo(ADDITIONAL_URI_1)
                assertThat(state.additionalUris[uuid2]?.uri).isEqualTo(ADDITIONAL_URI_2)
            }
        }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `initialize should handle empty additional uris`() =
        runTest {
            val viewModel = get<AdditionalUrisFormViewModel>()
            viewModel.onIntent(
                Initialize(
                    mode = resourceFormMode,
                    additionalUris = emptyAdditionalUrisModel,
                ),
            )

            viewModel.viewState.test {
                val state = awaitItem()
                assertThat(state.resourceFormMode).isEqualTo(resourceFormMode)
                assertThat(state.mainUri).isEqualTo(MAIN_URI)
                assertThat(state.additionalUris).isEmpty()
            }
        }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `main uri changed should update state and clear error`() =
        runTest {
            val viewModel = get<AdditionalUrisFormViewModel>()
            viewModel.onIntent(
                Initialize(
                    mode = resourceFormMode,
                    additionalUris = emptyAdditionalUrisModel,
                ),
            )

            val newUri = "https://new-uri.com"
            viewModel.onIntent(MainUriChanged(newUri))

            viewModel.viewState.test {
                val state = awaitItem()
                assertThat(state.mainUri).isEqualTo(newUri)
                assertThat(state.mainUriError).isNull()
            }
        }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `main uri changed should trim whitespace`() =
        runTest {
            val viewModel = get<AdditionalUrisFormViewModel>()
            viewModel.onIntent(
                Initialize(
                    mode = resourceFormMode,
                    additionalUris = emptyAdditionalUrisModel,
                ),
            )

            viewModel.onIntent(MainUriChanged("  https://trimmed.com  "))

            viewModel.viewState.test {
                val state = awaitItem()
                assertThat(state.mainUri).isEqualTo("https://trimmed.com")
            }
        }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `additional uri changed should update specific uri and clear error`() =
        runTest {
            val uuid1 = UUID.randomUUID()
            val uuid2 = UUID.randomUUID()

            val uuidProvider = get<UuidProvider>()

            whenever(uuidProvider.get()).thenReturn(uuid1.toString(), uuid2.toString())

            val viewModel = get<AdditionalUrisFormViewModel>()
            viewModel.onIntent(
                Initialize(
                    mode = resourceFormMode,
                    additionalUris = additionalUrisModel,
                ),
            )

            val newUri = "https://updated.com"
            viewModel.onIntent(AdditionalUriChanged(uuid1, newUri))

            viewModel.viewState.test {
                val state = awaitItem()
                assertThat(state.additionalUris[uuid1]?.uri).isEqualTo(newUri)
                assertThat(state.additionalUris[uuid1]?.error).isNull()
                assertThat(state.additionalUris[uuid2]?.uri).isEqualTo(ADDITIONAL_URI_2)
            }
        }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `additional uri changed should trim whitespace`() =
        runTest {
            val uuid = UUID.randomUUID()

            val uuidProvider = get<UuidProvider>()

            whenever(uuidProvider.get()).thenReturn(uuid.toString())

            val viewModel = get<AdditionalUrisFormViewModel>()
            viewModel.onIntent(
                Initialize(
                    mode = resourceFormMode,
                    additionalUris = AdditionalUrisUiModel(MAIN_URI, listOf(ADDITIONAL_URI_1)),
                ),
            )

            viewModel.onIntent(AdditionalUriChanged(uuid, "  https://trimmed.com  "))

            viewModel.viewState.test {
                val state = awaitItem()
                assertThat(state.additionalUris[uuid]?.uri).isEqualTo("https://trimmed.com")
            }
        }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `add additional uri should add new entry when limit not reached`() =
        runTest {
            val uuid = UUID.randomUUID()
            val newUuid = UUID.randomUUID()

            val uuidProvider = get<UuidProvider>()
            val limitChecker = get<LimitChecker>()

            whenever(uuidProvider.get()).thenReturn(uuid.toString(), newUuid.toString())
            whenever(limitChecker.checkLimit(any())) doReturn LimitChecker.LimitCheckResult.CanAdd

            val viewModel = get<AdditionalUrisFormViewModel>()
            viewModel.onIntent(
                Initialize(
                    mode = resourceFormMode,
                    additionalUris = AdditionalUrisUiModel(MAIN_URI, listOf(ADDITIONAL_URI_1)),
                ),
            )

            viewModel.onIntent(AddAdditionalUri)

            viewModel.viewState.test {
                val state = awaitItem()
                assertThat(state.additionalUris).hasSize(2)
                assertThat(state.additionalUris[newUuid]?.uri).isEmpty()
            }
        }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `add additional uri should show snackbar when limit reached`() =
        runTest {
            val limitChecker = get<LimitChecker>()
            whenever(limitChecker.checkLimit(any())) doReturn LimitChecker.LimitCheckResult.LimitReached(MAX_ADDITIONAL_URIS)

            val viewModel = get<AdditionalUrisFormViewModel>()
            viewModel.onIntent(
                Initialize(
                    mode = resourceFormMode,
                    additionalUris = emptyAdditionalUrisModel,
                ),
            )

            viewModel.sideEffect.test {
                viewModel.onIntent(AddAdditionalUri)

                val effect = awaitItem()
                assertThat(effect).isInstanceOf(ShowErrorSnackbar::class.java)
                assertThat((effect as ShowErrorSnackbar).type).isEqualTo(SnackbarErrorType.MAX_URIS_EXCEEDED)
                assertThat(effect.message).isEqualTo(MAX_ADDITIONAL_URIS.toString())
            }
        }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `remove additional uri should remove entry`() =
        runTest {
            val uuid1 = UUID.randomUUID()
            val uuid2 = UUID.randomUUID()

            val uuidProvider = get<UuidProvider>()

            whenever(uuidProvider.get()).thenReturn(uuid1.toString(), uuid2.toString())

            val viewModel = get<AdditionalUrisFormViewModel>()
            viewModel.onIntent(
                Initialize(
                    mode = resourceFormMode,
                    additionalUris = additionalUrisModel,
                ),
            )

            viewModel.onIntent(RemoveAdditionalUri(uuid1))

            viewModel.viewState.test {
                val state = awaitItem()
                assertThat(state.additionalUris).hasSize(1)
                assertThat(state.additionalUris.containsKey(uuid1)).isFalse()
                assertThat(state.additionalUris[uuid2]?.uri).isEqualTo(ADDITIONAL_URI_2)
            }
        }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `apply changes should update state with validation errors when validation fails`() =
        runTest {
            val uuid = UUID.randomUUID()

            val uuidProvider = get<UuidProvider>()
            val formValidator = get<FormValidator<AdditionalUrisValidationInput>>()

            whenever(uuidProvider.get()).thenReturn(uuid.toString())
            val validationResult =
                FormValidator.ValidationResult(
                    mainUriError = "1024",
                    additionalUris = linkedMapOf(uuid to AdditionalUriItemState(ADDITIONAL_URI_1, "1024")),
                    hasErrors = true,
                )
            whenever(formValidator.validateAll(any())) doReturn validationResult

            val viewModel = get<AdditionalUrisFormViewModel>()
            viewModel.onIntent(
                Initialize(
                    mode = resourceFormMode,
                    additionalUris = AdditionalUrisUiModel(MAIN_URI, listOf(ADDITIONAL_URI_1)),
                ),
            )

            viewModel.onIntent(ApplyChanges)

            viewModel.viewState.test {
                val state = awaitItem()
                assertThat(state.mainUriError).isEqualTo("1024")
                assertThat(state.additionalUris[uuid]?.error).isEqualTo("1024")
            }
        }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `apply changes should emit side effect when validation passes`() =
        runTest {
            val uuid1 = UUID.randomUUID()

            val uuidProvider = get<UuidProvider>()
            val formValidator = get<FormValidator<AdditionalUrisValidationInput>>()

            whenever(uuidProvider.get()).thenReturn(uuid1.toString())
            val validationResult =
                FormValidator.ValidationResult(
                    mainUriError = null,
                    additionalUris = linkedMapOf(uuid1 to AdditionalUriItemState(ADDITIONAL_URI_1, null)),
                    hasErrors = false,
                )
            whenever(formValidator.validateAll(any())) doReturn validationResult

            val viewModel = get<AdditionalUrisFormViewModel>()
            viewModel.onIntent(
                Initialize(
                    mode = resourceFormMode,
                    additionalUris = AdditionalUrisUiModel(MAIN_URI, listOf(ADDITIONAL_URI_1)),
                ),
            )

            viewModel.sideEffect.test {
                viewModel.onIntent(ApplyChanges)

                val sideEffect = awaitItem()
                assertThat(sideEffect).isInstanceOf(ApplyAndGoBack::class.java)
                assertThat((sideEffect as ApplyAndGoBack).model).isEqualTo(
                    AdditionalUrisUiModel(MAIN_URI, listOf(ADDITIONAL_URI_1)),
                )
            }
        }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `go back should emit navigate up side effect`() =
        runTest {
            val viewModel = get<AdditionalUrisFormViewModel>()
            viewModel.onIntent(
                Initialize(
                    mode = resourceFormMode,
                    additionalUris = emptyAdditionalUrisModel,
                ),
            )

            viewModel.sideEffect.test {
                viewModel.onIntent(GoBack)
                assertThat(awaitItem()).isInstanceOf(NavigateUp::class.java)
            }
        }

    private companion object {
        const val MAIN_URI = "https://main.passbolt.com"
        const val ADDITIONAL_URI_1 = "https://additional1.passbolt.com"
        const val ADDITIONAL_URI_2 = "https://additional2.passbolt.com"

        val resourceFormMode =
            Create(
                leadingContentType = LeadingContentType.PASSWORD,
                parentFolderId = null,
            )

        val additionalUrisModel =
            AdditionalUrisUiModel(
                mainUri = MAIN_URI,
                additionalUris = listOf(ADDITIONAL_URI_1, ADDITIONAL_URI_2),
            )

        val emptyAdditionalUrisModel =
            AdditionalUrisUiModel(
                mainUri = MAIN_URI,
                additionalUris = emptyList(),
            )
    }
}
