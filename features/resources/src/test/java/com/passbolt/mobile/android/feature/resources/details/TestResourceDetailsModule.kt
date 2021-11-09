package com.passbolt.mobile.android.feature.resources.details

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.passbolt.mobile.android.core.commonresource.ResourceTypeFactory
import com.passbolt.mobile.android.core.commonresource.usecase.DeleteResourceUseCase
import com.passbolt.mobile.android.core.mvp.coroutinecontext.CoroutineLaunchContext
import com.passbolt.mobile.android.database.DatabaseProvider
import com.passbolt.mobile.android.database.ResourceDatabase
import com.passbolt.mobile.android.database.dao.ResourceTypesDao
import com.passbolt.mobile.android.feature.resources.base.TestCoroutineLaunchContext
import com.passbolt.mobile.android.feature.secrets.usecase.decrypt.SecretInteractor
import com.passbolt.mobile.android.feature.secrets.usecase.decrypt.parser.SecretParser
import com.passbolt.mobile.android.featureflags.usecase.GetFeatureFlagsUseCase
import com.passbolt.mobile.android.mappers.ResourceMenuModelMapper
import com.passbolt.mobile.android.storage.usecase.selectedaccount.GetSelectedAccountUseCase
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

val mockSecretInterActor = mock<SecretInteractor>()
val mockResourceTypesDao = mock<ResourceTypesDao>()
val mockResourceDatabase = mock<ResourceDatabase> {
    on { resourceTypesDao() }.doReturn(mockResourceTypesDao)
}
val mockDatabaseProvider = mock<DatabaseProvider> {
    on { get(any()) }.doReturn(mockResourceDatabase)
}
val mockGetSelectedAccountUseCase = mock<GetSelectedAccountUseCase>() {
    on { execute(Unit) }.doReturn(GetSelectedAccountUseCase.Output("userId"))
}
val mockSecretParser = mock<SecretParser>()
val mockResourceTypeFactory = mock<ResourceTypeFactory>()
val mockGetFeatureFlagsUseCase = mock<GetFeatureFlagsUseCase>()
val resourceMenuModelMapper = ResourceMenuModelMapper()
val mockDeleteResourceUseCase = mock<DeleteResourceUseCase>()

val testResourceDetailsModule = module {
    factory<CoroutineLaunchContext> { TestCoroutineLaunchContext() }
    factory<ResourceDetailsContract.Presenter> {
        ResourceDetailsPresenter(
            secretInteractor = mockSecretInterActor,
            coroutineLaunchContext = get(),
            databaseProvider = mockDatabaseProvider,
            getSelectedAccountUseCase = mockGetSelectedAccountUseCase,
            secretParser = mockSecretParser,
            resourceTypeFactory = mockResourceTypeFactory,
            getFeatureFlagsUseCase = mockGetFeatureFlagsUseCase,
            resourceMenuModelMapper = resourceMenuModelMapper,
            deleteResourceUseCase = mockDeleteResourceUseCase
        )
    }
}
