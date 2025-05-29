package com.passbolt.mobile.android.common.search

import org.junit.Test
import kotlin.test.assertTrue

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
class ResourceSearchTest {
    private val matcher = SearchableMatcher()

    @Test
    fun `test if basic search matches`() {
        assertTrue { matcher.matches(searchable, SEARCH_1) }
    }

    @Test
    fun `test if case search matches`() {
        assertTrue { matcher.matches(searchable, SEARCH_1.uppercase()) }
    }

    @Test
    fun `test if adding whitespace matches`() {
        assertTrue { matcher.matches(searchable, "$SEARCH_1 $SEARCH_2 ") }
    }

    private companion object {
        private const val SEARCH_1 = "name"
        private const val SEARCH_2 = "username"

        private val searchable =
            object : Searchable {
                override val searchCriteria: String
                    get() = "$SEARCH_1$SEARCH_2"
            }
    }
}
