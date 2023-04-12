package com.passbolt.mobile.android.core.secrets.usecase.decrypt.parser

import com.google.gson.Gson
import com.passbolt.mobile.android.core.resourcetypes.ResourceTypeFactory
import com.passbolt.mobile.android.core.resourcetypes.ResourceTypeFactory.ResourceTypeEnum.PASSWORD_WITH_DESCRIPTION
import com.passbolt.mobile.android.core.resourcetypes.ResourceTypeFactory.ResourceTypeEnum.SIMPLE_PASSWORD
import com.passbolt.mobile.android.core.resourcetypes.ResourceTypeFactory.ResourceTypeEnum.STANDALONE_TOTP

class SecretParser(
    private val gson: Gson
) {

    fun extractPassword(resourceTypeEnum: ResourceTypeFactory.ResourceTypeEnum, decryptedSecret: ByteArray): String {
        return when (resourceTypeEnum) {
            SIMPLE_PASSWORD -> {
                String(decryptedSecret)
            }
            PASSWORD_WITH_DESCRIPTION -> {
                gson.fromJson(String(decryptedSecret), DecryptedSecret.PasswordWithDescription::class.java).password
            }
            STANDALONE_TOTP -> {
                throw IllegalArgumentException("Stanalone totp resource type does not contain password secret")
            }
        }
    }

    fun extractDescription(resourceTypeEnum: ResourceTypeFactory.ResourceTypeEnum, decryptedSecret: ByteArray): String {
        return when (resourceTypeEnum) {
            SIMPLE_PASSWORD -> {
                throw IllegalArgumentException("Simple password resource type does not contain description secret")
            }
            PASSWORD_WITH_DESCRIPTION -> {
                gson.fromJson(String(decryptedSecret), DecryptedSecret.PasswordWithDescription::class.java).description
            }
            STANDALONE_TOTP -> {
                throw IllegalArgumentException("Stanalone totp resource type does not contain description secret")
            }
        }
    }

    fun extractTotpData(
        resourceTypeEnum: ResourceTypeFactory.ResourceTypeEnum,
        decryptedSecret: ByteArray
    ): DecryptedSecret.StandaloneTotp {
        return when (resourceTypeEnum) {
            SIMPLE_PASSWORD -> {
                throw IllegalArgumentException("Simple password resource type does not contain totp data")
            }
            PASSWORD_WITH_DESCRIPTION -> {
                throw IllegalArgumentException("Password with description resource type does not contain totp data")
            }
            STANDALONE_TOTP -> {
                gson.fromJson(String(decryptedSecret), DecryptedSecret.StandaloneTotp::class.java)
            }
        }
    }
}
