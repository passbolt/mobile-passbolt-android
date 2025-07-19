package com.passbolt.mobile.android.feature.settings.termsandlicenses

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
import com.passbolt.mobile.android.entity.featureflags.FeatureFlagsModel
import com.passbolt.mobile.android.feature.settings.screen.termsandlicenses.TermsAndLicensesSettingsViewModel
import com.passbolt.mobile.android.featureflags.usecase.GetFeatureFlagsUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
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
class TermsAndLicensesSettingsViewModelTest : KoinTest {
    @get:Rule
    val koinTestRule =
        KoinTestRule.create {
            printLogger(Level.ERROR)
            modules(
                listOf(
                    module {
                        single { mock<GetFeatureFlagsUseCase>() }
                        factoryOf(::TermsAndLicensesSettingsViewModel)
                    },
                ),
            )
        }

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var viewModel: TermsAndLicensesSettingsViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state should be correct for absent policy and terms url`() =
        runTest {
            val getFeatureFlagsUseCase: GetFeatureFlagsUseCase = get()
            whenever(getFeatureFlagsUseCase.execute(Unit)) doReturn
                GetFeatureFlagsUseCase.Output(
                    FeatureFlagsModel(
                        null,
                        null,
                        isPreviewPasswordAvailable = true,
                        areFoldersAvailable = false,
                        areTagsAvailable = false,
                        isTotpAvailable = true,
                        isRbacAvailable = true,
                        isPasswordExpiryAvailable = true,
                        arePasswordPoliciesAvailable = true,
                        canUpdatePasswordPolicies = true,
                        isV5MetadataAvailable = false,
                    ),
                )

            viewModel = get()

            val state = viewModel.viewState.value

            assertThat(state.isPrivacyPolicyEnabled).isFalse()
            assertThat(state.isTermsAndConditionsEnabled).isFalse()
            assertThat(state.privacyPolicyUrl).isNull()
            assertThat(state.termsAndConditionsUrl).isNull()
        }

    @Test
    fun `initial state should be correct for present policy and terms url`() =
        runTest {
            val getFeatureFlagsUseCase: GetFeatureFlagsUseCase = get()
            whenever(getFeatureFlagsUseCase.execute(Unit)) doReturn
                GetFeatureFlagsUseCase.Output(
                    FeatureFlagsModel(
                        privacyPolicyUrl = URL_PRIVACY_POLICY,
                        termsAndConditionsUrl = URL_TERMS,
                        isPreviewPasswordAvailable = true,
                        areFoldersAvailable = false,
                        areTagsAvailable = false,
                        isTotpAvailable = true,
                        isRbacAvailable = true,
                        isPasswordExpiryAvailable = true,
                        arePasswordPoliciesAvailable = true,
                        canUpdatePasswordPolicies = true,
                        isV5MetadataAvailable = false,
                    ),
                )

            viewModel = get()

            val state = viewModel.viewState.drop(1).first()

            assertThat(state.isPrivacyPolicyEnabled).isTrue()
            assertThat(state.isTermsAndConditionsEnabled).isTrue()
            assertThat(state.privacyPolicyUrl).isEqualTo(URL_PRIVACY_POLICY)
            assertThat(state.termsAndConditionsUrl).isEqualTo(URL_TERMS)
        }

    private companion object {
        private const val URL_PRIVACY_POLICY = "url_privacy"
        private const val URL_TERMS = "url_terms"
    }
}
