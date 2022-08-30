package com.passbolt.mobile.android.permissions.permissions

import com.passbolt.mobile.android.common.validation.validation
import com.passbolt.mobile.android.core.mvp.authentication.BaseAuthenticatedPresenter
import com.passbolt.mobile.android.core.mvp.coroutinecontext.CoroutineLaunchContext
import com.passbolt.mobile.android.data.interactor.HomeDataInteractor
import com.passbolt.mobile.android.data.interactor.ResourceShareInteractor
import com.passbolt.mobile.android.database.impl.folders.GetLocalFolderDetailsUseCase
import com.passbolt.mobile.android.database.impl.folders.GetLocalFolderPermissionsUseCase
import com.passbolt.mobile.android.database.impl.resources.GetLocalResourcePermissionsUseCase
import com.passbolt.mobile.android.database.impl.resources.GetLocalResourceUseCase
import com.passbolt.mobile.android.feature.authentication.session.runAuthenticatedOperation
import com.passbolt.mobile.android.permissions.permissions.validation.HasAtLeastOneOwnerPermission
import com.passbolt.mobile.android.ui.PermissionModelUi
import com.passbolt.mobile.android.ui.ResourcePermission
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
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

class PermissionsPresenter(
    private val getLocalResourcePermissionsUseCase: GetLocalResourcePermissionsUseCase,
    private val getLocalResourceUseCase: GetLocalResourceUseCase,
    private val getLocalFolderPermissionsUseCase: GetLocalFolderPermissionsUseCase,
    private val getLocalFolderUseCase: GetLocalFolderDetailsUseCase,
    private val permissionModelUiComparator: PermissionModelUiComparator,
    private val resourceShareInteractor: ResourceShareInteractor,
    private val homeDataInteractor: HomeDataInteractor,
    coroutineLaunchContext: CoroutineLaunchContext
) : PermissionsContract.Presenter,
    BaseAuthenticatedPresenter<PermissionsContract.View>(coroutineLaunchContext) {

    override var view: PermissionsContract.View? = null
    private val job = SupervisorJob()
    private val scope = CoroutineScope(job + coroutineLaunchContext.ui)

    private lateinit var mode: PermissionsMode
    private lateinit var permissionsItem: PermissionsItem
    private lateinit var id: String
    private lateinit var recipients: MutableList<PermissionModelUi>

    override fun argsReceived(permissionsItem: PermissionsItem, id: String, mode: PermissionsMode) {
        this.mode = mode
        this.id = id
        this.permissionsItem = permissionsItem
        processItemsVisibility(mode)
        getPermissions(permissionsItem, id)
    }

    private fun getPermissions(permissionsItem: PermissionsItem, id: String) {
        scope.launch {
            if (!::recipients.isInitialized) {
                recipients = when (permissionsItem) {
                    PermissionsItem.RESOURCE ->
                        getLocalResourcePermissionsUseCase
                            .execute(GetLocalResourcePermissionsUseCase.Input(id))
                            .permissions
                            .toMutableList()
                    PermissionsItem.FOLDER ->
                        getLocalFolderPermissionsUseCase
                            .execute(GetLocalFolderPermissionsUseCase.Input(id))
                            .permissions
                            .toMutableList()
                }
            }
            view?.showPermissions(
                recipients.apply {
                    sortWith(permissionModelUiComparator)
                }
            )
            if (recipients.isEmpty()) {
                view?.showEmptyState()
            } else {
                view?.hideEmptyState()
            }
        }
    }

    private fun processItemsVisibility(mode: PermissionsMode) {
        when (mode) {
            PermissionsMode.VIEW -> {
                scope.launch {
                    val isOwner = when (permissionsItem) {
                        PermissionsItem.RESOURCE ->
                            getLocalResourceUseCase.execute(GetLocalResourceUseCase.Input(id))
                                .resource.permission == ResourcePermission.OWNER
                        PermissionsItem.FOLDER ->
                            getLocalFolderUseCase.execute(GetLocalFolderDetailsUseCase.Input(id))
                                .folder.permission == ResourcePermission.OWNER
                    }
                    // currently enable edit only on resource
                    if (isOwner && permissionsItem == PermissionsItem.RESOURCE) {
                        view?.showEditButton()
                    }
                }
            }
            PermissionsMode.EDIT -> {
                view?.apply {
                    showAddUserButton()
                    showSaveButton()
                }
            }
        }
    }

    override fun permissionClick(permission: PermissionModelUi) {
        when (permission) {
            is PermissionModelUi.GroupPermissionModel ->
                view?.navigateToGroupPermissionDetails(permission, mode)
            is PermissionModelUi.UserPermissionModel ->
                view?.navigateToUserPermissionDetails(permission, mode)
        }
    }

    override fun actionButtonClick() {
        when (mode) {
            PermissionsMode.VIEW -> view?.navigateToSelfWithMode(id, PermissionsMode.EDIT)
            PermissionsMode.EDIT -> validatePermissions()
        }
    }

    private fun validatePermissions() {
        validation {
            of(recipients) {
                withRules(HasAtLeastOneOwnerPermission) {
                    onInvalid { view?.showOneOwnerSnackbar() }
                }
            }
            onValid {
                shareResource()
            }
        }
    }

    private fun shareResource() {
        view?.showProgress()
        scope.launch {
            when (runAuthenticatedOperation(needSessionRefreshFlow, sessionRefreshedFlow) {
                resourceShareInteractor.simulateAndShareResource(id, recipients)
            }) {
                is ResourceShareInteractor.Output.SecretDecryptFailure -> view?.showSecretDecryptFailure()
                is ResourceShareInteractor.Output.SecretEncryptFailure -> view?.showSecretEncryptFailure()
                is ResourceShareInteractor.Output.SecretFetchFailure -> view?.showSecretFetchFailure()
                is ResourceShareInteractor.Output.ShareFailure -> view?.showShareFailure()
                is ResourceShareInteractor.Output.SimulateShareFailure -> view?.showShareSimulationFailure()
                is ResourceShareInteractor.Output.Success -> shareSuccess()
                is ResourceShareInteractor.Output.Unauthorized -> {
                    /* not interested */
                }
            }
            view?.hideProgress()
        }
    }

    private suspend fun shareSuccess() {
        runAuthenticatedOperation(needSessionRefreshFlow, sessionRefreshedFlow) {
            homeDataInteractor.refreshAllHomeScreenData()
        }
        view?.closeWithShareSuccessResult()
    }

    override fun addPermissionClick() {
        view?.navigateToSelectShareRecipients(
            recipients.filterIsInstance<PermissionModelUi.GroupPermissionModel>(),
            recipients.filterIsInstance<PermissionModelUi.UserPermissionModel>()
        )
    }

    override fun shareRecipientsAdded(shareRecipients: ArrayList<PermissionModelUi>?) {
        shareRecipients?.let { newRecipients ->
            recipients = newRecipients
        }
    }

    override fun userPermissionModified(permission: PermissionModelUi.UserPermissionModel) {
        recipients
            .filterIsInstance<PermissionModelUi.UserPermissionModel>()
            .find { it.user.userId == permission.user.userId }
            ?.let { existingPermission ->
                val existingPermissionIndex = recipients.indexOf(existingPermission)
                recipients[existingPermissionIndex] =
                    PermissionModelUi.UserPermissionModel(
                        permission.permission,
                        permission.permissionId,
                        existingPermission.user.copy()
                    )
            }
    }

    override fun userPermissionDeleted(permission: PermissionModelUi.UserPermissionModel) {
        recipients
            .filterIsInstance<PermissionModelUi.UserPermissionModel>()
            .find { it.user.userId == permission.user.userId }
            ?.let { recipients.remove(it) }
    }

    override fun groupPermissionModified(permission: PermissionModelUi.GroupPermissionModel) {
        recipients
            .filterIsInstance<PermissionModelUi.GroupPermissionModel>()
            .find { it.group.groupId == permission.group.groupId }
            ?.let { existingPermission ->
                val existingPermissionIndex = recipients.indexOf(existingPermission)
                recipients[existingPermissionIndex] =
                    PermissionModelUi.GroupPermissionModel(
                        permission.permission,
                        permission.permissionId,
                        existingPermission.group.copy()
                    )
            }
    }

    override fun groupPermissionDeleted(permission: PermissionModelUi.GroupPermissionModel) {
        recipients
            .filterIsInstance<PermissionModelUi.GroupPermissionModel>()
            .find { it.group.groupId == permission.group.groupId }
            ?.let { recipients.remove(it) }
    }

    override fun detach() {
        scope.coroutineContext.cancelChildren()
        super<BaseAuthenticatedPresenter>.detach()
    }
}
