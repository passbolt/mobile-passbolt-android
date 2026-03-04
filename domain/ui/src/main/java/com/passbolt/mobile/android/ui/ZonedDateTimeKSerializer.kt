package com.passbolt.mobile.android.ui

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class ZonedDateTimeKSerializer : KSerializer<ZonedDateTime> {
    private val formatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME

    override val descriptor = PrimitiveSerialDescriptor(ZonedDateTime::class.java.name, PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): ZonedDateTime = ZonedDateTime.parse(decoder.decodeString(), formatter)

    override fun serialize(
        encoder: Encoder,
        value: ZonedDateTime,
    ) {
        encoder.encodeString(formatter.format(value))
    }
}
