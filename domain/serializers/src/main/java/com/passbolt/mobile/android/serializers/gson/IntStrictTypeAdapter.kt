package com.passbolt.mobile.android.serializers.gson

import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter
import com.passbolt.mobile.android.serializers.gson.exception.InvalidJsonTokenType
import java.io.IOException

class IntStrictTypeAdapter : TypeAdapter<Int?>() {

    @Throws(IOException::class)
    override fun read(input: JsonReader): Int? {
        val peek = input.peek()
        return if (peek == JsonToken.NULL) {
            input.nextNull()
            null
        } else {
            if (peek != JsonToken.NUMBER) {
                throw InvalidJsonTokenType(peek.toString(), Int::class.java.simpleName)
            } else {
                val numberString = input.nextString()
                return if (numberString.all { it.isDigit() }) {
                    numberString.toInt()
                } else {
                    throw InvalidJsonTokenType(peek.toString(), Int::class.java.simpleName)
                }
            }
        }
    }

    @Throws(IOException::class)
    override fun write(out: JsonWriter, value: Int?) {
        out.value(value)
    }
}
