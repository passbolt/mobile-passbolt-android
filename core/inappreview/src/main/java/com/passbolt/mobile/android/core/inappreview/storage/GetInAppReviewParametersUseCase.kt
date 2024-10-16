package com.passbolt.mobile.android.core.inappreview.storage

import com.passbolt.mobile.android.common.usecase.UseCase
import com.passbolt.mobile.android.core.accounts.usecase.selectedaccount.GetSelectedAccountUseCase
import com.passbolt.mobile.android.encryptedstorage.EncryptedSharedPreferencesFactory
import java.time.LocalDate

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
class GetInAppReviewParametersUseCase(
    private val encryptedSharedPreferencesFactory: EncryptedSharedPreferencesFactory,
    private val getSelectedAccountUseCase: GetSelectedAccountUseCase
) : UseCase<Unit, GetInAppReviewParametersUseCase.Output> {

    override fun execute(input: Unit): Output {
        val userId = requireNotNull(getSelectedAccountUseCase.execute(Unit).selectedAccount)
        val fileName = InAppReviewFileName(userId).name
        return encryptedSharedPreferencesFactory.get("$fileName.xml").let {
            Output(
                inAppReviewShowIntervalStartDate = it.getLong(KEY_IN_APP_REVIEW_INTERVAL_START_DATE, -1)
                    .let { intervalStartEpochDaysDate ->
                        if (intervalStartEpochDaysDate == -1L) {
                            null
                        } else {
                            LocalDate.ofEpochDay(intervalStartEpochDaysDate)
                        }
                    },
                signInCount = it.getInt(KEY_SIGN_IN_COUNT, 0)
            )
        }
    }

    data class Output(
        val inAppReviewShowIntervalStartDate: LocalDate?,
        val signInCount: Int
    )
}
