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
import com.passbolt.mobile.android.core.resourcetypes.graph.redesigned.ResourceTypesUpdatesAdjacencyGraph2
import com.passbolt.mobile.android.core.resourcetypes.graph.redesigned.UpdateAction2
import com.passbolt.mobile.android.supportedresourceTypes.ContentType
import org.junit.Test


class ResourceTypesUpdatesAdjacencyGraph2Test {
    private val graph = ResourceTypesUpdatesAdjacencyGraph2()

    @Test
    fun `actions are correct for v4,v5 password string`() {
        val actionsV4 = graph.getUpdateAction2sMetadata(ContentType.PasswordString.slug)
        val actionsV5 = graph.getUpdateAction2sMetadata(ContentType.V5PasswordString.slug)

        setOf(actionsV4, actionsV5).forEach { actions ->
            assertThat(actions).hasSize(5)
            assertThat(actions.map { it.action }).containsExactly(
                UpdateAction2.EDIT_METADATA,
                UpdateAction2.ADD_PASSWORD,
                UpdateAction2.REMOVE_PASSWORD,
                UpdateAction2.ADD_METADATA_DESCRIPTION,
                UpdateAction2.REMOVE_METADATA_DESCRIPTION
            )
        }
    }

    @Test
    fun `actions are correct for v4, v5 password and description`() {
        val actionsv4 = graph.getUpdateAction2sMetadata(ContentType.PasswordAndDescription.slug)
        val actionsv5 = graph.getUpdateAction2sMetadata(ContentType.V5Default.slug)

        setOf(actionsv4, actionsv5).forEach { actions ->
            assertThat(actions).hasSize(6)
            assertThat(actions.map { it.action }).containsExactly(
                UpdateAction2.EDIT_METADATA,
                UpdateAction2.ADD_NOTE,
                UpdateAction2.REMOVE_NOTE,
                UpdateAction2.ADD_PASSWORD,
                UpdateAction2.REMOVE_PASSWORD,
                UpdateAction2.ADD_TOTP
            )
        }
    }

    @Test
    fun `actions are correct for v4, v5 password description totp`() {
        val actionsV4 = graph.getUpdateAction2sMetadata(ContentType.PasswordDescriptionTotp.slug)
        val actionsV5 = graph.getUpdateAction2sMetadata(ContentType.V5DefaultWithTotp.slug)

        setOf(actionsV4, actionsV5).forEach { actions ->
            assertThat(actions).hasSize(8)
            assertThat(actions.map { it.action }).containsExactly(
                UpdateAction2.EDIT_METADATA,
                UpdateAction2.ADD_TOTP,
                UpdateAction2.REMOVE_TOTP,
                UpdateAction2.ADD_NOTE,
                UpdateAction2.REMOVE_NOTE,
                UpdateAction2.ADD_PASSWORD,
                UpdateAction2.REMOVE_PASSWORD,
                UpdateAction2.REMOVE_PASSWORD_AND_NOTE
            )
        }
    }

    @Test
    fun `actions are correct for v4, v5 totp`() {
        val actionsV4 = graph.getUpdateAction2sMetadata(ContentType.Totp.slug)
        val actionsV5 = graph.getUpdateAction2sMetadata(ContentType.V5TotpStandalone.slug)

        setOf(actionsV4, actionsV5).forEach { actions ->
            assertThat(actions).hasSize(5)
            assertThat(actions.map { it.action }).containsExactly(
                UpdateAction2.EDIT_METADATA,
                UpdateAction2.ADD_TOTP,
                UpdateAction2.REMOVE_TOTP,
                UpdateAction2.ADD_NOTE,
                UpdateAction2.ADD_PASSWORD,
            )
        }
    }
}
