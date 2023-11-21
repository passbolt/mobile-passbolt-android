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

package com.passbolt.mobile.android.serializers.jsonschema.schamarepository

import android.content.Context
import com.passbolt.mobile.android.supportedresourceTypes.SupportedContentTypes
import net.jimblackler.jsonschemafriend.Schema
import net.jimblackler.jsonschemafriend.SchemaStore

class JSFSchemaRepository(
    private val context: Context
) : JsonSchemaRepository<Schema> {

    private val resourceSchemaRepository = hashMapOf<String, Schema>()
    private val secretsSchemaRepository = hashMapOf<String, Schema>()

    override fun loadLocalSchemas() {
        SupportedContentTypes.allSlugs.forEach { slug ->
            resourceSchemaRepository[slug] =
                SchemaStore().loadSchema(context.assets.open("$slug-resource-schema.json"))
            secretsSchemaRepository[slug] =
                SchemaStore().loadSchema(context.assets.open("$slug-secret-schema.json"))
        }
    }

    override fun schemaForResource(resourceSlug: String) =
        resourceSchemaRepository[resourceSlug]
            ?: error("Resource schema repository does not contain a schema for $resourceSlug")

    override fun schemaForSecret(resourceSlug: String) =
        secretsSchemaRepository[resourceSlug]
            ?: error("Secret schema repository does not contain a schema for $resourceSlug")
}
