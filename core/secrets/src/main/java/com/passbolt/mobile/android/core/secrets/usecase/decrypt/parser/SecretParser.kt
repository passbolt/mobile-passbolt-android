package com.passbolt.mobile.android.core.secrets.usecase.decrypt.parser

import com.google.gson.Gson
import com.passbolt.mobile.android.core.resourcetypes.ResourceTypeFactory
import com.passbolt.mobile.android.core.resourcetypes.ResourceTypeFactory.ResourceTypeEnum.PASSWORD_WITH_DESCRIPTION
import com.passbolt.mobile.android.core.resourcetypes.ResourceTypeFactory.ResourceTypeEnum.SIMPLE_PASSWORD
import com.passbolt.mobile.android.core.resourcetypes.ResourceTypeFactory.ResourceTypeEnum.STANDALONE_TOTP
import com.passbolt.mobile.android.ui.DecryptedSecretOrError

class SecretParser(
    private val gson: Gson
) {

    fun extractPassword(
        resourceTypeEnum: ResourceTypeFactory.ResourceTypeEnum,
        decryptedSecret: ByteArray
    ): DecryptedSecretOrError<String> {
        return when (resourceTypeEnum) {
            SIMPLE_PASSWORD -> {
                try {
                    DecryptedSecretOrError.DecryptedSecret(String(decryptedSecret))
                } catch (exception: Exception) {
                    DecryptedSecretOrError.Error("Error during secret parsing: ${exception.message}")
                }
            }
            PASSWORD_WITH_DESCRIPTION -> {
                try {
                    val parsedSecret =
                        gson.fromJson(String(decryptedSecret), DecryptedSecret.PasswordWithDescription::class.java)
                    DecryptedSecretOrError.DecryptedSecret(parsedSecret.password)
                } catch (exception: Exception) {
                    DecryptedSecretOrError.Error("Error during secret parsing: ${exception.message}")
                }
            }
            STANDALONE_TOTP -> {
                throw IllegalArgumentException("Standalone totp resource type does not contain password secret")
            }
        }
    }

    fun extractDescription(
        resourceTypeEnum: ResourceTypeFactory.ResourceTypeEnum,
        decryptedSecret: ByteArray
    ): DecryptedSecretOrError<String> {
        return when (resourceTypeEnum) {
            SIMPLE_PASSWORD -> {
                throw IllegalArgumentException("Simple password resource type does not contain description secret")
            }
            PASSWORD_WITH_DESCRIPTION -> {
                try {
                    val parsedSecret =
                        gson.fromJson(String(decryptedSecret), DecryptedSecret.PasswordWithDescription::class.java)
                    DecryptedSecretOrError.DecryptedSecret(parsedSecret.description)
                } catch (exception: Exception) {
                    DecryptedSecretOrError.Error("Error during secret parsing: ${exception.message}")
                }
            }
            STANDALONE_TOTP -> {
                throw IllegalArgumentException("Standalone totp resource type does not contain description secret")
            }
        }
    }

    fun extractTotpData(
        resourceTypeEnum: ResourceTypeFactory.ResourceTypeEnum,
        decryptedSecret: ByteArray
    ): DecryptedSecretOrError<DecryptedSecret.StandaloneTotp> {
        return when (resourceTypeEnum) {
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
                    DecryptedSecretOrError.DecryptedSecret(parsedSecret)
                } catch (exception: Exception) {
                    DecryptedSecretOrError.Error("Error during secret parsing: ${exception.message}")
                }
            }
        }
    }
}
