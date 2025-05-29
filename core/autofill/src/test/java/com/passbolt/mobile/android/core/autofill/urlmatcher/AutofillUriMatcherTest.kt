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

package com.passbolt.mobile.android.core.autofill.urlmatcher

import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test

// Supported protocols: http, https, ftp
class AutofillUriMatcherTest {
    private val autofillUriMatcher = AutofillUriMatcher()

    @Test
    fun `should suggest matching domain urls`() {
        listOf(
            "http://www.passbolt.com" to "http://www.passbolt.com",
            "https://www.passbolt.com" to "https://www.passbolt.com",
            "ftp://www.passbolt.com" to "ftp://www.passbolt.com",
            "https://www.passbolt.com:443" to "https://www.passbolt.com:443",
        ).map { (autofillUrl, resourceUrl) ->
            autofillUriMatcher.isMatching(autofillUrl, resourceUrl) to listOf(autofillUrl, resourceUrl)
        }.forEach { (result, uris) ->
            assertWithMessage("${uris[0]} should match ${uris[1]}")
                .that(result)
                .isTrue()
        }
    }

    @Test
    fun `should suggest matching international domain urls`() {
        listOf(
            "https://àáâãäåæçèéêëìíîïðñòóôõöøùúûüýþÿ.com" to "https://àáâãäåæçèéêëìíîïðñòóôõöøùúûüýþÿ.com",
            "https://الش.com" to "https://الش.com",
            "https://Ид.com" to "https://Ид.com",
            "https://完善.com" to "https://完善.com",
        ).map { (autofillUrl, resourceUrl) ->
            autofillUriMatcher.isMatching(autofillUrl, resourceUrl) to listOf(autofillUrl, resourceUrl)
        }.forEach { (result, uris) ->
            assertWithMessage("${uris[0]} should match ${uris[1]}")
                .that(result)
                .isTrue()
        }
    }

    @Test
    fun `should suggest matching IPv4 and IPv6 urls`() {
        listOf(
            "http://[0:0:0:0:0:0:0:1]" to "http://[0:0:0:0:0:0:0:1]",
            "http://127.0.0.1" to "http://127.0.0.1",
            "https://[0:0:0:0:0:0:0:1]" to "https://[0:0:0:0:0:0:0:1]",
            "https://127.0.0.1" to "https://127.0.0.1",
            "ftp://[0:0:0:0:0:0:0:1]" to "ftp://[0:0:0:0:0:0:0:1]",
            "ftp://127.0.0.1" to "ftp://127.0.0.1",
            "https://[0:0:0:0:0:0:0:1]:443" to "https://[0:0:0:0:0:0:0:1]:443",
            "https://127.0.0.1:443" to "https://127.0.0.1:443",
        ).map { (autofillUrl, resourceUrl) ->
            autofillUriMatcher.isMatching(autofillUrl, resourceUrl) to listOf(autofillUrl, resourceUrl)
        }.forEach { (result, uris) ->
            assertWithMessage("${uris[0]} should match ${uris[1]}")
                .that(result)
                .isTrue()
        }
    }

    @Test
    fun `should suggest urls without defined scheme`() {
        listOf(
            "http://127.0.0.1" to "127.0.0.1",
            "http://www.passbolt.com" to "www.passbolt.com",
            "https://127.0.0.1" to "127.0.0.1",
            "https://www.passbolt.com" to "www.passbolt.com",
            "ftp://127.0.0.1" to "127.0.0.1",
            "ftp://www.passbolt.com" to "www.passbolt.com",
        ).map { (autofillUrl, resourceUrl) ->
            autofillUriMatcher.isMatching(autofillUrl, resourceUrl) to listOf(autofillUrl, resourceUrl)
        }.forEach { (result, uris) ->
            assertWithMessage("${uris[0]} should match ${uris[1]}")
                .that(result)
                .isTrue()
        }
    }

    @Test
    fun `should suggest urls without defined port`() {
        listOf(
            "http://127.0.0.1:8080" to "127.0.0.1",
            "http://127.0.0.1:4443" to "127.0.0.1",
        ).map { (autofillUrl, resourceUrl) ->
            autofillUriMatcher.isMatching(autofillUrl, resourceUrl) to listOf(autofillUrl, resourceUrl)
        }.forEach { (result, uris) ->
            assertWithMessage("${uris[0]} should match ${uris[1]}")
                .that(result)
                .isTrue()
        }
    }

