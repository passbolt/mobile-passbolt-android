package com.passbolt.mobile.android.feature.authentication.auth.challenge

import com.passbolt.mobile.android.dto.response.ChallengeResponseDto
import io.fusionauth.jwt.InvalidJWTSignatureException
import io.fusionauth.jwt.JWTExpiredException
import io.fusionauth.jwt.Verifier
import io.fusionauth.jwt.domain.JWT
import io.fusionauth.jwt.rsa.RSAVerifier
import timber.log.Timber

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
class ChallengeVerifier {

    fun verify(challengeResponseDto: ChallengeResponseDto, rsaPublicKey: String): Output = try {

        val verifier: Verifier = RSAVerifier.newVerifier(rsaPublicKey)
        val jwt: JWT = JWT.getDecoder().decode(challengeResponseDto.accessToken, verifier)

        if (jwt.isExpired) {
            Output.TokenExpired
        }

        Output.Verified(challengeResponseDto.accessToken, challengeResponseDto.refreshToken)
    } catch (exception: InvalidJWTSignatureException) {
        Timber.e(exception)
        Output.InvalidSignature
    } catch (exception: JWTExpiredException) {
        Timber.e(exception)
        Output.TokenExpired
    } catch (exception: Exception) {
        Timber.e(exception)
        Output.Failure
    }

    sealed class Output {
        object TokenExpired : Output()
        class Verified(
            val accessToken: String,
            val refreshToken: String
        ) : Output()

        object InvalidSignature : Output()
        object Failure : Output()
    }
}
