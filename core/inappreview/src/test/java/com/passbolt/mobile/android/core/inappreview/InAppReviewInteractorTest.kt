package com.passbolt.mobile.android.core.inappreview

import com.google.common.truth.Truth.assertThat
import com.passbolt.mobile.android.core.inappreview.storage.GetInAppReviewParametersUseCase
import com.passbolt.mobile.android.core.inappreview.storage.GetInAppReviewShowModeUseCase
import com.passbolt.mobile.android.core.inappreview.storage.SaveInAppReviewParametersUseCase
import com.passbolt.mobile.android.core.inappreview.storage.SaveInAppShowModeUseCase
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.koin.core.logger.Level
import org.koin.test.KoinTest
import org.koin.test.KoinTestRule
import org.koin.test.inject
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.LocalDate
import kotlin.test.assertTrue

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

class InAppReviewInteractorTest : KoinTest {

    private val inAppReviewInteractor: InAppReviewInteractor by inject()

    @get:Rule
    val koinTestRule = KoinTestRule.create {
        printLogger(Level.ERROR)
        modules(testInAppReviewModule)
    }

    @After
    fun tearDown() {
        reset(
            getInAppReviewParametersUseCase,
            getInAppReviewShowModeUseCase,
            saveInAppReviewParametersUseCase,
            saveInAppShowModeUseCase
        )
    }

    @Test
    fun `sign in should increase sign in count and set date if not already set`() {
        whenever(getInAppReviewParametersUseCase.execute(Unit))
            .doReturn(GetInAppReviewParametersUseCase.Output(null, 0))

        inAppReviewInteractor.processSuccessfulSignIn()

        argumentCaptor<SaveInAppReviewParametersUseCase.Input> {
            verify(saveInAppReviewParametersUseCase).execute(capture())
            assertThat(firstValue.signInCount).isEqualTo(1)
            assertThat(firstValue.inAppReviewShowIntervalStartDate).isNotNull()
            firstValue.inAppReviewShowIntervalStartDate!!.isEqual(LocalDate.now(clockBaseTime))
        }
    }

    @Test
    fun `app review show should reset review parameters and change intervals to consecutive show`() {
        inAppReviewInteractor.inAppReviewFlowShowed()

        argumentCaptor<SaveInAppShowModeUseCase.Input> {
            verify(saveInAppShowModeUseCase).execute(capture())
            assertThat(firstValue.inAppReviewShowMode).isInstanceOf(InAppReviewShowMode.ConsecutiveShow::class.java)
        }
        argumentCaptor<SaveInAppReviewParametersUseCase.Input> {
            verify(saveInAppReviewParametersUseCase).execute(capture())
            assertThat(firstValue.signInCount).isEqualTo(0)
            assertThat(firstValue.inAppReviewShowIntervalStartDate).isNull()
        }
    }

    @Test
    fun `first app review show should be showed when parameters are met`() {
        val parameters = InAppReviewShowMode.FirstShow()
        val passedDate = LocalDate.now(clockBaseTime).minusDays(parameters.daysCount + 1L)
        val passedSignInCount = parameters.signInCount + 1
        whenever(getInAppReviewShowModeUseCase.execute(Unit))
            .doReturn(GetInAppReviewShowModeUseCase.Output(parameters))
        whenever(getInAppReviewParametersUseCase.execute(Unit))
            .doReturn(GetInAppReviewParametersUseCase.Output(passedDate, passedSignInCount))

        assertTrue(inAppReviewInteractor.shouldShowInAppReviewFlow())
    }
}
