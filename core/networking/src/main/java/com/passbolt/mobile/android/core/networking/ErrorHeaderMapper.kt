package com.passbolt.mobile.android.core.networking

import android.content.Context
import com.google.gson.Gson
import com.passbolt.mobile.android.dto.response.BaseResponse
import retrofit2.Response
import java.lang.Exception
import com.passbolt.mobile.android.core.mvp.session.AuthenticationState.Unauthenticated.Reason.Mfa.MfaProvider
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
class ErrorHeaderMapper(
    private val context: Context,
    private val gson: Gson
) {

    fun getBaseResponse(response: Response<*>? = null) =
        response?.errorBody()?.charStream()?.let {
            gson.fromJson(it, BaseResponse::class.java)
        }

    fun getMessage(baseResponse: BaseResponse<*>? = null) =
        baseResponse?.header?.message ?: context.getString(R.string.common_failure)

    fun getValidationFieldsError(baseResponse: BaseResponse<*>? = null): List<String>? =
        baseResponse?.let {
            return try {
                val map = baseResponse.body as Map<String, Map<String, String>>
                val invalidFields = mutableListOf<String>()
                map.values.forEach {
                    invalidFields.addAll(it.keys)
                }
                invalidFields
            } catch (e: Exception) {
                null
            }
        }

    fun checkMfaRequired(baseResponse: BaseResponse<*>? = null): MfaStatus {
        try {
            val map = baseResponse?.body as Map<String, List<String>>
            if (map.containsKey(MFA_PROVIDER_KEY)) {
                val mfaType = MfaProvider.parse(map[MFA_PROVIDER_KEY]?.first())
                return MfaStatus.Required(mfaType)
            }
        } catch (e: Exception) {
            return MfaStatus.NotRequired
        }
        return MfaStatus.NotRequired
    }

    companion object {
        private const val MFA_PROVIDER_KEY = "mfa_providers"
    }
}
