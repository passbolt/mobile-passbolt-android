package com.passbolt.mobile.android.feature.resources.userspermissiondetails

import com.passbolt.mobile.android.common.search.SearchableMatcher
import com.passbolt.mobile.android.commontest.TestCoroutineLaunchContext
import com.passbolt.mobile.android.core.mvp.coroutinecontext.CoroutineLaunchContext
import com.passbolt.mobile.android.database.impl.groups.GetLocalGroupsUseCase
import com.passbolt.mobile.android.database.impl.users.GetLocalUsersUseCase
import com.passbolt.mobile.android.feature.resources.permissionrecipients.PermissionRecipientsContract
import com.passbolt.mobile.android.feature.resources.permissionrecipients.PermissionRecipientsPresenter
import com.passbolt.mobile.android.mappers.GroupsModelMapper
import com.passbolt.mobile.android.mappers.PermissionsModelMapper
import com.passbolt.mobile.android.mappers.UsersModelMapper
import org.koin.dsl.module
import org.mockito.kotlin.mock

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

internal val mockGetLocalGroupsUseCase = mock<GetLocalGroupsUseCase>()
internal val mockGetLocalUsersUseCase = mock<GetLocalUsersUseCase>()

internal val testPermissionRecipientsModule = module {
    factory<CoroutineLaunchContext> { TestCoroutineLaunchContext() }
    factory { GroupsModelMapper() }
    factory { UsersModelMapper() }
    factory { PermissionsModelMapper(groupsModelMapper = get(), usersModelMapper = get()) }
    factory { SearchableMatcher() }
    factory<PermissionRecipientsContract.Presenter> {
        PermissionRecipientsPresenter(
            coroutineLaunchContext = get(),
            getLocalGroupsUseCase = mockGetLocalGroupsUseCase,
            getLocalUsersUseCase = mockGetLocalUsersUseCase,
            permissionsModelMapper = get(),
            searchableMatcher = get()
        )
    }
}
