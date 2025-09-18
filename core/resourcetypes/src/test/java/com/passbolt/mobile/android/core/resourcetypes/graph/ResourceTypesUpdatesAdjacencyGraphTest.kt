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
import com.passbolt.mobile.android.core.resourcetypes.graph.redesigned.ResourceTypesUpdatesAdjacencyGraph
import com.passbolt.mobile.android.core.resourcetypes.graph.redesigned.UpdateAction
import com.passbolt.mobile.android.supportedresourceTypes.ContentType
import org.junit.Test

class ResourceTypesUpdatesAdjacencyGraphTest {
    private val graph = ResourceTypesUpdatesAdjacencyGraph()

    @Test
    fun `actions are correct for password-string`() {
        val actions = graph.getUpdateActionsMetadata(ContentType.PasswordString.slug)

        assertThat(actions).hasSize(5)
        assertThat(actions.map { it.action }).containsExactly(
            UpdateAction.EDIT_METADATA,
            UpdateAction.ADD_PASSWORD,
            UpdateAction.REMOVE_PASSWORD,
            UpdateAction.ADD_METADATA_DESCRIPTION,
            UpdateAction.REMOVE_METADATA_DESCRIPTION,
        )
    }

    @Test
    fun `actions are correct for v5-password-string`() {
        val actions = graph.getUpdateActionsMetadata(ContentType.V5PasswordString.slug)

        assertThat(actions).hasSize(9)
        assertThat(actions.map { it.action }).containsExactly(
            UpdateAction.EDIT_METADATA,
            UpdateAction.ADD_PASSWORD,
            UpdateAction.REMOVE_PASSWORD,
            UpdateAction.ADD_METADATA_DESCRIPTION,
            UpdateAction.REMOVE_METADATA_DESCRIPTION,
            UpdateAction.ADD_TOTP,
            UpdateAction.ADD_NOTE,
            UpdateAction.EDIT_ADDITIONAL_URIS,
            UpdateAction.EDIT_APPEARANCE,
        )
    }

    @Test
    fun `actions are correct for password-and-description`() {
        val actions = graph.getUpdateActionsMetadata(ContentType.PasswordAndDescription.slug)

        assertThat(actions).hasSize(6)
        assertThat(actions.map { it.action }).containsExactly(
            UpdateAction.EDIT_METADATA,
            UpdateAction.ADD_NOTE,
            UpdateAction.REMOVE_NOTE,
            UpdateAction.ADD_PASSWORD,
            UpdateAction.REMOVE_PASSWORD,
            UpdateAction.ADD_TOTP,
        )
    }

    @Test
    fun `actions are correct for v5-default`() {
        val actions = graph.getUpdateActionsMetadata(ContentType.V5Default.slug)

        assertThat(actions).hasSize(11)
        assertThat(actions.map { it.action }).containsExactly(
            UpdateAction.EDIT_METADATA,
            UpdateAction.ADD_NOTE,
            UpdateAction.REMOVE_NOTE,
            UpdateAction.ADD_PASSWORD,
            UpdateAction.REMOVE_PASSWORD,
            UpdateAction.ADD_TOTP,
            UpdateAction.ADD_METADATA_DESCRIPTION,
            UpdateAction.REMOVE_METADATA_DESCRIPTION,
            UpdateAction.EDIT_ADDITIONAL_URIS,
            UpdateAction.EDIT_APPEARANCE,
            UpdateAction.ADD_CUSTOM_FIELDS,
        )
    }

    @Test
    fun `actions are correct for password-description-totp`() {
        val actions = graph.getUpdateActionsMetadata(ContentType.PasswordDescriptionTotp.slug)

        assertThat(actions).hasSize(8)
        assertThat(actions.map { it.action }).containsExactly(
            UpdateAction.EDIT_METADATA,
            UpdateAction.ADD_TOTP,
            UpdateAction.REMOVE_TOTP,
            UpdateAction.ADD_NOTE,
            UpdateAction.REMOVE_NOTE,
            UpdateAction.ADD_PASSWORD,
            UpdateAction.REMOVE_PASSWORD,
            UpdateAction.REMOVE_PASSWORD_AND_NOTE,
        )
    }

    @Test
    fun `actions are correct for v5-default-with-totp`() {
        val actions = graph.getUpdateActionsMetadata(ContentType.V5DefaultWithTotp.slug)

        assertThat(actions).hasSize(13)
        assertThat(actions.map { it.action }).containsExactly(
            UpdateAction.EDIT_METADATA,
            UpdateAction.ADD_TOTP,
            UpdateAction.REMOVE_TOTP,
            UpdateAction.ADD_NOTE,
            UpdateAction.REMOVE_NOTE,
            UpdateAction.ADD_PASSWORD,
            UpdateAction.REMOVE_PASSWORD,
            UpdateAction.REMOVE_PASSWORD_AND_NOTE,
            UpdateAction.ADD_METADATA_DESCRIPTION,
            UpdateAction.REMOVE_METADATA_DESCRIPTION,
            UpdateAction.EDIT_ADDITIONAL_URIS,
            UpdateAction.EDIT_APPEARANCE,
            UpdateAction.ADD_CUSTOM_FIELDS,
        )
    }

    @Test
    fun `actions are correct for totp`() {
        val actions = graph.getUpdateActionsMetadata(ContentType.Totp.slug)

        assertThat(actions).hasSize(5)
        assertThat(actions.map { it.action }).containsExactly(
            UpdateAction.EDIT_METADATA,
            UpdateAction.ADD_TOTP,
            UpdateAction.REMOVE_TOTP,
            UpdateAction.ADD_NOTE,
            UpdateAction.ADD_PASSWORD,
        )
    }

    @Test
    fun `actions are correct for v5-totp-standalone`() {
        val actions = graph.getUpdateActionsMetadata(ContentType.V5TotpStandalone.slug)

        assertThat(actions).hasSize(9)
        assertThat(actions.map { it.action }).containsExactly(
            UpdateAction.EDIT_METADATA,
            UpdateAction.ADD_TOTP,
            UpdateAction.REMOVE_TOTP,
            UpdateAction.ADD_NOTE,
            UpdateAction.ADD_PASSWORD,
            UpdateAction.REMOVE_METADATA_DESCRIPTION,
            UpdateAction.ADD_METADATA_DESCRIPTION,
            UpdateAction.EDIT_ADDITIONAL_URIS,
            UpdateAction.EDIT_APPEARANCE,
        )
    }

    @Test
    fun `actions are correct for v5-custom-fields`() {
        val actions = graph.getUpdateActionsMetadata(ContentType.V5CustomFields.slug)

        assertThat(actions).hasSize(8)
        assertThat(actions.map { it.action }).containsExactly(
            UpdateAction.EDIT_METADATA,
            UpdateAction.ADD_METADATA_DESCRIPTION,
            UpdateAction.REMOVE_METADATA_DESCRIPTION,
            UpdateAction.EDIT_ADDITIONAL_URIS,
            UpdateAction.EDIT_APPEARANCE,
            UpdateAction.ADD_CUSTOM_FIELDS,
            UpdateAction.ADD_PASSWORD,
            UpdateAction.ADD_TOTP,
        )
    }
}
