package com.passbolt.mobile.android.jsonmodel.delegates

import com.google.gson.Gson
import com.passbolt.mobile.android.jsonmodel.JSON_MODEL_GSON
import com.passbolt.mobile.android.jsonmodel.JsonModel
import com.passbolt.mobile.android.jsonmodel.jsonpathops.JsonPathsOps
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.qualifier.named
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

class RootRelativeJsonPathSecretCustomFieldsDelegate(
    private val jsonPath: String,
) : ReadWriteProperty<JsonModel, SecretCustomFieldsModel?>,
    KoinComponent {
    private val gson: Gson by inject(named(JSON_MODEL_GSON))
    private val jsonPathsOps: JsonPathsOps by inject()

    override fun getValue(
        thisRef: JsonModel,
        property: KProperty<*>,
    ): SecretCustomFieldsModel? =
        jsonPathsOps.readOrNull(thisRef) { "$.$jsonPath" }?.let {
            gson.fromJson(it, SecretCustomFieldsModel::class.java)
        }

    override fun setValue(
        thisRef: JsonModel,
        property: KProperty<*>,
        value: SecretCustomFieldsModel?,
    ) {
        if (value == null) {
            // if totp is null remove the property from json
            jsonPathsOps.delete(thisRef) { "$.$jsonPath" }
        } else {
            jsonPathsOps.setOrCreate(thisRef, { jsonPath }, gson.toJsonTree(value))
        }
    }
}
