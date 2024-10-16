package com.passbolt.mobile.android.core.commonfolders.usecase

import android.database.SQLException
import com.passbolt.mobile.android.core.mvp.authentication.AuthenticatedUseCaseOutput
import com.passbolt.mobile.android.core.mvp.authentication.AuthenticationState
import com.passbolt.mobile.android.featureflags.usecase.GetFeatureFlagsUseCase
import timber.log.Timber

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
class FoldersInteractor(
    private val fetchUserFoldersUseCase: FetchUserFoldersUseCase,
    private val rebuildLocalFoldersUseCase: RebuildFoldersTablesUseCase,
    private val getFeatureFlagsUseCase: GetFeatureFlagsUseCase,
    private val rebuildLocalFolderPermissionsUseCase: RebuildFolderPermissionsTablesUseCase
) {

    suspend fun fetchAndSaveFolders(): Output {
        return if (getFeatureFlagsUseCase.execute(Unit).featureFlags.areFoldersAvailable) {
            when (val fetched = fetchUserFoldersUseCase.execute(Unit)) {
                is FetchUserFoldersUseCase.Output.Failure -> Output.Failure(fetched.authenticationState)
                is FetchUserFoldersUseCase.Output.Success -> {
                    try {
                        rebuildLocalFoldersUseCase.execute(
                            RebuildFoldersTablesUseCase.Input(
                                fetched.foldersWithAttributes.map { it.folderModel })
                        )
                        rebuildLocalFolderPermissionsUseCase.execute(
                            RebuildFolderPermissionsTablesUseCase.Input(fetched.foldersWithAttributes)
                        )
                        Output.Success
                    } catch (exception: SQLException) {
                        Timber.e(
                            exception, "There was an error during folders and folders " +
                                    "permissions db insert"
                        )
                        Output.Failure(fetched.authenticationState)
                    }
                }
            }
        } else {
            Output.Success
        }
    }

    sealed class Output : AuthenticatedUseCaseOutput {

        data object Success : Output() {
            override val authenticationState: AuthenticationState
                get() = AuthenticationState.Authenticated
        }

        data class Failure(override val authenticationState: AuthenticationState) : Output()
    }
}
