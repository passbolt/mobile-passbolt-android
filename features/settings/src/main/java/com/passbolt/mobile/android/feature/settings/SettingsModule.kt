package com.passbolt.mobile.android.feature.settings

import com.passbolt.mobile.android.feature.settings.screen.accounts.keyinspector.keyInspectorModule
import com.passbolt.mobile.android.feature.settings.screen.accounts.keyinspector.keyinspectormoremenu.keyInspectorMoreMenuModule
import com.passbolt.mobile.android.feature.settings.screen.appsettings.appSettingsModule
import com.passbolt.mobile.android.feature.settings.screen.appsettings.autofill.settingsAutofillModule
import com.passbolt.mobile.android.feature.settings.screen.appsettings.defaultfilter.defaultFilterModule
import com.passbolt.mobile.android.feature.settings.screen.appsettings.expertsettings.expertSettingsModule
import com.passbolt.mobile.android.feature.settings.screen.debuglogssettings.debugLogsSettingsModule
import com.passbolt.mobile.android.feature.settings.screen.settingsModule
import com.passbolt.mobile.android.feature.settings.screen.termsandlicenses.licenses.licensesModule
import com.passbolt.mobile.android.feature.settings.screen.termsandlicenses.termsAndLicensesSettingsModule
import org.koin.dsl.module

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

val settingsModule =
    module {
        settingsModule()

        appSettingsModule()
        debugLogsSettingsModule()
        termsAndLicensesSettingsModule()
        expertSettingsModule()

        settingsAutofillModule()
        licensesModule()
        defaultFilterModule()
        keyInspectorModule()
        keyInspectorMoreMenuModule()
    }
