package com.passbolt.mobile.android.core.commonresource

import com.passbolt.mobile.android.core.commonresource.usecase.GetResourceTypesUseCase
import com.passbolt.mobile.android.core.commonresource.usecase.GetResourcesUseCase
import com.passbolt.mobile.android.core.commonresource.usecase.RebuildResourcesDatabaseUseCase
import com.passbolt.mobile.android.core.commonresource.validation.ResourceValidationRunner
import com.passbolt.mobile.android.core.mvp.authentication.AuthenticatedUseCaseOutput
import com.passbolt.mobile.android.core.mvp.authentication.AuthenticationState
import com.passbolt.mobile.android.database.usecase.AddLocalResourceTypesUseCase
import com.passbolt.mobile.android.ui.ResourcesDisplayView

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
class ResourceInteractor(
    private val getResourceTypesUseCase: GetResourceTypesUseCase,
    private val getResourcesUseCase: GetResourcesUseCase,
    private val addLocalResourceTypesUseCase: AddLocalResourceTypesUseCase,
    private val resourceValidationRunner: ResourceValidationRunner,
    private val rebuildAndGetResourcesDatabaseUseCase: RebuildResourcesDatabaseUseCase
) {

    suspend fun updateResourcesWithTypes(): Output {
        val resourcesResult = getResourcesUseCase.execute(Unit)

        return if (resourcesResult is GetResourcesUseCase.Output.Success) {
            val resourceTypesResult = getResourceTypesUseCase.execute(Unit)
            if (resourceTypesResult is GetResourceTypesUseCase.Output.Success) {
                addLocalResourceTypesUseCase.execute(
                    AddLocalResourceTypesUseCase.Input(resourceTypesResult.resourceTypes)
                )
                val validatedResources = resourcesResult.resources
                    .filter { resourceValidationRunner.isValid(it) }
                rebuildAndGetResourcesDatabaseUseCase.execute(
                    RebuildResourcesDatabaseUseCase.Input(validatedResources)
                )

                Output.Success
            } else {
                Output.Failure(resourcesResult.authenticationState + resourceTypesResult.authenticationState)
            }
        } else {
            Output.Failure(resourcesResult.authenticationState)
        }
    }

    private operator fun AuthenticationState.plus(other: AuthenticationState): AuthenticationState {
        return when {
            this is AuthenticationState.Authenticated && other is AuthenticationState.Authenticated ->
                AuthenticationState.Authenticated
            else -> AuthenticationState.Unauthenticated(AuthenticationState.Unauthenticated.Reason.Session)
        }
    }

    data class Input(val displayView: ResourcesDisplayView = ResourcesDisplayView.ALL)

    sealed class Output : AuthenticatedUseCaseOutput {

        object Success : Output() {
            override val authenticationState: AuthenticationState
                get() = AuthenticationState.Authenticated
        }

        class Failure(override val authenticationState: AuthenticationState) : Output()
    }
}
