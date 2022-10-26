package com.passbolt.mobile.android.intents

import android.content.Context
import android.content.Intent
import androidx.test.platform.app.InstrumentationRegistry
import com.passbolt.mobile.android.feature.startup.StartUpActivity
import java.util.Properties

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

class ManagedAccountIntentCreator {

    private val accountProperties = Properties()

    init {
        InstrumentationRegistry.getInstrumentation()
            .context
            .resources
            .assets
            .open(ACCOUNT_PROPERTIES_FILENAME).let {
                accountProperties.load(it)
            }
    }

    fun createIntent(context: Context): Intent =
        Intent(context, StartUpActivity::class.java).apply {
            action = ACTION_MANAGED_PROFILE
            putExtra(EXTRA_USER_ID, accountProperties.getProperty(PROPERTY_USER_ID))
            putExtra(EXTRA_USERNAME, accountProperties.getProperty(PROPERTY_USERNAME))
            putExtra(EXTRA_DOMAIN, accountProperties.getProperty(PROPERTY_DOMAIN))
            putExtra(EXTRA_FIRST_NAME, accountProperties.getProperty(PROPERTY_FIRST_NAME))
            putExtra(EXTRA_LAST_NAME, accountProperties.getProperty(PROPERTY_LAST_NAME))
            putExtra(EXTRA_AVATAR_URL, accountProperties.getProperty(PROPERTY_AVATAR_URL))
            putExtra(EXTRA_KEY_FINGERPRINT, accountProperties.getProperty(PROPERTY_KEY_FINGERPRINT))
            putExtra(EXTRA_ARMORED_KEY, accountProperties.getProperty(PROPERTY_ARMORED_KEY))
        }

    fun getFirstName(): String = accountProperties.getProperty(EXTRA_FIRST_NAME)

    fun getLastName(): String = accountProperties.getProperty(EXTRA_LAST_NAME)

    fun getUsername(): String = accountProperties.getProperty(EXTRA_USERNAME)

    fun getDomain(): String = accountProperties.getProperty(EXTRA_DOMAIN)


    private companion object {
        const val ACTION_MANAGED_PROFILE = "com.passbolt.mobile.android.MANAGED_PROFILE"

        const val EXTRA_USER_ID = "USER_ID"
        const val EXTRA_USERNAME = "USERNAME"
        const val EXTRA_DOMAIN = "DOMAIN"
        const val EXTRA_FIRST_NAME = "FIRST_NAME"
        const val EXTRA_LAST_NAME = "LAST_NAME"
        const val EXTRA_AVATAR_URL = "AVATAR_URL"
        const val EXTRA_KEY_FINGERPRINT = "KEY_FINGERPRINT"
        const val EXTRA_ARMORED_KEY = "ARMORED_KEY"

        const val PROPERTY_USER_ID = "USER_ID"
        const val PROPERTY_USERNAME = "USERNAME"
        const val PROPERTY_DOMAIN = "DOMAIN"
        const val PROPERTY_FIRST_NAME = "FIRST_NAME"
        const val PROPERTY_LAST_NAME = "LAST_NAME"
        const val PROPERTY_AVATAR_URL = "AVATAR_URL"
        const val PROPERTY_KEY_FINGERPRINT = "KEY_FINGERPRINT"
        const val PROPERTY_ARMORED_KEY = "ARMORED_KEY"

        private const val ACCOUNT_PROPERTIES_FILENAME = "profile_hedy.properties"
    }
}
