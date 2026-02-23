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

package com.passbolt.mobile.android.resourcepicker.screen.data

import com.passbolt.mobile.android.common.search.SearchableMatcher
import com.passbolt.mobile.android.common.urimatcher.AutofillUriMatcher
import com.passbolt.mobile.android.core.resources.usecase.db.GetLocalResourcesUseCase
import com.passbolt.mobile.android.core.resourcetypes.usecase.db.GetResourceTypeIdToSlugMappingUseCase
import com.passbolt.mobile.android.mappers.ResourcePickerMapper
import com.passbolt.mobile.android.resourcepicker.screen.ResourcePickerViewModel.Companion.SELECTABLE_RESOURCE_TYPES_SLUGS
import com.passbolt.mobile.android.supportedresourceTypes.SupportedContentTypes.allSlugs

class ResourcePickerDataProvider(
    private val getLocalResourcesUseCase: GetLocalResourcesUseCase,
    private val resourcePickerMapper: ResourcePickerMapper,
    private val getResourceTypeIdToSlugMappingUseCase: GetResourceTypeIdToSlugMappingUseCase,
    private val searchableMatcher: SearchableMatcher,
    private val autofillUriMatcher: AutofillUriMatcher,
) {
    suspend fun provideData(
        searchQuery: String?,
        suggestionUri: String?,
    ): ResourcePickerData {
        val selectableResourceTypesIds =
            getResourceTypeIdToSlugMappingUseCase
                .execute(Unit)
                .idToSlugMapping
                .filter { it.value in SELECTABLE_RESOURCE_TYPES_SLUGS }
                .keys

        val allResources =
            getLocalResourcesUseCase
                .execute(GetLocalResourcesUseCase.Input(allSlugs))
                .resources
                .map { resourcePickerMapper.map(it, selectableResourceTypesIds) }

        val suggestedResources =
            if (!suggestionUri.isNullOrBlank()) {
                allResources.filter {
                    autofillUriMatcher.isMatching(
                        suggestionUri,
                        it.resourceModel.metadataJsonModel.uris
                            .orEmpty() +
                            it.resourceModel.metadataJsonModel.uri
                                .orEmpty(),
                    )
                }
            } else {
                emptyList()
            }

        val filteredResources =
            if (!searchQuery.isNullOrBlank()) {
                allResources.filter { searchableMatcher.matches(it, searchQuery) }
            } else {
                allResources
            }

        return ResourcePickerData(
            suggestedResources = suggestedResources,
            resources = filteredResources,
        )
    }
}
