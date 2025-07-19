package com.passbolt.mobile.android.dto.response

import com.google.gson.annotations.SerializedName
import java.util.UUID

data class PermissionWithGroupDto(
    val id: UUID,
    val type: Int,
    val aco: String?,
    @SerializedName("aco_foreign_key")
    val acoForeignKey: UUID,
    val aro: String?,
    @SerializedName("aro_foreign_key")
    val aroForeignKey: UUID,
    val created: String?,
    val modified: String?,
    val group: PermissionGroupDto?,
)

class PermissionGroupDto(
    val id: UUID,
    val name: String,
)
