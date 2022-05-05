package com.passbolt.mobile.android

import com.passbolt.mobile.android.mappers.AccountModelMapper
import com.passbolt.mobile.android.mappers.CreateResourceMapper
import com.passbolt.mobile.android.mappers.FolderModelMapper
import com.passbolt.mobile.android.mappers.GroupsModelMapper
import com.passbolt.mobile.android.mappers.HomeDisplayViewMapper
import com.passbolt.mobile.android.mappers.ResourceMenuModelMapper
import com.passbolt.mobile.android.mappers.ResourceModelMapper
import com.passbolt.mobile.android.mappers.ResourceTypesModelMapper
import com.passbolt.mobile.android.mappers.SignInMapper
import com.passbolt.mobile.android.mappers.SignOutMapper
import com.passbolt.mobile.android.mappers.SwitchAccountModelMapper
import com.passbolt.mobile.android.mappers.TagsModelMapper
import com.passbolt.mobile.android.mappers.UpdateTransferMapper
import com.passbolt.mobile.android.mappers.UserProfileMapper
import com.passbolt.mobile.android.mappers.UsersModelMapper
import com.passbolt.mobile.android.mappers.comparator.SwitchAccountUiModelComparator
import org.koin.dsl.module

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
val mappersModule = module {
    single { UpdateTransferMapper() }
    single {
        AccountModelMapper(
            selectedAccountUseCase = get()
        )
    }
    single { SignInMapper() }
    single {
        ResourceModelMapper(
            initialsProvider = get()
        )
    }
    single { SignOutMapper() }
    single {
        CreateResourceMapper(
            gson = get()
        )
    }
    single {
        ResourceTypesModelMapper(
            gson = get()
        )
    }
    single { ResourceMenuModelMapper() }
    single { UsersModelMapper() }
    single { SwitchAccountUiModelComparator() }
    single {
        SwitchAccountModelMapper(
            selectedAccountUseCase = get(),
            comparator = get()
        )
    }
    single { UserProfileMapper() }
    single { HomeDisplayViewMapper() }
    single { FolderModelMapper() }
    single { TagsModelMapper() }
    single { GroupsModelMapper() }
}
