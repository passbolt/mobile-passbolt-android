package com.passbolt.mobile.android.core.networking.interceptor

import com.passbolt.mobile.android.core.networking.AuthPaths
import com.passbolt.mobile.android.domain.auth.usecase.GetSessionUseCase
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response

internal class AuthInterceptor(
    private val getSessionUseCase: GetSessionUseCase,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val newBuilder = request.newBuilder()
        if (!isAnonymous(request.url.encodedPath) &&
            !request.url.encodedPath.contains(AuthPaths.AVATAR_PATH) &&
            !request.url.encodedPath.contains(AuthPaths.TRANSFER_PATH)
        ) {
            addAuthTokens(newBuilder)
        }
        return chain.proceed(newBuilder.build())
    }

    private fun isAnonymous(encodedPath: String) = encodedPath in ANONYMOUS_PATHS

    private fun addAuthTokens(builder: Request.Builder) {
        val accessToken = getSessionUseCase.execute(Unit).accessToken
        accessToken?.let {
            builder.addHeader(AUTH_HEADER, "Bearer $it")
        }
    }

    companion object {
        private val ANONYMOUS_PATHS =
            setOf(
                AuthPaths.AUTH_SIGN_IN,
                AuthPaths.MFA_VERIFICATION_TOTP,
                AuthPaths.MFA_VERIFICATION_YUBIKEY,
                AuthPaths.MFA_VERIFICATION_DUO_PROMPT,
                AuthPaths.MFA_VERIFICATION_DUO_VERIFY,
                AuthPaths.MFA_VERIFICATION_WEBAUTHN_DELEGATED_PROMPT,
                AuthPaths.MFA_VERIFICATION_WEBAUTHN_DELEGATED_CALLBACK,
                AuthPaths.AUTH_RSA,
                AuthPaths.AUTH_VERIFY,
                AuthPaths.AUTH_JWT_REFRESH,
            )
        internal const val AUTH_HEADER = "Authorization"
    }
}
