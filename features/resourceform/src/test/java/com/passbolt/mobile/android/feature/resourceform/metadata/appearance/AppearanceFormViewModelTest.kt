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

package com.passbolt.mobile.android.feature.resourceform.metadata.appearance

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.passbolt.mobile.android.feature.resourceform.metadata.appearance.AppearanceFormIntent.ApplyChanges
import com.passbolt.mobile.android.feature.resourceform.metadata.appearance.AppearanceFormIntent.SetCustomIconBackgroundColor
import com.passbolt.mobile.android.feature.resourceform.metadata.appearance.AppearanceFormIntent.SetKeepassIcon
import com.passbolt.mobile.android.feature.resourceform.metadata.appearance.AppearanceFormIntent.ToggleDefaultColor
import com.passbolt.mobile.android.feature.resourceform.metadata.appearance.AppearanceFormIntent.ToggleDefaultIcon
import com.passbolt.mobile.android.feature.resourceform.metadata.appearance.AppearanceFormSideEffect.ApplyAndGoBack
import com.passbolt.mobile.android.mappers.ResourceFormMapper
import com.passbolt.mobile.android.ui.LeadingContentType
import com.passbolt.mobile.android.ui.ResourceAppearanceModel
import com.passbolt.mobile.android.ui.ResourceAppearanceModel.Companion.DEFAULT_BACKGROUND_COLOR_HEX_STRING
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
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalCoroutinesApi::class)
class AppearanceFormViewModelTest : KoinTest {
    @get:Rule
    val koinTestRule =
        KoinTestRule.create {
            printLogger(Level.ERROR)
            modules(
                listOf(
                    module {
                        single { mock<ResourceFormMapper>() }
                        factory { AppearanceFormViewModel(get()) }
                    },
                ),
            )
        }

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var viewModel: AppearanceFormViewModel
    private val resourceFormMapper: ResourceFormMapper by lazy { get() }

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
    fun `initialize should update state with provided model values for keepass icon`() =
        runTest {
            viewModel = get()
            viewModel.onIntent(
                AppearanceFormIntent.Initialize(
                    resourceFormMode = resourceFormMode,
                    model = keepassAppearanceModel,
                ),
            )

            viewModel.viewState.test {
                val state = expectItem()
                assertThat(state.resourceFormMode).isEqualTo(resourceFormMode)
                assertThat(state.isDefaultIconChecked).isFalse()
                assertThat(state.isDefaultColorChecked).isFalse()
                assertThat(state.keepassIconValue).isEqualTo(keepassAppearanceModel.iconValue)
                assertThat(state.iconBackgroundColorHex).isEqualTo(keepassAppearanceModel.iconBackgroundHexColor)
            }
        }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `initialize should update state with provided model values for passbolt icon`() =
        runTest {
            viewModel = get()
            viewModel.onIntent(
                AppearanceFormIntent.Initialize(
                    resourceFormMode = resourceFormMode,
                    model = passboltAppearanceModel,
                ),
            )

            viewModel.viewState.test {
                val state = expectItem()
                assertThat(state.resourceFormMode).isEqualTo(resourceFormMode)
                assertThat(state.isDefaultIconChecked).isTrue()
                assertThat(state.isDefaultColorChecked).isFalse()
                assertThat(state.keepassIconValue).isEqualTo(passboltAppearanceModel.iconValue)
                assertThat(state.iconBackgroundColorHex).isEqualTo(passboltAppearanceModel.iconBackgroundHexColor)
            }
        }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `initialize should update state with provided model values for unsupported icon`() =
        runTest {
            viewModel = get()
            viewModel.onIntent(
                AppearanceFormIntent.Initialize(
                    resourceFormMode = resourceFormMode,
                    model = unsupportedAppearanceModel,
                ),
            )

            viewModel.viewState.test {
                val state = expectItem()
                assertThat(state.resourceFormMode).isEqualTo(resourceFormMode)
                assertThat(state.isDefaultIconChecked).isFalse()
                assertThat(state.isDefaultColorChecked).isFalse()
                assertThat(state.keepassIconValue).isEqualTo(unsupportedAppearanceModel.iconValue)
                assertThat(state.iconBackgroundColorHex).isEqualTo(unsupportedAppearanceModel.iconBackgroundHexColor)
            }
        }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `toggle default color should update state correctly when toggled on`() =
        runTest {
            val colorHexString = "#FF0000"

            viewModel = get()
            viewModel.onIntent(
                AppearanceFormIntent.Initialize(
                    resourceFormMode = resourceFormMode,
                    model = keepassAppearanceModel,
                ),
            )

            viewModel = get()
            viewModel.onIntent(SetCustomIconBackgroundColor(colorHexString))
            viewModel.onIntent(ToggleDefaultColor)

            viewModel.viewState.test {
                val state = expectItem()
                assertThat(state.isDefaultColorChecked).isTrue()
                assertThat(state.iconBackgroundColorHex).isEqualTo(DEFAULT_BACKGROUND_COLOR_HEX_STRING)
            }
        }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `toggle default icon should update state correctly when toggled on`() =
        runTest {
            viewModel = get()
            viewModel.onIntent(
                AppearanceFormIntent.Initialize(
                    resourceFormMode = resourceFormMode,
                    model = keepassAppearanceModel,
                ),
            )
            viewModel.onIntent(SetKeepassIcon(5))
            viewModel.onIntent(ToggleDefaultIcon)

            viewModel.viewState.test {
                val state = expectItem()
                assertThat(state.isDefaultIconChecked).isTrue()
                assertThat(state.keepassIconValue).isNull()
            }
        }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `set custom icon background color should update state properly`() =
        runTest {
            val colorHexString = "#00FF00"

            viewModel = get()
            viewModel.onIntent(
                AppearanceFormIntent.Initialize(
                    resourceFormMode = resourceFormMode,
                    model = keepassAppearanceModel,
                ),
            )
            viewModel.onIntent(SetCustomIconBackgroundColor(colorHexString))

            viewModel.viewState.test {
                val state = expectItem()
                assertThat(state.iconBackgroundColorHex).isEqualTo(colorHexString)
                assertThat(state.isDefaultColorChecked).isFalse()
            }
        }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `set keepass icon should update state properly`() =
        runTest {
            val iconValue = 10

            viewModel = get()
            viewModel.onIntent(
                AppearanceFormIntent.Initialize(
                    resourceFormMode = resourceFormMode,
                    model = keepassAppearanceModel,
                ),
            )
            viewModel.onIntent(SetKeepassIcon(iconValue))

            viewModel.viewState.test {
                val state = expectItem()
                assertThat(state.keepassIconValue).isEqualTo(iconValue)
                assertThat(state.isDefaultIconChecked).isFalse()
            }
        }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `go back should emit navigate up side effect`() =
        runTest {
            viewModel = get()
            viewModel.onIntent(
                AppearanceFormIntent.Initialize(
                    resourceFormMode = resourceFormMode,
                    model = keepassAppearanceModel,
                ),
            )

            viewModel.sideEffect.test {
                viewModel.onIntent(AppearanceFormIntent.GoBack)
                assertThat(expectItem()).isInstanceOf(AppearanceFormSideEffect.NavigateUp::class.java)
            }
        }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `apply changes should map and emit apply and go back side effect`() =
        runTest {
            val iconValue = 3
            val colorHex = "#0000FF"
            val mockAppearanceModel = mock<ResourceAppearanceModel>()

            viewModel = get()
            viewModel.onIntent(
                AppearanceFormIntent.Initialize(
                    resourceFormMode = resourceFormMode,
                    model = keepassAppearanceModel,
                ),
            )
            viewModel.onIntent(SetKeepassIcon(iconValue))
            viewModel.onIntent(SetCustomIconBackgroundColor(colorHex))

            whenever(resourceFormMapper.toAppearanceModel(iconValue, colorHex)) doReturn mockAppearanceModel

            viewModel.sideEffect.test {
                viewModel.onIntent(ApplyChanges)

                val sideEffect = expectItem()
                assertThat(sideEffect).isInstanceOf(ApplyAndGoBack::class.java)
                assertThat((sideEffect as ApplyAndGoBack).model).isEqualTo(mockAppearanceModel)

                verify(resourceFormMapper).toAppearanceModel(iconValue, colorHex)
            }
        }

    private companion object {
        val keepassAppearanceModel =
            ResourceAppearanceModel(
                iconType = ResourceAppearanceModel.ICON_TYPE_KEEPASS,
                iconBackgroundHexColor = ResourceAppearanceModel.predefinedColorHexList.first(),
                iconValue = 1,
            )
        val passboltAppearanceModel =
            ResourceAppearanceModel(
                iconType = ResourceAppearanceModel.ICON_TYPE_PASSBOLT,
                iconBackgroundHexColor = ResourceAppearanceModel.predefinedColorHexList.first(),
                iconValue = null,
            )
        val unsupportedAppearanceModel =
            ResourceAppearanceModel(
                iconType = "not existing",
                iconBackgroundHexColor = null,
                iconValue = -1,
            )
        val resourceFormMode =
            Create(
                leadingContentType = LeadingContentType.PASSWORD,
                parentFolderId = null,
            )
    }
}
