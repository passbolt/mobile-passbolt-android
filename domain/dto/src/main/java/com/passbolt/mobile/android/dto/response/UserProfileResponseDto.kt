package com.passbolt.mobile.android.dto.response

import com.google.gson.annotations.SerializedName
import java.util.UUID

data class UserProfileResponseDto(
    val id: UUID,
    @SerializedName("first_name")
    val firstName: String?,
    @SerializedName("last_name")
    val lastName: String?,
    val avatar: AvatarResponseDto?,
)

data class AvatarResponseDto(
    val url: UrlAvatarResponseDto?,
)

data class UrlAvatarResponseDto(
    val medium: String,
)
