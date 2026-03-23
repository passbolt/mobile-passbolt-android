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

package com.passbolt.mobile.android.createfolder

import androidx.annotation.VisibleForTesting
import androidx.lifecycle.viewModelScope
import com.passbolt.mobile.android.common.validation.StringMaxLength
import com.passbolt.mobile.android.common.validation.StringNotBlank
import com.passbolt.mobile.android.common.validation.validation
import com.passbolt.mobile.android.core.commonfolders.usecase.AddLocalFolderPermissionsUseCase
import com.passbolt.mobile.android.core.commonfolders.usecase.CreateFolderUseCase
import com.passbolt.mobile.android.core.commonfolders.usecase.CreateFolderUseCase.Output.Failure
import com.passbolt.mobile.android.core.commonfolders.usecase.CreateFolderUseCase.Output.Success
import com.passbolt.mobile.android.core.commonfolders.usecase.FolderShareInteractor
import com.passbolt.mobile.android.core.commonfolders.usecase.db.AddLocalFolderUseCase
import com.passbolt.mobile.android.core.commonfolders.usecase.db.GetLocalFolderDetailsUseCase
import com.passbolt.mobile.android.core.commonfolders.usecase.db.GetLocalFolderLocationUseCase
import com.passbolt.mobile.android.core.commonfolders.usecase.db.GetLocalFolderPermissionsUseCase
import com.passbolt.mobile.android.core.commonfolders.usecase.db.GetLocalParentFolderPermissionsToApplyToNewItemUseCase
import com.passbolt.mobile.android.core.commonfolders.usecase.db.ItemIdFolderId
import com.passbolt.mobile.android.core.compose.SideEffectViewModel
import com.passbolt.mobile.android.core.idlingresource.CreateFolderIdlingResource
import com.passbolt.mobile.android.core.mvp.coroutinecontext.CoroutineLaunchContext
import com.passbolt.mobile.android.core.users.usecase.db.GetLocalCurrentUserUseCase
import com.passbolt.mobile.android.createfolder.CreateFolderIntent.FolderNameChanged
import com.passbolt.mobile.android.createfolder.CreateFolderIntent.GoBack
import com.passbolt.mobile.android.createfolder.CreateFolderIntent.Initialize
import com.passbolt.mobile.android.createfolder.CreateFolderIntent.Save
import com.passbolt.mobile.android.createfolder.CreateFolderSideEffect.FolderCreated
import com.passbolt.mobile.android.createfolder.CreateFolderSideEffect.NavigateUp
import com.passbolt.mobile.android.createfolder.CreateFolderSideEffect.ShowErrorSnackbar
import com.passbolt.mobile.android.createfolder.CreateFolderValidationError.MaxLengthExceeded
import com.passbolt.mobile.android.createfolder.SnackbarErrorType.CREATE_FOLDER_ERROR
import com.passbolt.mobile.android.createfolder.SnackbarErrorType.SHARE_FOLDER_ERROR
import com.passbolt.mobile.android.feature.authentication.session.runAuthenticatedOperation
import com.passbolt.mobile.android.mappers.SharePermissionsModelMapper
import com.passbolt.mobile.android.mappers.UsersModelMapper
import com.passbolt.mobile.android.ui.FolderModel
import com.passbolt.mobile.android.ui.PermissionModelUi
import com.passbolt.mobile.android.ui.ResourcePermission
import kotlinx.coroutines.launch

