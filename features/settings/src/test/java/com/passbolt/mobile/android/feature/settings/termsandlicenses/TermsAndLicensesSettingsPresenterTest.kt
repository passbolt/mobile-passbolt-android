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

package com.passbolt.mobile.android.feature.settings.termsandlicenses

import com.passbolt.mobile.android.entity.featureflags.FeatureFlagsModel
import com.passbolt.mobile.android.feature.settings.screen.termsandlicenses.TermsAndLicensesSettingsContract
import com.passbolt.mobile.android.featureflags.usecase.GetFeatureFlagsUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.koin.core.logger.Level
import org.koin.test.KoinTest
import org.koin.test.KoinTestRule
import org.koin.test.inject
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify


@ExperimentalCoroutinesApi
class TermsAndLicensesSettingsPresenterTest : KoinTest {

    private val presenter: TermsAndLicensesSettingsContract.Presenter by inject()
    private val view = mock<TermsAndLicensesSettingsContract.View>()

    @get:Rule
    val koinTestRule = KoinTestRule.create {
        printLogger(Level.ERROR)
        modules(testTermsAndLicensesSettingsModule)
    }

    @Before
    fun setup() {
        getFeatureFlagsUseCase.stub {
            onBlocking { execute(Unit) }.doReturn(
                GetFeatureFlagsUseCase.Output(
                    FeatureFlagsModel(
                        URL_PRIVACY_POLICY,
                        URL_TERMS,
                        isPreviewPasswordAvailable = true,
                        areFoldersAvailable = false,
                        areTagsAvailable = false,
                        isTotpAvailable = true,
                        isRbacAvailable = true,
                        isPasswordExpiryAvailable = true,
                        arePasswordPoliciesAvailable = true,
                        canUpdatePasswordPolicies = true,
                        isV5MetadataAvailable = false
                    )
                )
            )
        }
    }

    @Test
    fun `privacy policy and terms should hide if urls not provided`() {
        getFeatureFlagsUseCase.stub {
            onBlocking { execute(Unit) }.doReturn(
                GetFeatureFlagsUseCase.Output(
                    FeatureFlagsModel(
                        privacyPolicyUrl = null,
                        termsAndConditionsUrl = null,
                        isPreviewPasswordAvailable = true,
                        areFoldersAvailable = false,
                        areTagsAvailable = false,
                        isTotpAvailable = true,
                        isRbacAvailable = true,
                        isPasswordExpiryAvailable = true,
                        arePasswordPoliciesAvailable = true,
                        canUpdatePasswordPolicies = true,
                        isV5MetadataAvailable = false
                    )
                )
            )
        }

        presenter.attach(view)

        verify(view).disablePrivacyPolicySetting()
        verify(view).disableTermsAndConditionsSetting()
    }

    @Test
    fun `privacy policy and terms click should navigate to privacy policy website`() {
        presenter.attach(view)
        presenter.privacyPolicyClick()

        verify(view).openUrl(URL_PRIVACY_POLICY)
    }

    @Test
    fun `terms clicked should navigate to terms website`() {
        presenter.attach(view)
        presenter.termsAndConditionsClick()

        verify(view).openUrl(URL_TERMS)
    }

    @Test
    fun `refreshing feature flags should cause UI refresh`() {
        val featureFlags = FeatureFlagsModel(
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
            isV5MetadataAvailable = false
        )
        getFeatureFlagsUseCase.stub {
            onBlocking { execute(Unit) }.doReturn(
                GetFeatureFlagsUseCase.Output(featureFlags)
            )
        }

        presenter.attach(view)

        verify(view).disablePrivacyPolicySetting()
        verify(view).disableTermsAndConditionsSetting()

        getFeatureFlagsUseCase.apply {
            reset(this)
            stub {
                onBlocking { execute(Unit) }.doReturn(
                    GetFeatureFlagsUseCase.Output(
                        featureFlags.copy(
                            privacyPolicyUrl = URL_PRIVACY_POLICY,
                            termsAndConditionsUrl = URL_TERMS
                        )
                    )
                )
            }
        }
        presenter.viewResumed()

        verify(view).enablePrivacyPolicySetting()
        verify(view).enableTermsAndConditionsButton()
    }

    private companion object {
        private const val URL_PRIVACY_POLICY = "url_privacy"
        private const val URL_TERMS = "url_terms"
    }
}
