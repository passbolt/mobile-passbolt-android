package com.passbolt.mobile.android.feature.autofill.informationprovider

import android.content.Context
import android.provider.Settings
import android.view.autofill.AutofillManager
import com.passbolt.mobile.android.feature.autofill.accessibility.AccessibilityService

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

class AutofillInformationProviderImpl(
    private val autofillManager: AutofillManager,
    private val context: Context
) : AutofillInformationProvider {

    override fun isAutofillServiceSupported() = autofillManager.isAutofillSupported

    override fun isPassboltAutofillServiceSet() = autofillManager.hasEnabledAutofillServices()

    override fun isAccessibilityOverlayEnabled() = Settings.canDrawOverlays(context)

    override fun isAccessibilityServiceEnabled(): Boolean {
        val prefString =
            Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)

        return prefString != null && prefString.contains(
            context.packageName + "/" + AccessibilityService::class.java.name
        )
    }

    override fun isAccessibilityAutofillSetup() =
        isAccessibilityOverlayEnabled() && isAccessibilityServiceEnabled()
}
