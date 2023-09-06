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

package com.passbolt.mobile.android.core.secrets.usecase.decrypt.parser

import com.google.gson.Gson
import com.passbolt.mobile.android.core.resourcetypes.ResourceTypeFactory
import com.passbolt.mobile.android.core.resourcetypes.ResourceTypeFactory.ResourceTypeEnum.PASSWORD_DESCRIPTION_TOTP
import com.passbolt.mobile.android.core.resourcetypes.ResourceTypeFactory.ResourceTypeEnum.PASSWORD_WITH_DESCRIPTION
import com.passbolt.mobile.android.core.resourcetypes.ResourceTypeFactory.ResourceTypeEnum.SIMPLE_PASSWORD
import com.passbolt.mobile.android.core.resourcetypes.ResourceTypeFactory.ResourceTypeEnum.STANDALONE_TOTP
import com.passbolt.mobile.android.core.secrets.usecase.decrypt.parser.validation.SecretValidationRunner
import com.passbolt.mobile.android.ui.DecryptedSecretOrError
import timber.log.Timber

class SecretParser(
    private val gson: Gson,
    private val secretValidationRunner: SecretValidationRunner,
    private val resourceTypeFactory: ResourceTypeFactory
) {

    suspend fun extractPassword(
        resourceTypeId: String,
        decryptedSecret: ByteArray
    ): DecryptedSecretOrError<String> {
        return when (resourceTypeFactory.getResourceTypeEnum(resourceTypeId)) {
            SIMPLE_PASSWORD -> {
                try {
                    val parsedSecret = DecryptedSecret.SimplePassword(String(decryptedSecret))
                    if (secretValidationRunner.isPasswordStringSecretValid(parsedSecret)) {
                        DecryptedSecretOrError.DecryptedSecret(parsedSecret.password)
                    } else {
                        val errorMessage = "Invalid secret in password string resource type"
                        Timber.e(errorMessage)
                        DecryptedSecretOrError.Error.ValidationError(errorMessage)
                    }
                } catch (exception: Exception) {
                    val errorMessage = "Error during secret parsing: ${exception.message}"
                    Timber.e(exception, errorMessage)
                    DecryptedSecretOrError.Error.ParsingError(errorMessage)
                }
            }
            PASSWORD_WITH_DESCRIPTION -> {
                try {
                    val parsedSecret =
                        gson.fromJson(String(decryptedSecret), DecryptedSecret.PasswordWithDescription::class.java)
                    if (secretValidationRunner.isPasswordAndDescriptionSecretValid(parsedSecret)) {
                        DecryptedSecretOrError.DecryptedSecret(parsedSecret.password)
                    } else {
                        val errorMessage = "Invalid secret in password and description resource type"
                        Timber.e(errorMessage)
                        DecryptedSecretOrError.Error.ValidationError(errorMessage)
                    }
                } catch (exception: Exception) {
                    val errorMessage = "Error during secret parsing: ${exception.message}"
                    Timber.e(exception, errorMessage)
                    DecryptedSecretOrError.Error.ParsingError(errorMessage)
                }
            }
            STANDALONE_TOTP -> {
                throw IllegalArgumentException("Standalone totp resource type does not contain password secret")
            }
            PASSWORD_DESCRIPTION_TOTP -> {
                try {
                    val parsedSecret =
                        gson.fromJson(String(decryptedSecret), DecryptedSecret.PasswordDescriptionTotp::class.java)
                    if (secretValidationRunner.isPasswordDescriptionTotpSecretValid(parsedSecret)) {
                        DecryptedSecretOrError.DecryptedSecret(parsedSecret.password)
                    } else {
                        val errorMessage = "Invalid secret in password description totp resource type"
                        Timber.e(errorMessage)
                        DecryptedSecretOrError.Error.ValidationError(errorMessage)
                    }
                } catch (exception: Exception) {
                    val errorMessage = "Error during secret parsing: ${exception.message}"
                    Timber.e(exception, errorMessage)
                    DecryptedSecretOrError.Error.ParsingError(errorMessage)
                }
            }
        }
    }

    suspend fun extractDescription(
        resourceTypeId: String,
        decryptedSecret: ByteArray
    ): DecryptedSecretOrError<String> {
        return when (resourceTypeFactory.getResourceTypeEnum(resourceTypeId)) {
            SIMPLE_PASSWORD -> {
                throw IllegalArgumentException("Simple password resource type does not contain description secret")
            }
            PASSWORD_WITH_DESCRIPTION -> {
                try {
                    val parsedSecret =
                        gson.fromJson(String(decryptedSecret), DecryptedSecret.PasswordWithDescription::class.java)
                    if (secretValidationRunner.isPasswordAndDescriptionSecretValid(parsedSecret)) {
                        DecryptedSecretOrError.DecryptedSecret(parsedSecret.description.orEmpty())
                    } else {
                        val errorMessage = "Invalid secret in password and description resource type"
                        Timber.e(errorMessage)
                        DecryptedSecretOrError.Error.ValidationError(errorMessage)
                    }
                } catch (exception: Exception) {
                    val errorMessage = "Error during secret parsing: ${exception.message}"
                    Timber.e(exception, errorMessage)
                    DecryptedSecretOrError.Error.ParsingError(errorMessage)
                }
            }
            STANDALONE_TOTP -> {
                throw IllegalArgumentException("Standalone totp resource type does not contain description secret")
            }
            PASSWORD_DESCRIPTION_TOTP -> {
                try {
                    val parsedSecret =
                        gson.fromJson(String(decryptedSecret), DecryptedSecret.PasswordDescriptionTotp::class.java)
                    if (secretValidationRunner.isPasswordDescriptionTotpSecretValid(parsedSecret)) {
                        DecryptedSecretOrError.DecryptedSecret(parsedSecret.description.orEmpty())
                    } else {
                        val errorMessage = "Invalid secret in password description totp resource type"
                        Timber.e(errorMessage)
                        DecryptedSecretOrError.Error.ValidationError(errorMessage)
                    }
                } catch (exception: Exception) {
                    val errorMessage = "Error during secret parsing: ${exception.message}"
                    Timber.e(exception, errorMessage)
                    DecryptedSecretOrError.Error.ParsingError(errorMessage)
                }
            }
        }
    }

    suspend fun extractTotpData(
        resourceTypeId: String,
        decryptedSecret: ByteArray
    ): DecryptedSecretOrError<DecryptedSecret.StandaloneTotp.Totp> {
        return when (resourceTypeFactory.getResourceTypeEnum(resourceTypeId)) {
            SIMPLE_PASSWORD -> {
                throw IllegalArgumentException("Simple password resource type does not contain totp data")
            }
            PASSWORD_WITH_DESCRIPTION -> {
                throw IllegalArgumentException("Password with description resource type does not contain totp data")
            }
            STANDALONE_TOTP -> {
                try {
                    val parsedSecret =
                        gson.fromJson(String(decryptedSecret), DecryptedSecret.StandaloneTotp::class.java)
                    if (secretValidationRunner.isTotpSecretValid(parsedSecret)) {
                        DecryptedSecretOrError.DecryptedSecret(parsedSecret.totp)
                    } else {
                        val errorMessage = "Invalid secret in totp resource type"
                        Timber.e(errorMessage)
                        DecryptedSecretOrError.Error.ValidationError(errorMessage)
                    }
                } catch (exception: Exception) {
                    DecryptedSecretOrError.Error.ParsingError("Error during secret parsing: ${exception.message}")
                }
            }
            PASSWORD_DESCRIPTION_TOTP -> {
                try {
                    val parsedSecret =
                        gson.fromJson(String(decryptedSecret), DecryptedSecret.PasswordDescriptionTotp::class.java)
                    if (secretValidationRunner.isPasswordDescriptionTotpSecretValid(parsedSecret)) {
                        DecryptedSecretOrError.DecryptedSecret(parsedSecret.totp)
                    } else {
                        val errorMessage = "Invalid secret in password description totp resource type"
                        Timber.e(errorMessage)
                        DecryptedSecretOrError.Error.ValidationError(errorMessage)
                    }
                } catch (exception: Exception) {
                    DecryptedSecretOrError.Error.ParsingError("Error during secret parsing: ${exception.message}")
                }
            }
        }
    }
}
