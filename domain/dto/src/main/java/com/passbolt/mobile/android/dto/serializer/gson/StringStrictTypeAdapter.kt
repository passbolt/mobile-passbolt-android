package com.passbolt.mobile.android.dto.serializer.gson

import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter
import com.passbolt.mobile.android.dto.serializer.gson.exception.InvalidJsonTokenType
import java.io.IOException

class StringStrictTypeAdapter : TypeAdapter<String?>() {

    @Throws(IOException::class)
    override fun read(input: JsonReader): String? {
        val peek = input.peek()
        return if (peek == JsonToken.NULL) {
            input.nextNull()
            null
        } else {
            if (peek != JsonToken.STRING) {
                throw InvalidJsonTokenType(input.nextString(), peek.toString(), String::class.java.simpleName)
            } else {
                input.nextString()
            }
        }
    }

    @Throws(IOException::class)
    override fun write(out: JsonWriter, value: String?) {
        out.value(value)
    }
}
