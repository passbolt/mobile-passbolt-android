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

package com.passbolt.mobile.android.domain.preferences.usecase

import com.google.common.truth.Truth.assertThat
import com.passbolt.mobile.android.domain.preferences.GlobalPreferencesRepository
import com.passbolt.mobile.android.domain.preferences.GlobalPreferencesUpdate
import com.passbolt.mobile.android.ui.GlobalPreferencesUiModel
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class ApplyAutomaticPageSizeUseCaseTest {
    private val globalPreferencesRepository = mock<GlobalPreferencesRepository>()
    private val getAutomaticPageSizeUseCase = mock<GetAutomaticPageSizeUseCase>()
    private val useCase = ApplyAutomaticPageSizeUseCase(globalPreferencesRepository, getAutomaticPageSizeUseCase)

    @Test
    fun `manually set page size should not be overridden`() {
        whenever(globalPreferencesRepository.getGlobalPreferences()) doReturn
            globalPreferences(pageSize = 250, isManuallySet = true)

        useCase.execute(Unit)

        verify(globalPreferencesRepository, never()).updateGlobalPreferences(any())
    }

    @Test
    fun `automatic mode should persist the automatic page size`() {
        whenever(globalPreferencesRepository.getGlobalPreferences()) doReturn
            globalPreferences(pageSize = 2_000, isManuallySet = false)
        whenever(getAutomaticPageSizeUseCase.execute(Unit)) doReturn
            GetAutomaticPageSizeUseCase.Output(defaultPageSize = 3_000, recommendedLimit = 5_000)

        useCase.execute(Unit)

        argumentCaptor<GlobalPreferencesUpdate> {
            verify(globalPreferencesRepository).updateGlobalPreferences(capture())
            assertThat(firstValue.apiFetchPageSize).isEqualTo(3_000)
            assertThat(firstValue.isApiFetchPageSizeManuallySet).isNull()
        }
    }

    @Test
    fun `automatic mode should not write when the automatic page size is already persisted`() {
        whenever(globalPreferencesRepository.getGlobalPreferences()) doReturn
            globalPreferences(pageSize = 3_000, isManuallySet = false)
        whenever(getAutomaticPageSizeUseCase.execute(Unit)) doReturn
            GetAutomaticPageSizeUseCase.Output(defaultPageSize = 3_000, recommendedLimit = 5_000)

        useCase.execute(Unit)

        verify(globalPreferencesRepository, never()).updateGlobalPreferences(any())
    }

    private fun globalPreferences(
        pageSize: Int,
        isManuallySet: Boolean,
    ) = GlobalPreferencesUiModel(
        areDebugLogsEnabled = false,
        debugLogFileCreationDateTime = null,
        debugLogLastAppVersion = null,
        isHideRootDialogEnabled = false,
        isAuthRequiredOnEveryEntry = false,
        apiFetchPageSize = pageSize,
        isApiFetchPageSizeManuallySet = isManuallySet,
        accessibilityPoliciesConsentGiven = false,
        isCopyTotpOnAutofillEnabled = false,
    )
}
