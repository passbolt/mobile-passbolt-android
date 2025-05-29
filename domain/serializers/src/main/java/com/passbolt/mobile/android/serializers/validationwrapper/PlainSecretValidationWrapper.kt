package com.passbolt.mobile.android.serializers.validationwrapper

import com.google.gson.Gson
import com.passbolt.mobile.android.supportedresourceTypes.ContentType
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

// plain secret for simple password type is not a valid JSON (plain vs "plain")
// therefore JSON validator reports such secret as invalid
// this wrapper creates a valid JSON for validation for all resource types
class PlainSecretValidationWrapper(
    plainSecret: String?,
    contentType: ContentType,
) : KoinComponent {
    val gson: Gson by inject()
    val validationPlainSecret: String? =
        if (contentType.isSimplePassword()) gson.toJson(plainSecret) else plainSecret
}
