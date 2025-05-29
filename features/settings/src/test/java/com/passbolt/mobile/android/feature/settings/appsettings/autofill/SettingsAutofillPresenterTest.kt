package com.passbolt.mobile.android.feature.settings.appsettings.autofill

import com.passbolt.mobile.android.feature.autofill.informationprovider.AutofillInformationProvider.ChromeNativeAutofillStatus.DISABLED
import com.passbolt.mobile.android.feature.autofill.informationprovider.AutofillInformationProvider.ChromeNativeAutofillStatus.ENABLED
import com.passbolt.mobile.android.feature.autofill.informationprovider.AutofillInformationProvider.ChromeNativeAutofillStatus.NOT_SUPPORTED
import com.passbolt.mobile.android.feature.settings.screen.appsettings.autofill.SettingsAutofillContract
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

class SettingsAutofillPresenterTest : KoinTest {
    private val presenter: SettingsAutofillContract.Presenter by inject()
    private val view = mock<SettingsAutofillContract.View>()

    @get:Rule
    val koinTestRule =
        KoinTestRule.create {
            printLogger(Level.ERROR)
            modules(testSettingsAutofillModule)
        }

    @Test
    fun `view should show info message when autofill is not supported`() {
        whenever(mockAutofillInformationProvider.isAutofillServiceSupported())
            .doReturn(false)
        whenever(mockAutofillInformationProvider.getChromeNativeAutofillStatus())
            .doReturn(NOT_SUPPORTED)

        presenter.attach(view)
        presenter.autofillServiceSwitchClick()

        verify(view).setAutofillSwitchOff()
        verify(view).showAutofillServiceNotSupported()
        verify(view).disableChromeNativeAutofillLayout()
        verify(view).setChromeNativeAutofillSwitchOff()
        verify(view).showChromeNativeAutofillNotSupported()
    }

    @Test
    fun `view should set correct switches on state`() {
        whenever(mockAutofillInformationProvider.isAutofillServiceSupported())
            .doReturn(true)
        whenever(mockAutofillInformationProvider.isAccessibilityAutofillSetup())
            .doReturn(true)
        whenever(mockAutofillInformationProvider.isPassboltAutofillServiceSet())
            .doReturn(true)
        whenever(mockAutofillInformationProvider.getChromeNativeAutofillStatus())
            .doReturn(ENABLED)

        presenter.attach(view)

        verify(view).setAutofillSwitchOn()
        verify(view).setAccessibilitySwitchOn()
        verify(view).enableChromeNativeAutofillLayout()
        verify(view).setChromeNativeAutofillSwitchOn()
    }

    @Test
    fun `view should set correct switches off state`() {
        whenever(mockAutofillInformationProvider.isAutofillServiceSupported())
            .doReturn(false)
        whenever(mockAutofillInformationProvider.isAccessibilityAutofillSetup())
            .doReturn(false)
        whenever(mockAutofillInformationProvider.isPassboltAutofillServiceSet())
            .doReturn(false)
        whenever(mockAutofillInformationProvider.getChromeNativeAutofillStatus())
            .doReturn(DISABLED)

        presenter.attach(view)

        verify(view).setAutofillSwitchOff()
        verify(view).setAccessibilitySwitchOff()
        verify(view).setChromeNativeAutofillSwitchOff()
    }
}