internal class CreateFolderViewModel(
    private val getLocalFolderLocationUseCase: GetLocalFolderLocationUseCase,
    private val getLocalFolderPermissionsUseCase: GetLocalFolderPermissionsUseCase,
    private val createFolderUseCase: CreateFolderUseCase,
    private val getLocalFolderDetailsUseCase: GetLocalFolderDetailsUseCase,
    private val getLocalFolderPermissionsToCopyAsNew: GetLocalParentFolderPermissionsToApplyToNewItemUseCase,
    private val folderShareInteractor: FolderShareInteractor,
    private val addLocalFolderUseCase: AddLocalFolderUseCase,
    private val addLocalFolderPermissionsUseCase: AddLocalFolderPermissionsUseCase,
    private val getLocalCurrentUserUseCase: GetLocalCurrentUserUseCase,
    private val usersModelMapper: UsersModelMapper,
    private val createFolderIdlingResource: CreateFolderIdlingResource,
    private val coroutineLaunchContext: CoroutineLaunchContext,
) : SideEffectViewModel<CreateFolderState, CreateFolderSideEffect>(CreateFolderState()) {
    fun onIntent(intent: CreateFolderIntent) {
        when (intent) {
            GoBack -> emitSideEffect(NavigateUp)
            is Initialize -> initialize(intent.parentFolderId)
            is FolderNameChanged -> folderNameChanged(intent.folderName)
            Save -> save()
        }
    }

    private fun initialize(parentFolderId: String?) {
        updateViewState { copy(parentFolderId = parentFolderId) }

        viewModelScope.launch(coroutineLaunchContext.io) {
            loadFolderData(parentFolderId)
        }
    }

    private suspend fun loadFolderData(parentFolderId: String?) {
        val parentFolders =
            if (parentFolderId != null) {
                getLocalFolderLocationUseCase
                    .execute(GetLocalFolderLocationUseCase.Input(parentFolderId))
                    .parentFolders
                    .map { it.name }
            } else {
                emptyList()
            }

        val permissions =
            if (parentFolderId != null) {
                getLocalFolderPermissionsUseCase
                    .execute(GetLocalFolderPermissionsUseCase.Input(parentFolderId))
                    .permissions
            } else {
                // current user in root always has the owner permission
                listOf(
                    PermissionModelUi.UserPermissionModel(
                        ResourcePermission.OWNER,
                        SharePermissionsModelMapper.TEMPORARY_NEW_PERMISSION_ID,
                        usersModelMapper.mapToUserWithAvatar(getLocalCurrentUserUseCase.execute(Unit).user),
                    ),
                )
            }

        updateViewState {
            copy(
                locationPath = parentFolders,
                permissions = permissions,
            )
        }
    }

    private fun folderNameChanged(folderName: String) {
        updateViewState {
            copy(
                folderName = folderName,
                folderNameValidationErrors = emptyList(),
            )
        }
    }

    private fun save() {
        val folderName = viewState.value.folderName

        updateViewState { copy(folderNameValidationErrors = emptyList()) }
        validation {
            of(folderName) {
                withRules(StringNotBlank, StringMaxLength(FOLDER_NAME_MAX_LENGTH))
                onInvalid {
                    updateViewState {
                        copy(
                            folderNameValidationErrors = folderNameValidationErrors + MaxLengthExceeded(FOLDER_NAME_MAX_LENGTH),
                        )
                    }
                }
            }
            onValid {
                createFolder()
            }
        }
    }

    private fun createFolder() {
        val parentFolderId = viewState.value.parentFolderId
        val folderName = viewState.value.folderName

        updateViewState { copy(isLoading = true) }
        createFolderIdlingResource.setIdle(false)

        viewModelScope.launch(coroutineLaunchContext.io) {
            when (
                val output =
                    runAuthenticatedOperation {
                        createFolderUseCase.execute(CreateFolderUseCase.Input(folderName, parentFolderId))
                    }
            ) {
                is Failure -> {
                    emitSideEffect(ShowErrorSnackbar(CREATE_FOLDER_ERROR, output.result.headerMessage))
                }
                is Success -> {
                    addLocalFolderUseCase.execute(AddLocalFolderUseCase.Input(output.folderWithAttributes.folderModel))
                    addLocalFolderPermissionsUseCase.execute(
                        AddLocalFolderPermissionsUseCase.Input(listOf(output.folderWithAttributes)),
                    )

                    if (parentFolderId == null) {
                        // parent is root folder
                        emitSideEffect(FolderCreated(folderName))
                    } else {
                        val parentFolderDetails =
                            getLocalFolderDetailsUseCase
                                .execute(GetLocalFolderDetailsUseCase.Input(parentFolderId))
                                .folder

                        if (parentFolderDetails.isShared) {
                            applyFolderPermissionsToCreatedFolder(
                                output.folderWithAttributes.folderModel,
                                parentFolderId,
                                folderName,
                            )
                        } else {
                            emitSideEffect(FolderCreated(folderName))
                        }
                    }
                }
            }

            createFolderIdlingResource.setIdle(true)
            updateViewState { copy(isLoading = false) }
        }
    }

    private suspend fun applyFolderPermissionsToCreatedFolder(
        folderModel: FolderModel,
        parentFolderId: String,
        folderName: String,
    ) {
        val newPermissionsToApply =
            getLocalFolderPermissionsToCopyAsNew
                .execute(
                    GetLocalParentFolderPermissionsToApplyToNewItemUseCase.Input(
                        parentFolderId,
                        ItemIdFolderId(folderModel.folderId),
                    ),
                ).permissions

        when (
            val output =
                runAuthenticatedOperation {
                    folderShareInteractor.shareFolder(folderModel.folderId, newPermissionsToApply)
                }
        ) {
            is FolderShareInteractor.Output.ShareFailure -> {
                emitSideEffect(ShowErrorSnackbar(SHARE_FOLDER_ERROR, output.exception.message))
            }
            is FolderShareInteractor.Output.Success -> {
                emitSideEffect(FolderCreated(folderName))
            }
            is FolderShareInteractor.Output.Unauthorized -> {
                // handled by runAuthenticatedOperation
            }
        }
    }

    companion object {
        @VisibleForTesting
        const val FOLDER_NAME_MAX_LENGTH = 256
    }
}
