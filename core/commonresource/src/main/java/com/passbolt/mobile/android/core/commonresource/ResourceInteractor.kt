package com.passbolt.mobile.android.core.commonresource

import com.passbolt.mobile.android.core.mvp.session.AuthenticatedUseCaseOutput
import com.passbolt.mobile.android.core.mvp.session.AuthenticationState
import com.passbolt.mobile.android.core.mvp.session.UnauthenticatedReason
import com.passbolt.mobile.android.database.usecase.AddLocalResourceTypesUseCase
import com.passbolt.mobile.android.dto.response.ResourceTypeDto
import com.passbolt.mobile.android.ui.ResourceModel

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
    private val addLocalResourceTypesUseCase: AddLocalResourceTypesUseCase
) {

    suspend fun fetchResourcesWithTypes(): Output {
        val resourcesResult = getResourcesUseCase.execute(Unit)
        val resourceTypesResult = getResourceTypesUseCase.execute(Unit)

        return if (resourcesResult is GetResourcesUseCase.Output.Success &&
            resourceTypesResult is GetResourceTypesUseCase.Output.Success
        ) {
            addLocalResourceTypesUseCase.execute(
                AddLocalResourceTypesUseCase.Input(resourceTypesResult.resourceTypes)
            )
            Output.Success(resourcesResult.resources, resourceTypesResult.resourceTypes)
        } else {
            Output.Failure(resourcesResult.authenticationState + resourceTypesResult.authenticationState)
        }
    }

    private operator fun AuthenticationState.plus(other: AuthenticationState): AuthenticationState {
        return when {
            this is AuthenticationState.Authenticated && other is AuthenticationState.Authenticated ->
                AuthenticationState.Authenticated
            else -> AuthenticationState.Unauthenticated(UnauthenticatedReason.SESSION)
        }
    }

    sealed class Output : AuthenticatedUseCaseOutput {

        class Success(
            val resources: List<ResourceModel>,
            val resourceTypes: List<ResourceTypeDto>
        ) : Output() {
            override val authenticationState: AuthenticationState
                get() = AuthenticationState.Authenticated
        }

        class Failure(override val authenticationState: AuthenticationState) : Output()
    }
}
