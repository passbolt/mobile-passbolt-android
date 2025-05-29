package com.passbolt.mobile.android.core.inappreview

import com.passbolt.mobile.android.core.inappreview.storage.GetInAppReviewParametersUseCase
import com.passbolt.mobile.android.core.inappreview.storage.GetInAppReviewShowModeUseCase
import com.passbolt.mobile.android.core.inappreview.storage.SaveInAppReviewParametersUseCase
import com.passbolt.mobile.android.core.inappreview.storage.SaveInAppShowModeUseCase
import timber.log.Timber
import java.time.Clock
import java.time.LocalDate
import java.time.Period

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

class InAppReviewInteractor(
    private val getInAppReviewParametersUseCase: GetInAppReviewParametersUseCase,
    private val getInAppReviewShowModeUseCase: GetInAppReviewShowModeUseCase,
    private val saveInAppShowModeUseCase: SaveInAppShowModeUseCase,
    private val saveInAppReviewParametersUseCase: SaveInAppReviewParametersUseCase,
    private val clock: Clock,
) {
    fun shouldShowInAppReviewFlow(): Boolean {
        getInAppReviewParametersUseCase.execute(Unit).let {
            val showMode = getInAppReviewShowModeUseCase.execute(Unit).inAppReviewShowMode
            val minimumIntervalPassed =
                it.inAppReviewShowIntervalStartDate != null &&
                    Period
                        .between(
                            it.inAppReviewShowIntervalStartDate,
                            LocalDate.now(clock),
                        ).days > showMode.daysCount
            val minimumSignInsPassed = it.signInCount > showMode.signInCount

            Timber.d(
                "Checking in app review show parameters. " +
                    "Show mode is: %s. " +
                    "Show interval start date: %s. " +
                    "Sign in count is: %d. " +
                    "Should show review: %s",
                showMode.javaClass.simpleName,
                it.inAppReviewShowIntervalStartDate,
                it.signInCount,
                (minimumSignInsPassed && minimumIntervalPassed).toString(),
            )

            return minimumSignInsPassed && minimumIntervalPassed
        }
    }

    fun inAppReviewFlowShowed() {
        saveInAppShowModeUseCase.execute(
            SaveInAppShowModeUseCase.Input(
                InAppReviewShowMode.ConsecutiveShow(),
            ),
        )
        saveInAppReviewParametersUseCase.execute(
            SaveInAppReviewParametersUseCase.Input(
                inAppReviewShowIntervalStartDate = null,
                signInCount = 0,
            ),
        )
    }

    fun processSuccessfulSignIn() {
        val currentParameters = getInAppReviewParametersUseCase.execute(Unit)
        val newSignInCount = currentParameters.signInCount + 1
        val newStartIntervalDate = currentParameters.inAppReviewShowIntervalStartDate ?: LocalDate.now(clock)

        saveInAppReviewParametersUseCase.execute(
            SaveInAppReviewParametersUseCase.Input(
                inAppReviewShowIntervalStartDate = newStartIntervalDate,
                signInCount = newSignInCount,
            ),
        )
    }
}
