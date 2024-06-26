package com.passbolt.mobile.android.pwnedpasswordsapi.range

import retrofit2.http.GET
import retrofit2.http.Path

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
interface PwnedPasswordsApi {
    /**
    @param passwordPartialHash HIBP requires the first 5 characters of the SHA-1 hash of the password
    to be sent to the API. See https://haveibeenpwned.com/API/v3#PwnedPasswords for more information.
    @return The response body contains the suffixes of the SHA-1 hashes of the passwords that have been compromised.
     **/
    @GET(PWNED_PASSWORDS_BY_RANGE)
    suspend fun getPwnedPasswordsSuffixes(
        @Path(PATH_PASSWORD_PARTIAL_HASH) passwordPartialHash: String
    ): String

    companion object {
        const val PARTIAL_HASH_LENGTH = 5

        private const val PATH_PASSWORD_PARTIAL_HASH = "passwordPartialHash"
        private const val PWNED_PASSWORDS_BY_RANGE = "/range/{$PATH_PASSWORD_PARTIAL_HASH}"
    }
}
