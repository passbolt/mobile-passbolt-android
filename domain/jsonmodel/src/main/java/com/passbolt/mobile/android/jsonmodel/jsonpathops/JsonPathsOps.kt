package com.passbolt.mobile.android.jsonmodel.jsonpathops

import com.google.gson.JsonElement
import com.passbolt.mobile.android.jsonmodel.JsonModel

interface JsonPathsOps {

    fun read(model: JsonModel, pathProvider: () -> String): JsonElement

    fun readOrNull(model: JsonModel, pathProvider: () -> String): JsonElement?

    fun setOrCreate(model: JsonModel, pathProvider: () -> String, value: JsonElement)

    fun delete(model: JsonModel, pathProvider: () -> String)
}
