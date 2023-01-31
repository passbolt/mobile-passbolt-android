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

package com.passbolt.mobile.android.feature.settings

import com.passbolt.mobile.android.feature.settings.screen.SettingsContract
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.koin.core.logger.Level
import org.koin.test.KoinTest
import org.koin.test.KoinTestRule
import org.koin.test.inject
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

@ExperimentalCoroutinesApi
class SettingsPresenterTest : KoinTest {

    private val presenter: SettingsContract.Presenter by inject()
    private val view = mock<SettingsContract.View>()

    @get:Rule
    val koinTestRule = KoinTestRule.create {
        printLogger(Level.ERROR)
        modules(testModule)
    }

    @Before
    fun setup() {
        presenter.attach(view)
    }


    @Test
    fun `navigation should work correct`() {
        presenter.appSettingsClick()
        presenter.accountsSettingsClick()
        presenter.debugLogsSettingsClick()
        presenter.termsAndLicensesClick()

        verify(view).navigateToAppSettings()
        verify(view).navigateAccountsSettings()
        verify(view).navigateToDebugLogsSettings()
        verify(view).navigateToTermsAndLicensesSettings()
        verify(view).navigateToTermsAndLicensesSettings()
    }

    @Test
    fun `sign out flow should work correct`() {
        presenter.signOutClick()
        presenter.logoutConfirmed()

        verify(view).showLogoutDialog()
        verify(view).showProgress()
        verify(view).hideProgress()
        verify(view).navigateToSignInWithLogout()
    }
}
