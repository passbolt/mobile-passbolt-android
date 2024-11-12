package com.passbolt.mobile.android.serializers.gson

import com.google.gson.JsonElement
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import com.passbolt.mobile.android.dto.request.CreateResourceDto
import com.passbolt.mobile.android.dto.request.CreateV4ResourceDto
import com.passbolt.mobile.android.dto.request.CreateV5ResourceDto
import java.lang.reflect.Type

class CreateResourceModelSerializer : JsonSerializer<CreateResourceDto> {
    override fun serialize(
        src: CreateResourceDto,
        typeOfSrc: Type,
        context: JsonSerializationContext
    ): JsonElement {
        when (src) {
            is CreateV4ResourceDto -> {
                return context.serialize(src, CreateV4ResourceDto::class.java)
            }
            is CreateV5ResourceDto -> {
                return context.serialize(src, CreateV5ResourceDto::class.java)
            }
            else -> {
                throw IllegalArgumentException("Unknown type of CreateResourceModel")
            }
        }
    }
}
