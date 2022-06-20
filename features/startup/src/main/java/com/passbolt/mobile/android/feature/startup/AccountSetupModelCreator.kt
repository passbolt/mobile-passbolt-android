package com.passbolt.mobile.android.feature.startup

import android.content.Intent
import com.passbolt.mobile.android.core.navigation.AccountSetupDataModel

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
class AccountSetupModelCreator {

    fun createFromIntent(intent: Intent): AccountSetupDataModel? =
        if (BuildConfig.DEBUG && intent.action == ACTION_MANAGED_PROFILE) {
            AccountSetupDataModel(
                userId = intent.getStringExtra(EXTRA_USER_ID).orEmpty(),
                userName = intent.getStringExtra(EXTRA_USERNAME).orEmpty(),
                domain = intent.getStringExtra(EXTRA_DOMAIN).orEmpty(),
                firstName = intent.getStringExtra(EXTRA_FIRST_NAME).orEmpty(),
                lastName = intent.getStringExtra(EXTRA_LAST_NAME).orEmpty(),
                avatarUrl = intent.getStringExtra(EXTRA_AVATAR_URL).orEmpty(),
                keyFingerprint = intent.getStringExtra(EXTRA_KEY_FINGERPRINT).orEmpty(),
                armoredKey = intent.getStringExtra(EXTRA_ARMORED_KEY).orEmpty()
            )
        } else {
            null
        }

    private companion object {
        private const val ACTION_MANAGED_PROFILE = "com.passbolt.mobile.android.MANAGED_PROFILE"

        private const val EXTRA_USER_ID = "USER_ID"
        private const val EXTRA_USERNAME = "USERNAME"
        private const val EXTRA_DOMAIN = "DOMAIN"
        private const val EXTRA_FIRST_NAME = "FIRST_NAME"
        private const val EXTRA_LAST_NAME = "LAST_NAME"
        private const val EXTRA_AVATAR_URL = "AVATAR_URL"
        private const val EXTRA_KEY_FINGERPRINT = "KEY_FINGERPRINT"
        private const val EXTRA_ARMORED_KEY = "ARMORED_KEY"
    }
}
