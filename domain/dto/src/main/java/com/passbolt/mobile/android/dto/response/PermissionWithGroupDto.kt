package com.passbolt.mobile.android.dto.response

import com.google.gson.annotations.SerializedName

data class PermissionWithGroupDto(
    val id: String,
    val type: Int,
    val aco: String?,
    @SerializedName("aco_foreign_key")
    val acoForeignKey: String?,
    val aro: String?,
    @SerializedName("aro_foreign_key")
    val aroForeignKey: String?,
    val created: String?,
    val modified: String?,
    val group: PermissionGroupDto?
)

class PermissionGroupDto(
    val id: String,
    val name: String
)
