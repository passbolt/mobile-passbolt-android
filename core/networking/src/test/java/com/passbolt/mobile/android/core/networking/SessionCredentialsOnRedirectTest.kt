package com.passbolt.mobile.android.core.networking

import com.google.common.truth.Truth.assertThat
import com.passbolt.mobile.android.domain.accounts.usecase.GetCurrentApiUrlUseCase
import com.passbolt.mobile.android.domain.auth.usecase.GetSessionUseCase
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.koin.core.qualifier.named
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.koin.test.KoinTestRule
import org.koin.test.inject
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import java.net.HttpURLConnection.HTTP_MOVED_TEMP
import java.net.HttpURLConnection.HTTP_OK
import java.util.concurrent.TimeUnit

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

class SessionCredentialsOnRedirectTest : KoinTest {
    private val apiServer = MockWebServer()
    private val foreignServer = MockWebServer()

    private val getCurrentApiUrlUseCase =
        mock<GetCurrentApiUrlUseCase> {
            on { execute(Unit) } doAnswer { GetCurrentApiUrlUseCase.Output(apiServer.url(API_SUB_PATH).toString()) }
        }
    private val getSessionUseCase =
        mock<GetSessionUseCase> {
            on { execute(Unit) } doReturn GetSessionUseCase.Output(accessToken = ACCESS_TOKEN, refreshToken = null, mfaToken = MFA_COOKIE)
        }

    @get:Rule
    val koinTestRule =
        KoinTestRule.create {
            modules(
                networkingModule,
                module {
                    single { getCurrentApiUrlUseCase }
                    single { getSessionUseCase }
                },
            )
        }

    private val client: OkHttpClient by inject(named(DEFAULT_HTTP_CLIENT))

    @Before
    fun setUp() {
        apiServer.start()
        foreignServer.start()
    }

    @After
    fun tearDown() {
        apiServer.shutdown()
        foreignServer.shutdown()
    }

    @Test
    fun `should send session credentials to the api origin`() {
        apiServer.enqueue(MockResponse().setResponseCode(HTTP_OK))

        client.get("$PLACEHOLDER_BASE_URL/resources.json")

        assertSessionCredentialsSent(apiServer.nextRequest())
    }

    @Test
    fun `should strip session credentials when redirected to another origin`() {
        val redirectTarget = foreignServer.url("/landing.json")
        apiServer.enqueue(redirectTo(redirectTarget))
        foreignServer.enqueue(MockResponse().setResponseCode(HTTP_OK))

        val response = client.get("$PLACEHOLDER_BASE_URL/resources.json")

        assertThat(response.code).isEqualTo(HTTP_OK)
        assertThat(response.request.url).isEqualTo(redirectTarget)
        assertSessionCredentialsSent(apiServer.nextRequest())
        assertNoSessionCredentialsSent(foreignServer.nextRequest())
    }

    @Test
    fun `should ignore cookies set by another origin when redirected there`() {
        val redirectTarget = foreignServer.url("/landing.json")
        apiServer.enqueue(redirectTo(redirectTarget))
        foreignServer.enqueue(
            MockResponse()
                .setResponseCode(HTTP_OK)
                .addHeader(SET_COOKIE_HEADER, "$FOREIGN_MFA_COOKIE; Path=/; HttpOnly"),
        )
        apiServer.enqueue(MockResponse().setResponseCode(HTTP_OK))

        val redirectedResponse = client.get("$PLACEHOLDER_BASE_URL/resources.json")
        client.get("$PLACEHOLDER_BASE_URL/resources.json")

        assertThat(redirectedResponse.headers(SET_COOKIE_HEADER)).isEmpty()
        apiServer.nextRequest()
        foreignServer.nextRequest()
        assertSessionCredentialsSent(apiServer.nextRequest())
    }

    @Test
    fun `should keep session credentials when redirected within the api origin`() {
        val redirectTarget = apiServer.url("$API_SUB_PATH/moved.json")
        apiServer.enqueue(redirectTo(redirectTarget))
        apiServer.enqueue(MockResponse().setResponseCode(HTTP_OK))

        val response = client.get("$PLACEHOLDER_BASE_URL/resources.json")

        assertThat(response.code).isEqualTo(HTTP_OK)
        assertThat(response.request.url).isEqualTo(redirectTarget)
        assertSessionCredentialsSent(apiServer.nextRequest())
        assertSessionCredentialsSent(apiServer.nextRequest())
    }

    private fun OkHttpClient.get(url: String): Response = newCall(Request.Builder().url(url).build()).execute().apply { close() }

    private fun redirectTo(location: HttpUrl): MockResponse =
        MockResponse()
            .setResponseCode(HTTP_MOVED_TEMP)
            .addHeader(LOCATION_HEADER, location.toString())

    private fun MockWebServer.nextRequest(): RecordedRequest =
        checkNotNull(takeRequest(REQUEST_TIMEOUT_SECONDS, TimeUnit.SECONDS)) { "No request received by $this" }

    private fun assertSessionCredentialsSent(request: RecordedRequest) {
        assertThat(request.getHeader(AUTHORIZATION_HEADER)).isEqualTo("Bearer $ACCESS_TOKEN")
        assertThat(request.getHeader(COOKIE_HEADER)).isEqualTo(MFA_COOKIE)
    }

    private fun assertNoSessionCredentialsSent(request: RecordedRequest) {
        assertThat(request.getHeader(AUTHORIZATION_HEADER)).isNull()
        assertThat(request.getHeader(COOKIE_HEADER)).isNull()
    }

    private companion object {
        private const val API_SUB_PATH = "/passbolt"
        private const val ACCESS_TOKEN = "access-token"
        private const val MFA_COOKIE = "passbolt_mfa=mfa-jwt"
        private const val FOREIGN_MFA_COOKIE = "passbolt_mfa=foreign-jwt"
        private const val AUTHORIZATION_HEADER = "Authorization"
        private const val COOKIE_HEADER = "Cookie"
        private const val SET_COOKIE_HEADER = "Set-Cookie"
        private const val LOCATION_HEADER = "Location"
        private const val REQUEST_TIMEOUT_SECONDS = 5L
    }
}
