package com.passbolt.mobile.android.metadata.interactor

import com.passbolt.mobile.android.core.mvp.authentication.AuthenticatedUseCaseOutput
import com.passbolt.mobile.android.core.mvp.authentication.AuthenticationState
import com.passbolt.mobile.android.metadata.usecase.FetchMetadataTypesSettingsUseCase
import com.passbolt.mobile.android.storage.usecase.metadata.SaveMetadataTypesSettingsUseCase
import com.passbolt.mobile.android.ui.MetadataTypesSettingsModel

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

class MetadataTypesSettingsInteractor(
    private val fetchMetadataTypesSettingsUseCase: FetchMetadataTypesSettingsUseCase,
    private val saveMetadataTypesSettingsUseCase: SaveMetadataTypesSettingsUseCase
) {

    suspend fun fetchAndSaveMetadataTypesSettings(): Output {
        return when (val response = fetchMetadataTypesSettingsUseCase.execute(Unit)) {
            is FetchMetadataTypesSettingsUseCase.Output.Success -> {
                saveMetadataTypesSettings(response.metadataTypesSettings)
            }
            is FetchMetadataTypesSettingsUseCase.Output.Failure<*> ->
                Output.Failure(response.authenticationState)
        }
    }

    private suspend fun saveMetadataTypesSettings(metadataTypesSettingsModel: MetadataTypesSettingsModel): Output {
        saveMetadataTypesSettingsUseCase.execute(SaveMetadataTypesSettingsUseCase.Input(metadataTypesSettingsModel))
        return Output.Success
    }

    sealed class Output : AuthenticatedUseCaseOutput {

        data object Success : Output() {
            override val authenticationState: AuthenticationState
                get() = AuthenticationState.Authenticated
        }

        data class Failure(override val authenticationState: AuthenticationState) : Output()
    }
}
