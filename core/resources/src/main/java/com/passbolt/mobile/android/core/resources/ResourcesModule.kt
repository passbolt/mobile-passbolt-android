package com.passbolt.mobile.android.core.resources

import com.passbolt.mobile.android.common.search.SearchableMatcher
import com.passbolt.mobile.android.core.resources.actions.ResourceCommonActionsInteractor
import com.passbolt.mobile.android.core.resources.actions.ResourceCreateActionsInteractor
import com.passbolt.mobile.android.core.resources.actions.ResourcePropertiesActionsInteractor
import com.passbolt.mobile.android.core.resources.actions.ResourceUpdateActionsInteractor
import com.passbolt.mobile.android.core.resources.actions.SecretPropertiesActionsInteractor
import com.passbolt.mobile.android.core.resources.interactor.create.CreateResourceInteractor
import com.passbolt.mobile.android.core.resources.interactor.update.UpdateResourceInteractor
import com.passbolt.mobile.android.core.resources.resourceicon.BackgroundColorIconProvider
import com.passbolt.mobile.android.core.resources.resourceicon.ResourceIconProvider
import com.passbolt.mobile.android.core.resources.usecase.AddToFavouritesUseCase
import com.passbolt.mobile.android.core.resources.usecase.DeleteResourceUseCase
import com.passbolt.mobile.android.core.resources.usecase.FavouritesInteractor
import com.passbolt.mobile.android.core.resources.usecase.GetResourcesPaginatedUseCase
import com.passbolt.mobile.android.core.resources.usecase.GetResourcesUseCase
import com.passbolt.mobile.android.core.resources.usecase.RebuildResourcePermissionsTablesUseCase
import com.passbolt.mobile.android.core.resources.usecase.RebuildResourceTablesUseCase
import com.passbolt.mobile.android.core.resources.usecase.RemoveFromFavouritesUseCase
import com.passbolt.mobile.android.core.resources.usecase.ResourceInteractor
import com.passbolt.mobile.android.core.resources.usecase.ResourceShareInteractor
import com.passbolt.mobile.android.core.resources.usecase.ShareResourceUseCase
import com.passbolt.mobile.android.core.resources.usecase.SimulateShareResourceUseCase
import com.passbolt.mobile.android.core.resources.usecase.db.resourcesDbModule
import com.passbolt.mobile.android.ui.ResourceModel
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.core.parameter.parametersOf
import org.koin.dsl.module

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
val resourcesModule =
    module {
        resourcesDbModule()

        singleOf(::GetResourcesUseCase)
        singleOf(::GetResourcesPaginatedUseCase)
        singleOf(::ResourceInteractor)
        singleOf(::SearchableMatcher)
        singleOf(::DeleteResourceUseCase)
        singleOf(::RebuildResourceTablesUseCase)
        singleOf(::RebuildResourcePermissionsTablesUseCase)
        singleOf(::SimulateShareResourceUseCase)
        singleOf(::ShareResourceUseCase)
        singleOf(::AddToFavouritesUseCase)
        singleOf(::RemoveFromFavouritesUseCase)
        singleOf(::FavouritesInteractor)
        singleOf(::ResourceShareInteractor)
        singleOf(::UpdateResourceInteractor)
        singleOf(::CreateResourceInteractor)
        factoryOf(::ResourceIconProvider)
        factoryOf(::BackgroundColorIconProvider)

        factory { (resource: ResourceModel) ->
            ResourcePropertiesActionsInteractor(
                resource,
                idToSlugMappingProvider = get(),
            )
        }
        factory { (resource: ResourceModel) ->
            ResourceCommonActionsInteractor(
                resource,
                favouritesInteractor = get(),
                deleteResourceUseCase = get(),
            )
        }
        factory { (resource: ResourceModel) ->
            SecretPropertiesActionsInteractor(
                resource,
                secretParser = get(),
                secretInteractor = get(),
                idToSlugMappingProvider = get(),
            )
        }
        factory { (resource: ResourceModel) ->
            ResourceUpdateActionsInteractor(
                resource,
                secretPropertiesActionsInteractor = get { parametersOf(resource) },
                updateResourceInteractor = get(),
                resourceTypesUpdateGraph = get(),
                updateLocalResourceUseCase = get(),
                idToSlugMappingProvider = get(),
                getLocalCurrentUserUseCase = get(),
                metadataPrivateKeysInteractor = get(),
                getLocalFolderPermissionsUseCase = get(),
                getLocalResourcePermissionsUseCase = get(),
                getMetadataKeysSettingsUseCase = get(),
                getMetadataKeysUseCase = get(),
                resourceTypeIdToSlugMappingProvider = get(),
            )
        }
        factory {
            ResourceCreateActionsInteractor(
                createResourceInteractor = get(),
                addLocalResourceUseCase = get(),
                addLocalResourcePermissionsUseCase = get(),
                resourceShareInteractor = get(),
                getLocalParentFolderPermissionsToApplyUseCase = get(),
                getLocalFolderPermissionsUseCase = get(),
                getMetadataKeysSettingsUseCase = get(),
                getMetadataTypesSettingsUseCase = get(),
                getMetadataKeysUseCase = get(),
                getLocalCurrentUserUseCase = get(),
                metadataPrivateKeysInteractor = get(),
                resourceTypeIdToSlugMappingProvider = get(),
            )
        }
    }
