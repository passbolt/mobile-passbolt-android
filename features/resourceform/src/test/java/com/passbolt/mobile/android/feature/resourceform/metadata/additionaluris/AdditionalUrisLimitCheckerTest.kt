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

import com.google.common.truth.Truth.assertThat
import com.passbolt.mobile.android.feature.resourceform.metadata.additionaluris.AdditionalUrisLimitChecker.Companion.MAX_ADDITIONAL_URIS
import com.passbolt.mobile.android.feature.resourceform.metadata.additionaluris.LimitChecker.LimitCheckResult.CanAdd
import com.passbolt.mobile.android.feature.resourceform.metadata.additionaluris.LimitChecker.LimitCheckResult.LimitReached
import org.junit.Before
import org.junit.Test

class AdditionalUrisLimitCheckerTest {
    private lateinit var limitChecker: AdditionalUrisLimitChecker

    @Before
    fun setUp() {
        limitChecker = AdditionalUrisLimitChecker()
    }

    @Test
    fun `checkLimit should return CanAdd when count is zero`() {
        val result = limitChecker.checkLimit(0)

        assertThat(result).isEqualTo(CanAdd)
    }

    @Test
    fun `checkLimit should return CanAdd when count is below max limit`() {
        val result = limitChecker.checkLimit(10)

        assertThat(result).isEqualTo(CanAdd)
    }

    @Test
    fun `checkLimit should return CanAdd when count is one below max limit`() {
        val result = limitChecker.checkLimit(MAX_ADDITIONAL_URIS - 1)

        assertThat(result).isEqualTo(CanAdd)
    }

    @Test
    fun `checkLimit should return LimitReached when count equals max limit`() {
        val result = limitChecker.checkLimit(MAX_ADDITIONAL_URIS)

        assertThat(result).isInstanceOf(LimitReached::class.java)
        assertThat((result as LimitReached).maxLimit).isEqualTo(MAX_ADDITIONAL_URIS)
    }

    @Test
    fun `checkLimit should return LimitReached when count exceeds max limit`() {
        val result = limitChecker.checkLimit(MAX_ADDITIONAL_URIS + 5)

        assertThat(result).isInstanceOf(LimitReached::class.java)
        assertThat((result as LimitReached).maxLimit).isEqualTo(MAX_ADDITIONAL_URIS)
    }
}
