package com.passbolt.mobile.android.feature.secrets.usecase.decrypt.parser

import com.google.gson.Gson
import com.passbolt.mobile.android.core.commonresource.ResourceTypeFactory

class SecretParser(
    private val gson: Gson
) {

    fun extractPassword(resourceTypeEnum: ResourceTypeFactory.ResourceTypeEnum, decryptedSecret: ByteArray): String {
        return when (resourceTypeEnum) {
            ResourceTypeFactory.ResourceTypeEnum.SIMPLE_PASSWORD -> {
                String(decryptedSecret)
            }
            ResourceTypeFactory.ResourceTypeEnum.PASSWORD_WITH_DESCRIPTION -> {
                gson.fromJson(String(decryptedSecret), DecryptedSecret.PasswordWithDescription::class.java).password
            }
        }
    }

    fun extractDescription(resourceTypeEnum: ResourceTypeFactory.ResourceTypeEnum, decryptedSecret: ByteArray): String {
        return when (resourceTypeEnum) {
            ResourceTypeFactory.ResourceTypeEnum.SIMPLE_PASSWORD -> {
                throw IllegalArgumentException("Simple password resource type does not contain description secret")
            }
            ResourceTypeFactory.ResourceTypeEnum.PASSWORD_WITH_DESCRIPTION -> {
                gson.fromJson(String(decryptedSecret), DecryptedSecret.PasswordWithDescription::class.java).description
            }
        }
    }
}
