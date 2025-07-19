package com.passbolt.mobile.android.dto.request

import com.google.gson.annotations.SerializedName

data class RefreshSessionRequest(
    @SerializedName("refresh_token")
    val refreshToken: String,
    @SerializedName("user_id")
    val userId: String,
)
