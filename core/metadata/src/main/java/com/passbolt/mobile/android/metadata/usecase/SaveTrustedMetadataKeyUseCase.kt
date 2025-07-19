package com.passbolt.mobile.android.metadata.usecase

import com.passbolt.mobile.android.common.usecase.AsyncUseCase
import com.passbolt.mobile.android.core.accounts.usecase.SelectedAccountUseCase
import com.passbolt.mobile.android.encryptedstorage.EncryptedSharedPreferencesFactory
import java.time.ZonedDateTime
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
class SaveTrustedMetadataKeyUseCase(
    private val encryptedSharedPreferencesFactory: EncryptedSharedPreferencesFactory,
) : AsyncUseCase<SaveTrustedMetadataKeyUseCase.Input, Unit>,
    SelectedAccountUseCase {
    override suspend fun execute(input: Input) {
        val fileName = TrustedMetadataKeyFileName(selectedAccountId).name
        val sharedPreferences = encryptedSharedPreferencesFactory.get("$fileName.xml")
        with(sharedPreferences.edit()) {
            putString(
                MetadataTypesStorageConstants.TRUSTED_MD_KEY_ID,
                input.id.toString(),
            )
            putString(
                MetadataTypesStorageConstants.TRUSTED_MD_KEY_USER_ID,
                input.userId.toString(),
            )
            putString(
                MetadataTypesStorageConstants.TRUSTED_MD_KEY_KEY_DATA,
                input.keyData,
            )
            putString(
                MetadataTypesStorageConstants.TRUSTED_MD_KEY_PASSPHRASE,
                input.passphrase,
            )
            putString(
                MetadataTypesStorageConstants.TRUSTED_MD_KEY_CREATED,
                input.created.toString(),
            )
            putString(
                MetadataTypesStorageConstants.TRUSTED_MD_KEY_CREATED_BY,
                input.createdBy.toString(),
            )
            putString(
                MetadataTypesStorageConstants.TRUSTED_MD_KEY_MODIFIED,
                input.modified.toString(),
            )
            putString(
                MetadataTypesStorageConstants.TRUSTED_MD_KEY_MODIFIED_BY,
                input.modifiedBy.toString(),
            )
            putString(
                MetadataTypesStorageConstants.TRUSTED_MD_KEY_KEY_PGP_MESSAGE,
                input.keyPgpMessage,
            )
            putString(
                MetadataTypesStorageConstants.TRUSTED_MD_KEY_SIGNING_KEY_FINGERPRINT,
                input.signingKeyFingerprint,
            )
            putString(
                MetadataTypesStorageConstants.TRUSTED_MD_KEY_SIGNED_USERNAME,
                input.signedUsername,
            )
            putLong(
                MetadataTypesStorageConstants.TRUSTED_MD_SIGNATURE_CREATION_TIMESTAMP,
                input.signatureCreationTimestampSeconds,
            )
            putString(
                MetadataTypesStorageConstants.TRUSTED_MD_KEY_SIGNED_NAME,
                input.signedName,
            )
            apply()
        }
    }

    data class Input(
        val id: UUID,
        val userId: UUID,
        val keyData: String,
        val passphrase: String,
        val created: ZonedDateTime,
        val createdBy: UUID?,
        val modified: ZonedDateTime,
        val modifiedBy: UUID?,
        val keyPgpMessage: String,
        val signingKeyFingerprint: String,
        val signatureCreationTimestampSeconds: Long,
        val signedUsername: String,
        val signedName: String,
    )
}
