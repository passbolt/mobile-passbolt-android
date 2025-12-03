package com.passbolt.mobile.android.core.resources.usecase

import android.database.SQLException
import com.passbolt.mobile.android.common.usecase.UserIdInput
import com.passbolt.mobile.android.core.accounts.usecase.SelectedAccountUseCase
import com.passbolt.mobile.android.core.mvp.authentication.AuthenticatedUseCaseOutput
import com.passbolt.mobile.android.core.mvp.authentication.AuthenticationState
import com.passbolt.mobile.android.core.resources.usecase.GetResourcesPaginatedUseCase.Output.Failure
import com.passbolt.mobile.android.core.resources.usecase.GetResourcesPaginatedUseCase.Output.Success
import com.passbolt.mobile.android.core.resources.usecase.db.AddLocalResourcePermissionsUseCase
import com.passbolt.mobile.android.core.resources.usecase.db.RemoveLocalResourcePermissionsUseCase
import com.passbolt.mobile.android.core.resources.usecase.db.RemoveLocalResourcesWithUpdateStateUseCase
import com.passbolt.mobile.android.core.resources.usecase.db.RemoveLocalUrisUseCase
import com.passbolt.mobile.android.core.resources.usecase.db.SetLocalResourcesUpdateStateUseCase
import com.passbolt.mobile.android.core.resources.usecase.db.UpsertLocalResourcesUseCase
import com.passbolt.mobile.android.core.tags.usecase.db.AddLocalTagsUseCase
import com.passbolt.mobile.android.core.tags.usecase.db.RemoveLocalTagsUseCase
import com.passbolt.mobile.android.entity.resource.ResourceUpdateState.PENDING
import com.passbolt.mobile.android.ui.ResourceModelWithAttributes
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
class ResourceInteractor(
    private val removeLocalResourcePermissionsUseCase: RemoveLocalResourcePermissionsUseCase,
    private val removeLocalTagsUseCase: RemoveLocalTagsUseCase,
    private val removeLocalUrisUseCase: RemoveLocalUrisUseCase,
    private val getResourcesPaginatedUseCase: GetResourcesPaginatedUseCase,
    private val upsertLocalResourcesUseCase: UpsertLocalResourcesUseCase,
    private val addLocalTagsUseCase: AddLocalTagsUseCase,
    private val addLocalResourcePermissionsUseCase: AddLocalResourcePermissionsUseCase,
    private val setLocalResourcesUpdateStateUseCase: SetLocalResourcesUpdateStateUseCase,
    private val removeLocalResourcesWithUpdateStateUseCase: RemoveLocalResourcesWithUpdateStateUseCase,
) : SelectedAccountUseCase {
    @Suppress("ReturnCount")
    suspend fun fetchAndSaveResources(): Output {
        try {
            // set all resource update states to pending
            setLocalResourcesUpdateStateUseCase.execute(
                SetLocalResourcesUpdateStateUseCase.Input(
                    PENDING,
                ),
            )

            // uris, tags and resource permissions are re-inserted
            removeLocalUrisUseCase.execute(UserIdInput(selectedAccountId))
            removeLocalTagsUseCase.execute(UserIdInput(selectedAccountId))
            removeLocalResourcePermissionsUseCase.execute(UserIdInput(selectedAccountId))

            // get first page
            val firstPageResult =
                getResourcesPaginatedUseCase.execute(
                    GetResourcesPaginatedUseCase.Input(page = FIRST_PAGE, limit = RESOURCES_PAGE_SIZE),
                )

            when (firstPageResult) {
                is Failure<*> -> return Output.Failure(firstPageResult.authenticationState)
                is Success -> {
                    // process first page
                    processResources(firstPageResult.resources)

                    // process remaining pages
                    val totalPages = ceil(firstPageResult.pagination.count.toDouble() / RESOURCES_PAGE_SIZE).toInt()

                    for (page in SECOND_PAGE..totalPages) {
                        when (
                            val pageResult =
                                getResourcesPaginatedUseCase.execute(
                                    GetResourcesPaginatedUseCase.Input(page = page, limit = RESOURCES_PAGE_SIZE),
                                )
                        ) {
                            is Failure<*> -> return Output.Failure(pageResult.authenticationState)
                            is Success -> processResources(pageResult.resources)
                        }
                    }

                    // remove resources that are still pending - they have been deleted on the server
                    removeLocalResourcesWithUpdateStateUseCase.execute(
                        RemoveLocalResourcesWithUpdateStateUseCase.Input(
                            PENDING,
                        ),
                    )
                }
            }

            return Output.Success
        } catch (exception: SQLException) {
            Timber.e(
                exception,
                "There was an error during resources, tags and resource permissions db insert",
            )
            return Output.Failure(AuthenticationState.Authenticated)
        }
    }

    private suspend fun processResources(resources: List<ResourceModelWithAttributes>) {
        upsertLocalResourcesUseCase.execute(
            UpsertLocalResourcesUseCase.Input(resources.map { it.resourceModel }, selectedAccountId),
        )

        addLocalTagsUseCase.execute(
            AddLocalTagsUseCase.Input(resources, selectedAccountId),
        )
        addLocalResourcePermissionsUseCase.execute(
            AddLocalResourcePermissionsUseCase.Input(resources),
        )
    }

    sealed class Output : AuthenticatedUseCaseOutput {
        data object Success : Output() {
            override val authenticationState: AuthenticationState
                get() = AuthenticationState.Authenticated
        }

        class Failure(
            override val authenticationState: AuthenticationState,
        ) : Output()
    }

    private companion object {
        private const val RESOURCES_PAGE_SIZE = 2_000
        private const val FIRST_PAGE = 1
        private const val SECOND_PAGE = 2
    }
}
