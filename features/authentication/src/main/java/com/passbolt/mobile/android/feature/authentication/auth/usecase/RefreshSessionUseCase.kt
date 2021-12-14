package com.passbolt.mobile.android.feature.authentication.auth.usecase

import com.passbolt.mobile.android.common.CookieExtractor
import com.passbolt.mobile.android.common.usecase.AsyncUseCase
import com.passbolt.mobile.android.dto.request.RefreshSessionRequest
import com.passbolt.mobile.android.passboltapi.auth.AuthRepository
import com.passbolt.mobile.android.storage.usecase.accountdata.GetAccountDataUseCase
import com.passbolt.mobile.android.storage.usecase.input.UserIdInput
import com.passbolt.mobile.android.storage.usecase.selectedaccount.GetSelectedAccountUseCase
import com.passbolt.mobile.android.storage.usecase.session.GetSessionUseCase
import com.passbolt.mobile.android.storage.usecase.session.SaveSessionUseCase
import retrofit2.HttpException
import timber.log.Timber
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
class RefreshSessionUseCase(
    private val authRepository: AuthRepository,
    private val getSelectedAccountUseCase: GetSelectedAccountUseCase,
    private val getAccountDataUseCase: GetAccountDataUseCase,
    private val getSessionUseCase: GetSessionUseCase,
    private val saveSessionUseCase: SaveSessionUseCase,
    private val cookieExtractor: CookieExtractor
) : AsyncUseCase<Unit, RefreshSessionUseCase.Output> {

    override suspend fun execute(input: Unit): Output = try {
        val userId = requireNotNull(getSelectedAccountUseCase.execute(Unit).selectedAccount)
        val serverUserId = requireNotNull(getAccountDataUseCase.execute(UserIdInput(userId)).serverId)
        val refreshToken = requireNotNull(getSessionUseCase.execute(Unit).refreshToken)
        val refreshSessionRequest = RefreshSessionRequest(refreshToken, serverUserId)

        val response = authRepository.refreshSession(refreshSessionRequest)
        if (response.code() == HttpURLConnection.HTTP_OK) {
            val newAccessToken = requireNotNull(
                response.body()
            ).body.accessToken
            val newRefreshToken = requireNotNull(
                cookieExtractor.getCookieValue(response, CookieExtractor.REFRESH_TOKEN_COOKIE)
            )
            val mfaToken = cookieExtractor.get(response, CookieExtractor.MFA_COOKIE)

            saveSessionUseCase.execute(
                SaveSessionUseCase.Input(
                    userId, newRefreshToken, newAccessToken, mfaToken
                )
            )
            Output.Success
        } else {
            throw HttpException(response)
        }
    } catch (throwable: Throwable) {
        Timber.e(throwable)
        Output.Failure
    }

    sealed class Output {

        object Success : Output()

        object Failure : Output()
    }
}
