package com.passbolt.mobile.android.core.networking

import android.content.Context
import com.google.gson.Gson
import com.passbolt.mobile.android.core.mvp.authentication.AuthenticationState.Unauthenticated.Reason.Mfa.MfaProvider
import com.passbolt.mobile.android.dto.response.BaseResponse
import okhttp3.ResponseBody
import retrofit2.Response
import timber.log.Timber
import com.passbolt.mobile.android.core.localization.R as LocalizationR

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
        response?.errorBody()?.charStream()?.let { charStream ->
            try {
                gson.fromJson(charStream, BaseResponse::class.java)
            } catch (exception: Exception) {
                Timber.e("Encountered a non-standard backend error response")
                null
            }
        }

    fun getMessage(baseResponse: BaseResponse<*>? = null) =
        baseResponse?.header?.message ?: context.getString(LocalizationR.string.common_failure)

    fun getValidationFieldsError(responseBody: ResponseBody?): List<String>? =
        responseBody?.let {
            return try {
                val map = gson.fromJson(it.string(), BaseResponse::class.java).body as Map<String, Map<String, String>>
                val invalidFields = mutableListOf<String>()
                map.values.forEach {
                    invalidFields.addAll(it.keys)
                }
                invalidFields
            } catch (e: Exception) {
                Timber.e(e, "There was an error during getting validation fields")
                null
            }
        }

    fun checkMfaRequired(baseResponse: BaseResponse<*>? = null): MfaStatus {
        try {
            val map = baseResponse?.body as Map<String, List<String>>
            if (map.containsKey(MFA_PROVIDER_KEY)) {
                val mfaType = map[MFA_PROVIDER_KEY]?.map { MfaProvider.parse(it) }
                return MfaStatus.Required(mfaType)
            }
        } catch (e: Exception) {
            Timber.e(e, "There was an error during checking if MFA is required")
            return MfaStatus.NotRequired
        }
        return MfaStatus.NotRequired
    }

    companion object {
        private const val MFA_PROVIDER_KEY = "mfa_providers"
    }
}
