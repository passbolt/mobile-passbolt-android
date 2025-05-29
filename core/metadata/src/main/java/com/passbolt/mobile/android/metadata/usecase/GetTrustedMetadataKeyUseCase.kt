package com.passbolt.mobile.android.metadata.usecase

import com.passbolt.mobile.android.common.usecase.AsyncUseCase
import com.passbolt.mobile.android.core.accounts.usecase.SelectedAccountUseCase
import com.passbolt.mobile.android.encryptedstorage.EncryptedSharedPreferencesFactory
import timber.log.Timber
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
class GetTrustedMetadataKeyUseCase(
    private val encryptedSharedPreferencesFactory: EncryptedSharedPreferencesFactory,
) : AsyncUseCase<Unit, GetTrustedMetadataKeyUseCase.Output>,
    SelectedAccountUseCase {
    @Suppress("LongMethod")
    override suspend fun execute(input: Unit): Output {
        val fileName = TrustedMetadataKeyFileName(selectedAccountId).name
        val sharedPreferences = encryptedSharedPreferencesFactory.get("$fileName.xml")

        return if (sharedPreferences.contains(MetadataTypesStorageConstants.TRUSTED_MD_KEY_KEY_PGP_MESSAGE)) {
            try {
                Output.TrustedKey(
                    id =
                        UUID.fromString(
                            sharedPreferences.getString(
                                MetadataTypesStorageConstants.TRUSTED_MD_KEY_ID,
                                "",
                            ) ?: "",
                        ),
                    userId =
                        UUID.fromString(
                            sharedPreferences.getString(
                                MetadataTypesStorageConstants.TRUSTED_MD_KEY_USER_ID,
                                "",
                            ),
                        ),
                    keyData =
                        sharedPreferences.getString(
                            MetadataTypesStorageConstants.TRUSTED_MD_KEY_KEY_DATA,
                            "",
                        ) ?: "",
                    passphrase =
                        sharedPreferences.getString(
                            MetadataTypesStorageConstants.TRUSTED_MD_KEY_PASSPHRASE,
                            "",
                        ) ?: "",
                    created =
                        ZonedDateTime.parse(
                            sharedPreferences.getString(
                                MetadataTypesStorageConstants.TRUSTED_MD_KEY_CREATED,
                                "",
                            ) ?: "",
                        ),
                    createdBy =
                        try {
                            UUID.fromString(
                                sharedPreferences.getString(
                                    MetadataTypesStorageConstants.TRUSTED_MD_KEY_CREATED_BY,
                                    "",
                                ),
                            )
                        } catch (e: Exception) {
                            null
                        },
                    modified =
                        ZonedDateTime.parse(
                            sharedPreferences.getString(
                                MetadataTypesStorageConstants.TRUSTED_MD_KEY_MODIFIED,
                                "",
                            ) ?: "",
                        ),
                    modifiedBy =
                        try {
                            UUID.fromString(
                                sharedPreferences.getString(
                                    MetadataTypesStorageConstants.TRUSTED_MD_KEY_MODIFIED_BY,
                                    "",
                                ),
                            )
                        } catch (e: Exception) {
                            null
                        },
                    keyPgpMessage =
                        sharedPreferences.getString(
                            MetadataTypesStorageConstants.TRUSTED_MD_KEY_KEY_PGP_MESSAGE,
                            "",
                        ) ?: "",
                    signingKeyFingerprint =
                        sharedPreferences.getString(
                            MetadataTypesStorageConstants.TRUSTED_MD_KEY_SIGNING_KEY_FINGERPRINT,
                            "",
                        ) ?: "",
                    signatureCreationTimestampSeconds =
                        sharedPreferences.getLong(
                            MetadataTypesStorageConstants.TRUSTED_MD_SIGNATURE_CREATION_TIMESTAMP,
                            0L,
                        ),
                    signedUsername =
                        sharedPreferences.getString(
                            MetadataTypesStorageConstants.TRUSTED_MD_KEY_SIGNED_USERNAME,
                            "",
                        ) ?: "",
                    signedName =
                        sharedPreferences.getString(
                            MetadataTypesStorageConstants.TRUSTED_MD_KEY_SIGNED_NAME,
                            "",
                        ) ?: "",
                )
            } catch (e: Exception) {
                Timber.e(e, "There was an error while getting the trusted metadata key")
                Output.NoTrustedKey
            }
        } else {
            Output.NoTrustedKey
        }
    }

    sealed class Output {
        data class TrustedKey(
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
        ) : Output()

        data object NoTrustedKey : Output()
    }
}
