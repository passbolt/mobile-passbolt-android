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

package com.passbolt.mobile.android.core.autofill.system

import android.view.autofill.AutofillId
import com.google.common.truth.Truth.assertThat
import com.passbolt.mobile.android.ui.ParsedStructure
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

class AssistStructureParserTest {
    private val trustedBrowserVerifier =
        mock<BrowserSignatureVerifier> {
            on { isTrustedBrowser("com.android.chrome") } doReturn true
        }
    private val parser = AssistStructureParser(trustedBrowserVerifier)

    @Test
    fun `applyDomainTrust keeps reported domains for a verified trusted browser`() {
        val structures =
            setOf(
                ParsedStructure(id = mock<AutofillId>(), autofillHints = listOf("username"), domain = "victim.example.com"),
                ParsedStructure(id = mock<AutofillId>(), autofillHints = listOf("password"), domain = "victim.example.com"),
            )

        val result = parser.applyDomainTrust(structures, "com.android.chrome")

        assertThat(result.structures.map { it.domain }).containsExactly("victim.example.com", "victim.example.com")
    }

    @Test
    fun `applyDomainTrust drops reported domains for an untrusted application`() {
        val username = ParsedStructure(id = mock<AutofillId>(), autofillHints = listOf("username"), domain = "victim.example.com")
        val password = ParsedStructure(id = mock<AutofillId>(), autofillHints = listOf("password"), domain = "victim.example.com")

        val result = parser.applyDomainTrust(setOf(username, password), "com.example.spoofer")

        assertThat(result.structures.map { it.domain }).containsExactly(null, null)
        assertThat(result.structures.map { it.autofillHints }).containsExactly(listOf("username"), listOf("password"))
    }

    @Test
    fun `applyDomainTrust drops reported domains for a null requesting package`() {
        val structures =
            setOf(
                ParsedStructure(id = mock<AutofillId>(), autofillHints = listOf("username"), domain = "victim.example.com"),
            )

        val result = parser.applyDomainTrust(structures, null)

        assertThat(result.structures.single().domain).isNull()
    }

    @Test
    fun `applyDomainTrust preserves field identity for an untrusted application`() {
        val id = mock<AutofillId>()
        val structures = setOf(ParsedStructure(id = id, autofillHints = listOf("username"), domain = "victim.example.com"))

        val result = parser.applyDomainTrust(structures, "com.example.spoofer")

        assertThat(result.structures.single().id).isEqualTo(id)
    }
}
