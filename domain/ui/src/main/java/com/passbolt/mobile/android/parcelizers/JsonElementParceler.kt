package com.passbolt.mobile.android.parcelizers

import android.os.Parcel
import com.google.gson.Gson
import com.google.gson.JsonElement
import kotlinx.parcelize.Parceler
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

object JsonElementParceler : Parceler<JsonElement>, KoinComponent {

    private val gson: Gson by inject()

    override fun create(parcel: Parcel): JsonElement =
        gson.fromJson(parcel.readString(), JsonElement::class.java)

    override fun JsonElement.write(parcel: Parcel, flags: Int) {
        parcel.writeString(toString())
    }
}
