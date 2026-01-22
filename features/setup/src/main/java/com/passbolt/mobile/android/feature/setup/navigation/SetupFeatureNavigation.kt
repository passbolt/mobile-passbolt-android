package com.passbolt.mobile.android.feature.setup.navigation

import PassboltTheme
import com.passbolt.mobile.android.core.navigation.compose.base.EntryProviderInstaller
import com.passbolt.mobile.android.core.navigation.compose.base.FeatureModuleNavigation
import com.passbolt.mobile.android.core.navigation.compose.keys.SetupNavigationKey.FingerprintSetup
import com.passbolt.mobile.android.core.navigation.compose.keys.SetupNavigationKey.ImportProfile
import com.passbolt.mobile.android.core.navigation.compose.keys.SetupNavigationKey.ScanQrCodes
import com.passbolt.mobile.android.core.navigation.compose.keys.SetupNavigationKey.Summary
import com.passbolt.mobile.android.core.navigation.compose.keys.SetupNavigationKey.TransferDetails
import com.passbolt.mobile.android.core.navigation.compose.keys.SetupNavigationKey.Welcome
import com.passbolt.mobile.android.feature.setup.fingerprint.FingerprintSetupScreen
import com.passbolt.mobile.android.feature.setup.importprofile.ImportProfileScreen
import com.passbolt.mobile.android.feature.setup.scanqr.ScanQrScreen
import com.passbolt.mobile.android.feature.setup.summary.SummaryScreen
import com.passbolt.mobile.android.feature.setup.transferdetails.TransferDetailsScreen
import com.passbolt.mobile.android.feature.setup.welcome.WelcomeScreen

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
class SetupFeatureNavigation : FeatureModuleNavigation {
    override fun provideEntryProviderInstaller(): EntryProviderInstaller =
        {
            entry<Welcome> {
                PassboltTheme { WelcomeScreen() }
            }
            entry<TransferDetails> {
                PassboltTheme { TransferDetailsScreen() }
            }
            entry<ScanQrCodes> {
                PassboltTheme { ScanQrScreen() }
            }
            entry<FingerprintSetup> {
                PassboltTheme { FingerprintSetupScreen() }
            }
            entry<ImportProfile> {
                PassboltTheme { ImportProfileScreen() }
            }
            entry<Summary> {
                PassboltTheme { SummaryScreen(status = it.status) }
            }
        }
}
