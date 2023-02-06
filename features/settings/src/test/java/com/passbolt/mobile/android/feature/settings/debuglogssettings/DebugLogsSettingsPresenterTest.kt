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

package com.passbolt.mobile.android.feature.settings.debuglogssettings

import com.passbolt.mobile.android.feature.settings.screen.debuglogssettings.DebugLogsSettingsContract
import com.passbolt.mobile.android.storage.usecase.preferences.GetGlobalPreferencesUseCase
import org.junit.Rule
import org.junit.Test
import org.koin.core.logger.Level
import org.koin.test.KoinTest
import org.koin.test.KoinTestRule
import org.koin.test.inject
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.LocalDateTime


class DebugLogsSettingsPresenterTest : KoinTest {

    private val presenter: DebugLogsSettingsContract.Presenter by inject()
    private val view = mock<DebugLogsSettingsContract.View>()

    @get:Rule
    val koinTestRule = KoinTestRule.create {
        printLogger(Level.ERROR)
        modules(testDebugLogsModule)
    }

    @Test
    fun `view should have correct state when debug logs are on`() {
        whenever(mockGetGlobalPreferencesUseCase.execute(Unit))
            .doReturn(
                GetGlobalPreferencesUseCase.Output(
                    areDebugLogsEnabled = true,
                    debugLogFileCreationDateTime = LocalDateTime.now(),
                    isDeveloperModeEnabled = false,
                    isHideRootDialogEnabled = false
                )
            )

        presenter.attach(view)

        verify(view).setEnableLogsSwitchOn()
        verify(view).enableAccessLogs()
    }

    @Test
    fun `view should have correct state when debug logs are off`() {
        whenever(mockGetGlobalPreferencesUseCase.execute(Unit))
            .doReturn(
                GetGlobalPreferencesUseCase.Output(
                    areDebugLogsEnabled = false,
                    debugLogFileCreationDateTime = null,
                    isDeveloperModeEnabled = false,
                    isHideRootDialogEnabled = false
                )
            )

        presenter.attach(view)

        verify(view).setEnableLogsSwitchOff()
        verify(view).disableAccessLogs()
    }
}
