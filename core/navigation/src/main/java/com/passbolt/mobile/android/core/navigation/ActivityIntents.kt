package com.passbolt.mobile.android.core.navigation

import android.content.Context
import android.content.Intent
import com.gaelmarhic.quadrant.Authentication
import com.gaelmarhic.quadrant.Main
import com.gaelmarhic.quadrant.Setup
import com.gaelmarhic.quadrant.Startup

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

    const val EXTRA_AUTH_STRATEGY_TYPE = "AUTH_STRATEGY_TYPE"
    const val EXTRA_AUTH_TARGET = "AUTH_TARGET"
    const val EXTRA_MANAGE_ACCOUNTS_SIGN_OUT = "MANAGE_ACCOUNTS_LOG_OUT"
    const val EXTRA_USER_ID = "EXTRA_USER_ID"

    fun setup(context: Context) = Intent().apply {
        setClassName(context, Setup.SET_UP_ACTIVITY)
    }

    fun authentication(
        context: Context,
        authenticationType: AuthenticationType,
        withSignOut: Boolean = false,
        userId: String? = null
    ) =
        Intent().apply {
            setClassName(context, Authentication.AUTHENTICATION_MAIN_ACTIVITY)
            putExtra(EXTRA_AUTH_STRATEGY_TYPE, authenticationType)
            putExtra(EXTRA_AUTH_TARGET, AuthenticationTarget.AUTHENTICATE)
            putExtra(EXTRA_MANAGE_ACCOUNTS_SIGN_OUT, withSignOut)
            userId?.let { putExtra(EXTRA_USER_ID, it) }
        }

    fun refreshAuthentication(context: Context, authenticationType: AuthenticationType) = Intent().apply {
        setClassName(context, Authentication.AUTHENTICATION_MAIN_ACTIVITY)
        putExtra(EXTRA_AUTH_TARGET, AuthenticationTarget.AUTHENTICATE)
        putExtra(EXTRA_AUTH_STRATEGY_TYPE, authenticationType)
        flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_SINGLE_TOP
    }

    fun manageAccounts(context: Context, withSignOut: Boolean = false) = Intent().apply {
        setClassName(context, Authentication.AUTHENTICATION_MAIN_ACTIVITY)
        putExtra(EXTRA_AUTH_TARGET, AuthenticationTarget.MANAGE_ACCOUNTS)
        putExtra(EXTRA_MANAGE_ACCOUNTS_SIGN_OUT, withSignOut)
    }

    fun home(context: Context) = Intent().apply {
        setClassName(context, Main.MAIN_ACTIVITY)
    }

    fun start(context: Context) = Intent().apply {
        setClassName(context, Startup.START_UP_ACTIVITY)
        addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
    }
}
