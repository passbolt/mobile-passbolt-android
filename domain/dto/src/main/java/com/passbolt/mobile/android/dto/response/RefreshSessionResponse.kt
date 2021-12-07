package com.passbolt.mobile.android.dto.response

import com.google.gson.annotations.SerializedName

data class RefreshSessionResponse(
    @SerializedName("access_token")
    val accessToken: String
)
