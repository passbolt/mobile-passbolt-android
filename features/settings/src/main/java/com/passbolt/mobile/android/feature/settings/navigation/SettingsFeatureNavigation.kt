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
package com.passbolt.mobile.android.feature.settings.navigation

import PassboltTheme
import androidx.navigation3.runtime.entry
import com.passbolt.mobile.android.core.navigation.compose.base.EntryProviderInstaller
import com.passbolt.mobile.android.core.navigation.compose.base.FeatureModuleNavigation
import com.passbolt.mobile.android.core.navigation.compose.keys.SettingsNavigationKey
import com.passbolt.mobile.android.core.navigation.compose.keys.SettingsNavigationKey.SettingsMain
import com.passbolt.mobile.android.feature.settings.screen.SettingsScreen
import com.passbolt.mobile.android.feature.settings.screen.accounts.AccountsSettingsScreen
import com.passbolt.mobile.android.feature.settings.screen.accounts.keyinspector.KeyInspectorScreen
import com.passbolt.mobile.android.feature.settings.screen.appsettings.AppSettingsScreen
import com.passbolt.mobile.android.feature.settings.screen.appsettings.autofill.AutofillSettingsScreen
import com.passbolt.mobile.android.feature.settings.screen.appsettings.defaultfilter.DefaultFilterScreen
import com.passbolt.mobile.android.feature.settings.screen.appsettings.expertsettings.ExpertSettingsScreen
import com.passbolt.mobile.android.feature.settings.screen.debuglogssettings.DebugLogsSettingsScreen
import com.passbolt.mobile.android.feature.settings.screen.termsandlicenses.TermsAndLicensesScreen
import com.passbolt.mobile.android.feature.settings.screen.termsandlicenses.licenses.LicensesScreen

class SettingsFeatureNavigation : FeatureModuleNavigation {
    override fun provideEntryProviderInstaller(): EntryProviderInstaller =
        {
            entry<SettingsNavigationKey.Accounts> {
                PassboltTheme { AccountsSettingsScreen() }
            }
            entry<SettingsNavigationKey.AppSettings> {
                PassboltTheme { AppSettingsScreen() }
            }
            entry<SettingsNavigationKey.DebugLogs> {
                PassboltTheme { DebugLogsSettingsScreen() }
            }
            entry<SettingsMain> {
                PassboltTheme { SettingsScreen() }
            }
            entry<SettingsNavigationKey.TermsAndLicenses> {
                PassboltTheme { TermsAndLicensesScreen() }
            }
            entry<SettingsNavigationKey.KeyInspector> {
                PassboltTheme { KeyInspectorScreen() }
            }
            entry<SettingsNavigationKey.OpenSourceLicences> {
                PassboltTheme { LicensesScreen() }
            }
            entry<SettingsNavigationKey.Autofill> {
                PassboltTheme { AutofillSettingsScreen() }
            }
            entry<SettingsNavigationKey.DefaultFilter> {
                PassboltTheme { DefaultFilterScreen() }
            }
            entry<SettingsNavigationKey.ExpertSettings> {
                PassboltTheme { ExpertSettingsScreen() }
            }
        }
}
