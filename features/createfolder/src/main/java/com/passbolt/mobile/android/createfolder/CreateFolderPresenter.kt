package com.passbolt.mobile.android.createfolder

import com.passbolt.mobile.android.common.validation.StringMaxLength
import com.passbolt.mobile.android.common.validation.StringNotBlank
import com.passbolt.mobile.android.common.validation.validation
import com.passbolt.mobile.android.core.commonfolders.usecase.AddLocalFolderPermissionsUseCase
import com.passbolt.mobile.android.core.commonfolders.usecase.CreateFolderUseCase
import com.passbolt.mobile.android.core.commonfolders.usecase.FolderShareInteractor
import com.passbolt.mobile.android.core.commonfolders.usecase.db.AddLocalFolderUseCase
import com.passbolt.mobile.android.core.commonfolders.usecase.db.GetLocalFolderDetailsUseCase
import com.passbolt.mobile.android.core.commonfolders.usecase.db.GetLocalFolderLocationUseCase
import com.passbolt.mobile.android.core.commonfolders.usecase.db.GetLocalFolderPermissionsUseCase
import com.passbolt.mobile.android.core.commonfolders.usecase.db.GetLocalParentFolderPermissionsToApplyToNewItemUseCase
import com.passbolt.mobile.android.core.commonfolders.usecase.db.ItemIdFolderId
import com.passbolt.mobile.android.core.idlingresource.CreateFolderIdlingResource
import com.passbolt.mobile.android.core.mvp.authentication.BaseAuthenticatedPresenter
import com.passbolt.mobile.android.core.mvp.coroutinecontext.CoroutineLaunchContext
import com.passbolt.mobile.android.core.users.usecase.db.GetLocalCurrentUserUseCase
import com.passbolt.mobile.android.feature.authentication.session.runAuthenticatedOperation
import com.passbolt.mobile.android.mappers.SharePermissionsModelMapper
import com.passbolt.mobile.android.mappers.UsersModelMapper
import com.passbolt.mobile.android.permissions.recycler.PermissionsDatasetCreator
import com.passbolt.mobile.android.ui.FolderModel
import com.passbolt.mobile.android.ui.PermissionModelUi
import com.passbolt.mobile.android.ui.ResourcePermission
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

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

class CreateFolderPresenter(
    private val getLocalFolderLocation: GetLocalFolderLocationUseCase,
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
    coroutineLaunchContext: CoroutineLaunchContext,
) : BaseAuthenticatedPresenter<CreateFolderContract.View>(coroutineLaunchContext),
    CreateFolderContract.Presenter {
    override var view: CreateFolderContract.View? = null
    private val job = SupervisorJob()
    private val scope = CoroutineScope(job + coroutineLaunchContext.ui)
    private var folderName: String = ""
    private var parentFolderId: String? = null

    override fun argsRetrieved(
        parentFolderId: String?,
        sharedWithWidth: Int,
        sharedWithAvatarWidth: Float,
    ) {
        this.parentFolderId = parentFolderId
        scope.launch {
            getAndShowLocation(parentFolderId)
            getAndShowPermissions(parentFolderId, sharedWithWidth, sharedWithAvatarWidth)
        }
    }

    private suspend fun getAndShowPermissions(
        parentFolderId: String?,
        sharedWithWidth: Int,
        sharedWithAvatarWidth: Float,
    ) {
        val permissions =
            if (parentFolderId != null) {
                getLocalFolderPermissionsUseCase
                    .execute(
                        GetLocalFolderPermissionsUseCase.Input(parentFolderId),
                    ).permissions
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

        val permissionsDisplayDataset =
            PermissionsDatasetCreator(
                sharedWithWidth,
                sharedWithAvatarWidth,
            ).prepareDataset(permissions)

        view?.showPermissions(
            permissionsDisplayDataset.groupPermissions,
            permissionsDisplayDataset.userPermissions,
            permissionsDisplayDataset.counterValue,
            permissionsDisplayDataset.overlap,
        )
    }

    private suspend fun getAndShowLocation(parentFolderId: String?) {
        val parentFolders =
            if (parentFolderId != null) {
                getLocalFolderLocation
                    .execute(GetLocalFolderLocationUseCase.Input(parentFolderId))
                    .parentFolders
                    .map { folder -> folder.name }
            } else {
                emptyList()
            }
        view?.showFolderLocation(parentFolders)
    }

    override fun folderNameChanged(folderName: String) {
        this.folderName = folderName
    }

    override fun saveClick() {
        view?.clearValidationErrors()
        validation {
            of(folderName) {
                withRules(StringNotBlank, StringMaxLength(FOLDER_NAME_MAX_LENGTH)) {
                    onInvalid { view?.showFolderNameLenghtValidationError(FOLDER_NAME_MAX_LENGTH) }
                }
            }
            onValid {
                createFolder()
            }
        }
    }

    private fun createFolder() {
        view?.showProgress()
        createFolderIdlingResource.setIdle(false)
        scope.launch {
            when (
                val output =
                    runAuthenticatedOperation(needSessionRefreshFlow, sessionRefreshedFlow) {
                        createFolderUseCase.execute(CreateFolderUseCase.Input(folderName, parentFolderId))
                    }
            ) {
                is CreateFolderUseCase.Output.Failure -> view?.showCreateFolderError(output.result.headerMessage)
                is CreateFolderUseCase.Output.Success -> {
                    addLocalFolderUseCase.execute(AddLocalFolderUseCase.Input(output.folderWithAttributes.folderModel))
                    addLocalFolderPermissionsUseCase.execute(
                        AddLocalFolderPermissionsUseCase.Input(listOf(output.folderWithAttributes)),
                    )
                    parentFolderId.let {
                        if (it == null) { // parent is root folder
                            view?.setFolderCreatedResultAndClose(folderName)
                        } else {
                            val parentFolderDetails =
                                getLocalFolderDetailsUseCase
                                    .execute(
                                        GetLocalFolderDetailsUseCase.Input(it),
                                    ).folder
                            if (parentFolderDetails.isShared) {
                                applyFolderPermissionsToCreatedFolder(output.folderWithAttributes.folderModel, it)
                            } else {
                                view?.setFolderCreatedResultAndClose(folderName)
                            }
                        }
                    }
                }
            }
            createFolderIdlingResource.setIdle(true)
            view?.hideProgress()
        }
    }

    private suspend fun applyFolderPermissionsToCreatedFolder(
        folderModel: FolderModel,
        parentFolderId: String,
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
                runAuthenticatedOperation(needSessionRefreshFlow, sessionRefreshedFlow) {
                    folderShareInteractor.shareFolder(folderModel.folderId, newPermissionsToApply)
                }
        ) {
            is FolderShareInteractor.Output.ShareFailure -> view?.showShareFailure(output.exception.message.orEmpty())
            is FolderShareInteractor.Output.Success -> {
                view?.setFolderCreatedResultAndClose(folderModel.name)
            }
            is FolderShareInteractor.Output.Unauthorized -> {
                // handled by runAuthenticatedOperation
            }
        }
    }

    private companion object {
        private const val FOLDER_NAME_MAX_LENGTH = 256
    }
}
