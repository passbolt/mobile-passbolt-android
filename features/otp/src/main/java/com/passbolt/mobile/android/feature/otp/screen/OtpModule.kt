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

import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.adapters.ItemAdapter
import com.passbolt.mobile.android.core.mvp.authentication.UnauthenticatedReason
import com.passbolt.mobile.android.core.resources.actions.ResourceAuthenticatedActionsInteractor
import com.passbolt.mobile.android.feature.otp.screen.recycler.OtpItem
import com.passbolt.mobile.android.ui.ResourceModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.koin.core.module.Module
import org.koin.core.module.dsl.scopedOf

fun Module.otpModule() {
    scope<OtpFragment> {
        scopedOf(::OtpSpeedDialFabFactory)
        scoped<OtpContract.Presenter> {
            OtpPresenter(
                getSelectedAccountDataUseCase = get(),
                searchableMatcher = get(),
                getLocalOtpResourcesUseCase = get(),
                otpModelMapper = get(),
                getLocalResourceUseCase = get(),
                totpParametersProvider = get(),
                createResourceMoreMenuModelUseCase = get(),
                updateLocalResourceUseCase = get(),
                coroutineLaunchContext = get(),
                updateStandaloneTotpResourceInteractor = get()
            )
        }

        scoped { FastAdapter.with(get<ItemAdapter<OtpItem>>()) }
        scoped<ItemAdapter<OtpItem>> {
            ItemAdapter.items()
        }
    }
    scope<OtpPresenter> {
        factory { (
                      resource: ResourceModel,
                      needSessionRefreshFlow: MutableStateFlow<UnauthenticatedReason?>,
                      sessionRefreshedFlow: StateFlow<Unit?>
                  ) ->
            ResourceAuthenticatedActionsInteractor(
                needSessionRefreshFlow,
                sessionRefreshedFlow,
                resource,
                resourceTypeFactory = get(),
                secretParser = get(),
                secretInteractor = get(),
                favouritesInteractor = get(),
                deleteResourceUseCase = get()
            )
        }
    }
}
