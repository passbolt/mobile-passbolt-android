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

import com.google.common.truth.Truth.assertThat
import org.junit.Test


class AutofillUrlMatcherTest {

    private val autofillUrlMatcher = AutofillUrlMatcher()

    @Test
    fun `autofill and resource urls should be matched correct with domains`() {
        val positiveAutofillUrlToResourceUrl = listOf(
            "https://www.passbolt.com" to "https://www.passbolt.com",
            "https://www.passbolt.com" to "https://passbolt.com",
            "https://passbolt.com" to "https://passbolt.com",
            "https://àáâãäåæçèéêëìíîïðñòóôõöøùúûüýþÿ.com" to "https://àáâãäåæçèéêëìíîïðñòóôõöøùúûüýþÿ.com",
            "https://الش.com" to "https://الش.com",
            "https://完善.com" to "https://完善.com",
            "https://email" to "https://email",
            "https://www.passbolt.com/path" to "https://www.passbolt.com",
        )

        val negativeAutofillUrlToResourceUrl = listOf(
            "http://passbolt.com" to "https://passbolt.com",
            "https://passbolt.com.test.com" to "https://passbolt.com",
            "http://www.passbolt.com" to "https://www.passbolt.com",
            "https://passbolt.com" to "https://www.passbolt.com",
            "https://my.passbolt.com" to "https://www.passbolt.com",
            "" to "https://www.passbolt.com",
            "https://www.passbolt.com" to "",
            "https://attacker-passbolt.com" to "https://passbolt.com",
            "https://titan.email" to "https://email"
        )

        val positiveResults = positiveAutofillUrlToResourceUrl.map { (autofillUrl, resourceUrl) ->
            autofillUrlMatcher.isMatching(autofillUrl, resourceUrl)
        }

        val negativeResults = negativeAutofillUrlToResourceUrl.map { (autofillUrl, resourceUrl) ->
            autofillUrlMatcher.isMatching(autofillUrl, resourceUrl)
        }

        assertThat(positiveResults.all { true }).isTrue()
        assertThat(negativeResults.none { false }).isTrue()
    }

    @Test
    fun `autofill and resource urls should be matched correct with IPs`() {
        val positiveAutofillUrlToResourceUrl = listOf(
            "https://1.1.1.1" to "https://1.1.1.1",
            "1.1.1.1" to "1.1.1.1",
            "1.1.1.1:1" to "1.1.1.1:1",
            "http://[0:0:0:0:0:0:0:1" to "http://[0:0:0:0:0:0:0:1"
        )

        val negativeAutofillUrlToResourceUrl = listOf(
            "https://1.1.1.1:1" to "https://1.1.1.1:2",
            "https://1.1.1.1:1" to "https://1.1.1.1",
            "http://[0:0:0:0:0:0:0:1" to "http://[0:0:0:0:0:0:0:3",
            "https://fake.127.0.0.1" to "127.0.0.1",
            "http://www.passbolt.com" to "www.passbolt.com:444"
        )

        val positiveResults = positiveAutofillUrlToResourceUrl.map { (autofillUrl, resourceUrl) ->
            autofillUrlMatcher.isMatching(autofillUrl, resourceUrl)
        }

        val negativeResults = negativeAutofillUrlToResourceUrl.map { (autofillUrl, resourceUrl) ->
            autofillUrlMatcher.isMatching(autofillUrl, resourceUrl)
        }

        assertThat(positiveResults.all { true }).isTrue()
        assertThat(negativeResults.none { false }).isTrue()
    }
}