    @Test
    fun `should suggest url with a parent domain`() {
        listOf(
            "https://www.passbolt.com" to "passbolt.com",
            "https://www.passbolt.com" to "https://passbolt.com",
            "https://billing.admin.passbolt.com" to "passbolt.com",
        ).map { (autofillUrl, resourceUrl) ->
            autofillUriMatcher.isMatching(autofillUrl, resourceUrl) to listOf(autofillUrl, resourceUrl)
        }.forEach { (result, uris) ->
            assertWithMessage("${uris[0]} should match ${uris[1]}")
                .that(result)
                .isTrue()
        }
    }

    @Test
    fun `should NOT suggest urls not matching the exact domain`() {
        listOf(
            "https://www.not-passbolt.com" to "passbolt.com",
            "https://bolt.com" to "passbolt.com",
            "https://pass" to "passbolt.com",
            "https://www.attacker-passbolt.com" to "passbolt.com",
            "https://titan.email" to "email",
            "https://email" to "http://email",
            "https://titan.email" to "https://email",
        ).map { (autofillUrl, resourceUrl) ->
            autofillUriMatcher.isMatching(autofillUrl, resourceUrl) to listOf(autofillUrl, resourceUrl)
        }.forEach { (result, uris) ->
            assertWithMessage("${uris[0]} should NOT match ${uris[1]}")
                .that(result)
                .isFalse()
        }
    }

    @Test
    fun `should NOT suggest IPs not matching the exact domain`() {
        listOf(
            // fake IPs url with a subdomain "fake" trying to phish a suggested IP url.
            "https://fake.127.0.0.1" to "127.0.0.1",
            // fake IPs url with a subdomain "127", only composed of digit,  trying to phish a suggested IP url.
            "https://127.127.0.0.1" to "127.0.0.1",
        ).map { (autofillUrl, resourceUrl) ->
            autofillUriMatcher.isMatching(autofillUrl, resourceUrl) to listOf(autofillUrl, resourceUrl)
        }.forEach { (result, uris) ->
            assertWithMessage("${uris[0]} should NOT match ${uris[1]}")
                .that(result)
                .isFalse()
        }
    }

    @Test
    fun `should NOT suggest urls with not matching subdomain to parent urls`() {
        listOf(
            "https://passbolt.com" to "www.passbolt.com",
            "https://passbolt.com" to "https://www.passbolt.com",
        ).map { (autofillUrl, resourceUrl) ->
            autofillUriMatcher.isMatching(autofillUrl, resourceUrl) to listOf(autofillUrl, resourceUrl)
        }.forEach { (result, uris) ->
            assertWithMessage("${uris[0]} should NOT match ${uris[1]}")
                .that(result)
                .isFalse()
        }
    }

    @Test
    fun `should NOT suggest urls to an attacker url containing a subdomain looking alike a stored password url`() {
        listOf(
            "https://www.passbolt.com.attacker.com" to "passbolt.com",
            "https://www.passbolt.com-attacker.com" to "passbolt.com",
        ).map { (autofillUrl, resourceUrl) ->
            autofillUriMatcher.isMatching(autofillUrl, resourceUrl) to listOf(autofillUrl, resourceUrl)
        }.forEach { (result, uris) ->
            assertWithMessage("${uris[0]} should NOT match ${uris[1]}")
                .that(result)
                .isFalse()
        }
    }

    @Test
    fun `should NOT suggest urls to an attacker url containing a parameter looking alike a stored password url`() {
        listOf(
            "https://attacker.com?passbolt.com" to "passbolt.com",
            "https://attacker.com?passbolt.com" to "passbolt.com",
            "https://attacker.com?url=https://passbolt.com" to "passbolt.com",
        ).map { (autofillUrl, resourceUrl) ->
            autofillUriMatcher.isMatching(autofillUrl, resourceUrl) to listOf(autofillUrl, resourceUrl)
        }.forEach { (result, uris) ->
            assertWithMessage("${uris[0]} should NOT match ${uris[1]}")
                .that(result)
                .isFalse()
        }
    }

