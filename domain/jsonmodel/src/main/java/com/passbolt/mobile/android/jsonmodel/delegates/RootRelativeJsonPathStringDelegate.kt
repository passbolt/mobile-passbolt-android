package com.passbolt.mobile.android.jsonmodel.delegates

import com.google.gson.JsonPrimitive
import com.passbolt.mobile.android.jsonmodel.JsonModel
import com.passbolt.mobile.android.jsonmodel.jsonpathops.JsonPathsOps
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

class RootRelativeJsonPathStringDelegate(private val jsonPath: String) :
    ReadWriteProperty<JsonModel, String>, KoinComponent {

    private val jsonPathsOps: JsonPathsOps by inject()

    override fun getValue(thisRef: JsonModel, property: KProperty<*>): String =
        jsonPathsOps.read(thisRef) { "$.$jsonPath" }.asString

    override fun setValue(thisRef: JsonModel, property: KProperty<*>, value: String) {
        jsonPathsOps.setOrCreate(thisRef, { jsonPath }, JsonPrimitive(value))
    }
}
