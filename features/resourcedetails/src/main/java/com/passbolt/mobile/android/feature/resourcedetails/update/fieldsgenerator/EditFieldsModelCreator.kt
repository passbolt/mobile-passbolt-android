package com.passbolt.mobile.android.feature.resourcedetails.update.fieldsgenerator

import com.passbolt.mobile.android.core.resources.actions.ResourcePropertiesActionsInteractor
import com.passbolt.mobile.android.core.resourcetypes.usecase.db.ResourceTypeIdToSlugMappingProvider
import com.passbolt.mobile.android.core.secrets.usecase.decrypt.parser.SecretParser
import com.passbolt.mobile.android.feature.resourcedetails.update.ResourceValue
import com.passbolt.mobile.android.supportedresourceTypes.ContentType
import com.passbolt.mobile.android.supportedresourceTypes.ContentType.PasswordAndDescription
import com.passbolt.mobile.android.supportedresourceTypes.ContentType.PasswordDescriptionTotp
import com.passbolt.mobile.android.supportedresourceTypes.ContentType.PasswordString
import com.passbolt.mobile.android.supportedresourceTypes.ContentType.V5Default
import com.passbolt.mobile.android.supportedresourceTypes.ContentType.V5DefaultWithTotp
import com.passbolt.mobile.android.supportedresourceTypes.ContentType.V5PasswordString
import com.passbolt.mobile.android.ui.DecryptedSecretOrError
import com.passbolt.mobile.android.ui.ResourceModel
import kotlinx.coroutines.flow.first
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koin.core.parameter.parametersOf
import java.util.UUID

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
    private val secretParser: SecretParser,
    private val idToSlugMappingProvider: ResourceTypeIdToSlugMappingProvider,
    private val newFieldsModelCreator: NewFieldsModelCreator
) : KoinComponent {

    @Throws(ClassCastException::class)
    suspend fun create(
        existingResource: ResourceModel,
        secret: ByteArray
    ): List<ResourceValue> {

        val editedResourceTypeSlug = idToSlugMappingProvider.provideMappingForSelectedAccount()[
            UUID.fromString(existingResource.resourceTypeId)
        ]

        requireNotNull(editedResourceTypeSlug)

        val resourceTypeFields = newFieldsModelCreator.create(ContentType.fromSlug(editedResourceTypeSlug))

        return resourceTypeFields
            .filter { it.field.name in updateResourceFormSupportedFieldNames }
            .map { field ->
                val initialValue = when (field.field.name) {
                    in listOf(FieldNamesMapper.PASSWORD_FIELD, FieldNamesMapper.SECRET_FIELD) -> {
                        val parsedSecret = (secretParser.parseSecret(
                            existingResource.resourceTypeId,
                            secret
                        ) as DecryptedSecretOrError.DecryptedSecret).secret
                        when (editedResourceTypeSlug) {
                            PasswordString.slug, V5PasswordString.slug -> parsedSecret.password
                            PasswordAndDescription.slug, PasswordDescriptionTotp.slug,
                            V5Default.slug, V5DefaultWithTotp.slug -> parsedSecret.secret
                            else -> {
                                throw IllegalArgumentException("Standalone totp does not contain password or secret")
                            }
                        }
                    }
                    FieldNamesMapper.DESCRIPTION_FIELD -> {
                        when (editedResourceTypeSlug) {
                            PasswordString.slug, V5PasswordString.slug -> existingResource.metadataJsonModel.description
                            // there can be parsing errors when secret data is invalid - do not create the input then
                            PasswordAndDescription.slug, PasswordDescriptionTotp.slug,
                            V5Default.slug, V5DefaultWithTotp.slug -> (secretParser.parseSecret(
                                existingResource.resourceTypeId,
                                secret
                            ) as DecryptedSecretOrError.DecryptedSecret).secret.description.orEmpty()
                            else -> {
                                throw IllegalArgumentException("Standalone totp does not contain description field")
                            }
                        }
                    }
                    else -> {
                        when (field.field.name) {
                            FieldNamesMapper.NAME_FIELD -> existingResource.metadataJsonModel.name
                            FieldNamesMapper.USERNAME_FIELD -> existingResource.metadataJsonModel.username
                            FieldNamesMapper.URI_FIELD -> {
                                val resourcePropertiesActionsInteractor = get<ResourcePropertiesActionsInteractor> {
                                    parametersOf(existingResource)
                                }
                                resourcePropertiesActionsInteractor.provideWebsiteUrl().first().result
                            }
                            else -> ""
                        }
                    }
                }
                ResourceValue(field.field, initialValue)
            }
    }

    private companion object {
        /*
        currently on update resource form the following resource types can be edited:
         * "simple password" (can be edited in whole)
         * "password with description" (can be edited in whole)
         * "password with description and totp" can be edited (only common fields can be edited; the totp fields have a
            separate form)
        */
        private val updateResourceFormSupportedFieldNames = listOf(
            FieldNamesMapper.NAME_FIELD,
            FieldNamesMapper.DESCRIPTION_FIELD,
            FieldNamesMapper.USERNAME_FIELD,
            FieldNamesMapper.URI_FIELD,
            FieldNamesMapper.PASSWORD_FIELD,
            FieldNamesMapper.SECRET_FIELD
        )
    }
}
