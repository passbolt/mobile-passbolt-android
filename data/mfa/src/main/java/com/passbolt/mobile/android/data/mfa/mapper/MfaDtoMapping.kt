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
package com.passbolt.mobile.android.data.mfa.mapper

import com.passbolt.mobile.android.common.CookieExtractor
import com.passbolt.mobile.android.core.networking.ErrorHeaderMapper
import com.passbolt.mobile.android.domain.mfa.model.DuoPrompt
import com.passbolt.mobile.android.domain.mfa.model.DuoVerification
import com.passbolt.mobile.android.domain.mfa.model.TotpVerification
import com.passbolt.mobile.android.domain.mfa.model.WebauthnDelegatedPrompt
import com.passbolt.mobile.android.domain.mfa.model.WebauthnVerification
import com.passbolt.mobile.android.domain.mfa.model.YubikeyVerification
import com.passbolt.mobile.android.dto.response.BaseResponse
import com.passbolt.mobile.android.dto.response.WebauthnDelegatedPromptResponseDto
import org.json.JSONException
import org.json.JSONObject
import retrofit2.Response
import java.net.HttpURLConnection.HTTP_BAD_REQUEST
import java.net.HttpURLConnection.HTTP_MOVED_TEMP
import java.net.HttpURLConnection.HTTP_UNAUTHORIZED

private const val VALID_OTP = "isValidOtp"
private const val LOCATION_HEADER = "location"
private const val DUO_UUID_COOKIE = "passbolt_duo_state"
private const val RESPONSE_BODY = "body"
private const val RESPONSE_BODY_HOTP = "hotp"
private const val HOTP_BODY_IS_SAME_YUBIKEY = "isSameYubikeyId"

internal fun Response<Void>.toTotpVerification(
    cookieExtractor: CookieExtractor,
    errorHeaderMapper: ErrorHeaderMapper,
): TotpVerification =
    when {
        isSuccessful -> TotpVerification.Succeeded(mfaHeader(cookieExtractor))
        code() == HTTP_UNAUTHORIZED -> TotpVerification.Unauthorized
        errorHeaderMapper.getValidationFieldsError(errorBody())?.contains(VALID_OTP) == true -> TotpVerification.WrongOtp
        else -> TotpVerification.OtherFailure(code())
    }

internal fun Response<Void>.toYubikeyVerification(cookieExtractor: CookieExtractor): YubikeyVerification =
    when {
        isSuccessful -> YubikeyVerification.Succeeded(mfaHeader(cookieExtractor))
        code() == HTTP_UNAUTHORIZED -> YubikeyVerification.Unauthorized
        code() == HTTP_BAD_REQUEST && isYubikeyFromAnotherUser() -> YubikeyVerification.NotFromCurrentUser
        else -> YubikeyVerification.OtherFailure(code())
    }

internal fun Response<Void>.toDuoPrompt(cookieExtractor: CookieExtractor): DuoPrompt =
    when (code()) {
        HTTP_MOVED_TEMP -> {
            val location = headers()[LOCATION_HEADER]
            val duoUuid = cookieExtractor.getCookieValue(this, DUO_UUID_COOKIE)
            if (location != null && duoUuid != null) {
                DuoPrompt.Found(location, duoUuid)
            } else {
                DuoPrompt.NotFound
            }
        }
        HTTP_UNAUTHORIZED -> DuoPrompt.Unauthorized
        else -> DuoPrompt.OtherFailure(code())
    }

internal fun Response<Void>.toDuoVerification(cookieExtractor: CookieExtractor): DuoVerification =
    if (isSuccessful) {
        DuoVerification.Succeeded(mfaHeader(cookieExtractor))
    } else {
        DuoVerification.Failed(message())
    }

internal fun Response<BaseResponse<WebauthnDelegatedPromptResponseDto>>.toWebauthnDelegatedPrompt(): WebauthnDelegatedPrompt {
    val prompt = body()?.body
    return when {
        isSuccessful && prompt != null && prompt.state.isNotBlank() && prompt.secret.isNotBlank() ->
            WebauthnDelegatedPrompt.Succeeded(prompt.state, prompt.secret)
        code() == HTTP_UNAUTHORIZED -> WebauthnDelegatedPrompt.Unauthorized
        else -> WebauthnDelegatedPrompt.OtherFailure(code())
    }
}

internal fun Response<Void>.toWebauthnVerification(cookieExtractor: CookieExtractor): WebauthnVerification =
    when {
        isSuccessful -> WebauthnVerification.Succeeded(mfaHeader(cookieExtractor))
        code() == HTTP_UNAUTHORIZED -> WebauthnVerification.Unauthorized
        else -> WebauthnVerification.OtherFailure(code())
    }

private fun Response<Void>.mfaHeader(cookieExtractor: CookieExtractor): String? = cookieExtractor.get(this, CookieExtractor.MFA_COOKIE)

// checks if field "isSameYubikeyId" exists in the response - that means that it's not a Yubikey
// that is associated with the account
private fun Response<Void>.isYubikeyFromAnotherUser(): Boolean =
    try {
        errorBody()?.string()?.let { body ->
            JSONObject(body)
                .getJSONObject(RESPONSE_BODY)
                .getJSONObject(RESPONSE_BODY_HOTP)
                .has(HOTP_BODY_IS_SAME_YUBIKEY)
        } ?: false
    } catch (_: JSONException) {
        false
    }
