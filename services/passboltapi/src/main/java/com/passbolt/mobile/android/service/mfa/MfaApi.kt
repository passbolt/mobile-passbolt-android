package com.passbolt.mobile.android.service.mfa

import com.passbolt.mobile.android.dto.request.HotpRequest
import com.passbolt.mobile.android.dto.request.TotpRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

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

internal interface MfaApi {

    @POST(MFA_VERIFICATION_TOTP)
    suspend fun verifyTotp(
        @Body totpRequest: TotpRequest,
        // auth header needs to be added manually because this request
        // requires auth token and is before user is signed in
        @Header(MFA_AUTH_HEADER) authHeader: String
    ): Response<Void>

    @POST(MFA_VERIFICATION_YUBIKEY)
    suspend fun verifyYubikeyOtp(
        @Body hotpRequest: HotpRequest,
        // auth header needs to be added manually because this request
        // requires auth token and is before user is signed in
        @Header(MFA_AUTH_HEADER) authHeader: String
    ): Response<Void>

    private companion object {
        private const val MFA = "mfa"
        private const val MFA_VERIFICATION = "$MFA/verify"
        private const val MFA_VERIFICATION_TOTP = "$MFA_VERIFICATION/totp.json"
        private const val MFA_VERIFICATION_YUBIKEY = "$MFA_VERIFICATION/yubikey.json"
        private const val MFA_AUTH_HEADER = "Authorization"
    }
}
