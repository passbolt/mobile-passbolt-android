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

package com.passbolt.mobile.android.core.resources.actions

import com.passbolt.mobile.android.serializers.jsonschema.SchemaEntity
import com.passbolt.mobile.android.ui.NewMetadataKeyToTrustModel
import com.passbolt.mobile.android.ui.TrustedKeyDeletedModel

sealed class ResourceUpdateActionResult {

    data class Success(val resourceId: String, val resourceName: String) : ResourceUpdateActionResult()

    data class Failure(val message: String? = null) : ResourceUpdateActionResult()

    data object FetchFailure : ResourceUpdateActionResult()

    data object Unauthorized : ResourceUpdateActionResult()

    class CryptoFailure(val message: String? = null) : ResourceUpdateActionResult()

    data object CannotUpdateWithCurrentConfig : ResourceUpdateActionResult()

    class JsonSchemaValidationFailure(val entity: SchemaEntity) : ResourceUpdateActionResult()

    data object MetadataKeyVerificationFailure : ResourceUpdateActionResult()

    data class MetadataKeyModified(val keyToTrust: NewMetadataKeyToTrustModel) : ResourceUpdateActionResult()

    data class MetadataKeyDeleted(val deletedKey: TrustedKeyDeletedModel) : ResourceUpdateActionResult()
}
