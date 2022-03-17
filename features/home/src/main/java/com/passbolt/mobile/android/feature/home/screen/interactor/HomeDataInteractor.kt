package com.passbolt.mobile.android.feature.home.screen.interactor

import com.passbolt.mobile.android.core.commonfolders.usecase.FoldersInteractor
import com.passbolt.mobile.android.core.commonresource.ResourceInteractor
import com.passbolt.mobile.android.core.mvp.authentication.AuthenticatedUseCaseOutput
import com.passbolt.mobile.android.core.mvp.authentication.AuthenticationState
import com.passbolt.mobile.android.core.mvp.authentication.plus

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
    private val resourcesInteractor: ResourceInteractor
) {

    suspend fun refreshAllHomeScreenData(): Output {
        val foldersRefreshOutput = foldersInteractor.fetchAndSaveFolders()
        val resourcesAndResourcesTypesOutput = resourcesInteractor.updateResourcesWithTypes()
        return if (foldersRefreshOutput is FoldersInteractor.Output.Success &&
            resourcesAndResourcesTypesOutput is ResourceInteractor.Output.Success
        ) {
            Output.Success
        } else {
            Output.Failure(
                foldersRefreshOutput.authenticationState +
                        resourcesAndResourcesTypesOutput.authenticationState
            )
        }
    }

    sealed class Output : AuthenticatedUseCaseOutput {

        object Success : Output() {
            override val authenticationState: AuthenticationState = AuthenticationState.Authenticated
        }

        class Failure(override val authenticationState: AuthenticationState) : Output()
    }
}
