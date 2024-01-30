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

import com.google.gson.Gson
import com.passbolt.mobile.android.core.resources.SecretInputCreator
import com.passbolt.mobile.android.core.resourcetypes.usecase.db.GetResourceTypeIdToSlugMappingUseCase
import com.passbolt.mobile.android.core.users.usecase.FetchUsersUseCase
import com.passbolt.mobile.android.gopenpgp.OpenPgp
import com.passbolt.mobile.android.mappers.ResourceModelMapper
import com.passbolt.mobile.android.passboltapi.resource.ResourceRepository
import com.passbolt.mobile.android.serializers.gson.validation.JsonSchemaValidationRunner
import com.passbolt.mobile.android.storage.cache.passphrase.PassphraseMemoryCache
import com.passbolt.mobile.android.storage.usecase.privatekey.GetPrivateKeyUseCase
import com.passbolt.mobile.android.storage.usecase.selectedaccount.GetSelectedAccountUseCase

class UpdateSimplePasswordResourceInteractor(
    private val secretInputCreator: SecretInputCreator,
    getSelectedAccountUseCase: GetSelectedAccountUseCase,
    getPrivateKeyUseCase: GetPrivateKeyUseCase,
    openPgp: OpenPgp,
    passphraseMemoryCache: PassphraseMemoryCache,
    resourceModelMapper: ResourceModelMapper,
    resourceRepository: ResourceRepository,
    fetchUsersUseCase: FetchUsersUseCase,
    getResourceTypeIdToSlugMappingUseCase: GetResourceTypeIdToSlugMappingUseCase,
    jsonSchemaValidationRunner: JsonSchemaValidationRunner,
    gson: Gson
) :
    UpdateResourceInteractor<UpdateSimplePasswordResourceInteractor.UpdateSimplePasswordInput>(
        passphraseMemoryCache,
        resourceModelMapper,
        resourceRepository,
        fetchUsersUseCase,
        getResourceTypeIdToSlugMappingUseCase,
        jsonSchemaValidationRunner,
        gson,
        getSelectedAccountUseCase,
        getPrivateKeyUseCase,
        openPgp
    ) {

    override val slug = "password-string"

    override fun createSecret(
        input: CommonInput,
        customInput: UpdateSimplePasswordInput,
        passphrase: ByteArray
    ) =
        secretInputCreator.createSimplePasswordSecretInput(customInput.password)

    override suspend fun createCommonDescription(customInput: UpdateSimplePasswordInput): String? =
        customInput.description

    class UpdateSimplePasswordInput(
        val password: String,
        val description: String?
    )
}
