package com.passbolt.mobile.android.core.navigation

import android.content.Context
import android.content.Intent
import com.gaelmarhic.quadrant.Accountdetails
import com.gaelmarhic.quadrant.Authentication
import com.gaelmarhic.quadrant.Autofillresources
import com.gaelmarhic.quadrant.Main
import com.gaelmarhic.quadrant.Setup
import com.gaelmarhic.quadrant.Startup
import com.gaelmarhic.quadrant.Transferaccounttoanotherdevice
import java.io.Serializable

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
object ActivityIntents {

    const val EXTRA_AUTH_CONFIG = "AUTH_CONFIG"
    const val EXTRA_CONTEXT = "CONTEXT"
    const val EXTRA_USER_ID = "USER_ID"
    const val EXTRA_AUTOFILL_URI = "URI"
    const val EXTRA_AUTOFILL_MODE_NAME = "AUTOFILL_MODE"
    const val EXTRA_ACCOUNT_SETUP_DATA = "ACCOUNT_SETUP_DATA"

    fun setup(context: Context, accountSetupData: AccountSetupDataModel? = null) = Intent().apply {
        setClassName(context, Setup.SET_UP_ACTIVITY)
        putExtra(EXTRA_ACCOUNT_SETUP_DATA, accountSetupData)
    }

    fun home(context: Context) = Intent().apply {
        setClassName(context, Main.MAIN_ACTIVITY)
        flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
    }

    fun bringHome(context: Context) = Intent().apply {
        setClassName(context, Main.MAIN_ACTIVITY)
        flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
    }

    fun start(context: Context) = Intent().apply {
        setClassName(context, Startup.START_UP_ACTIVITY)
        addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    fun authentication(
        context: Context,
        authConfig: AuthConfig,
        appContext: AppContext = AppContext.APP,
        userId: String? = null
    ) =
        Intent().apply {
            setClassName(context, Authentication.AUTHENTICATION_MAIN_ACTIVITY)
            putExtra(EXTRA_AUTH_CONFIG, authConfig)
            putExtra(EXTRA_USER_ID, userId)
            putExtra(EXTRA_CONTEXT, appContext)
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }

    // have to pass the name as there is a bug in android when passing serializable
    // in an intent that is next set as PendingIntent payload (AutofillService)
    fun autofill(context: Context, autofillModeName: String, uri: String? = null) =
        Intent().apply {
            setClassName(context, Autofillresources.AUTOFILL_RESOURCES_ACTIVITY)
            putExtra(EXTRA_AUTOFILL_MODE_NAME, autofillModeName)
            uri?.let { putExtra(EXTRA_AUTOFILL_URI, uri) }
        }

    fun autofillReorderToFront(context: Context) =
        Intent().apply {
            setClassName(context, Autofillresources.AUTOFILL_RESOURCES_ACTIVITY)
            flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

    fun accountDetails(context: Context) =
        Intent().apply {
            setClassName(context, Accountdetails.ACCOUNT_DETAILS_ACTIVITY)
        }

    fun transferAccountToAnotherDevice(context: Context) =
        Intent().apply {
            setClassName(context, Transferaccounttoanotherdevice.TRANSFER_ACCOUNT_TO_ANOTHER_DEVICE_ACTIVITY)
        }

    sealed class AuthConfig : Serializable {
        object Startup : AuthConfig()
        object Setup : AuthConfig()
        object ManageAccount : AuthConfig()

        // refreshes backend session and passphrase
        object SignIn : AuthConfig()

        // refreshes only passphrase
        object RefreshPassphrase : AuthConfig()

        // tries to refresh background session and if fails fallbacks to SignIn
        object RefreshSession : AuthConfig()

        class Mfa(
            val provider: String
        ) : AuthConfig()
    }
}
