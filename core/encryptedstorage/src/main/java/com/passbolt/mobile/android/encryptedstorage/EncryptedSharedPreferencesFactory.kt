package com.passbolt.mobile.android.encryptedstorage

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import timber.log.Timber
import java.util.concurrent.ConcurrentHashMap

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

class EncryptedSharedPreferencesFactory internal constructor(
    private val context: Context,
    private val masterKey: MasterKey,
) {
    // EncryptedSharedPreferences.create() unwraps the master key through the
    // Android Keystore (a binder round-trip) and builds a Tink keyset manager
    // every time it is called. SharedPreferences instances are process-wide
    // singletons and thread-safe, so build each file once and hand out the same
    // instance afterwards. The auth and base-url interceptors read preferences
    // on every HTTP request, so without this cache every request paid for
    // several Keystore round-trips before it was even sent.
    private val cache = ConcurrentHashMap<String, SharedPreferences>()

    fun get(fileName: String): SharedPreferences =
        cache.getOrPut(fileName) {
            try {
                EncryptedSharedPreferences.create(
                    context,
                    fileName,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
                )
            } catch (e: Exception) {
                Timber.e(e, "Failed to open encrypted preferences")
                throw e
            }
        }
}
