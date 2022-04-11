package com.passbolt.mobile.android.database

import com.passbolt.mobile.android.database.usecase.AddLocalFoldersUseCase
import com.passbolt.mobile.android.database.usecase.AddLocalGroupsUseCase
import com.passbolt.mobile.android.database.usecase.AddLocalResourceAndGroupsCrossRefUseCase
import com.passbolt.mobile.android.database.usecase.AddLocalResourceTypesUseCase
import com.passbolt.mobile.android.database.usecase.AddLocalResourceUseCase
import com.passbolt.mobile.android.database.usecase.AddLocalResourcesUseCase
import com.passbolt.mobile.android.database.usecase.AddLocalTagsUseCase
import com.passbolt.mobile.android.database.usecase.GetLocalGroupsUseCase
import com.passbolt.mobile.android.database.usecase.GetLocalResourceUseCase
import com.passbolt.mobile.android.database.usecase.GetLocalResourcesAndFoldersUseCase
import com.passbolt.mobile.android.database.usecase.GetLocalResourcesUseCase
import com.passbolt.mobile.android.database.usecase.GetLocalResourcesWithGroupUseCase
import com.passbolt.mobile.android.database.usecase.GetLocalResourcesWithTagUseCase
import com.passbolt.mobile.android.database.usecase.GetLocalSubFolderResourcesFilteredUseCase
import com.passbolt.mobile.android.database.usecase.GetLocalSubFoldersForFolderUseCase
import com.passbolt.mobile.android.database.usecase.GetLocalTagsUseCase
import com.passbolt.mobile.android.database.usecase.GetResourceTypeWithFieldsByIdUseCase
import com.passbolt.mobile.android.database.usecase.GetResourceTypeWithFieldsBySlugUseCase
import com.passbolt.mobile.android.database.usecase.GetResourcesDatabasePassphraseUseCase
import com.passbolt.mobile.android.database.usecase.RemoveLocalFoldersUseCase
import com.passbolt.mobile.android.database.usecase.RemoveLocalGroupsUseCase
import com.passbolt.mobile.android.database.usecase.RemoveLocalResourceAndGroupsCrossRefUseCase
import com.passbolt.mobile.android.database.usecase.RemoveLocalResourcesUseCase
import com.passbolt.mobile.android.database.usecase.RemoveLocalTagsUseCase
import com.passbolt.mobile.android.database.usecase.SaveResourcesDatabasePassphraseUseCase
import com.passbolt.mobile.android.database.usecase.UpdateLocalResourceUseCase
import org.koin.android.ext.koin.androidApplication
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
val databaseModule = module {
    single {
        DatabaseProvider(
            getResourcesDatabasePassphraseUseCase = get(),
            androidApplication()
        )
    }
    single {
        GetResourcesDatabasePassphraseUseCase(
            encryptedSharedPreferencesFactory = get(),
            getSelectedAccountUseCase = get()
        )
    }
    single {
        SaveResourcesDatabasePassphraseUseCase(
            encryptedSharedPreferencesFactory = get(),
            getSelectedAccountUseCase = get()
        )
    }
    single {
        AddLocalResourcesUseCase(
            databaseProvider = get(),
            resourceModelMapper = get()
        )
    }
    single {
        AddLocalResourceUseCase(
            databaseProvider = get(),
            resourceModelMapper = get(),
            getSelectedAccountUseCase = get()
        )
    }
    single {
        GetLocalResourcesUseCase(
            databaseProvider = get(),
            resourceModelMapper = get(),
            getSelectedAccountUseCase = get(),
            resourceDisplayViewMapper = get()
        )
    }
    single {
        GetResourceTypeWithFieldsByIdUseCase(
            databaseProvider = get(),
            getSelectedAccountUseCase = get()
        )
    }
    single {
        GetResourceTypeWithFieldsBySlugUseCase(
            databaseProvider = get(),
            getSelectedAccountUseCase = get()
        )
    }
    single {
        RemoveLocalResourcesUseCase(
            databaseProvider = get()
        )
    }
    single {
        AddLocalResourceTypesUseCase(
            databaseProvider = get(),
            resourceTypesModelMapper = get(),
            getSelectedAccountUseCase = get()
        )
    }
    single {
        UpdateLocalResourceUseCase(
            databaseProvider = get(),
            resourceModelMapper = get(),
            getSelectedAccountUseCase = get()
        )
    }
    single {
        GetLocalResourceUseCase(
            databaseProvider = get(),
            resourceModelMapper = get(),
            getSelectedAccountUseCase = get()
        )
    }
    single {
        AddLocalFoldersUseCase(
            databaseProvider = get(),
            folderModelMapper = get(),
            getSelectedAccountUseCase = get()
        )
    }
    single {
        RemoveLocalFoldersUseCase(
            databaseProvider = get()
        )
    }
    single {
        GetLocalResourcesAndFoldersUseCase(
            databaseProvider = get(),
            folderModelMapper = get(),
            resourceModelMapper = get(),
            getSelectedAccountUseCase = get()
        )
    }
    single {
        GetLocalSubFoldersForFolderUseCase(
            databaseProvider = get(),
            folderModelMapper = get(),
            getSelectedAccountUseCase = get()
        )
    }
    single {
        GetLocalSubFolderResourcesFilteredUseCase(
            databaseProvider = get(),
            resourceModelMapper = get(),
            getSelectedAccountUseCase = get()
        )
    }

    single {
        RemoveLocalTagsUseCase(
            databaseProvider = get()
        )
    }
    single {
        AddLocalTagsUseCase(
            databaseProvider = get(),
            tagModelMapper = get()
        )
    }
    single {
        GetLocalTagsUseCase(
            databaseProvider = get(),
            tagModelMapper = get(),
            getSelectedAccountUseCase = get()
        )
    }
    single {
        GetLocalResourcesWithTagUseCase(
            databaseProvider = get(),
            resourceModelMapper = get(),
            getSelectedAccountUseCase = get()
        )
    }
    single {
        AddLocalGroupsUseCase(
            databaseProvider = get(),
            groupsModelMapper = get(),
            getSelectedAccountUseCase = get()
        )
    }
    single {
        RemoveLocalGroupsUseCase(
            databaseProvider = get()
        )
    }
    single {
        GetLocalGroupsUseCase(
            databaseProvider = get(),
            groupModelMapper = get(),
            getSelectedAccountUseCase = get()
        )
    }
    single {
        AddLocalResourceAndGroupsCrossRefUseCase(
            databaseProvider = get()
        )
    }
    single {
        RemoveLocalResourceAndGroupsCrossRefUseCase(
            databaseProvider = get()
        )
    }
    single {
        GetLocalResourcesWithGroupUseCase(
            databaseProvider = get(),
            resourceModelMapper = get(),
            getSelectedAccountUseCase = get()
        )
    }
}
