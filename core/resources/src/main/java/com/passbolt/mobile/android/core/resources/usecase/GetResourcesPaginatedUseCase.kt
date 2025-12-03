package com.passbolt.mobile.android.core.resources.usecase

import com.passbolt.mobile.android.common.usecase.AsyncUseCase
import com.passbolt.mobile.android.core.mvp.authentication.AuthenticatedUseCaseOutput
import com.passbolt.mobile.android.core.mvp.authentication.AuthenticationState
import com.passbolt.mobile.android.core.networking.MfaTypeProvider
import com.passbolt.mobile.android.core.networking.NetworkResult
import com.passbolt.mobile.android.dto.PassphraseNotInCacheException
import com.passbolt.mobile.android.dto.response.Pagination
import com.passbolt.mobile.android.mappers.PermissionsModelMapper
import com.passbolt.mobile.android.mappers.ResourceModelMapper
import com.passbolt.mobile.android.mappers.TagsModelMapper
import com.passbolt.mobile.android.passboltapi.resource.ResourceRepository
import com.passbolt.mobile.android.ui.ResourceModelWithAttributes

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
class GetResourcesPaginatedUseCase(
    private val resourceRepository: ResourceRepository,
    private val resourceModelMapper: ResourceModelMapper,
    private val tagModelMapper: TagsModelMapper,
    private val permissionsModelMapper: PermissionsModelMapper,
) : AsyncUseCase<GetResourcesPaginatedUseCase.Input, GetResourcesPaginatedUseCase.Output> {
    override suspend fun execute(input: Input): Output =
        when (val response = resourceRepository.getResourcesPaginated(input.limit, input.page)) {
            is NetworkResult.Failure -> Output.Failure(response)
            is NetworkResult.Success ->
                Output.Success(
                    pagination = response.value.header.pagination,
                    response.value.body.map {
                        ResourceModelWithAttributes(
                            resourceModelMapper.map(it),
                            it.tags?.map { tag -> tagModelMapper.map(tag) }.orEmpty(),
                            it.permissions?.map { permission -> permissionsModelMapper.map(permission) }.orEmpty(),
                            it.favorite?.id?.toString(),
                        )
                    },
                )
        }

    data class Input(
        val page: Int,
        val limit: Int,
    )

    sealed class Output : AuthenticatedUseCaseOutput {
        override val authenticationState: AuthenticationState
            get() =
                when {
                    this is Failure<*> && this.response.isUnauthorized -> {
                        AuthenticationState.Unauthenticated(AuthenticationState.Unauthenticated.Reason.Session)
                    }
                    this is Failure<*> && this.response.exception is PassphraseNotInCacheException -> {
                        AuthenticationState.Unauthenticated(AuthenticationState.Unauthenticated.Reason.Passphrase)
                    }
                    this is Failure<*> && this.response.isMfaRequired -> {
                        val providers = MfaTypeProvider.get(this.response)
                        AuthenticationState.Unauthenticated(
                            AuthenticationState.Unauthenticated.Reason.Mfa(providers),
                        )
                    }
                    else -> {
                        AuthenticationState.Authenticated
                    }
                }

        data class Success(
            val pagination: Pagination,
            val resources: List<ResourceModelWithAttributes>,
        ) : Output()

        class Failure<T : Any>(
            val response: NetworkResult.Failure<T>,
        ) : Output()
    }
}
