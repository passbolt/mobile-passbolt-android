package com.passbolt.mobile.android.common

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import timber.log.Timber
import com.passbolt.mobile.android.core.common.R as CommonR
import com.passbolt.mobile.android.core.localization.R as LocalizationR

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
class ExternalDeeplinkHandler {

    fun openWebsite(context: Context, url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))

        runCatching {
            context.startActivity(intent)
        }
            .onFailure {
                Timber.e(it)
                Toast.makeText(context, LocalizationR.string.common_failure, Toast.LENGTH_SHORT).show()
            }
    }

    fun openChromeNativeAutofillSettings(context: Context) {
        val autofillSettingsIntent = Intent(Intent.ACTION_APPLICATION_PREFERENCES).apply {
            addCategory(Intent.CATEGORY_DEFAULT)
            addCategory(Intent.CATEGORY_APP_BROWSER)
            addCategory(Intent.CATEGORY_PREFERENCE)
            setPackage(context.getString(CommonR.string.chrome_native_autofill_target_package))
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }

        runCatching {
            context.startActivity(autofillSettingsIntent)
        }
            .onFailure {
                Timber.e(it)
                Toast.makeText(context, LocalizationR.string.common_failure, Toast.LENGTH_SHORT).show()
            }
    }
}
