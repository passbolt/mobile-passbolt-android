package com.passbolt.mobile.android.core.resources.usecase

import com.passbolt.mobile.android.common.usecase.AsyncUseCase
import com.passbolt.mobile.android.core.mvp.authentication.AuthenticatedUseCaseOutput
import com.passbolt.mobile.android.core.mvp.authentication.AuthenticationState
import com.passbolt.mobile.android.core.networking.MfaTypeProvider
import com.passbolt.mobile.android.core.networking.NetworkResult
import com.passbolt.mobile.android.dto.request.EncryptedSharedSecret
import com.passbolt.mobile.android.dto.request.ResourceShareRequest
import com.passbolt.mobile.android.dto.request.SharePermission
import com.passbolt.mobile.android.passboltapi.share.ShareRepository
import com.passbolt.mobile.android.ui.EncryptedSecretOrError

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
class ShareResourceUseCase(
    private val shareRepository: ShareRepository
) : AsyncUseCase<ShareResourceUseCase.Input, ShareResourceUseCase.Output> {

    override suspend fun execute(input: Input) =
        when (val response = shareRepository.shareResource(
            input.resourceId,
            ResourceShareRequest(
                input.sharePermissions,
                input.encryptedSecrets.map { EncryptedSharedSecret(input.resourceId, it.userId, it.data) })
        )) {
            is NetworkResult.Failure -> Output.Failure(response)
            is NetworkResult.Success -> Output.Success
        }

    sealed class Output : AuthenticatedUseCaseOutput {

        override val authenticationState: AuthenticationState
            get() = when {
                this is Failure<*> && this.response.isUnauthorized -> {
                    AuthenticationState.Unauthenticated(AuthenticationState.Unauthenticated.Reason.Session)
                }
                this is Failure<*> && this.response.isMfaRequired -> {
                    val providers = MfaTypeProvider.get(this.response)
                    AuthenticationState.Unauthenticated(
                        AuthenticationState.Unauthenticated.Reason.Mfa(providers)
                    )
                }
                else -> {
                    AuthenticationState.Authenticated
                }
            }

        object Success : Output()

        data class Failure<T : Any>(val response: NetworkResult.Failure<T>) : Output()
    }

    data class Input(
        val resourceId: String,
        val sharePermissions: List<SharePermission>,
        val encryptedSecrets: List<EncryptedSecretOrError.EncryptedSecret>
    )
}
