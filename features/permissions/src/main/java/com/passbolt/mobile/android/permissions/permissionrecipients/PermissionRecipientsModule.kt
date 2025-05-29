package com.passbolt.mobile.android.permissions.permissionrecipients

import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.adapters.ItemAdapter
import com.passbolt.mobile.android.permissions.permissionrecipients.recipientsrecycler.ExistingUsersAndGroupsHeaderItem
import com.passbolt.mobile.android.permissions.permissionrecipients.recipientsrecycler.GroupRecipientItem
import com.passbolt.mobile.android.permissions.permissionrecipients.recipientsrecycler.UserRecipientItem
import com.passbolt.mobile.android.permissions.permissions.recycler.PermissionItem
import com.passbolt.mobile.android.permissions.recycler.CounterItem
import com.passbolt.mobile.android.permissions.recycler.GroupItem
import com.passbolt.mobile.android.permissions.recycler.UserItem
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.ScopeDSL

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
internal const val USERS_AND_GROUPS_ADAPTER = "USERS_AND_GROUPS_ADAPTER"

internal const val ALREADY_ADDED_GROUP_ITEM_ADAPTER = "ALREADY_ADDED_GROUP_ITEM_ADAPTER"
internal const val ALREADY_ADDED_USER_ITEM_ADAPTER = "ALREADY_ADDED_USER_ITEM_ADAPTER"
internal const val ALREADY_ADDED_COUNTER_ITEM_ADAPTER = "ALREADY_ADDED_COUNTER_ITEM_ADAPTER"
internal const val EXISTING_USERS_AND_GROUPS_ITEM_ADAPTER = "EXISTING_USERS_AND_GROUPS_ITEM_ADAPTER"
internal const val EXISTING_USERS_AND_GROUPS_HEADER_ITEM_ADAPTER = "EXISTING_USERS_AND_GROUPS_HEADER_ITEM_ADAPTER"
internal const val ALREADY_ADDED_ADAPTER = "ALREADY_ADDED_ADAPTER"

fun Module.permissionRecipientsModule() {
    scope<PermissionRecipientsFragment> {
        scoped<PermissionRecipientsContract.Presenter> {
            PermissionRecipientsPresenter(
                getLocalGroupsUseCase = get(),
                getLocalUsersUseCase = get(),
                permissionsModelMapper = get(),
                searchableMatcher = get(),
                coroutineLaunchContext = get(),
            )
        }
        usersAndGroupsRecyclerDependencies()
        alreadyAddedRecyclerDependencies()
    }
}

private fun ScopeDSL.alreadyAddedRecyclerDependencies() {
    scoped<ItemAdapter<GroupItem>>(named(ALREADY_ADDED_GROUP_ITEM_ADAPTER)) {
        ItemAdapter.items()
    }
    scoped<ItemAdapter<UserItem>>(named(ALREADY_ADDED_USER_ITEM_ADAPTER)) {
        ItemAdapter.items()
    }
    scoped<ItemAdapter<CounterItem>>(named(ALREADY_ADDED_COUNTER_ITEM_ADAPTER)) {
        ItemAdapter.items()
    }
    scoped(named(ALREADY_ADDED_ADAPTER)) {
        FastAdapter.with(
            listOf(
                get<ItemAdapter<GroupItem>>(named(ALREADY_ADDED_GROUP_ITEM_ADAPTER)),
                get<ItemAdapter<UserItem>>(named(ALREADY_ADDED_USER_ITEM_ADAPTER)),
                get<ItemAdapter<CounterItem>>(named(ALREADY_ADDED_COUNTER_ITEM_ADAPTER)),
            ),
        )
    }
}

private fun ScopeDSL.usersAndGroupsRecyclerDependencies() {
    scoped<ItemAdapter<GroupRecipientItem>>(named(GROUP_ITEM_ADAPTER)) {
        ItemAdapter.items()
    }
    scoped<ItemAdapter<UserRecipientItem>>(named(USER_ITEM_ADAPTER)) {
        ItemAdapter.items()
    }
    scoped<ItemAdapter<PermissionItem>>(named(EXISTING_USERS_AND_GROUPS_ITEM_ADAPTER)) {
        ItemAdapter.items()
    }
    scoped<ItemAdapter<ExistingUsersAndGroupsHeaderItem>>(named(EXISTING_USERS_AND_GROUPS_HEADER_ITEM_ADAPTER)) {
        ItemAdapter.items()
    }
    scoped(named(USERS_AND_GROUPS_ADAPTER)) {
        FastAdapter.with(
            listOf(
                get<ItemAdapter<GroupRecipientItem>>(
                    named(GROUP_ITEM_ADAPTER),
                ),
                get<ItemAdapter<UserRecipientItem>>(
                    named(USER_ITEM_ADAPTER),
                ),
                get<ItemAdapter<ExistingUsersAndGroupsHeaderItem>>(
                    named(EXISTING_USERS_AND_GROUPS_HEADER_ITEM_ADAPTER),
                ),
                get<ItemAdapter<PermissionItem>>(
                    named(EXISTING_USERS_AND_GROUPS_ITEM_ADAPTER),
                ),
            ),
        )
    }
}
