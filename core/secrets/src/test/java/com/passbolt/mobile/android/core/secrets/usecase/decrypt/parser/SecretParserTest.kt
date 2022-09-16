package com.passbolt.mobile.android.feature.secrets.usecase.decrypt.parser

import com.google.common.truth.Truth.assertThat
import com.google.gson.GsonBuilder
import com.passbolt.mobile.android.core.resourcetypes.ResourceTypeFactory
import com.passbolt.mobile.android.core.secrets.usecase.decrypt.parser.SecretParser
import org.junit.Test


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
class SecretParserTest {

    private val secretParser = SecretParser(GsonBuilder().create())

    @Test
    fun `password with special characters should parse correct for string secret`() {
        val secret = "\\\"!@#_$%^&*()".toByteArray()

        val extracted = secretParser.extractPassword(com.passbolt.mobile.android.core.resourcetypes.ResourceTypeFactory.ResourceTypeEnum.SIMPLE_PASSWORD, secret)

        assertThat(extracted).isEqualTo(String(secret))
    }

    @Test
    fun `password with special characters should parse correct for complex secret`() {
        val secret = "{\"password\":\"\\\"!@#_\$%^&*()\", \"description\":\"desc\"}".toByteArray()

        val extracted = secretParser.extractPassword(
            com.passbolt.mobile.android.core.resourcetypes.ResourceTypeFactory.ResourceTypeEnum.PASSWORD_WITH_DESCRIPTION, secret
        )

        assertThat(extracted).isEqualTo("\"!@#_\$%^&*()")
    }
}
