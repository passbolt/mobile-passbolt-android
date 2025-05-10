package com.passbolt.mobile.android.passboltapi.metadata

import com.passbolt.mobile.android.core.networking.ResponseHandler
import com.passbolt.mobile.android.core.networking.callWithHandler
import com.passbolt.mobile.android.dto.request.EncryptedDataAndModifiedRequest
import com.passbolt.mobile.android.dto.request.EncryptedDataRequest

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
class MetadataRepository(
    private val metadataDataSource: MetadataDataSource,
    private val responseHandler: ResponseHandler
) {

    suspend fun getMetadataKeys() = callWithHandler(responseHandler) {
        metadataDataSource.getMetadataKeys()
    }

    suspend fun updateMetadataPrivateKey(uuid: String, request: EncryptedDataRequest) =
        callWithHandler(responseHandler) {
            metadataDataSource.putMetadataPrivateKey(uuid, request)
        }

    suspend fun getMetadataTypesSettings() = callWithHandler(responseHandler) {
        metadataDataSource.getMetadataTypesSettings()
    }

    suspend fun getMetadataKeysSettings() = callWithHandler(responseHandler) {
        metadataDataSource.getMetadataKeysSettings()
    }

    suspend fun getMetadataSessionKeys() = callWithHandler(responseHandler) {
        metadataDataSource.getMetadataSessionKeys()
    }

    suspend fun postMetadataSessionKeys(request: EncryptedDataRequest) = callWithHandler(responseHandler) {
        metadataDataSource.postMetadataSessionKeys(request)
    }

    suspend fun updateMetadataSessionKeys(uuid: String, request: EncryptedDataAndModifiedRequest) =
        callWithHandler(responseHandler) {
            metadataDataSource.updateMetadataSessionKeys(uuid, request)
        }
}
