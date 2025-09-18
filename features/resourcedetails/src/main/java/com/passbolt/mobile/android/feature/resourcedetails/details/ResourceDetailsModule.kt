package com.passbolt.mobile.android.feature.resourcedetails.details

import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.adapters.ItemAdapter
import com.passbolt.mobile.android.permissions.recycler.CounterItem
import com.passbolt.mobile.android.permissions.recycler.GroupItem
import com.passbolt.mobile.android.permissions.recycler.UserItem
import org.koin.core.module.Module
import org.koin.core.qualifier.named

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

internal const val GROUP_ITEM_ADAPTER = "GROUP_ITEM_ADAPTER"
internal const val USER_ITEM_ADAPTER = "USER_ITEM_ADAPTER"
internal const val COUNTER_ITEM_ADAPTER = "COUNTER_ITEM_ADAPTER"

fun Module.detailsModule() {
    scope<ResourceDetailsFragment> {
        scoped<ResourceDetailsContract.Presenter> {
            ResourceDetailsPresenter(
                getFeatureFlagsUseCase = get(),
                getLocalResourceUseCase = get(),
                getLocalResourcePermissionsUseCase = get(),
                getLocalResourceTagsUseCase = get(),
                getLocalFolderLocation = get(),
                totpParametersProvider = get(),
                otpModelMapper = get(),
                idToSlugMappingProvider = get(),
                getRbacRulesUseCase = get(),
                resourceDetailActionIdlingResource = get(),
                resourceFormMapper = get(),
                coroutineLaunchContext = get(),
            )
        }
        scoped<ItemAdapter<GroupItem>>(named(GROUP_ITEM_ADAPTER)) {
            ItemAdapter.items()
        }
        scoped<ItemAdapter<UserItem>>(named(USER_ITEM_ADAPTER)) {
            ItemAdapter.items()
        }
        scoped<ItemAdapter<CounterItem>>(named(COUNTER_ITEM_ADAPTER)) {
            ItemAdapter.items()
        }
        scoped {
            FastAdapter.with(
                listOf(
                    get<ItemAdapter<GroupItem>>(named(GROUP_ITEM_ADAPTER)),
                    get<ItemAdapter<UserItem>>(named(USER_ITEM_ADAPTER)),
                    get<ItemAdapter<CounterItem>>(named(COUNTER_ITEM_ADAPTER)),
                ),
            )
        }
    }
}
