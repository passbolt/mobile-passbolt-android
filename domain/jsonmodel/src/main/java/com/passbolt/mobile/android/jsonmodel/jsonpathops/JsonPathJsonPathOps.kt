package com.passbolt.mobile.android.jsonmodel.jsonpathops

import com.google.gson.JsonElement
import com.jayway.jsonpath.Configuration
import com.jayway.jsonpath.JsonPath
import com.jayway.jsonpath.PathNotFoundException
import com.passbolt.mobile.android.jsonmodel.JsonModel

class JsonPathJsonPathOps(jsonPathConfig: Configuration) : JsonPathsOps {

    private val parseContext = JsonPath.using(jsonPathConfig)

    override fun read(model: JsonModel, pathProvider: () -> String): JsonElement =
        parseContext.parse(model.json).read(pathProvider())

    override fun readOrNull(model: JsonModel, pathProvider: () -> String): JsonElement? =
        try {
            val value = parseContext.parse(model.json).read<JsonElement>(pathProvider())
            if (value.isJsonNull) null else value
        } catch (e: Exception) {
            null
        }

    override fun setOrCreate(model: JsonModel, pathProvider: () -> String, value: JsonElement) {
        model.json = try {
            parseContext.parse(model.json).set(pathProvider(), value).jsonString()
        } catch (exception: PathNotFoundException) {
            parseContext.parse(model.json).put(ROOT_PATH, pathProvider(), value).jsonString()
        }
    }

    override fun delete(model: JsonModel, pathProvider: () -> String) {
        model.json = parseContext.parse(model.json).delete(pathProvider()).jsonString()
    }

    private companion object {
        const val ROOT_PATH = "$"
    }
}
