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

package com.passbolt.mobile.android.feature.otp.screen

import com.passbolt.mobile.android.common.InitialsProvider
import com.passbolt.mobile.android.common.search.SearchableMatcher
import com.passbolt.mobile.android.commontest.TestCoroutineLaunchContext
import com.passbolt.mobile.android.core.fulldatarefresh.FullDataRefreshExecutor
import com.passbolt.mobile.android.core.mvp.coroutinecontext.CoroutineLaunchContext
import com.passbolt.mobile.android.core.otpcore.TotpParametersProvider
import com.passbolt.mobile.android.core.resources.interactor.update.UpdateStandaloneTotpResourceInteractor
import com.passbolt.mobile.android.core.resources.usecase.db.GetLocalOtpResourcesUseCase
import com.passbolt.mobile.android.core.resources.usecase.db.GetLocalResourceUseCase
import com.passbolt.mobile.android.core.resources.usecase.db.UpdateLocalResourceUseCase
import com.passbolt.mobile.android.mappers.GroupsModelMapper
import com.passbolt.mobile.android.mappers.OtpModelMapper
import com.passbolt.mobile.android.mappers.PermissionsModelMapper
import com.passbolt.mobile.android.mappers.UsersModelMapper
import com.passbolt.mobile.android.otpmoremenu.usecase.CreateOtpMoreMenuModelUseCase
import com.passbolt.mobile.android.storage.usecase.accountdata.GetSelectedAccountDataUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.bind
import org.koin.dsl.module
import org.mockito.kotlin.mock

internal val mockGetLocalResourceUseCase = mock<GetLocalResourceUseCase>()
internal val mockSelectedAccountDataCase = mock<GetSelectedAccountDataUseCase>()
internal val mockGetLocalOtpResourcesUseCase = mock<GetLocalOtpResourcesUseCase>()
internal val mockTotpParametersProvider = mock<TotpParametersProvider>()
internal val mockUpdateLocalResourceUseCase = mock<UpdateLocalResourceUseCase>()
internal val mockUpdateStandaloneTotpResourceInteractor = mock<UpdateStandaloneTotpResourceInteractor>()
internal val mockCreateOtpMoreMenuModelUseCase = mock<CreateOtpMoreMenuModelUseCase>()

@ExperimentalCoroutinesApi
internal val testOtpModule = module {
    factoryOf(::TestCoroutineLaunchContext) bind CoroutineLaunchContext::class
    factoryOf(::SearchableMatcher)
    factoryOf(::OtpModelMapper)
    factoryOf(::UsersModelMapper)
    factoryOf(::GroupsModelMapper)
    factoryOf(::PermissionsModelMapper)
    factoryOf(::InitialsProvider)
    single { mock<FullDataRefreshExecutor>() }
    factory<OtpContract.Presenter> {
        OtpPresenter(
            getLocalResourceUseCase = mockGetLocalResourceUseCase,
            getSelectedAccountDataUseCase = mockSelectedAccountDataCase,
            getLocalOtpResourcesUseCase = mockGetLocalOtpResourcesUseCase,
            totpParametersProvider = mockTotpParametersProvider,
            coroutineLaunchContext = get(),
            searchableMatcher = get(),
            otpModelMapper = get(),
            updateLocalResourceUseCase = mockUpdateLocalResourceUseCase,
            updateStandaloneTotpResourceInteractor = mockUpdateStandaloneTotpResourceInteractor,
            createOtpMoreMenuModelUseCase = mockCreateOtpMoreMenuModelUseCase
        )
    }
}
