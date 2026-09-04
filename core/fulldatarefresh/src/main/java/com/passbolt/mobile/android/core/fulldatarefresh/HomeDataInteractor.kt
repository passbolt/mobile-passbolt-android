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

package com.passbolt.mobile.android.core.fulldatarefresh

import com.passbolt.mobile.android.core.idlingresource.ResourcesFullRefreshIdlingResource
import com.passbolt.mobile.android.core.mvp.authentication.AuthenticatedUseCaseOutput
import com.passbolt.mobile.android.core.mvp.authentication.AuthenticationState
import com.passbolt.mobile.android.core.mvp.authentication.plus
import com.passbolt.mobile.android.database.snapshot.ResourcesSnapshot
import com.passbolt.mobile.android.domain.folders.usecase.FoldersInteractor
import com.passbolt.mobile.android.domain.groups.usecase.GroupsInteractor
import com.passbolt.mobile.android.domain.metadata.interactor.MetadataKeysInteractor
import com.passbolt.mobile.android.domain.metadata.interactor.MetadataKeysSettingsInteractor
import com.passbolt.mobile.android.domain.metadata.interactor.MetadataPrivateKeysInteractor
import com.passbolt.mobile.android.domain.metadata.interactor.MetadataSessionKeysInteractor
import com.passbolt.mobile.android.domain.metadata.interactor.MetadataTypesSettingsInteractor
import com.passbolt.mobile.android.domain.resources.usecase.ResourceInteractor
import com.passbolt.mobile.android.domain.resourcetypes.usecase.ResourceTypesInteractor
import com.passbolt.mobile.android.domain.users.usecase.UsersInteractor
import com.passbolt.mobile.android.featureflags.usecase.GetFeatureFlagsUseCase
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import timber.log.Timber

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
    private val metadataPrivateKeysInteractor: MetadataPrivateKeysInteractor,
    private val metadataSessionKeysInteractor: MetadataSessionKeysInteractor,
    private val metadataTypesSettingsInteractor: MetadataTypesSettingsInteractor,
    private val metadataKeysSettingsInteractor: MetadataKeysSettingsInteractor,
    private val featureFlagsUseCase: GetFeatureFlagsUseCase,
    private val resourcesFullRefreshIdlingResource: ResourcesFullRefreshIdlingResource,
    private val resourcesSnapshot: ResourcesSnapshot,
    private val refreshProgressTrackerFactory: RefreshProgressTrackerFactory,
) {
    // TODO start multiple async where possible
    @Suppress("LongMethod", "CyclomaticComplexMethod")
    suspend fun refreshAllHomeScreenData(onProgress: suspend (Float) -> Unit = {}): Output {
        resourcesFullRefreshIdlingResource.setIdle(false)
        resourcesSnapshot.populateForCurrentAccount()

        val progressCounter = refreshProgressTrackerFactory.create(TOTAL_REFRESH_STEPS, onProgress)
        val featureFlagsOutput = featureFlagsUseCase.execute(Unit).featureFlags
        val (metadataTypesSettingsOutput, metadataKeysSettingsOutput) =
            if (featureFlagsOutput.isV5MetadataAvailable) {
                coroutineScope {
                    val typesDeferred =
                        async {
                            metadataTypesSettingsInteractor
                                .fetchAndSaveMetadataTypesSettings()
                                .also { progressCounter.onStepCompleted() }
                        }
                    val keysDeferred =
                        async {
                            metadataKeysSettingsInteractor
                                .fetchAndSaveMetadataKeysSettings()
                                .also { progressCounter.onStepCompleted() }
                        }
                    typesDeferred.await() to keysDeferred.await()
                }
            } else {
                progressCounter.onStepsSkipped(count = 2)
                MetadataTypesSettingsInteractor.Output.Success to MetadataKeysSettingsInteractor.Output.Success
            }
        val metadataKeysOutput =
            if (featureFlagsOutput.isV5MetadataAvailable) {
                metadataKeysInteractor
                    .fetchAndSaveMetadataKeys()
                    .also { progressCounter.onStepCompleted() }
            } else {
                progressCounter.onStepsSkipped(count = 1)
                MetadataKeysInteractor.Output.Success
            }
        val metadataSessionKeysOutput =
            if (featureFlagsOutput.isV5MetadataAvailable) {
                metadataSessionKeysInteractor
                    .fetchMetadataSessionKeys()
                    .also { progressCounter.onStepCompleted() }
            } else {
                progressCounter.onStepsSkipped(count = 1)
                MetadataSessionKeysInteractor.Output.Success
            }

        val resourceTypesOutput =
            resourceTypesInteractor
                .fetchAndSaveResourceTypes()
                .also { progressCounter.onStepCompleted() }
        val userInteractorOutput =
            usersInteractor
                .fetchAndSaveUsers()
                .also { progressCounter.onStepCompleted() }

        if (featureFlagsOutput.isV5MetadataAvailable) {
            establishMetadataKeyTrust()
            progressCounter.onStepCompleted()
        } else {
            progressCounter.onStepsSkipped(count = 1)
        }

        val groupsRefreshOutput =
            groupsInteractor
                .fetchAndSaveGroups()
                .also { progressCounter.onStepCompleted() }
        val foldersRefreshOutput =
            foldersInteractor
                .fetchAndSaveFolders(progressCounter::onStepPageDownloaded)
                .also { progressCounter.onStepCompleted() }
        val resourcesOutput =
            resourcesInteractor
                .fetchAndSaveResources(progressCounter::onStepPageDownloaded)
                .also { progressCounter.onStepCompleted() }

        val saveSessionKeysOutput =
            if (featureFlagsOutput.isV5MetadataAvailable) {
                metadataSessionKeysInteractor
                    .saveMetadataSessionKeysCache()
                    .also { progressCounter.onStepCompleted() }
            } else {
                progressCounter.onStepsSkipped(count = 1)
                MetadataSessionKeysInteractor.Output.Success
            }

        resourcesSnapshot.clear()
        resourcesFullRefreshIdlingResource.setIdle(true)

        @Suppress("ComplexCondition")
        return if (metadataTypesSettingsOutput is MetadataTypesSettingsInteractor.Output.Success &&
            metadataKeysSettingsOutput is MetadataKeysSettingsInteractor.Output.Success &&
            metadataKeysOutput is MetadataKeysInteractor.Output.Success &&
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
                metadataTypesSettingsOutput.authenticationState +
                    metadataKeysSettingsOutput.authenticationState +
                    metadataKeysOutput.authenticationState +
                    metadataSessionKeysOutput.authenticationState +
                    resourceTypesOutput.authenticationState +
                    userInteractorOutput.authenticationState +
                    groupsRefreshOutput.authenticationState +
                    foldersRefreshOutput.authenticationState +
                    resourcesOutput.authenticationState +
                    saveSessionKeysOutput.authenticationState,
            )
        }
    }

    private suspend fun establishMetadataKeyTrust() {
        // Metadata keys were fetched and decrypted a few steps earlier in this
        // very refresh - do not do it again inside the trust verification.
        val result = metadataPrivateKeysInteractor.verifyMetadataPrivateKey(refreshFromServer = false)
        Timber.d("Metadata key trust verification during data refresh: $result")
    }

    private companion object {
        private const val TOTAL_REFRESH_STEPS = 11
    }

    sealed class Output : AuthenticatedUseCaseOutput {
        data object Success : Output() {
            override val authenticationState: AuthenticationState = AuthenticationState.Authenticated
        }

        class Failure(
            override val authenticationState: AuthenticationState,
        ) : Output()
    }
}
