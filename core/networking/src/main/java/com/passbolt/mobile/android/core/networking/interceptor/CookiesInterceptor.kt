package com.passbolt.mobile.android.core.networking.interceptor

import com.passbolt.mobile.android.common.MfaTokenExtractor
import com.passbolt.mobile.android.core.networking.AuthPaths
import com.passbolt.mobile.android.core.networking.AuthPaths.AVATAR_PATH
import com.passbolt.mobile.android.core.networking.AuthPaths.TRANSFER_PATH
import com.passbolt.mobile.android.storage.usecase.session.GetSessionUseCase
import okhttp3.Interceptor
import okhttp3.Response

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
class CookiesInterceptor {

    class ReceivedCookiesInterceptor(
        private val mfaTokenExtractor: MfaTokenExtractor
    ) : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            val originalResponse: Response = chain.proceed(chain.request())
            val cookie = mfaTokenExtractor.get(originalResponse)
            if (cookie != null) {
                mfaCookie = cookie
            }
            return originalResponse
        }
    }

    class AddCookiesInterceptor(
        private val getSessionUseCase: GetSessionUseCase
    ) : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            val request = chain.request()
            val newBuilder = request.newBuilder()
            if (request.url.encodedPath == AuthPaths.AUTH_SIGN_IN) {
                // mfa token is saved for 40 days so we add it to login request
                getSessionUseCase.execute(Unit).mfaToken?.let {
                    newBuilder.addHeader("Cookie", it)
                }
            } else if (ANONYMOUS_PATHS.none { request.url.encodedPath == it } &&
                !request.url.encodedPath.contains(AVATAR_PATH) && !request.url.encodedPath.contains(TRANSFER_PATH)) {
                mfaCookie?.let {
                    newBuilder.addHeader("Cookie", it)
                }
            }
            return chain.proceed(newBuilder.build())
        }
    }

    companion object {
        private var mfaCookie: String? = null

        private val ANONYMOUS_PATHS = setOf(
            AuthPaths.AUTH_SIGN_IN,
            AuthPaths.SETTINGS,
            AuthPaths.AUTH_RSA,
            AuthPaths.MFA_VERIFICATION_TOTP,
            AuthPaths.AUTH_VERIFY
        )
    }
}
