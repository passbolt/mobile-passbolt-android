package com.passbolt.mobile.android.serializers.gson

import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter
import com.passbolt.mobile.android.serializers.gson.exception.InvalidJsonTokenType
import java.io.IOException

class LongStrictTypeAdapter : TypeAdapter<Long?>() {

    @Throws(IOException::class)
    override fun read(input: JsonReader): Long? {
        val peek = input.peek()
        return if (peek == JsonToken.NULL) {
            input.nextNull()
            null
        } else {
            if (peek != JsonToken.NUMBER) {
                throw InvalidJsonTokenType(peek.toString(), Long::class.java.simpleName)
            } else {
                val numberString = input.nextString()
                return if (numberString.all { it.isDigit() }) {
                    numberString.toLong()
                } else {
                    throw InvalidJsonTokenType(peek.toString(), Long::class.java.simpleName)
                }
            }
        }
    }

    @Throws(IOException::class)
    override fun write(out: JsonWriter, value: Long?) {
        out.value(value)
    }
}
