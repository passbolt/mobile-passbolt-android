package com.passbolt.mobile.android.core.resources.actions

import androidx.annotation.VisibleForTesting
import com.passbolt.mobile.android.core.resourcetypes.usecase.db.ResourceTypeIdToSlugMappingProvider
import com.passbolt.mobile.android.supportedresourceTypes.ContentType
import com.passbolt.mobile.android.ui.ResourceModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.single
import java.util.UUID

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
class ResourcePropertiesActionsInteractor(
    private val resource: ResourceModel,
    private val idToSlugMappingProvider: ResourceTypeIdToSlugMappingProvider
) {

    suspend fun provideWebsiteUrl(): Flow<ResourcePropertyActionResult<String>> {
        val resourceContentType = ContentType.fromSlug(
            idToSlugMappingProvider.provideMappingForSelectedAccount()[
                UUID.fromString(resource.resourceTypeId)
            ]!!
        )
        return flowOf(
            ResourcePropertyActionResult(
                URL_LABEL,
                isSecret = false,
                if (resourceContentType.isV5()) {
                    resource.metadataJsonModel.uris?.firstOrNull()
                } else {
                    resource.metadataJsonModel.uri
                }.orEmpty()
            )
        )
    }

    fun provideUsername(): Flow<ResourcePropertyActionResult<String>> =
        flowOf(
            ResourcePropertyActionResult(
                USERNAME_LABEL,
                isSecret = false,
                resource.metadataJsonModel.username.orEmpty()
            )
        )

    // provides description from resource model (for description from secret model see
    // ResourceAuthenticatedActionsInteractor
    fun provideDescription(): Flow<ResourcePropertyActionResult<String>> =
        flowOf(
            ResourcePropertyActionResult(
                DESCRIPTION_LABEL,
                isSecret = false,
                resource.metadataJsonModel.description.orEmpty()
            )
        )

    companion object {
        @VisibleForTesting
        const val USERNAME_LABEL = "Username"

        @VisibleForTesting
        const val URL_LABEL = "Url"

        @VisibleForTesting
        const val DESCRIPTION_LABEL = "Description"
    }
}

suspend fun <T> performResourcePropertyAction(
    action: suspend () -> Flow<ResourcePropertyActionResult<T>>,
    doOnResult: (ResourcePropertyActionResult<T>) -> Unit
) {
    doOnResult(action().single())
}
