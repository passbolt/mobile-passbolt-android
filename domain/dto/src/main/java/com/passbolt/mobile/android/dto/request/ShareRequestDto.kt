package com.passbolt.mobile.android.dto.request

import com.google.gson.annotations.SerializedName

/**
 * Passbolt - Open source password manager for teams
 * Copyright (c) 2021 Passbolt SA
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General
 * Public License (AGPL) as published by the Free Software Foundation version 3.
 *
 * The name "Passbolt" is a registered trademark of Passbolt SA, and Passbolt SA hereby declines to grant a trademark
 * license to "Passbolt" pursuant to the GNU Affero General Public License version 3 Section 7(e), without a separate
 * agreement with Passbolt SA.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program. If not,
 * see GNU Affero General Public License v3 (http://www.gnu.org/licenses/agpl-3.0.html).
 *
 * @copyright Copyright (c) Passbolt SA (https://www.passbolt.com)
 * @license https://opensource.org/licenses/AGPL-3.0 AGPL License
 * @link https://www.passbolt.com Passbolt (tm)
 * @since v1.0
 */
data class SimulateShareRequest(
    val permissions: List<SharePermission>
)

data class ShareRequest(
    val permissions: List<SharePermission>,
    val secrets: List<EncryptedSharedSecret>
)

data class EncryptedSharedSecret(
    @SerializedName("resource_id")
    val resourceId: String,
    @SerializedName("user_id")
    val userId: String,
    val data: String
)

sealed class SharePermission(
    val aro: String,
    @SerializedName("aro_foreign_key")
    val aroForeignKey: String,
    val aco: String,
    @SerializedName("aco_foreign_key")
    val acoForeignKey: String,
    val type: Int
) {

    class UpdatedSharePermission(
        val id: String,
        aro: String,
        aroForeignKey: String,
        aco: String,
        acoForeignKey: String,
        type: Int
    ) : SharePermission(
        aro,
        aroForeignKey,
        aco,
        acoForeignKey,
        type
    )

    class DeletedSharePermission(
        val delete: Boolean = true,
        val id: String,
        aro: String,
        aroForeignKey: String,
        aco: String,
        acoForeignKey: String,
        type: Int
    ) : SharePermission(
        aro,
        aroForeignKey,
        aco,
        acoForeignKey,
        type
    )

    class NewSharePermission(
        @SerializedName("is_new")
        val isNew: Boolean = true,
        aro: String,
        aroForeignKey: String,
        aco: String,
        acoForeignKey: String,
        type: Int
    ) : SharePermission(
        aro,
        aroForeignKey,
        aco,
        acoForeignKey,
        type
    )
}
