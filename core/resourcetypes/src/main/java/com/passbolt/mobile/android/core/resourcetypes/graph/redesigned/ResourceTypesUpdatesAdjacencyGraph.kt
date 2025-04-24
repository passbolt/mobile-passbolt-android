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

package com.passbolt.mobile.android.core.resourcetypes.graph.redesigned

import com.passbolt.mobile.android.core.resourcetypes.graph.UpdateActionMetadata
import com.passbolt.mobile.android.core.resourcetypes.graph.base.ResourceTypeEdge
import com.passbolt.mobile.android.core.resourcetypes.graph.base.ResourceTypeVertex
import com.passbolt.mobile.android.supportedresourceTypes.ContentType.PasswordAndDescription
import com.passbolt.mobile.android.supportedresourceTypes.ContentType.PasswordDescriptionTotp
import com.passbolt.mobile.android.supportedresourceTypes.ContentType.PasswordString
import com.passbolt.mobile.android.supportedresourceTypes.ContentType.Totp
import com.passbolt.mobile.android.supportedresourceTypes.ContentType.V5Default
import com.passbolt.mobile.android.supportedresourceTypes.ContentType.V5DefaultWithTotp
import com.passbolt.mobile.android.supportedresourceTypes.ContentType.V5PasswordString
import com.passbolt.mobile.android.supportedresourceTypes.ContentType.V5TotpStandalone

class ResourceTypesUpdatesAdjacencyGraph {
    private val adjacencyMap: Map<ResourceTypeVertex, List<ResourceTypeEdge>>