    @Test
    fun `should NOT suggest urls to an attacker url containing a hash looking alike a stored password url`() {
        listOf(
            "https://attacker.com#passbolt.com" to "passbolt.com",
            "https://attacker.com#passbolt.com" to "passbolt.com",
            "https://attacker.com#url=https://passbolt.com" to "passbolt.com",
        ).map { (autofillUrl, resourceUrl) ->
            autofillUriMatcher.isMatching(autofillUrl, resourceUrl) to listOf(autofillUrl, resourceUrl)
        }.forEach { (result, uris) ->
            assertWithMessage("${uris[0]} should NOT match ${uris[1]}")
                .that(result)
                .isFalse()
        }
    }

    @Test
    fun `should NOT suggest urls with a port looking alike a stored password url`() {
        listOf(
            "https://www.attacker.com:www.passbolt.com" to "passbolt.com",
        ).map { (autofillUrl, resourceUrl) ->
            autofillUriMatcher.isMatching(autofillUrl, resourceUrl) to listOf(autofillUrl, resourceUrl)
        }.forEach { (result, uris) ->
            assertWithMessage("${uris[0]} should NOT match ${uris[1]}")
                .that(result)
                .isFalse()
        }
    }

    @Test
    fun `should NOT suggest IP urls to fake IPs urls`() {
        listOf(
            "https://[::1]" to "[::2]",
            "https://[2001:4860:4860::8844]" to "[2001:4860:4860::8888]",
            "https://127.0.0.1" to "127.0.0.2",
            "https://127.1" to "127.2",
        ).map { (autofillUrl, resourceUrl) ->
            autofillUriMatcher.isMatching(autofillUrl, resourceUrl) to listOf(autofillUrl, resourceUrl)
        }.forEach { (result, uris) ->
            assertWithMessage("${uris[0]} should NOT match ${uris[1]}")
                .that(result)
                .isFalse()
        }
    }

    @Test
    fun `should NOT suggest urls if the scheme is different`() {
        listOf(
            "http://127.0.0.1" to "https://127.0.0.1",
            "https://127.0.0.1" to "http://127.0.0.1",
            "http://[::1]" to "https://[::1]",
            "https://[::1]" to "http://[::1]",
            "http://www.passbolt.com" to "https://www.passbolt.com",
            "https://www.passbolt.com" to "http://www.passbolt.com",
        ).map { (autofillUrl, resourceUrl) ->
            autofillUriMatcher.isMatching(autofillUrl, resourceUrl) to listOf(autofillUrl, resourceUrl)
        }.forEach { (result, uris) ->
            assertWithMessage("${uris[0]} should NOT match ${uris[1]}")
                .that(result)
                .isFalse()
        }
    }

    @Test
    fun `should NOT suggest urls if the port is different`() {
        listOf(
            "http://127.0.0.1" to "127.0.0.1:444",
            "http://www.passbolt.com" to "www.passbolt.com:444",
            "https://127.0.0.1" to "127.0.0.1:80",
            "https://www.passbolt.com" to "www.passbolt.com:80",
            "https://127.0.0.1:444" to "127.0.0.1:443",
            "https://www.passbolt.com:444" to "www.passbolt.com:443",
            /*
             * Ports are not deducted from urls schemes, that's why we expect http scheme to not match an url with a defined
             * port, even if it is the correct one.
             */
            "http://127.0.0.1" to "127.0.0.1:80",
            "https://www.passbolt.com" to "www.passbolt.com:443",
        ).map { (autofillUrl, resourceUrl) ->
            autofillUriMatcher.isMatching(autofillUrl, resourceUrl) to listOf(autofillUrl, resourceUrl)
        }.forEach { (result, uris) ->
            assertWithMessage("${uris[0]} should NOT match ${uris[1]}")
                .that(result)
                .isFalse()
        }
    }

    @Test
    fun `should NOT suggest urls with no hostname to url with no hostname`() {
        listOf(
            "https://no%20identified%20domain%20url.com" to
                "no%20identified%20domain%20url",
            "about:addons" to "about:addons",
            "about:addons" to "no%20identified%20domain%20url",
        ).map { (autofillUrl, resourceUrl) ->
            autofillUriMatcher.isMatching(autofillUrl, resourceUrl) to listOf(autofillUrl, resourceUrl)
        }.forEach { (result, uris) ->
            assertWithMessage("${uris[0]} should NOT match ${uris[1]}")
                .that(result)
                .isFalse()
        }
    }
}
