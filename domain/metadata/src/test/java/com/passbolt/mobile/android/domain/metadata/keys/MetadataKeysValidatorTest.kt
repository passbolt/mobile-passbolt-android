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

import com.google.common.truth.Truth.assertThat
import com.passbolt.mobile.android.domain.metadata.keys.MetadataKeysValidator.Reason.CANNOT_COMPUTE_FINGERPRINT
import com.passbolt.mobile.android.domain.metadata.keys.MetadataKeysValidator.Reason.FINGERPRINT_NOT_MATCHING_KEY_DATA
import com.passbolt.mobile.android.domain.metadata.keys.MetadataKeysValidator.Reason.FINGERPRINT_NOT_MATCHING_METADATA_KEY
import com.passbolt.mobile.android.domain.metadata.keys.MetadataKeysValidator.Reason.INVALID_OBJECT_TYPE
import com.passbolt.mobile.android.domain.metadata.keys.MetadataKeysValidator.Reason.MISSING_FINGERPRINT
import com.passbolt.mobile.android.domain.metadata.keys.MetadataKeysValidator.Validation.Invalid
import com.passbolt.mobile.android.domain.metadata.keys.MetadataKeysValidator.Validation.Valid
import com.passbolt.mobile.android.gopenpgp.OpenPgp
import com.passbolt.mobile.android.gopenpgp.exception.OpenPgpError
import com.passbolt.mobile.android.gopenpgp.exception.OpenPgpFailure
import com.passbolt.mobile.android.gopenpgp.exception.OpenPgpResult
import com.passbolt.mobile.android.ui.DecryptedMetadataPrivateKeyJsonModel
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class MetadataKeysValidatorTest {
    private val openPgp = mock<OpenPgp>()
    private val validator = MetadataKeysValidator(openPgp)

    @Test
    fun `public key whose declared fingerprint matches key data ignoring case is valid`() =
        runTest {
            whenever(openPgp.getKeyFingerprint(PUBLIC_KEY)).thenReturn(OpenPgpResult.Result(FINGERPRINT_LOWER))

            val result = validator.validatePublicKey(PUBLIC_KEY, FINGERPRINT_UPPER)

            assertThat(result).isEqualTo(Valid(FINGERPRINT_LOWER))
        }

    @Test
    fun `public key without declared fingerprint is invalid`() =
        runTest {
            val result = validator.validatePublicKey(PUBLIC_KEY, null)

            assertThat(result).isEqualTo(Invalid(MISSING_FINGERPRINT))
            verify(openPgp, never()).getKeyFingerprint(any())
        }

    @Test
    fun `public key whose fingerprint cannot be computed is invalid`() =
        runTest {
            whenever(openPgp.getKeyFingerprint(PUBLIC_KEY)).thenReturn(pgpError())

            val result = validator.validatePublicKey(PUBLIC_KEY, FINGERPRINT_UPPER)

            assertThat(result).isEqualTo(Invalid(CANNOT_COMPUTE_FINGERPRINT))
        }

    @Test
    fun `public key whose declared fingerprint does not match key data is invalid`() =
        runTest {
            whenever(openPgp.getKeyFingerprint(PUBLIC_KEY)).thenReturn(OpenPgpResult.Result(OTHER_FINGERPRINT))

            val result = validator.validatePublicKey(PUBLIC_KEY, FINGERPRINT_UPPER)

            assertThat(result).isEqualTo(Invalid(FINGERPRINT_NOT_MATCHING_KEY_DATA))
        }

    @Test
    fun `private key matching its key data and its metadata key ignoring case is valid`() =
        runTest {
            whenever(openPgp.getKeyFingerprint(PRIVATE_KEY)).thenReturn(OpenPgpResult.Result(FINGERPRINT_LOWER))

            val result = validator.validatePrivateKey(privateKey(fingerprint = FINGERPRINT_UPPER), FINGERPRINT_UPPER)

            assertThat(result).isEqualTo(Valid(FINGERPRINT_LOWER))
        }

    @Test
    fun `private key with invalid object type is invalid without computing fingerprint`() =
        runTest {
            val result = validator.validatePrivateKey(privateKey(objectType = "OTHER"), FINGERPRINT_UPPER)

            assertThat(result).isEqualTo(Invalid(INVALID_OBJECT_TYPE))
            verify(openPgp, never()).getKeyFingerprint(any())
        }

    @Test
    fun `private key whose fingerprint cannot be computed is invalid`() =
        runTest {
            whenever(openPgp.getKeyFingerprint(PRIVATE_KEY)).thenReturn(pgpError())

            val result = validator.validatePrivateKey(privateKey(), FINGERPRINT_UPPER)

            assertThat(result).isEqualTo(Invalid(CANNOT_COMPUTE_FINGERPRINT))
        }

    @Test
    fun `private key whose declared fingerprint does not match key data is invalid`() =
        runTest {
            whenever(openPgp.getKeyFingerprint(PRIVATE_KEY)).thenReturn(OpenPgpResult.Result(FINGERPRINT_LOWER))

            val result = validator.validatePrivateKey(privateKey(fingerprint = OTHER_FINGERPRINT), FINGERPRINT_UPPER)

            assertThat(result).isEqualTo(Invalid(FINGERPRINT_NOT_MATCHING_KEY_DATA))
        }

    @Test
    fun `private key not belonging to its metadata key is invalid`() =
        runTest {
            whenever(openPgp.getKeyFingerprint(PRIVATE_KEY)).thenReturn(OpenPgpResult.Result(OTHER_FINGERPRINT))

            val result = validator.validatePrivateKey(privateKey(fingerprint = OTHER_FINGERPRINT), FINGERPRINT_UPPER)

            assertThat(result).isEqualTo(Invalid(FINGERPRINT_NOT_MATCHING_METADATA_KEY))
        }

    private fun privateKey(
        fingerprint: String = FINGERPRINT_UPPER,
        objectType: String = "PASSBOLT_METADATA_PRIVATE_KEY",
    ) = DecryptedMetadataPrivateKeyJsonModel(
        objectType = objectType,
        armoredKey = PRIVATE_KEY,
        passphrase = "",
        fingerprint = fingerprint,
        domain = "https://passbolt.local",
    )

    private fun pgpError() = OpenPgpResult.Error(OpenPgpFailure.Generic(OpenPgpError("error")))

    private companion object {
        private const val PUBLIC_KEY = "-----BEGIN PGP PUBLIC KEY BLOCK-----"
        private const val PRIVATE_KEY = "-----BEGIN PGP PRIVATE KEY BLOCK-----"
        private const val FINGERPRINT_LOWER = "03f60e958f4cb29dbe4be4eb3bd91e325cc7d42c"
        private const val FINGERPRINT_UPPER = "03F60E958F4CB29DBE4BE4EB3BD91E325CC7D42C"
        private const val OTHER_FINGERPRINT = "0000000000000000000000000000000000000000"
    }
}
