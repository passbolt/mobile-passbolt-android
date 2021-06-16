package com.passbolt.mobile.android.core.networking

import retrofit2.HttpException
import timber.log.Timber
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
class ResponseHandler {

    fun <T : Any> handleSuccess(data: T): NetworkResult<T> =
        NetworkResult.Success(data)

    fun <T : Any> handleException(e: Exception): NetworkResult<T> {
        Timber.d(e)
        return when (e) {
            is HttpException -> {
                NetworkResult.Failure.ServerError(e, e.code())
            }
            is UnknownHostException -> {
                NetworkResult.Failure.NetworkError(e)
            }
            is ConnectException -> {
                NetworkResult.Failure.NetworkError(e)
            }
            is SocketTimeoutException -> {
                NetworkResult.Failure.ServerError(e)
            }
            else -> {
                NetworkResult.Failure.ServerError(e)
            }
        }
    }
}

inline fun <T : Any> callWithHandler(responseHandler: ResponseHandler, apiCall: () -> T) = try {
    responseHandler.handleSuccess(apiCall())
} catch (e: Exception) {
    responseHandler.handleException(e)
}
