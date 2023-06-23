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

package com.passbolt.mobile.android.common

import com.google.common.truth.Truth.assertThat
import org.junit.Test


class DomainProviderTest {

    private val domainProvider = DomainProvider()

    @Test
    fun `domain provider should provide domain for valid https url`() {
        val validUrl = "https://www.google.com"

        val result = domainProvider.getHost(validUrl)

        assertThat(result).isEqualTo("www.google.com")
    }

    @Test
    fun `domain provider should provide domain for valid http url`() {
        val validUrl = "http://www.google.com"

        val result = domainProvider.getHost(validUrl)

        assertThat(result).isEqualTo("www.google.com")
    }

    @Test
    fun `domain provider should provide domain for url with no scheme`() {
        val validUrl = "www.google.com"

        val result = domainProvider.getHost(validUrl)

        assertThat(result).isEqualTo("www.google.com")
    }

    @Test
    fun `domain provider should provide domain for url with no scheme and no www`() {
        val validUrl = "google.com"

        val result = domainProvider.getHost(validUrl)

        assertThat(result).isEqualTo("google.com")
    }

    @Test
    fun `domain provider should provide domain for url with port number`() {
        val invalidUrl = "google.com:123"

        val result = domainProvider.getHost(invalidUrl)

        assertThat(result).isEqualTo("google.com")
    }

    @Test
    fun `domain provider should provide domain for url with empty port number`() {
        val invalidUrl = "google.com:"

        val result = domainProvider.getHost(invalidUrl)

        assertThat(result).isEqualTo("google.com")
    }

    @Test
    fun `domain provider should provide domain for url with invalid port number`() {
        val invalidUrl = "google.com:a"

        val result = domainProvider.getHost(invalidUrl)

        assertThat(result).isNull()
    }
}
