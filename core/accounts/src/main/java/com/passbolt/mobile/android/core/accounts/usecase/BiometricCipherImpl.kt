package com.passbolt.mobile.android.core.accounts.usecase

import com.passbolt.mobile.android.common.usecase.UserIdInput
import com.passbolt.mobile.android.core.accounts.usecase.biometrickey.GetBiometricKeyIvUseCase
import com.passbolt.mobile.android.encryptedstorage.biometric.BiometricCipher
import com.passbolt.mobile.android.encryptedstorage.biometric.BiometricCrypto.Companion.BIOMETRIC_KEY_ALIAS
import com.passbolt.mobile.android.encryptedstorage.biometric.KeyStoreWrapper
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec

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

class BiometricCipherImpl(
    private val keyStoreWrapper: KeyStoreWrapper,
    private val getBiometricKeyIvUseCase: GetBiometricKeyIvUseCase,
) : BiometricCipher {
    override fun getBiometricEncryptCipher(): Cipher =
        newSymmetricCipher().apply {
            val biometricKey = keyStoreWrapper.getOrCreateSymmetricKey(BIOMETRIC_KEY_ALIAS)
            init(Cipher.ENCRYPT_MODE, biometricKey)
        }

    override fun getBiometricDecryptCipher(userId: String): Cipher =
        newSymmetricCipher().apply {
            val key =
                keyStoreWrapper.getSymmetricKey(BIOMETRIC_KEY_ALIAS)
                    ?: throw SecurityException("Unable to decrypt: No keys found")
            val ivOutput = getBiometricKeyIvUseCase.execute(UserIdInput(userId))
            init(Cipher.DECRYPT_MODE, key, IvParameterSpec(ivOutput.iv))
        }

    companion object {
        private const val TRANSFORMATION_SYMMETRIC = "AES/CBC/PKCS7Padding"

        private fun newSymmetricCipher() = Cipher.getInstance(TRANSFORMATION_SYMMETRIC)
    }
}
