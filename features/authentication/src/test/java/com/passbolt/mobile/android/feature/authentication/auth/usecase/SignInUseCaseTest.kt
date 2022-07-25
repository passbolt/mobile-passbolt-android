package com.passbolt.mobile.android.feature.authentication.auth.usecase

import com.google.common.truth.Truth.assertThat
import com.passbolt.mobile.android.core.networking.NetworkResult
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Rule
import org.junit.Test
import org.koin.core.logger.Level
import org.koin.test.KoinTest
import org.koin.test.KoinTestRule
import org.koin.test.inject
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.stub
import retrofit2.HttpException
import retrofit2.Response
import java.net.HttpURLConnection

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

@ExperimentalCoroutinesApi
class SignInUseCaseTest : KoinTest {

    private val signInUseCase: SignInUseCase by inject()

    @get:Rule
    val koinTestRule = KoinTestRule.create {
        printLogger(Level.ERROR)
        modules(testSignInUseCaseModule)
    }

    @Test
    fun `test account not found should be mapped correct`() = runTest {
        mockAuthRepository.stub {
            onBlocking { signIn(any(), any()) }.doReturn(
                NetworkResult.Failure.ServerError(
                    HttpException(
                        Response.error<String>(
                            HttpURLConnection.HTTP_NOT_FOUND,
                            "".toResponseBody("application/json".toMediaTypeOrNull())
                        )
                    ),
                    HttpURLConnection.HTTP_NOT_FOUND,
                    "error message"
                )
            )
        }

        val response = signInUseCase.execute(SignInUseCase.Input("test", "test", "mfa"))
        assertThat(response).isInstanceOf(SignInUseCase.Output.Failure::class.java)
        assertThat((response as SignInUseCase.Output.Failure).type).isEqualTo(SignInFailureType.ACCOUNT_DOES_NOT_EXIST)
    }
}
