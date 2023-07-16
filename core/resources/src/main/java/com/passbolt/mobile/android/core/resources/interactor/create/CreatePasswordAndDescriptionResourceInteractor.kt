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

package com.passbolt.mobile.android.core.resources.interactor.create

import com.passbolt.mobile.android.core.resources.SecretInputCreator
import com.passbolt.mobile.android.database.impl.resourcetypes.GetResourceTypeIdToSlugMappingUseCase
import com.passbolt.mobile.android.gopenpgp.OpenPgp
import com.passbolt.mobile.android.gopenpgp.exception.OpenPgpResult
import com.passbolt.mobile.android.mappers.PermissionsModelMapper
import com.passbolt.mobile.android.mappers.ResourceModelMapper
import com.passbolt.mobile.android.passboltapi.resource.ResourceRepository
import com.passbolt.mobile.android.storage.cache.passphrase.PassphraseMemoryCache
import com.passbolt.mobile.android.storage.usecase.accountdata.GetSelectedAccountDataUseCase
import com.passbolt.mobile.android.storage.usecase.input.UserIdInput
import com.passbolt.mobile.android.storage.usecase.privatekey.GetPrivateKeyUseCase
import com.passbolt.mobile.android.storage.usecase.selectedaccount.GetSelectedAccountUseCase
import com.passbolt.mobile.android.ui.EncryptedSecretOrError

class CreatePasswordAndDescriptionResourceInteractor(
    private val getSelectedAccountUseCase: GetSelectedAccountUseCase,
    private val secretInputCreator: SecretInputCreator,
    private val getPrivateKeyUseCase: GetPrivateKeyUseCase,
    private val openPgp: OpenPgp,
    private val getSelectedAccountDataUseCase: GetSelectedAccountDataUseCase,
    resourceRepository: ResourceRepository,
    resourceModelMapper: ResourceModelMapper,
    passphraseMemoryCache: PassphraseMemoryCache,
    permissionsModelMapper: PermissionsModelMapper,
    getResourceTypeIdToSlugMappingUseCase: GetResourceTypeIdToSlugMappingUseCase
) :
    CreateResourceInteractor<CreatePasswordAndDescriptionResourceInteractor.CreatePasswordAndDescriptionInput>(
        resourceRepository,
        resourceModelMapper,
        passphraseMemoryCache,
        permissionsModelMapper,
        getResourceTypeIdToSlugMappingUseCase
    ) {

    override val slug = "password-and-description"

    override suspend fun createCommonDescription(customInput: CreatePasswordAndDescriptionInput): String? =
        null

    override suspend fun createSecret(
        input: CommonInput,
        customInput: CreatePasswordAndDescriptionInput,
        passphrase: ByteArray
    ): EncryptedSecretOrError {
        val userId = requireNotNull(getSelectedAccountUseCase.execute(Unit).selectedAccount)
        val userServerId = requireNotNull(getSelectedAccountDataUseCase.execute(Unit).serverId)
        val privateKey = getPrivateKeyUseCase.execute(UserIdInput(userId)).privateKey
        val secret = secretInputCreator.createPasswordWithDescriptionSecretInput(
            password = customInput.password,
            description = customInput.description
        )

        return when (val publicKey = openPgp.generatePublicKey(privateKey)) {
            is OpenPgpResult.Error -> EncryptedSecretOrError.Error(publicKey.error.message)
            is OpenPgpResult.Result -> {
                when (val encryptedSecret =
                    openPgp.encryptSignMessageArmored(publicKey.result, privateKey, passphrase, secret)) {
                    is OpenPgpResult.Error -> EncryptedSecretOrError.Error(encryptedSecret.error.message)
                    is OpenPgpResult.Result -> EncryptedSecretOrError.EncryptedSecret(
                        userServerId,
                        encryptedSecret.result
                    )
                }
            }
        }
    }

    class CreatePasswordAndDescriptionInput(
        val password: String,
        val description: String?
    )
}
