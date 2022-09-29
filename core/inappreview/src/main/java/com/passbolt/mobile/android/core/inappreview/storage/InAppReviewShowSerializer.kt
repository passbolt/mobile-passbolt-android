package com.passbolt.mobile.android.core.inappreview.storage

import com.passbolt.mobile.android.core.inappreview.InAppReviewShowMode

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

class InAppReviewShowSerializer {

    fun serialize(model: InAppReviewShowMode) =
        when (model) {
            is InAppReviewShowMode.ConsecutiveShow -> InAppReviewShowModeEnum.CONSECUTIVE_SHOW.ordinal
            is InAppReviewShowMode.FirstShow -> InAppReviewShowModeEnum.FIRST_SHOW.ordinal
        }

    fun deserialize(ordinal: Int) =
        when (InAppReviewShowModeEnum.values()[ordinal]) {
            InAppReviewShowModeEnum.FIRST_SHOW -> InAppReviewShowMode.FirstShow()
            InAppReviewShowModeEnum.CONSECUTIVE_SHOW -> InAppReviewShowMode.ConsecutiveShow()
        }

    enum class InAppReviewShowModeEnum {
        FIRST_SHOW,
        CONSECUTIVE_SHOW
    }
}
