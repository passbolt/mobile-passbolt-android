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

package com.passbolt.mobile.android.data.mfa.datasource.remote

import com.passbolt.mobile.android.common.CookieExtractor
import com.passbolt.mobile.android.core.architecture.result.DomainResult
import com.passbolt.mobile.android.core.architecture.result.map
import com.passbolt.mobile.android.core.networking.ErrorHeaderMapper
import com.passbolt.mobile.android.core.networking.ResponseHandler
import com.passbolt.mobile.android.core.networking.callWithHandler
import com.passbolt.mobile.android.core.networking.toDomainResult
import com.passbolt.mobile.android.data.mfa.datasource.remote.api.MfaApi
import com.passbolt.mobile.android.data.mfa.mapper.toDuoPrompt
import com.passbolt.mobile.android.data.mfa.mapper.toDuoVerification
import com.passbolt.mobile.android.data.mfa.mapper.toTotpVerification
import com.passbolt.mobile.android.data.mfa.mapper.toWebauthnDelegatedPrompt
import com.passbolt.mobile.android.data.mfa.mapper.toWebauthnVerification
import com.passbolt.mobile.android.data.mfa.mapper.toYubikeyVerification
import com.passbolt.mobile.android.domain.mfa.MfaDataSource
import com.passbolt.mobile.android.domain.mfa.model.DuoPrompt
import com.passbolt.mobile.android.domain.mfa.model.DuoVerification
import com.passbolt.mobile.android.domain.mfa.model.TotpVerification
import com.passbolt.mobile.android.domain.mfa.model.WebauthnDelegatedPrompt
import com.passbolt.mobile.android.domain.mfa.model.WebauthnVerification
import com.passbolt.mobile.android.domain.mfa.model.YubikeyVerification
import com.passbolt.mobile.android.dto.request.HotpRequest
import com.passbolt.mobile.android.dto.request.TotpRequest
import com.passbolt.mobile.android.dto.request.WebauthnDelegatedCallbackRequest

internal class MfaRemoteDataSource(
    private val mfaApi: MfaApi,
    private val responseHandler: ResponseHandler,
    private val cookieExtractor: CookieExtractor,
    private val errorHeaderMapper: ErrorHeaderMapper,
) : MfaDataSource {
    override suspend fun verifyTotp(
        otp: String,
        remember: Boolean,
        authToken: String?,
    ): DomainResult<TotpVerification> =
        callWithHandler(responseHandler) {
            mfaApi.verifyTotp(TotpRequest(otp, remember), authToken.toBearer())
        }.toDomainResult().map { it.toTotpVerification(cookieExtractor, errorHeaderMapper) }

    override suspend fun verifyYubikeyOtp(
        otp: String,
        remember: Boolean,
        authToken: String?,
    ): DomainResult<YubikeyVerification> =
        callWithHandler(responseHandler) {
            mfaApi.verifyYubikeyOtp(HotpRequest(otp, remember), authToken.toBearer())
        }.toDomainResult().map { it.toYubikeyVerification(cookieExtractor) }

    override suspend fun getDuoPrompt(authToken: String?): DomainResult<DuoPrompt> =
        callWithHandler(responseHandler) {
            mfaApi.getDuoPromptUrl(authToken.toBearer())
        }.toDomainResult().map { it.toDuoPrompt(cookieExtractor) }

    override suspend fun verifyDuoCallback(
        authToken: String?,
        duoStateUuid: String,
        state: String?,
        code: String?,
    ): DomainResult<DuoVerification> =
        callWithHandler(responseHandler) {
            mfaApi.verifyDuoCallback(
                authHeader = authToken.toBearer(),
                passboltDuoState = DUO_STATE_COOKIE_TEMPLATE.format(duoStateUuid),
                state = state,
                code = code,
            )
        }.toDomainResult().map { it.toDuoVerification(cookieExtractor) }

    override suspend fun getWebauthnDelegatedPrompt(authToken: String?): DomainResult<WebauthnDelegatedPrompt> =
        callWithHandler(responseHandler) {
            mfaApi.getWebauthnDelegatedPrompt(authToken.toBearer())
        }.toDomainResult().map { it.toWebauthnDelegatedPrompt() }

    override suspend fun verifyWebauthnExchangeCode(
        authToken: String?,
        exchangeCode: String,
        remember: Boolean,
    ): DomainResult<WebauthnVerification> =
        callWithHandler(responseHandler) {
            mfaApi.verifyWebauthnExchangeCode(
                WebauthnDelegatedCallbackRequest(exchangeCode, remember),
                authToken.toBearer(),
            )
        }.toDomainResult().map { it.toWebauthnVerification(cookieExtractor) }

    private fun String?.toBearer(): String? = this?.let { "Bearer $it" }

    private companion object {
        private const val DUO_STATE_COOKIE_TEMPLATE = "passbolt_duo_state=%s"
    }
}
