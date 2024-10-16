package com.passbolt.mobile.android.feature.settings.appsettings.expertsettings

import com.passbolt.mobile.android.core.preferences.usecase.GetGlobalPreferencesUseCase
import com.passbolt.mobile.android.feature.settings.screen.appsettings.expertsettings.ExpertSettingsContract
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

class ExpertSettingsPresenterTest : KoinTest {

    private val presenter: ExpertSettingsContract.Presenter by inject()
    private val view = mock<ExpertSettingsContract.View>()

    @get:Rule
    val koinTestRule = KoinTestRule.create {
        printLogger(Level.ERROR)
        modules(testExpertSettingsModule)
    }

    @Test
    fun `turning developer settings off should disable and turn off all developer settings`() {
        whenever(mockGetGlobalPreferencesUseCase.execute(Unit))
            .doReturn(
                GetGlobalPreferencesUseCase.Output(
                    areDebugLogsEnabled = false, debugLogFileCreationDateTime = null,
                    isDeveloperModeEnabled = true, isHideRootDialogEnabled = true
                )
            )

        presenter.attach(view)
        presenter.onDeveloperModeStateChanged(isDeveloperModeEnabled = false)

        verify(view).disableHideRootSwitch()
        verify(view).setHideRootDialogSwitchToOff()
    }

    @Test
    fun `turning developer settings on should enable all developer settings`() {
        whenever(mockGetGlobalPreferencesUseCase.execute(Unit))
            .doReturn(
                GetGlobalPreferencesUseCase.Output(
                    areDebugLogsEnabled = false, debugLogFileCreationDateTime = null,
                    isDeveloperModeEnabled = false, isHideRootDialogEnabled = false
                )
            )

        presenter.attach(view)
        presenter.onDeveloperModeStateChanged(isDeveloperModeEnabled = true)

        verify(view).enableHideRootSwitch()
    }

    @Test
    fun `settings switches should have correct initial position for off settings`() {
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

        verify(view).setDeveloperModeSwitchToOff()
        verify(view).setHideRootDialogSwitchToOff()
    }

    @Test
    fun `settings switches should have correct initial position for on settings`() {
        whenever(mockGetGlobalPreferencesUseCase.execute(Unit))
            .doReturn(
                GetGlobalPreferencesUseCase.Output(
                    areDebugLogsEnabled = false,
                    debugLogFileCreationDateTime = null,
                    isDeveloperModeEnabled = true,
                    isHideRootDialogEnabled = true
                )
            )

        presenter.attach(view)

        verify(view).setDeveloperModeSwitchToOn()
        verify(view).enableHideRootSwitch()
        verify(view).setHideRootDialogSwitchToOn()
    }
}
