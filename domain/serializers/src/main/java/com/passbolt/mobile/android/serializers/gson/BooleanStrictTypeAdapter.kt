package com.passbolt.mobile.android.serializers.gson

import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter
import com.passbolt.mobile.android.serializers.gson.exception.InvalidJsonTokenType
import java.io.IOException

class BooleanStrictTypeAdapter : TypeAdapter<Boolean?>() {

    @Throws(IOException::class)
    override fun read(input: JsonReader): Boolean? {
        val peek = input.peek()
        return if (peek == JsonToken.NULL) {
            input.nextNull()
            null
        } else {
            if (peek != JsonToken.BOOLEAN) {
                throw InvalidJsonTokenType(input.nextString(), peek.toString(), Boolean::class.java.simpleName)
            } else {
                input.nextBoolean()
            }
        }
    }

    @Throws(IOException::class)
    override fun write(out: JsonWriter, value: Boolean?) {
        out.value(value)
    }
}
