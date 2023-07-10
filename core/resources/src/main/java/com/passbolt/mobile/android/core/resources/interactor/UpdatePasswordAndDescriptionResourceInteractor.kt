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

package com.passbolt.mobile.android.core.resources.interactor

import com.passbolt.mobile.android.core.resources.SecretInputCreator
import com.passbolt.mobile.android.core.users.FetchUsersUseCase
import com.passbolt.mobile.android.database.impl.resourcetypes.GetResourceTypeIdToSlugMappingUseCase
import com.passbolt.mobile.android.gopenpgp.OpenPgp
import com.passbolt.mobile.android.gopenpgp.exception.OpenPgpResult.Error
import com.passbolt.mobile.android.gopenpgp.exception.OpenPgpResult.Result
import com.passbolt.mobile.android.mappers.ResourceModelMapper
import com.passbolt.mobile.android.passboltapi.resource.ResourceRepository
import com.passbolt.mobile.android.storage.cache.passphrase.PassphraseMemoryCache
import com.passbolt.mobile.android.storage.usecase.input.UserIdInput
import com.passbolt.mobile.android.storage.usecase.privatekey.GetPrivateKeyUseCase
import com.passbolt.mobile.android.storage.usecase.selectedaccount.GetSelectedAccountUseCase
import com.passbolt.mobile.android.ui.EncryptedSecretOrError
import com.passbolt.mobile.android.ui.UserModel

class UpdatePasswordAndDescriptionResourceInteractor(
    private val secretInputCreator: SecretInputCreator,
    private val getSelectedAccountUseCase: GetSelectedAccountUseCase,
    private val getPrivateKeyUseCase: GetPrivateKeyUseCase,
    private val openPgp: OpenPgp,
    passphraseMemoryCache: PassphraseMemoryCache,
    resourceModelMapper: ResourceModelMapper,
    resourceRepository: ResourceRepository,
    fetchUsersUseCase: FetchUsersUseCase,
    getResourceTypeIdToSlugMappingUseCase: GetResourceTypeIdToSlugMappingUseCase
) :
    UpdateResourceInteractor<UpdatePasswordAndDescriptionResourceInteractor.UpdatePasswordAndDescriptionInput>(
        passphraseMemoryCache,
        resourceModelMapper,
        resourceRepository,
        fetchUsersUseCase,
        getResourceTypeIdToSlugMappingUseCase
    ) {

    override val slug = "password-and-description"

    override suspend fun createSecrets(
        input: CommonInput,
        customInput: UpdatePasswordAndDescriptionInput,
        passphrase: ByteArray,
        usersWhoHaveAccess: List<UserModel>
    ): List<EncryptedSecretOrError> {
        return usersWhoHaveAccess.mapTo(mutableListOf()) {
            val userId = requireNotNull(getSelectedAccountUseCase.execute(Unit).selectedAccount)
            val privateKey = getPrivateKeyUseCase.execute(UserIdInput(userId)).privateKey
            val publicKey = it.gpgKey.armoredKey
            val secret = secretInputCreator.createPasswordWithDescriptionSecretInput(
                customInput.password,
                customInput.description
            )

            when (val encryptedSecret = openPgp.encryptSignMessageArmored(publicKey, privateKey, passphrase, secret)) {
                is Error -> EncryptedSecretOrError.Error(encryptedSecret.error.message)
                is Result -> EncryptedSecretOrError.EncryptedSecret(it.id, encryptedSecret.result)
            }
        }
    }

    override suspend fun createCommonDescription(customInput: UpdatePasswordAndDescriptionInput): String? =
        null

    class UpdatePasswordAndDescriptionInput(
        val password: String,
        val description: String?
    )
}