    init {
        val map = mutableMapOf<ResourceTypeVertex, List<ResourceTypeEdge>>()

        // vertexes (resource types)
        val simplePassword = ResourceTypeVertex(PasswordString)
        val passwordAndDescription = ResourceTypeVertex(PasswordAndDescription)
        val totp = ResourceTypeVertex(Totp)
        val passwordDescriptionTotp = ResourceTypeVertex(PasswordDescriptionTotp)
        val v5PasswordString = ResourceTypeVertex(V5PasswordString)
        val v5Default = ResourceTypeVertex(V5Default)
        val v5Totp = ResourceTypeVertex(V5TotpStandalone)
        val v5DefaultWithTotp = ResourceTypeVertex(V5DefaultWithTotp)

        // edges (actions)
        map[simplePassword] = listOf(
            ResourceTypeEdge(simplePassword, simplePassword, UpdateAction.EDIT_METADATA),
            ResourceTypeEdge(simplePassword, simplePassword, UpdateAction.ADD_PASSWORD),
            ResourceTypeEdge(simplePassword, simplePassword, UpdateAction.REMOVE_PASSWORD),
            ResourceTypeEdge(simplePassword, simplePassword, UpdateAction.ADD_METADATA_DESCRIPTION),
            ResourceTypeEdge(simplePassword, simplePassword, UpdateAction.REMOVE_METADATA_DESCRIPTION)
        )
        map[v5PasswordString] = listOf(
            ResourceTypeEdge(v5PasswordString, v5PasswordString, UpdateAction.EDIT_METADATA),
            ResourceTypeEdge(v5PasswordString, v5PasswordString, UpdateAction.ADD_PASSWORD),
            ResourceTypeEdge(v5PasswordString, v5PasswordString, UpdateAction.REMOVE_PASSWORD),
            ResourceTypeEdge(v5PasswordString, v5PasswordString, UpdateAction.ADD_METADATA_DESCRIPTION),
            ResourceTypeEdge(v5PasswordString, v5PasswordString, UpdateAction.REMOVE_METADATA_DESCRIPTION),
            ResourceTypeEdge(v5PasswordString, v5DefaultWithTotp, UpdateAction.ADD_TOTP),
            ResourceTypeEdge(v5PasswordString, v5Default, UpdateAction.ADD_NOTE)
        )

        map[passwordAndDescription] = listOf(
            ResourceTypeEdge(passwordAndDescription, passwordAndDescription, UpdateAction.EDIT_METADATA),
            ResourceTypeEdge(passwordAndDescription, passwordAndDescription, UpdateAction.ADD_NOTE),
            ResourceTypeEdge(passwordAndDescription, passwordAndDescription, UpdateAction.REMOVE_NOTE),
            ResourceTypeEdge(passwordAndDescription, passwordAndDescription, UpdateAction.ADD_PASSWORD),
            ResourceTypeEdge(passwordAndDescription, passwordAndDescription, UpdateAction.REMOVE_PASSWORD),
            ResourceTypeEdge(passwordAndDescription, passwordDescriptionTotp, UpdateAction.ADD_TOTP)
        )
        map[v5Default] = listOf(
            ResourceTypeEdge(v5Default, v5Default, UpdateAction.EDIT_METADATA),
            ResourceTypeEdge(v5Default, v5Default, UpdateAction.ADD_NOTE),
            ResourceTypeEdge(v5Default, v5Default, UpdateAction.REMOVE_NOTE),
            ResourceTypeEdge(v5Default, v5Default, UpdateAction.ADD_PASSWORD),
            ResourceTypeEdge(v5Default, v5Default, UpdateAction.REMOVE_PASSWORD),
            ResourceTypeEdge(v5Default, v5Default, UpdateAction.ADD_METADATA_DESCRIPTION),
            ResourceTypeEdge(v5Default, v5Default, UpdateAction.REMOVE_METADATA_DESCRIPTION),
            ResourceTypeEdge(v5Default, v5DefaultWithTotp, UpdateAction.ADD_TOTP)
        )

        map[passwordDescriptionTotp] = listOf(
            ResourceTypeEdge(passwordDescriptionTotp, passwordDescriptionTotp, UpdateAction.EDIT_METADATA),
            ResourceTypeEdge(passwordDescriptionTotp, passwordDescriptionTotp, UpdateAction.ADD_NOTE),
            ResourceTypeEdge(passwordDescriptionTotp, passwordDescriptionTotp, UpdateAction.REMOVE_NOTE),
            ResourceTypeEdge(passwordDescriptionTotp, passwordDescriptionTotp, UpdateAction.ADD_TOTP),
            ResourceTypeEdge(passwordDescriptionTotp, passwordAndDescription, UpdateAction.REMOVE_TOTP),
            ResourceTypeEdge(passwordDescriptionTotp, passwordDescriptionTotp, UpdateAction.ADD_PASSWORD),
            ResourceTypeEdge(passwordDescriptionTotp, passwordDescriptionTotp, UpdateAction.REMOVE_PASSWORD),
            ResourceTypeEdge(passwordDescriptionTotp, totp, UpdateAction.REMOVE_PASSWORD_AND_NOTE)
        )
        map[v5DefaultWithTotp] = listOf(
            ResourceTypeEdge(v5DefaultWithTotp, v5DefaultWithTotp, UpdateAction.EDIT_METADATA),
            ResourceTypeEdge(v5DefaultWithTotp, v5DefaultWithTotp, UpdateAction.ADD_NOTE),
            ResourceTypeEdge(v5DefaultWithTotp, v5DefaultWithTotp, UpdateAction.REMOVE_NOTE),
            ResourceTypeEdge(v5DefaultWithTotp, v5DefaultWithTotp, UpdateAction.ADD_TOTP),
            ResourceTypeEdge(v5DefaultWithTotp, v5Default, UpdateAction.REMOVE_TOTP),
            ResourceTypeEdge(v5DefaultWithTotp, v5DefaultWithTotp, UpdateAction.ADD_PASSWORD),
            ResourceTypeEdge(v5DefaultWithTotp, v5DefaultWithTotp, UpdateAction.REMOVE_PASSWORD),
            ResourceTypeEdge(v5DefaultWithTotp, v5Totp, UpdateAction.REMOVE_PASSWORD_AND_NOTE),
            ResourceTypeEdge(v5DefaultWithTotp, v5DefaultWithTotp, UpdateAction.ADD_METADATA_DESCRIPTION),
            ResourceTypeEdge(v5DefaultWithTotp, v5DefaultWithTotp, UpdateAction.REMOVE_METADATA_DESCRIPTION)
        )

        map[totp] = listOf(
            ResourceTypeEdge(totp, totp, UpdateAction.EDIT_METADATA),
            ResourceTypeEdge(totp, totp, UpdateAction.ADD_TOTP),
            ResourceTypeEdge(totp, totp, UpdateAction.REMOVE_TOTP),
            ResourceTypeEdge(totp, passwordDescriptionTotp, UpdateAction.ADD_NOTE),
            ResourceTypeEdge(totp, passwordDescriptionTotp, UpdateAction.ADD_PASSWORD)
        )
        map[v5Totp] = listOf(
            ResourceTypeEdge(v5Totp, v5Totp, UpdateAction.EDIT_METADATA),
            ResourceTypeEdge(v5Totp, v5Totp, UpdateAction.ADD_TOTP),
            ResourceTypeEdge(v5Totp, v5Totp, UpdateAction.REMOVE_TOTP),
            ResourceTypeEdge(v5Totp, v5DefaultWithTotp, UpdateAction.ADD_NOTE),
            ResourceTypeEdge(v5Totp, v5DefaultWithTotp, UpdateAction.ADD_PASSWORD),
            ResourceTypeEdge(v5Totp, v5Totp, UpdateAction.ADD_METADATA_DESCRIPTION),
            ResourceTypeEdge(v5Totp, v5Totp, UpdateAction.REMOVE_METADATA_DESCRIPTION)
        )

        adjacencyMap = map
    }

    /**
     * Get the update actions metadata (what action can update to what resource type)
     * that can be performed on the passed resource type.
     *
     * @param resourceTypeSlug The resource type slug.
     * @return The list of update actions with new resource type ids.
     */
    fun getUpdateActionsMetadata(resourceTypeSlug: String) =
        adjacencyMap
            .asSequence()
            .first { it.key.contentType.slug == resourceTypeSlug }
            .value
            .map { edge ->
                UpdateActionMetadata(edge.updateAction, edge.destination.contentType)
            }
            .toList()

    fun getResourceTypeSlugAfterUpdate(currentResourceTypeSlug: String, update: UpdateAction) =
        getUpdateActionsMetadata(currentResourceTypeSlug)
            .first { it.action == update }
            .newResourceType
}
