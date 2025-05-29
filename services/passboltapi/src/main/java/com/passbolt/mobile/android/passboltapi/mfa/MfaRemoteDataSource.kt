package com.passbolt.mobile.android.passboltapi.mfa

import com.passbolt.mobile.android.dto.request.HotpRequest
import com.passbolt.mobile.android.dto.request.TotpRequest
import retrofit2.Response

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
internal class MfaRemoteDataSource(
    private val mfaApi: MfaApi,
) : MfaDataSource {
    override suspend fun verifyTotp(
        totpRequest: TotpRequest,
        authHeader: String?,
    ): Response<Void> = mfaApi.verifyTotp(totpRequest, authHeader)

    override suspend fun verifyYubikeyOtp(
        hotpRequest: HotpRequest,
        authHeader: String?,
    ): Response<Void> = mfaApi.verifyYubikeyOtp(hotpRequest, authHeader)

    override suspend fun getDuoPromptUrl(authHeader: String?): Response<Void> = mfaApi.getDuoPromptUrl(authHeader)

    override suspend fun verifyDuoCallback(
        authHeader: String?,
        passboltDuoStateUuid: String,
        state: String?,
        code: String?,
    ): Response<Void> =
        mfaApi.verifyDuoCallback(
            authHeader = authHeader,
            passboltDuoState = "passbolt_duo_state=%s".format(passboltDuoStateUuid),
            state = state,
            code = code,
        )
}
