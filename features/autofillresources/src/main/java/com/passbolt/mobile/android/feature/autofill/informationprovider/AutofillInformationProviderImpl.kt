package com.passbolt.mobile.android.feature.autofill.informationprovider

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.Settings
import android.view.autofill.AutofillManager
import com.passbolt.mobile.android.feature.autofill.accessibility.AccessibilityService
import com.passbolt.mobile.android.feature.autofill.informationprovider.AutofillInformationProvider.ChromeNativeAutofillStatus
import com.passbolt.mobile.android.feature.autofill.informationprovider.AutofillInformationProvider.ChromeNativeAutofillStatus.DISABLED
import com.passbolt.mobile.android.feature.autofill.informationprovider.AutofillInformationProvider.ChromeNativeAutofillStatus.ENABLED
import com.passbolt.mobile.android.feature.autofill.informationprovider.AutofillInformationProvider.ChromeNativeAutofillStatus.NOT_SUPPORTED
import com.passbolt.mobile.android.core.common.R as CommonR

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

    override fun getChromeNativeAutofillStatus(): ChromeNativeAutofillStatus {
        val chromeChannelPackage = context.getString(CommonR.string.chrome_native_autofill_target_package)
        val uri = Uri.Builder()
            .scheme(ContentResolver.SCHEME_CONTENT)
            .authority(chromeChannelPackage + CHROME_AUTOFILL_PROVIDER_NAME)
            .path(CHROME_PROVIDER_THIRD_PARTY_MODE_ACTIONS_URI_PATH)
            .build()

        return context.contentResolver.query(
            uri,
            arrayOf(CHROME_PROVIDER_THIRD_PARTY_MODE_COLUMN),
            null,
            null,
            null
        ).use { cursor ->
            if (cursor == null) {
                NOT_SUPPORTED
            } else {
                cursor.moveToFirst()
                val thirdPartyColumnIndex = cursor.getColumnIndex(CHROME_PROVIDER_THIRD_PARTY_MODE_COLUMN)

                if (0 == cursor.getInt(thirdPartyColumnIndex)) {
                    DISABLED
                } else {
                    ENABLED
                }
            }
        }
    }

    private companion object {
        // Query "Chrome Stable"
        private const val CHROME_AUTOFILL_PROVIDER_NAME = ".AutofillThirdPartyModeContentProvider"
        private const val CHROME_PROVIDER_THIRD_PARTY_MODE_COLUMN = "autofill_third_party_state"
        private const val CHROME_PROVIDER_THIRD_PARTY_MODE_ACTIONS_URI_PATH = "autofill_third_party_mode"
    }
}
