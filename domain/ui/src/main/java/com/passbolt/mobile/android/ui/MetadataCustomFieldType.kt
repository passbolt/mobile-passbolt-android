package com.passbolt.mobile.android.ui

import com.google.gson.annotations.SerializedName

enum class MetadataCustomFieldType {
    @SerializedName("text")
    TEXT,

    @SerializedName("password")
    PASSWORD,

    @SerializedName("boolean")
    BOOLEAN,

    @SerializedName("number")
    NUMBER,

    @SerializedName("uri")
    URI,
}
