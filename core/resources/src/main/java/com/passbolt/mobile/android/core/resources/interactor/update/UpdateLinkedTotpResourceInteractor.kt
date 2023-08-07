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

package com.passbolt.mobile.android.core.resources.interactor.update

import com.passbolt.mobile.android.core.resources.SecretInputCreator
import com.passbolt.mobile.android.core.resourcetypes.usecase.db.GetResourceTypeIdToSlugMappingUseCase
import com.passbolt.mobile.android.core.secrets.usecase.decrypt.parser.SecretParser
import com.passbolt.mobile.android.core.users.usecase.FetchUsersUseCase
import com.passbolt.mobile.android.gopenpgp.OpenPgp
import com.passbolt.mobile.android.gopenpgp.exception.OpenPgpResult
import com.passbolt.mobile.android.mappers.ResourceModelMapper
import com.passbolt.mobile.android.passboltapi.resource.ResourceRepository
import com.passbolt.mobile.android.storage.cache.passphrase.PassphraseMemoryCache
import com.passbolt.mobile.android.storage.usecase.input.UserIdInput
import com.passbolt.mobile.android.storage.usecase.privatekey.GetPrivateKeyUseCase
import com.passbolt.mobile.android.storage.usecase.selectedaccount.GetSelectedAccountUseCase
import com.passbolt.mobile.android.ui.DecryptedSecretOrError
import com.passbolt.mobile.android.ui.EncryptedSecretOrError
import com.passbolt.mobile.android.ui.UserModel
import timber.log.Timber

class UpdateLinkedTotpResourceInteractor(
    private val secretInputCreator: SecretInputCreator,
    private val getSelectedAccountUseCase: GetSelectedAccountUseCase,
    private val getPrivateKeyUseCase: GetPrivateKeyUseCase,
    private val openPgp: OpenPgp,
    private val secretParser: SecretParser,
    passphraseMemoryCache: PassphraseMemoryCache,
    resourceModelMapper: ResourceModelMapper,
    resourceRepository: ResourceRepository,
    fetchUsersUseCase: FetchUsersUseCase,
    getResourceTypeIdToSlugMappingUseCase: GetResourceTypeIdToSlugMappingUseCase
) :
    UpdateResourceInteractor<UpdateLinkedTotpResourceInteractor.UpdateToLinkedTotpInput>(
        passphraseMemoryCache,
        resourceModelMapper,
        resourceRepository,
        fetchUsersUseCase,
        getResourceTypeIdToSlugMappingUseCase
    ) {

    override val slug = "password-description-totp"

    override suspend fun createSecrets(
        input: CommonInput,
        customInput: UpdateToLinkedTotpInput,
        passphrase: ByteArray,
        usersWhoHaveAccess: List<UserModel>
    ): List<EncryptedSecretOrError> {
        return usersWhoHaveAccess.mapTo(mutableListOf()) {
            val userId = requireNotNull(getSelectedAccountUseCase.execute(Unit).selectedAccount)
            val privateKey = getPrivateKeyUseCase.execute(UserIdInput(userId)).privateKey
            val publicKey = it.gpgKey.armoredKey

            try {
                val secret = secretInputCreator.createPasswordDescriptionTotpSecretInput(
                    password = createSecretPassword(customInput, customInput.existingResourceTypeId),
                    description = createSecretDescription(customInput, customInput.existingResourceTypeId),
                    algorithm = customInput.algorithm,
                    key = customInput.secretKey,
                    digits = customInput.digits,
                    period = customInput.period
                )
                when (val encryptedSecret =
                    openPgp.encryptSignMessageArmored(publicKey, privateKey, passphrase, secret)) {
                    is OpenPgpResult.Error -> EncryptedSecretOrError.Error(encryptedSecret.error.message)
                    is OpenPgpResult.Result -> EncryptedSecretOrError.EncryptedSecret(it.id, encryptedSecret.result)
                }
            } catch (exception: RuntimeException) {
                EncryptedSecretOrError.Error(exception.message.orEmpty())
            }
        }
    }

    @Throws(RuntimeException::class)
    private suspend fun createSecretPassword(
        customInput: UpdateToLinkedTotpInput,
        resourceTypeId: String
    ) =
        customInput.password
            ?: when (val password = secretParser.extractPassword(resourceTypeId, customInput.existingSecret)) {
                is DecryptedSecretOrError.DecryptedSecret -> password.secret
                else -> {
                    Timber.e("Could not parse password for existing resource or password is invalid")
                    throw RuntimeException()
                }
            }

    @Throws(RuntimeException::class)
    private suspend fun createSecretDescription(
        customInput: UpdateToLinkedTotpInput,
        resourceTypeId: String
    ) =
        customInput.description
            ?: when (val description = secretParser.extractDescription(resourceTypeId, customInput.existingSecret)) {
                is DecryptedSecretOrError.DecryptedSecret -> description.secret
                else -> {
                    Timber.e("Could not parse description for existing resource or description is invalid")
                    throw RuntimeException()
                }
            }

    override suspend fun createCommonDescription(customInput: UpdateToLinkedTotpInput): String? =
        null

    class UpdateToLinkedTotpInput(
        val period: Long,
        val digits: Int,
        val algorithm: String,
        val secretKey: String,
        val existingSecret: ByteArray,
        val existingResourceTypeId: String,
        val description: String?, // if description is null it's taken from existing secret
        val password: String? // if password is null it's taken from existing secret
    )
}
