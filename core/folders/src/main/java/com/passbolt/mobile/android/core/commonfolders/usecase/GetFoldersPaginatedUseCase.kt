package com.passbolt.mobile.android.core.commonfolders.usecase

import com.passbolt.mobile.android.common.usecase.AsyncUseCase
import com.passbolt.mobile.android.core.mvp.authentication.AuthenticatedUseCaseOutput
import com.passbolt.mobile.android.core.mvp.authentication.AuthenticationState
import com.passbolt.mobile.android.core.networking.MfaTypeProvider
import com.passbolt.mobile.android.core.networking.NetworkResult
import com.passbolt.mobile.android.dto.PassphraseNotInCacheException
import com.passbolt.mobile.android.dto.response.Pagination
import com.passbolt.mobile.android.mappers.FolderModelMapper
import com.passbolt.mobile.android.passboltapi.folders.FoldersRepository
import com.passbolt.mobile.android.ui.FolderModelWithAttributes

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
class GetFoldersPaginatedUseCase(
    private val foldersRepository: FoldersRepository,
    private val folderModelMapper: FolderModelMapper,
) : AsyncUseCase<GetFoldersPaginatedUseCase.Input, GetFoldersPaginatedUseCase.Output> {
    override suspend fun execute(input: Input): Output =
        when (val response = foldersRepository.getFoldersPaginated(input.limit, input.page)) {
            is NetworkResult.Failure -> Output.Failure(response)
            is NetworkResult.Success ->
                Output.Success(
                    pagination = response.value.header.pagination,
                    folders = response.value.body.map { folderModelMapper.map(it) },
                )
        }

    data class Input(
        val page: Int,
        val limit: Int,
    )

    sealed class Output : AuthenticatedUseCaseOutput {
        override val authenticationState: AuthenticationState
            get() =
                when (this) {
                    is Failure<*> if this.response.isUnauthorized -> {
                        AuthenticationState.Unauthenticated(AuthenticationState.Unauthenticated.Reason.Session)
                    }
                    is Failure<*> if this.response.exception is PassphraseNotInCacheException -> {
                        AuthenticationState.Unauthenticated(AuthenticationState.Unauthenticated.Reason.Passphrase)
                    }
                    is Failure<*> if this.response.isMfaRequired -> {
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
            val folders: List<FolderModelWithAttributes>,
        ) : Output()

        class Failure<T : Any>(
            val response: NetworkResult.Failure<T>,
        ) : Output()
    }
}
