package com.passbolt.mobile.android.jsonmodel.delegates

import com.google.gson.JsonNull
import com.google.gson.JsonPrimitive
import com.passbolt.mobile.android.jsonmodel.JsonModel
import com.passbolt.mobile.android.jsonmodel.jsonpathops.JsonPathsOps
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

class RootRelativeJsonPathNullableStringDelegate(
    private val jsonPath: String,
) : ReadWriteProperty<JsonModel, String?>,
    KoinComponent {
    private val jsonPathsOps: JsonPathsOps by inject()

    override fun getValue(
        thisRef: JsonModel,
        property: KProperty<*>,
    ): String? = jsonPathsOps.readOrNull(thisRef) { "$.$jsonPath" }?.asString

    override fun setValue(
        thisRef: JsonModel,
        property: KProperty<*>,
        value: String?,
    ) {
        jsonPathsOps.setOrCreate(thisRef, { jsonPath }, value?.let { JsonPrimitive(it) } ?: JsonNull.INSTANCE)
    }
}
