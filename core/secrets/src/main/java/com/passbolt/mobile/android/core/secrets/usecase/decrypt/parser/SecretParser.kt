package com.passbolt.mobile.android.core.secrets.usecase.decrypt.parser

import com.google.gson.Gson
import com.passbolt.mobile.android.core.resourcetypes.ResourceTypeFactory
import com.passbolt.mobile.android.core.resourcetypes.ResourceTypeFactory.ResourceTypeEnum.PASSWORD_WITH_DESCRIPTION
import com.passbolt.mobile.android.core.resourcetypes.ResourceTypeFactory.ResourceTypeEnum.SIMPLE_PASSWORD

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
        }
    }
}
