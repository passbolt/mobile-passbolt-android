package com.passbolt.mobile.android.mappers

import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken
import com.passbolt.mobile.android.dto.response.ResourceTypeDto
import com.passbolt.mobile.android.entity.resource.ResourceField
import com.passbolt.mobile.android.entity.resource.ResourceType
import com.passbolt.mobile.android.entity.resource.ResourceTypeIdWithFields

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

/*
 * Mapper that uses Resource Types JSON Schema returned from backend to produce application resource fileds model
 * that can be saved into the database and easily be understood by the application.
 *
 * Note: only a small subset of JSON Schema is supported for now as new resources are not expected have the full
 * possible primitives range and full possible type enumerators range. For now the only supported type is `string`
 * and the only supported type enumerator is `anyOf`. Nullable field type is handled via a separate flag returned by
 * the backend in `required` properties array.
 */
class ResourceTypesModelMapper(
    private val gson: Gson
) {

    fun map(resourceTypesDto: List<ResourceTypeDto>): List<ResourceTypeIdWithFields> =
        resourceTypesDto.map {
            val resourceFields = processFieldsDefinition(it.definition.resource, FieldKind.RESOURCE)
            val secretFields = processFieldsDefinition(it.definition.secret, FieldKind.SECRET)

            ResourceTypeIdWithFields(
                ResourceType(
                    resourceTypeId = it.id.toString(),
                    name = it.name,
                    slug = it.slug,
                    resourceSchemaJson = it.definition.resource,
                    secretSchemaJson = it.definition.secret
                ),
                resourceFields + secretFields
            )
        }

    /*
     * Processes fields definition. Currently supports objects with flat properties list and objects with property
     * object "totp" + primitive types: string and number.
     */
    private fun processFieldsDefinition(
        resourceDefinitionElement: JsonElement,
        resourceFieldKind: FieldKind
    ): List<ResourceField> {
        val result = mutableListOf<ResourceField>()

        when (resourceDefinitionElement.asJsonObject.getMemberAsString(FIELD_TYPE)) {
            FieldType.OBJECT.type -> {
                val propertiesObject = resourceDefinitionElement
                    .asJsonObject
                    .getAsJsonObject(FIELD_PROPERTIES)
                propertiesObject.keySet().forEach { propertyName ->
                    if (propertyName != OBJECT_NAME_TOTP) {
                        processFlatProperties(
                            resourceDefinitionElement,
                            propertiesObject,
                            propertyName,
                            result,
                            resourceFieldKind
                        )
                    } else {
                        // FIXME validation rules for otp not parsed
                        // to support fully dynamic schemas a bigger rework of the existing mechanism is needed
                        processTotpObject(result, resourceDefinitionElement)
                    }
                }
            }
            FieldType.STRING.type, FieldType.NUMBER.type -> {
                processPrimitiveFields(resourceDefinitionElement, result, resourceFieldKind)
            }
        }

        return result
    }

    private fun processPrimitiveFields(
        resourceDefinitionElement: JsonElement,
        result: MutableList<ResourceField>,
        resourceFieldKind: FieldKind
    ) {
        val type = gson.fromJson(resourceDefinitionElement, FieldTypeDefinition::class.java)
        result += ResourceField(
            name = resourceFieldKind.kind,
            isSecret = resourceFieldKind == FieldKind.SECRET,
            maxLength = getFieldMaxLength(listOf(type)),
            isRequired = true,
            type = getFieldType(TypeEnumerator.SINGLE, listOf(type))
        )
    }

    private fun processFlatProperties(
        resourceDefinitionElement: JsonElement,
        propertiesElement: JsonElement,
        propertyName: String,
        result: MutableList<ResourceField>,
        resourceFieldKind: FieldKind
    ) {
        val requiredPropertiesNames = resourceDefinitionElement.asJsonObject.getAsJsonArray(FIELD_REQUIRED)
            .map { it.asString }
        val possibleTypeObject = propertiesElement.asJsonObject.getAsJsonObject(propertyName)
        val typeEnumerator: TypeEnumerator
        val typesList = if (possibleTypeObject.hasTypeEnumeration()) {
            typeEnumerator = TypeEnumerator.ANY_OF // only `anyOf` is supported for now
            val typesObject = possibleTypeObject.entrySet().iterator().next().value
            gson.fromJson(typesObject, FIELD_TYPE_DEFINITION_LIST_TYPE_TOKEN)
        } else {
            typeEnumerator = TypeEnumerator.SINGLE
            listOf(
                gson.fromJson(possibleTypeObject, FieldTypeDefinition::class.java)
            )
        }

        result += ResourceField(
            name = propertyName,
            isSecret = resourceFieldKind == FieldKind.SECRET,
            maxLength = getFieldMaxLength(typesList),
            isRequired = requiredPropertiesNames.contains(propertyName),
            type = getFieldType(typeEnumerator, typesList)
        )
    }

    private fun processTotpObject(
        result: MutableList<ResourceField>,
        resourceDefinitionObject: JsonElement
    ) {
        result += processFieldsDefinition(
            resourceDefinitionObject
                .asJsonObject
                .getAsJsonObject(FIELD_PROPERTIES)
                .getAsJsonObject(OBJECT_NAME_TOTP),
            FieldKind.SECRET
        )
    }

    /*
     * This method returns a String representation of possible resource field types.
     *
     * typesList - contains possible types("string", "number", "boolean", etc and their validations("maxLength", etc);
     *   Currently only "string" and "number" is supported; type nullability is supported using "isRequired" property
     * typeEnumerator - contains types relation ("anyOf", "oneOf", etc);
     *   Currently only "anyOf" is supported
     */
    @Suppress("UnusedParameter")
    private fun getFieldType(typeEnumerator: TypeEnumerator, typesList: List<FieldTypeDefinition>?): String =
        if (typesList?.any { it.type == FieldType.NUMBER.type } == true) {
            FieldType.NUMBER.type
        } else {
            FieldType.STRING.type
        }

    private fun getFieldMaxLength(typesList: List<FieldTypeDefinition>?) =
        typesList
            ?.mapNotNull { it.maxLength }
            ?.maxOrNull()

    private fun JsonObject.hasTypeEnumeration() =
        TypeEnumerator.values().map { it.enumerator }.any { this.has(it) }

    private fun JsonObject.getMemberAsString(fieldType: String) =
        getAsJsonPrimitive(fieldType).asString

    private companion object {
        private const val FIELD_TYPE = "type"
        private const val FIELD_PROPERTIES = "properties"
        private const val FIELD_REQUIRED = "required"
        private const val OBJECT_NAME_TOTP = "totp"
        private val FIELD_TYPE_DEFINITION_LIST_TYPE_TOKEN =
            object : TypeToken<List<FieldTypeDefinition>>() {}.type
    }
}

data class FieldTypeDefinition(
    val type: String,
    val maxLength: Int?
)

enum class FieldType(val type: String) {
    STRING("string"),
    OBJECT("object"),
    NUMBER("number")
}

enum class FieldKind(val kind: String) {
    RESOURCE("resource"),
    SECRET("secret")
}

enum class TypeEnumerator(val enumerator: String) {
    ANY_OF("anyOf"),
    SINGLE("single")
}
