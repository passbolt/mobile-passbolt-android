package com.passbolt.mobile.android.jsonmodel.delegates

import com.google.gson.annotations.SerializedName

enum class SecretCustomFieldType {
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
