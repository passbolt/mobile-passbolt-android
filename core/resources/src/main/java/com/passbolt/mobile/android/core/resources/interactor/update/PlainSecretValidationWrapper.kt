package com.passbolt.mobile.android.core.resources.interactor.update

import com.google.gson.Gson
import com.passbolt.mobile.android.core.resourcetypes.ResourceTypeFactory
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

// plain secret for simple password type is not a valid JSON (plain vs "plain")
// therefore JSON validator reports such secret as invalid
// this wrapper creates a valid JSON for validation for all resource types
class PlainSecretValidationWrapper(plainSecret: String, slug: String) : KoinComponent {
    val gson: Gson by inject()
    val validationPlainSecret: String =
        if (slug == ResourceTypeFactory.SLUG_SIMPLE_PASSWORD) gson.toJson(plainSecret) else plainSecret
}
