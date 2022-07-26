package com.passbolt.mobile.android.core.commonresource.validation

import com.google.common.truth.Truth.assertThat
import com.passbolt.mobile.android.ui.ResourceModel
import com.passbolt.mobile.android.ui.ResourcePermission
import org.junit.Test
import java.time.ZonedDateTime
import java.util.UUID
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
class ResourceUuidValidationTest {

    private val uuidValidation = ResourceUuidValidation()

    @Test
    fun `resource uuid validation should validate valid uuid correct`() {
        val validUuids = List(10) { UUID.randomUUID() }
            .map { dummyResourceModel.copy(resourceId = it.toString()) }

        val validations = validUuids.map { it to uuidValidation.invoke(it) }

        assertTrue("Invalid uuid found in ${validations.map { it.first.resourceId }}") {
            validations.all { it.second }
        }
    }

    @Test
    fun `resource uuid validation should report invalid uuids correct`() {
        val invalidUuids = listOf(
            "",
            "---",
            "1-1-1",
            "a_word"
        ).map { dummyResourceModel.copy(resourceId = it) }

        assertThat(invalidUuids.associateWith { uuidValidation.invoke(it) }.values)
            .doesNotContain(true)
    }

    private companion object {
        private val dummyResourceModel = ResourceModel(
            "", "","folderid", "",
            "", "", "", "",
            "", ResourcePermission.OWNER, null, ZonedDateTime.now()
        )
    }
}
