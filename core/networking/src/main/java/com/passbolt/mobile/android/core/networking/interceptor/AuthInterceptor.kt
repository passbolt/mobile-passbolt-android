package com.passbolt.mobile.android.core.networking.interceptor

import com.passbolt.mobile.android.core.networking.AuthPaths
import com.passbolt.mobile.android.storage.usecase.session.GetSessionUseCase
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response

internal class AuthInterceptor(
    private val getSessionUseCase: GetSessionUseCase
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val newBuilder = request.newBuilder()
        if (ANONYMOUS_PATHS.none { request.url.encodedPath == it }) {
            addAuthTokens(newBuilder)
        }
        return chain.proceed(newBuilder.build())
    }

    private fun addAuthTokens(builder: Request.Builder) {
        val accessToken = getSessionUseCase.execute(Unit).accessToken
        accessToken?.let {
            builder.addHeader(AUTH_HEADER, "Bearer $it")
        }
    }

    companion object {
        private val ANONYMOUS_PATHS = setOf(
            AuthPaths.AUTH_SIGN_IN
        )
        private const val AUTH_HEADER = "Authorization"
    }
}
