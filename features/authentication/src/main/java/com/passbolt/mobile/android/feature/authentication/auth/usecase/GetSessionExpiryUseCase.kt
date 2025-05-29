package com.passbolt.mobile.android.feature.authentication.auth.usecase

import com.passbolt.mobile.android.common.usecase.UseCase
import com.passbolt.mobile.android.common.usecase.UserIdInput
import com.passbolt.mobile.android.core.accounts.usecase.SessionFileName
import com.passbolt.mobile.android.core.accounts.usecase.selectedaccount.GetSelectedAccountUseCase
import com.passbolt.mobile.android.core.authenticationcore.session.ACCESS_TOKEN_KEY
import com.passbolt.mobile.android.encryptedstorage.EncryptedSharedPreferencesFactory
import io.fusionauth.jwt.JWTExpiredException
import io.fusionauth.jwt.Verifier
import io.fusionauth.jwt.domain.JWT
import io.fusionauth.jwt.rsa.RSAVerifier
import java.time.ZonedDateTime

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

class GetSessionExpiryUseCase(
    private val encryptedSharedPreferencesFactory: EncryptedSharedPreferencesFactory,
    private val getSelectedAccountUseCase: GetSelectedAccountUseCase,
    private val getServerRsaPublicKeyUseCase: GetServerPublicRsaKeyUseCase,
) : UseCase<Unit, GetSessionExpiryUseCase.Output> {
    override fun execute(input: Unit): Output {
        val userId = getSelectedAccountUseCase.execute(Unit).selectedAccount
        userId?.let {
            val alias = SessionFileName(it).name
            val sharedPreferences = encryptedSharedPreferencesFactory.get("$alias.xml")
            val accessToken = sharedPreferences.getString(ACCESS_TOKEN_KEY, null)
            val rsaPublicKey = getServerRsaPublicKeyUseCase.execute(UserIdInput(userId)).rsaKey

            val verifier: Verifier = RSAVerifier.newVerifier(rsaPublicKey)

            return try {
                val accessTokenJwt = JWT.getDecoder().decode(accessToken, verifier)
                Output.JwtWillExpire(accessTokenJwt.expiration)
            } catch (exception: JWTExpiredException) {
                Output.JwtAlreadyExpired
            } catch (exception: Exception) {
                Output.NoJwt
            }
        } ?: return Output.NoJwt
    }

    sealed class Output {
        data object JwtAlreadyExpired : Output()

        data class JwtWillExpire(
            val accessTokenExpirySeconds: ZonedDateTime,
        ) : Output()

        data object NoJwt : Output()
    }
}
