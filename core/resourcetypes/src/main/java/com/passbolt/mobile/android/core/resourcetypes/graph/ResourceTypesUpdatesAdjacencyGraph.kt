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

import com.passbolt.mobile.android.core.resourcetypes.ResourceTypeFactory.Companion.SLUG_PASSWORD_AND_DESCRIPTION
import com.passbolt.mobile.android.core.resourcetypes.ResourceTypeFactory.Companion.SLUG_PASSWORD_DESCRIPTION_TOTP
import com.passbolt.mobile.android.core.resourcetypes.ResourceTypeFactory.Companion.SLUG_SIMPLE_PASSWORD
import com.passbolt.mobile.android.core.resourcetypes.ResourceTypeFactory.Companion.SLUG_TOTP
import com.passbolt.mobile.android.core.resourcetypes.graph.base.ResourceTypeEdge
import com.passbolt.mobile.android.core.resourcetypes.graph.base.ResourceTypeVertex

class ResourceTypesUpdatesAdjacencyGraph {
    private val adjacencyMap: Map<ResourceTypeVertex, List<ResourceTypeEdge>>

    init {
        val map = mutableMapOf<ResourceTypeVertex, List<ResourceTypeEdge>>()

        // vertexes (resource types)
        val simplePassword = ResourceTypeVertex(SLUG_SIMPLE_PASSWORD)
        val passwordAndDescription = ResourceTypeVertex(SLUG_PASSWORD_AND_DESCRIPTION)
        val totp = ResourceTypeVertex(SLUG_TOTP)
        val passwordDescriptionTotp = ResourceTypeVertex(SLUG_PASSWORD_DESCRIPTION_TOTP)

        // edges (actions)
        map[simplePassword] = listOf(
            ResourceTypeEdge(simplePassword, simplePassword, UpdateAction.EDIT_PASSWORD)
        )
        map[passwordAndDescription] = listOf(
            ResourceTypeEdge(passwordAndDescription, passwordAndDescription, UpdateAction.EDIT_PASSWORD),
            ResourceTypeEdge(passwordAndDescription, passwordDescriptionTotp, UpdateAction.ADD_TOTP)
        )
        map[passwordDescriptionTotp] = listOf(
            ResourceTypeEdge(passwordDescriptionTotp, passwordDescriptionTotp, UpdateAction.EDIT_PASSWORD),
            ResourceTypeEdge(passwordDescriptionTotp, passwordDescriptionTotp, UpdateAction.EDIT_TOTP),
            ResourceTypeEdge(passwordDescriptionTotp, passwordAndDescription, UpdateAction.REMOVE_TOTP)
        )
        map[totp] = listOf(
            ResourceTypeEdge(totp, totp, UpdateAction.EDIT_TOTP)
        )

        adjacencyMap = map
    }

    /**
     * Get the update actions metadata (what action can update to what resource type)
     * that can be performed on the passed resource type.
     *
     * @param resourceTypeId The resource type id.
     * @return The list of update actions with new resource type ids.
     */
    fun getUpdateActionsMetadata(resourceTypeSlug: String) =
        adjacencyMap
            .asSequence()
            .first { it.key.slug == resourceTypeSlug }
            .value
            .map { edge ->
                UpdateActionMetadata(edge.updateAction, edge.destination.slug)
            }
            .toList()
}
