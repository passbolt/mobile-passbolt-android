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
import com.google.common.truth.Truth.assertWithMessage
import com.passbolt.mobile.android.feature.authentication.mfa.webauthn.WebauthnCeremonyResult.Cancelled
import com.passbolt.mobile.android.feature.authentication.mfa.webauthn.WebauthnCeremonyResult.Completed
import com.passbolt.mobile.android.feature.authentication.mfa.webauthn.WebauthnCeremonyResult.Failed
import org.junit.Test

class WebauthnCallbackUriParserTest {
    @Test
    fun `should return completed with delegated token`() {
        val result = parseWebauthnCallbackUri("$CALLBACK?mfa_delegated_token=abc-123")

        assertThat(result).isEqualTo(Completed("abc-123"))
    }

    @Test
    fun `should url decode delegated token`() {
        val result = parseWebauthnCallbackUri("$CALLBACK?mfa_delegated_token=a%2Bb%3Dc")

        assertThat(result).isEqualTo(Completed("a+b=c"))
    }

    @Test
    fun `should ignore unrelated query parameters`() {
        val result = parseWebauthnCallbackUri("$CALLBACK?utm=x&mfa_delegated_token=abc&other=y")

        assertThat(result).isEqualTo(Completed("abc"))
    }

    @Test
    fun `should return cancelled for cancelled error`() {
        val result = parseWebauthnCallbackUri("$CALLBACK?error=cancelled")

        assertThat(result).isEqualTo(Cancelled)
    }

    @Test
    fun `should return failed with error for other errors`() {
        val result = parseWebauthnCallbackUri("$CALLBACK?error=ceremony_failed")

        assertThat(result).isEqualTo(Failed("ceremony_failed"))
    }

    @Test
    fun `should return failed without error when query is missing`() {
        assertThat(parseWebauthnCallbackUri(CALLBACK)).isEqualTo(Failed(null))
    }

    @Test
    fun `should treat delegated token accompanied by error as failure`() {
        val result = parseWebauthnCallbackUri("$CALLBACK?error=x&mfa_delegated_token=abc")

        assertThat(result).isEqualTo(Failed("x"))
    }

    @Test
    fun `should reject uris that are not the callback uri`() {
        listOf(
            null,
            "",
            "::not a uri::",
            "https://mfa/webauthn/callback?mfa_delegated_token=abc",
            "passbolt://attacker/webauthn/callback?mfa_delegated_token=abc",
            "passbolt://mfa/webauthn/callback/extra?mfa_delegated_token=abc",
            "passbolt://mfa/webauthn?mfa_delegated_token=abc",
            "passbolt://mfa.attacker.com/webauthn/callback?mfa_delegated_token=abc",
            "https://attacker.com?url=passbolt://mfa/webauthn/callback?mfa_delegated_token=abc",
        ).forEach { uri ->
            assertWithMessage("$uri should be rejected")
                .that(parseWebauthnCallbackUri(uri))
                .isEqualTo(Failed("invalid_callback_uri"))
        }
    }

    @Test
    fun `should reject malformed or ambiguous query`() {
        listOf(
            "$CALLBACK?mfa_delegated_token=%zz",
            "$CALLBACK?mfa_delegated_token=abc&mfa_delegated_token=def",
            "$CALLBACK?error=cancelled&error=x",
        ).forEach { uri ->
            assertWithMessage("$uri should be rejected")
                .that(parseWebauthnCallbackUri(uri))
                .isEqualTo(Failed("invalid_callback_uri"))
        }
    }

    @Test
    fun `should not read delegated token from fragment`() {
        listOf(
            "$CALLBACK#mfa_delegated_token=abc",
            "$CALLBACK#?mfa_delegated_token=abc",
        ).forEach { uri ->
            assertWithMessage("$uri should not complete")
                .that(parseWebauthnCallbackUri(uri))
                .isEqualTo(Failed(null))
        }
    }

    private companion object {
        const val CALLBACK = "passbolt://mfa/webauthn/callback"
    }
}
