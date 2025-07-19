package com.passbolt.mobile.android.permissions.recycler

import android.view.LayoutInflater
import android.view.ViewGroup
import coil3.load
import coil3.request.error
import coil3.request.placeholder
import coil3.request.transformations
import coil3.transform.CircleCropTransformation
import com.mikepenz.fastadapter.binding.AbstractBindingItem
import com.passbolt.mobile.android.core.coil.transformation.AlphaTransformation
import com.passbolt.mobile.android.feature.permissions.R
import com.passbolt.mobile.android.feature.permissions.databinding.ItemUserBinding
import com.passbolt.mobile.android.ui.PermissionModelUi
import com.passbolt.mobile.android.core.ui.R as CoreUiR

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
class UserItem(
    val model: PermissionModelUi.UserPermissionModel,
) : AbstractBindingItem<ItemUserBinding>() {
    override val type: Int
        get() = R.id.userItem

    override fun bindView(
        binding: ItemUserBinding,
        payloads: List<Any>,
    ) {
        with(binding) {
            root.apply {
                load(model.user.avatarUrl) {
                    error(CoreUiR.drawable.ic_user_avatar)
                    transformations(
                        CircleCropTransformation(),
                        AlphaTransformation(shouldLowerOpacity = model.user.isDisabled),
                    )
                    placeholder(CoreUiR.drawable.ic_user_avatar)
                }
            }
        }
    }

    override fun createBinding(
        inflater: LayoutInflater,
        parent: ViewGroup?,
    ): ItemUserBinding = ItemUserBinding.inflate(inflater, parent, false)
}
