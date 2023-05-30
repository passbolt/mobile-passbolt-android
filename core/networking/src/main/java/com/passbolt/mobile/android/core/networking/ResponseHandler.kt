package com.passbolt.mobile.android.core.networking

import com.passbolt.mobile.android.core.mvp.authentication.AuthenticationState.Unauthenticated.Reason.Mfa.MfaProvider
import com.passbolt.mobile.android.dto.response.BaseResponse
import retrofit2.HttpException
import retrofit2.Response
import timber.log.Timber
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

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
class ResponseHandler(
    private val errorHeaderMapper: ErrorHeaderMapper
) {

    fun <T : Any> handleSuccess(data: T): NetworkResult<T> =
        NetworkResult.Success(data)

    fun <T : Any> handleException(e: Exception): NetworkResult<T> =
        when (e) {
            is HttpException -> {
                val baseResponse = parseErrorResponseBody(e.response())
                NetworkResult.Failure.ServerError(
                    exception = e,
                    errorCode = e.code(),
                    headerMessage = getHeaderMessage(baseResponse),
                    mfaStatus = checkIfMfaRequired(baseResponse)
                )
            }

            is UnknownHostException -> NetworkResult.Failure.NetworkError(
                exception = e,
                headerMessage = errorHeaderMapper.getMessage()
            )

            is ConnectException -> NetworkResult.Failure.NetworkError(
                exception = e,
                headerMessage = errorHeaderMapper.getMessage()
            )

            is SocketTimeoutException -> NetworkResult.Failure.ServerError(
                exception = e,
                headerMessage = errorHeaderMapper.getMessage()
            )

            else -> NetworkResult.Failure.ServerError(
                exception = e,
                headerMessage = errorHeaderMapper.getMessage()
            )
        }

    fun checkIfMfaRequired(response: BaseResponse<*>?) =
        errorHeaderMapper.checkMfaRequired(response)

    fun getHeaderMessage(response: BaseResponse<*>?) =
        errorHeaderMapper.getMessage(response)

    fun parseErrorResponseBody(response: Response<*>?) =
        errorHeaderMapper.getBaseResponse(response)
}

sealed class MfaStatus {
    class Required(
        val providers: List<MfaProvider?>?
    ) : MfaStatus()

    object NotRequired : MfaStatus()
}

inline fun <T : Any> callWithHandler(responseHandler: ResponseHandler, apiCall: () -> T) = try {
    responseHandler.handleSuccess(apiCall())
} catch (e: Exception) {
    Timber.e(e)
    responseHandler.handleException(e)
}

inline fun <T : Any> callWithLibraryResponseHandler(
    responseHandler: ResponseHandler,
    apiCall: () -> Response<T>
): NetworkResult<Response<T>> {
    val response = apiCall()
    return if (response.isSuccessful) {
        responseHandler.handleSuccess(response)
    } else {
        try {
            val errorResponse = requireNotNull(responseHandler.parseErrorResponseBody(response))
            NetworkResult.Failure.ServerError(
                exception = IOException("There was an error during API invocation"),
                errorCode = errorResponse.header.code,
                headerMessage = responseHandler.getHeaderMessage(errorResponse),
                mfaStatus = responseHandler.checkIfMfaRequired(errorResponse)
            )
        } catch (exception: Exception) {
            NetworkResult.Failure.ServerError(
                exception = exception,
                headerMessage = "There was an exception during sign in: ${exception.message}"
            )
        }
    }
}
