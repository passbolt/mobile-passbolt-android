package com.passbolt.mobile.android.dto.response

import java.util.UUID

/**
 * Minimal projection of `resources.json?contain[secret]=1` used by the offline cache:
 * the resource id, its `modified` stamp and the current user's secret.
 */
data class ResourceSecretsResponseDto(
    val id: UUID,
    val modified: String,
    val secrets: List<ResourceSecretDto>?,
)

data class ResourceSecretDto(
    val id: UUID,
    val data: String,
    val modified: String?,
)
