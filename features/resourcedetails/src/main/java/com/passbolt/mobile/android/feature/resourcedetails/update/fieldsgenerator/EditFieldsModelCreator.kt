package com.passbolt.mobile.android.feature.resourcedetails.update.fieldsgenerator

import com.passbolt.mobile.android.core.resourcetypes.ResourceTypeFactory.ResourceTypeEnum.PASSWORD_DESCRIPTION_TOTP
import com.passbolt.mobile.android.core.resourcetypes.ResourceTypeFactory.ResourceTypeEnum.PASSWORD_WITH_DESCRIPTION
import com.passbolt.mobile.android.core.resourcetypes.ResourceTypeFactory.ResourceTypeEnum.SIMPLE_PASSWORD
import com.passbolt.mobile.android.core.resourcetypes.ResourceTypeFactory.ResourceTypeEnum.STANDALONE_TOTP
import com.passbolt.mobile.android.core.secrets.usecase.decrypt.parser.SecretParser
import com.passbolt.mobile.android.core.resourcetypes.usecase.db.GetResourceTypeWithFieldsByIdUseCase
import com.passbolt.mobile.android.feature.resourcedetails.update.ResourceValue
import com.passbolt.mobile.android.ui.DecryptedSecretOrError
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
class EditFieldsModelCreator(
    private val getResourceTypeWithFieldsByIdUseCase: GetResourceTypeWithFieldsByIdUseCase,
    private val secretParser: SecretParser,
    private val resourceTypeEnumFactory: com.passbolt.mobile.android.core.resourcetypes.ResourceTypeFactory,
    private val resourceFieldsComparator: ResourceFieldsComparator
) {

    @Throws(ClassCastException::class)
    suspend fun create(
        existingResource: ResourceModel,
        secret: ByteArray
    ): List<ResourceValue> {

        val editedResourceType = getResourceTypeWithFieldsByIdUseCase.execute(
            GetResourceTypeWithFieldsByIdUseCase.Input(existingResource.resourceTypeId)
        )

        val resourceTypeEnum = resourceTypeEnumFactory.getResourceTypeEnum(existingResource.resourceTypeId)

        return editedResourceType
            .fields
            .sortedWith(resourceFieldsComparator)
            .map { field ->
                val initialValue = when (field.name) {
                    in listOf(FieldNamesMapper.PASSWORD_FIELD, FieldNamesMapper.SECRET_FIELD) -> {
                        // there can be parsing errors when secret data is invalid - do not create the input then
                        (secretParser.extractPassword(
                            resourceTypeEnum,
                            secret
                        ) as DecryptedSecretOrError.DecryptedSecret<String>).secret
                    }
                    FieldNamesMapper.DESCRIPTION_FIELD -> {
                        when (resourceTypeEnum) {
                            SIMPLE_PASSWORD -> existingResource.description
                            // there can be parsing errors when secret data is invalid - do not create the input then
                            // TODO confirm if this will be reused for PASSWORD_DESCRIPTION_TOTP
                            PASSWORD_WITH_DESCRIPTION, PASSWORD_DESCRIPTION_TOTP -> (secretParser.extractDescription(
                                resourceTypeEnum,
                                secret
                            ) as DecryptedSecretOrError.DecryptedSecret<String>).secret
                            STANDALONE_TOTP -> {
                                throw IllegalArgumentException("Standalone totp does not contain description field")
                            }
                        }
                    }
                    else -> {
                        when (field.name) {
                            FieldNamesMapper.NAME_FIELD -> existingResource.name
                            FieldNamesMapper.USERNAME_FIELD -> existingResource.username
                            FieldNamesMapper.URI_FIELD -> existingResource.url
                            else -> ""
                        }
                    }
                }
                ResourceValue(field, initialValue)
            }
    }
}
