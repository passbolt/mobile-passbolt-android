package com.passbolt.mobile.android.feature.resources.update

import com.passbolt.mobile.android.core.security.PasswordGenerator
import com.passbolt.mobile.android.feature.resources.update.fieldsgenerator.EditFieldsModelCreator
import com.passbolt.mobile.android.feature.resources.update.fieldsgenerator.FieldNamesMapper
import com.passbolt.mobile.android.feature.resources.update.fieldsgenerator.NewFieldsModelCreator
import com.passbolt.mobile.android.feature.resources.update.fieldsgenerator.ResourceFieldsComparator
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module

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

fun Module.updateResourceModule() {
    scope<UpdateResourceFragment> {
        scoped<UpdateResourceContract.Presenter> {
            UpdateResourcePresenter(
                coroutineLaunchContext = get(),
                passwordGenerator = get(),
                entropyViewMapper = get(),
                createResourceUseCase = get(),
                addLocalResourceUseCase = get(),
                updateLocalResourceUseCase = get(),
                updateResourceUseCase = get(),
                fetchUsersUseCase = get(),
                getResourceTypeWithFieldsBySlugUseCase = get(),
                resourceTypeFactory = get(),
                editFieldsModelCreator = get(),
                newFieldsModelCreator = get(),
                secretInteractor = get(),
                fieldNamesMapper = get()
            )
        }
        scoped {
            ViewProvider()
        }
        scoped {
            PasswordGenerator()
        }
        scoped {
            EntropyViewMapper()
        }
    }
    factory {
        NewFieldsModelCreator(
            getResourceTypeWithFieldsBySlugUseCase = get(),
            resourceFieldsComparator = get()
        )
    }
    factory {
        ResourceFieldsComparator()
    }
    factory {
        EditFieldsModelCreator(
            getResourceTypeWithFieldsByIdUseCase = get(),
            secretParser = get(),
            resourceTypeEnumFactory = get(),
            resourceFieldsComparator = get()
        )
    }
    factory {
        FieldNamesMapper(androidContext())
    }
}
