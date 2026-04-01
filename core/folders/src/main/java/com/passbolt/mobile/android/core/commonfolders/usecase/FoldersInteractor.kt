package com.passbolt.mobile.android.core.commonfolders.usecase

import android.database.SQLException
import com.passbolt.mobile.android.common.usecase.UserIdInput
import com.passbolt.mobile.android.core.accounts.usecase.SelectedAccountUseCase
import com.passbolt.mobile.android.core.commonfolders.usecase.GetFoldersPaginatedUseCase.Output.Failure
import com.passbolt.mobile.android.core.commonfolders.usecase.GetFoldersPaginatedUseCase.Output.Success
import com.passbolt.mobile.android.core.commonfolders.usecase.db.RemoveLocalFoldersWithUpdateStateUseCase
import com.passbolt.mobile.android.core.commonfolders.usecase.db.SetLocalFoldersUpdateStateUseCase
import com.passbolt.mobile.android.core.commonfolders.usecase.db.UpsertLocalFoldersUseCase
import com.passbolt.mobile.android.core.mvp.authentication.AuthenticatedUseCaseOutput
import com.passbolt.mobile.android.core.mvp.authentication.AuthenticationState
import com.passbolt.mobile.android.entity.folder.FolderUpdateState.PENDING
import com.passbolt.mobile.android.featureflags.usecase.GetFeatureFlagsUseCase
import com.passbolt.mobile.android.ui.FolderModelWithAttributes
import timber.log.Timber
import kotlin.math.ceil

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
    private val getFeatureFlagsUseCase: GetFeatureFlagsUseCase,
    private val getFoldersPaginatedUseCase: GetFoldersPaginatedUseCase,
    private val setLocalFoldersUpdateStateUseCase: SetLocalFoldersUpdateStateUseCase,
    private val upsertLocalFoldersUseCase: UpsertLocalFoldersUseCase,
    private val removeLocalFoldersWithUpdateStateUseCase: RemoveLocalFoldersWithUpdateStateUseCase,
    private val removeLocalFolderPermissionsUseCase: RemoveLocalFolderPermissionsUseCase,
    private val addLocalFolderPermissionsUseCase: AddLocalFolderPermissionsUseCase,
) : SelectedAccountUseCase {
    @Suppress("ReturnCount")
    suspend fun fetchAndSaveFolders(): Output {
        if (!getFeatureFlagsUseCase.execute(Unit).featureFlags.areFoldersAvailable) {
            return Output.Success
        }

        try {
            markAllLocalFoldersAsPending()
            clearLocalFolderPermissions()
            fetchAndProcessAllPages()?.let { failure -> return failure }
            removeStaleLocalFolders()
            return Output.Success
        } catch (exception: SQLException) {
            Timber.e(exception)
            return Output.Failure(AuthenticationState.Authenticated)
        }
    }

    private suspend fun markAllLocalFoldersAsPending() {
        setLocalFoldersUpdateStateUseCase.execute(
            SetLocalFoldersUpdateStateUseCase.Input(PENDING),
        )
    }

    private suspend fun clearLocalFolderPermissions() {
        removeLocalFolderPermissionsUseCase.execute(UserIdInput(selectedAccountId))
    }

    private suspend fun fetchAndProcessAllPages(): Output.Failure? {
        when (val firstPageResult = fetchFoldersPage(FIRST_PAGE)) {
            is Failure<*> -> return Output.Failure(firstPageResult.authenticationState)
            is Success -> {
                processFolders(firstPageResult.folders)

                val totalPages = ceil(firstPageResult.pagination.count.toDouble() / FOLDERS_PAGE_SIZE).toInt()
                for (page in SECOND_PAGE..totalPages) {
                    when (val pageResult = fetchFoldersPage(page)) {
                        is Failure<*> -> return Output.Failure(pageResult.authenticationState)
                        is Success -> processFolders(pageResult.folders)
                    }
                }
            }
        }
        return null
    }

    private suspend fun fetchFoldersPage(page: Int) =
        getFoldersPaginatedUseCase.execute(
            GetFoldersPaginatedUseCase.Input(page = page, limit = FOLDERS_PAGE_SIZE),
        )

    private suspend fun processFolders(foldersWithAttributes: List<FolderModelWithAttributes>) {
        upsertLocalFoldersUseCase.execute(
            UpsertLocalFoldersUseCase.Input(foldersWithAttributes.map { it.folderModel }),
        )
        addLocalFolderPermissionsUseCase.execute(
            AddLocalFolderPermissionsUseCase.Input(foldersWithAttributes),
        )
    }

    private suspend fun removeStaleLocalFolders() {
        removeLocalFoldersWithUpdateStateUseCase.execute(
            RemoveLocalFoldersWithUpdateStateUseCase.Input(PENDING),
        )
    }

    sealed class Output : AuthenticatedUseCaseOutput {
        data object Success : Output() {
            override val authenticationState: AuthenticationState
                get() = AuthenticationState.Authenticated
        }

        data class Failure(
            override val authenticationState: AuthenticationState,
        ) : Output()
    }

    private companion object {
        private const val FOLDERS_PAGE_SIZE = 2_000
        private const val FIRST_PAGE = 1
        private const val SECOND_PAGE = 2
    }
}
