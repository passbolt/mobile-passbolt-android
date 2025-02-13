package com.passbolt.mobile.android.jsonmodel.delegates

import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonNull
import com.google.gson.reflect.TypeToken
import com.passbolt.mobile.android.jsonmodel.JsonModel
import com.passbolt.mobile.android.jsonmodel.JSON_MODEL_GSON
import com.passbolt.mobile.android.jsonmodel.jsonpathops.JsonPathsOps
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.qualifier.named
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

class RootRelativeJsonPathNullableStringListDelegate(private val jsonPath: String) :
    ReadWriteProperty<JsonModel, List<String>?>, KoinComponent {

    private val gson: Gson by inject(named(JSON_MODEL_GSON))
    private val jsonPathsOps: JsonPathsOps by inject()

    override fun getValue(thisRef: JsonModel, property: KProperty<*>): List<String>? =
        jsonPathsOps.readOrNull(thisRef) { "$.$jsonPath" }?.let {
            val type = TypeToken.getParameterized(List::class.java, String::class.java).type
            gson.fromJson(it, type)
        }

    override fun setValue(thisRef: JsonModel, property: KProperty<*>, value: List<String>?) {
        jsonPathsOps.setOrCreate(thisRef, { jsonPath }, value?.let {
            JsonArray(value.size).apply {
                value.forEach { add(it) }
            }
        } ?: JsonNull.INSTANCE)
    }
}
