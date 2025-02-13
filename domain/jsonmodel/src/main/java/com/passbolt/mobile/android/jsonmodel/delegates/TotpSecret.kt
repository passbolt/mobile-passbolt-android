package com.passbolt.mobile.android.jsonmodel.delegates

import com.google.gson.annotations.SerializedName

data class TotpSecret(
    val algorithm: String,
    @SerializedName("secret_key")
    val key: String,
    val digits: Int,
    val period: Long
)
