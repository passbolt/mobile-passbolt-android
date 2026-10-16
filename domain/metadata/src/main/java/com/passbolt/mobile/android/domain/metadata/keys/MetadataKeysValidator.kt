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

package com.passbolt.mobile.android.domain.metadata.keys

import com.passbolt.mobile.android.gopenpgp.OpenPgp
import com.passbolt.mobile.android.gopenpgp.exception.OpenPgpResult
import com.passbolt.mobile.android.ui.DecryptedMetadataPrivateKeyJsonModel

class MetadataKeysValidator(
    private val openPgp: OpenPgp,
) {
    suspend fun validatePublicKey(
        armoredKey: String,
        declaredFingerprint: String?,
    ): Validation {
        if (declaredFingerprint == null) {
            return Validation.Invalid(Reason.MISSING_FINGERPRINT)
        }
        return validateFingerprintMatchesKeyData(armoredKey, declaredFingerprint)
    }

    suspend fun validatePrivateKey(
        privateKey: DecryptedMetadataPrivateKeyJsonModel,
        metadataKeyFingerprint: String,
    ): Validation {
        if (privateKey.objectType != METADATA_PRIVATE_KEY_OBJECT_TYPE) {
            return Validation.Invalid(Reason.INVALID_OBJECT_TYPE)
        }
        return when (val validation = validateFingerprintMatchesKeyData(privateKey.armoredKey, privateKey.fingerprint)) {
            is Validation.Invalid -> validation
            is Validation.Valid ->
                if (validation.fingerprint.equals(metadataKeyFingerprint, ignoreCase = true)) {
                    validation
                } else {
                    Validation.Invalid(Reason.FINGERPRINT_NOT_MATCHING_METADATA_KEY)
                }
        }
    }

    private suspend fun validateFingerprintMatchesKeyData(
        armoredKey: String,
        declaredFingerprint: String,
    ): Validation =
        when (val computedFingerprint = openPgp.getKeyFingerprint(armoredKey)) {
            is OpenPgpResult.Error -> Validation.Invalid(Reason.CANNOT_COMPUTE_FINGERPRINT)
            is OpenPgpResult.Result ->
                if (computedFingerprint.result.equals(declaredFingerprint, ignoreCase = true)) {
                    Validation.Valid(computedFingerprint.result)
                } else {
                    Validation.Invalid(Reason.FINGERPRINT_NOT_MATCHING_KEY_DATA)
                }
        }

    sealed class Validation {
        data class Valid(
            val fingerprint: String,
        ) : Validation()

        data class Invalid(
            val reason: Reason,
        ) : Validation()
    }

    enum class Reason(
        val logMessage: String,
    ) {
        MISSING_FINGERPRINT("Metadata key does not declare a fingerprint"),
        CANNOT_COMPUTE_FINGERPRINT("Unable to compute fingerprint from key data"),
        FINGERPRINT_NOT_MATCHING_KEY_DATA("Declared fingerprint does not match the key data"),
        INVALID_OBJECT_TYPE("Metadata private key has an invalid object type"),
        FINGERPRINT_NOT_MATCHING_METADATA_KEY("Metadata private key fingerprint does not match its metadata key"),
    }

    private companion object {
        private const val METADATA_PRIVATE_KEY_OBJECT_TYPE = "PASSBOLT_METADATA_PRIVATE_KEY"
    }
}
