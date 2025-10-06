package com.passbolt.mobile.android.feature.main.mainscreen

import com.passbolt.mobile.android.common.datarefresh.DataRefreshStatus
import com.passbolt.mobile.android.common.datarefresh.DataRefreshTrackingFlow
import com.passbolt.mobile.android.entity.featureflags.FeatureFlagsModel
import com.passbolt.mobile.android.feature.main.mainscreen.bottomnavigation.MainBottomNavigationModel
import com.passbolt.mobile.android.featureflags.usecase.GetFeatureFlagsUseCase
import junit.framework.TestCase.assertTrue
import org.junit.Rule
import org.junit.Test
import org.koin.core.component.get
import org.koin.core.logger.Level
import org.koin.test.KoinTest
import org.koin.test.KoinTestRule
import org.koin.test.inject
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import org.mockito.kotlin.times
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

class MainPresenterTest : KoinTest {
    private val presenter: MainContract.Presenter by inject()
    private val view = mock<MainContract.View>()

    @get:Rule
    val koinTestRule =
        KoinTestRule.create {
            printLogger(Level.ERROR)
            modules(testMainModule)
        }

    @Test
    fun `full data refresh should start on attach`() {
        presenter.attach(view)

        verify(view).performFullDataRefresh()
    }

    @Test
    fun `in app app update flow should be started on attach`() {
        presenter.attach(view)

        verify(view).checkForAppUpdates()
    }

    @Test
    fun `chrome native autofill should be shown when needed`() {
        whenever(mockEncouragementsInteractor.shouldShowChromeNativeAutofillEncouragement())
            .doReturn(true)
        presenter.attach(view)

        verify(view).showChromeNativeAutofillEncouragement()
    }

    @Test
    fun `in app review flow should be shown when needed`() {
        whenever(mockInAppReviewInteractor.shouldShowInAppReviewFlow())
            .doReturn(true)
        presenter.attach(view)

        verify(view).tryLaunchReviewFlow()
    }

    @Test
    fun `totp should be visible based on feature flag`() {
        mockGetFeatureFlagsUseCase.stub {
            onBlocking { execute(Unit) } doReturn
                GetFeatureFlagsUseCase.Output(
                    FeatureFlagsModel(
                        privacyPolicyUrl = null,
                        termsAndConditionsUrl = null,
                        isPreviewPasswordAvailable = false,
                        areFoldersAvailable = true,
                        areTagsAvailable = true,
                        isTotpAvailable = true,
                        isRbacAvailable = true,
                        isPasswordExpiryAvailable = true,
                        arePasswordPoliciesAvailable = true,
                        canUpdatePasswordPolicies = true,
                        isV5MetadataAvailable = false,
                    ),
                )

            presenter.attach(view)
            get<DataRefreshTrackingFlow>().updateStatus(DataRefreshStatus.Idle.FinishedWithSuccess)

            argumentCaptor<MainBottomNavigationModel> {
                verify(view, times(2)).setupBottomNavigation(capture())
                assertTrue(firstValue.isOtpTabVisible)
            }
        }
    }
}
