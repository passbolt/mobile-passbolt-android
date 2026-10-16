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

package com.passbolt.mobile.android.feature.authentication.mfa.webauthn

import com.google.common.truth.Truth.assertThat
import com.passbolt.mobile.android.feature.authentication.auth.challenge.TrailingSlashDomainComparator
import com.passbolt.mobile.android.feature.authentication.mfa.webauthn.WebauthnDelegatedTokenVerifier.Output
import io.fusionauth.jwt.domain.JWT
import io.fusionauth.jwt.rsa.RSASigner
import io.fusionauth.pem.domain.PEM
import org.junit.Test
import java.security.KeyPairGenerator
import java.time.ZoneOffset
import java.time.ZonedDateTime

class WebauthnDelegatedTokenVerifierTest {
    private val keyPair = KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.generateKeyPair()
    private val otherKeyPair = KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.generateKeyPair()
    private val publicKeyPem = PEM.encode(keyPair.public)
    private val signer = RSASigner.newSHA256Signer(PEM.encode(keyPair.private))

    private val verifier = WebauthnDelegatedTokenVerifier(TrailingSlashDomainComparator())

    @Test
    fun `should verify a well formed delegated token`() {
        val token = JWT.getEncoder().encode(delegatedJwt(), signer)

        assertThat(verifier.verify(token, publicKeyPem, ACCOUNT_URL, STATE)).isEqualTo(Output.Verified)
    }

    @Test
    fun `should accept issuer differing only by trailing slash`() {
        val token = JWT.getEncoder().encode(delegatedJwt(issuer = "$ACCOUNT_URL/"), signer)

        assertThat(verifier.verify(token, publicKeyPem, ACCOUNT_URL, STATE)).isEqualTo(Output.Verified)
    }

    @Test
    fun `should reject a signature from another key`() {
        val token = JWT.getEncoder().encode(delegatedJwt(), RSASigner.newSHA256Signer(PEM.encode(otherKeyPair.private)))

        assertThat(verifier.verify(token, publicKeyPem, ACCOUNT_URL, STATE)).isEqualTo(Output.InvalidSignature)
    }

    @Test
    fun `should reject an expired delegated token`() {
        val token = JWT.getEncoder().encode(delegatedJwt(expiresInSeconds = -5), signer)

        assertThat(verifier.verify(token, publicKeyPem, ACCOUNT_URL, STATE)).isEqualTo(Output.Expired)
    }

    @Test
    fun `should reject an issuer from another instance`() {
        val token = JWT.getEncoder().encode(delegatedJwt(issuer = "https://other.example.com"), signer)

        assertThat(verifier.verify(token, publicKeyPem, ACCOUNT_URL, STATE)).isEqualTo(Output.IssuerMismatch)
    }

    @Test
    fun `should reject an unexpected audience`() {
        val token = JWT.getEncoder().encode(delegatedJwt(audience = "something-else"), signer)

        assertThat(verifier.verify(token, publicKeyPem, ACCOUNT_URL, STATE)).isEqualTo(Output.AudienceMismatch)
    }

    @Test
    fun `should reject a ceremony id that does not match the delegated state`() {
        val token = JWT.getEncoder().encode(delegatedJwt(ceremonyId = "other-state"), signer)

        assertThat(verifier.verify(token, publicKeyPem, ACCOUNT_URL, STATE)).isEqualTo(Output.StateMismatch)
    }

    @Test
    fun `should reject a missing ceremony id claim`() {
        val token = JWT.getEncoder().encode(delegatedJwt(ceremonyId = null), signer)

        assertThat(verifier.verify(token, publicKeyPem, ACCOUNT_URL, STATE)).isEqualTo(Output.StateMismatch)
    }

    @Test
    fun `should reject a string that is not a jwt`() {
        assertThat(verifier.verify("not-a-jwt", publicKeyPem, ACCOUNT_URL, STATE)).isEqualTo(Output.Malformed)
    }

    private fun delegatedJwt(
        issuer: String = ACCOUNT_URL,
        audience: String = "passbolt-mobile-mfa-exchange",
        ceremonyId: String? = STATE,
        expiresInSeconds: Long = 60,
    ): JWT =
        JWT()
            .setIssuer(issuer)
            .setAudience(audience)
            .setSubject("user-id")
            .setUniqueId("delegated-token-id")
            .setExpiration(ZonedDateTime.now(ZoneOffset.UTC).plusSeconds(expiresInSeconds))
            .apply { if (ceremonyId != null) addClaim("cid", ceremonyId) }

    private companion object {
        const val ACCOUNT_URL = "https://passbolt.example.com"
        const val STATE = "6f5a8a2e-2f3a-4a4e-9b1e-2c1b6f4c8d10"
    }
}
