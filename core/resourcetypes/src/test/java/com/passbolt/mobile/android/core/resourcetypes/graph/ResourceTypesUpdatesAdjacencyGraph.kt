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

package com.passbolt.mobile.android.core.resourcetypes.graph

import com.google.common.truth.Truth.assertThat
import com.passbolt.mobile.android.core.resourcetypes.ResourceTypeFactory.Companion.SLUG_PASSWORD_AND_DESCRIPTION
import com.passbolt.mobile.android.core.resourcetypes.ResourceTypeFactory.Companion.SLUG_PASSWORD_DESCRIPTION_TOTP
import com.passbolt.mobile.android.core.resourcetypes.ResourceTypeFactory.Companion.SLUG_SIMPLE_PASSWORD
import com.passbolt.mobile.android.core.resourcetypes.ResourceTypeFactory.Companion.SLUG_TOTP
import org.junit.Test


class ResourceTypesUpdatesAdjacencyGraphTest {
    private val graph = ResourceTypesUpdatesAdjacencyGraph()

    @Test
    fun `actions are correct for simple password`() {
        val actions = graph.getUpdateActionsMetadata(SLUG_SIMPLE_PASSWORD)

        assertThat(actions).hasSize(1)
        assertThat(actions.any { it.action == UpdateAction.EDIT_PASSWORD }).isTrue()
    }

    @Test
    fun `actions are correct for password and description`() {
        val actions = graph.getUpdateActionsMetadata(SLUG_PASSWORD_AND_DESCRIPTION)

        assertThat(actions).hasSize(2)
        assertThat(actions.any { it.action == UpdateAction.EDIT_PASSWORD }).isTrue()
        assertThat(actions.any { it.action == UpdateAction.ADD_TOTP }).isTrue()
    }

    @Test
    fun `actions are correct for password description totp`() {
        val actions = graph.getUpdateActionsMetadata(SLUG_PASSWORD_DESCRIPTION_TOTP)

        assertThat(actions).hasSize(3)
        assertThat(actions.any { it.action == UpdateAction.EDIT_PASSWORD }).isTrue()
        assertThat(actions.any { it.action == UpdateAction.EDIT_TOTP }).isTrue()
        assertThat(actions.any { it.action == UpdateAction.REMOVE_TOTP }).isTrue()
    }

    @Test
    fun `actions are correct for totp`() {
        val actions = graph.getUpdateActionsMetadata(SLUG_TOTP)

        assertThat(actions).hasSize(1)
        assertThat(actions.any { it.action == UpdateAction.EDIT_TOTP }).isTrue()
    }
}
