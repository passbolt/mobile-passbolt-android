package com.passbolt.mobile.android.featureflags.usecase

import com.passbolt.mobile.android.common.usecase.AsyncUseCase
import com.passbolt.mobile.android.core.networking.NetworkResult
import com.passbolt.mobile.android.core.networking.session.NetworkingUseCaseOutput
import com.passbolt.mobile.android.mappers.FeatureFlagsMapper
import com.passbolt.mobile.android.service.settings.SettingsRepository
import com.passbolt.mobile.android.ui.FeatureFlagsModel

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
class GetFeatureFlagsUseCase(
    private val settingsRepository: SettingsRepository,
    private val featureFlagsMapper: FeatureFlagsMapper
) : AsyncUseCase<Unit, GetFeatureFlagsUseCase.Output> {

    override suspend fun execute(input: Unit): Output =
        when (val response = settingsRepository.getSettings()) {
            is NetworkResult.Failure -> Output.Failure(response)
            is NetworkResult.Success -> {
                Output.Success(
                    featureFlagsMapper.map(response.value.body)
                )
            }
        }

    sealed class Output : NetworkingUseCaseOutput {

        override val isUnauthorized: Boolean
            get() = this is Failure<*> && this.response.isUnauthorized

        class Success(
            val featureFlags: FeatureFlagsModel
        ) : Output()

        class Failure<T : Any>(val response: NetworkResult.Failure<T>) : Output()
    }
}
