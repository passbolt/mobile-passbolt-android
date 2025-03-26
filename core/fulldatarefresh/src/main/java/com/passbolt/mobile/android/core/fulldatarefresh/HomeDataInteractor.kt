package com.passbolt.mobile.android.core.fulldatarefresh

import com.passbolt.mobile.android.core.commonfolders.usecase.FoldersInteractor
import com.passbolt.mobile.android.core.commongroups.usecase.GroupsInteractor
import com.passbolt.mobile.android.core.idlingresource.ResourcesFullRefreshIdlingResource
import com.passbolt.mobile.android.core.mvp.authentication.AuthenticatedUseCaseOutput
import com.passbolt.mobile.android.core.mvp.authentication.AuthenticationState
import com.passbolt.mobile.android.core.mvp.authentication.plus
import com.passbolt.mobile.android.core.resources.usecase.ResourceInteractor
import com.passbolt.mobile.android.core.resourcetypes.ResourceTypesInteractor
import com.passbolt.mobile.android.core.users.UsersInteractor
import com.passbolt.mobile.android.database.snapshot.ResourcesSnapshot
import com.passbolt.mobile.android.featureflags.usecase.GetFeatureFlagsUseCase
import com.passbolt.mobile.android.metadata.interactor.MetadataKeysInteractor
import com.passbolt.mobile.android.metadata.interactor.MetadataSessionKeysInteractor

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
/**
 * Interactor that is responsible for fetching and updating the database for all home screen resources
 * (resources, resource types, folders)
 */
class HomeDataInteractor(
    private val foldersInteractor: FoldersInteractor,
    private val resourcesInteractor: ResourceInteractor,
    private val groupsInteractor: GroupsInteractor,
    private val usersInteractor: UsersInteractor,
    private val resourceTypesInteractor: ResourceTypesInteractor,
    private val metadataKeysInteractor: MetadataKeysInteractor,
    private val metadataSessionKeysInteractor: MetadataSessionKeysInteractor,
    private val featureFlagsUseCase: GetFeatureFlagsUseCase,
    private val resourcesFullRefreshIdlingResource: ResourcesFullRefreshIdlingResource,
    private val resourcesSnapshot: ResourcesSnapshot
) {

    // TODO start multiple async where possible
    suspend fun refreshAllHomeScreenData(): Output {
        resourcesFullRefreshIdlingResource.setIdle(false)
        resourcesSnapshot.populateForCurrentAccount()

        val featureFlagsOutput = featureFlagsUseCase.execute(Unit).featureFlags
        val metadataKeysOutput = if (featureFlagsOutput.isV5MetadataAvailable) {
            metadataKeysInteractor.fetchAndSaveMetadataKeys()
        } else {
            MetadataKeysInteractor.Output.Success
        }
        val metadataSessionKeysOutput = if (featureFlagsOutput.isV5MetadataAvailable) {
            metadataSessionKeysInteractor.fetchMetadataSessionKeys()
        } else {
            MetadataSessionKeysInteractor.Output.Success
        }

        val resourceTypesOutput = resourceTypesInteractor.fetchAndSaveResourceTypes()
        val userInteractorOutput = usersInteractor.fetchAndSaveUsers()
        val groupsRefreshOutput = groupsInteractor.fetchAndSaveGroups()
        val foldersRefreshOutput = foldersInteractor.fetchAndSaveFolders()
        val resourcesOutput = resourcesInteractor.fetchAndSaveResources()

        val saveSessionKeysOutput = if (featureFlagsOutput.isV5MetadataAvailable) {
            metadataSessionKeysInteractor.saveMetadataSessionKeysCache()
        } else {
            MetadataSessionKeysInteractor.Output.Success
        }

        resourcesSnapshot.clear()
        resourcesFullRefreshIdlingResource.setIdle(true)

        @Suppress("ComplexCondition")
        return if (metadataKeysOutput is MetadataKeysInteractor.Output.Success &&
            metadataSessionKeysOutput is MetadataSessionKeysInteractor.Output.Success &&
            resourceTypesOutput is ResourceTypesInteractor.Output.Success &&
            userInteractorOutput is UsersInteractor.Output.Success &&
            groupsRefreshOutput is GroupsInteractor.Output.Success &&
            foldersRefreshOutput is FoldersInteractor.Output.Success &&
            resourcesOutput is ResourceInteractor.Output.Success &&
            saveSessionKeysOutput is MetadataSessionKeysInteractor.Output.Success
        ) {
            Output.Success
        } else {
            Output.Failure(
                metadataKeysOutput.authenticationState +
                        metadataSessionKeysOutput.authenticationState +
                        resourceTypesOutput.authenticationState +
                        userInteractorOutput.authenticationState +
                        groupsRefreshOutput.authenticationState +
                        foldersRefreshOutput.authenticationState +
                        resourcesOutput.authenticationState +
                        saveSessionKeysOutput.authenticationState
            )
        }
    }

    sealed class Output : AuthenticatedUseCaseOutput {

        data object Success : Output() {
            override val authenticationState: AuthenticationState = AuthenticationState.Authenticated
        }

        class Failure(override val authenticationState: AuthenticationState) : Output()
    }
}
