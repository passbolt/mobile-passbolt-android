package com.passbolt.mobile.android.permissions.permissions

import com.passbolt.mobile.android.common.validation.validation
import com.passbolt.mobile.android.core.commonfolders.usecase.db.GetLocalFolderDetailsUseCase
import com.passbolt.mobile.android.core.commonfolders.usecase.db.GetLocalFolderPermissionsUseCase
import com.passbolt.mobile.android.core.fulldatarefresh.HomeDataInteractor
import com.passbolt.mobile.android.core.fulldatarefresh.base.DataRefreshViewReactivePresenter
import com.passbolt.mobile.android.core.mvp.coroutinecontext.CoroutineLaunchContext
import com.passbolt.mobile.android.core.resources.actions.ResourceUpdateActionResult
import com.passbolt.mobile.android.core.resources.actions.ResourceUpdateActionsInteractor
import com.passbolt.mobile.android.core.resources.usecase.ResourceShareInteractor
import com.passbolt.mobile.android.core.resources.usecase.ResourceShareInteractor.Output
import com.passbolt.mobile.android.core.resources.usecase.db.GetLocalResourcePermissionsUseCase
import com.passbolt.mobile.android.core.resources.usecase.db.GetLocalResourceUseCase
import com.passbolt.mobile.android.core.resourcetypes.usecase.db.ResourceTypeIdToSlugMappingProvider
import com.passbolt.mobile.android.feature.authentication.session.runAuthenticatedOperation
import com.passbolt.mobile.android.metadata.usecase.db.GetLocalMetadataKeysUseCase
import com.passbolt.mobile.android.metadata.usecase.db.GetLocalMetadataKeysUseCase.MetadataKeyPurpose.ENCRYPT
import com.passbolt.mobile.android.permissions.permissions.validation.HasAtLeastOneOwnerPermission
import com.passbolt.mobile.android.supportedresourceTypes.ContentType
import com.passbolt.mobile.android.ui.MetadataKeyTypeModel
import com.passbolt.mobile.android.ui.PermissionModelUi
import com.passbolt.mobile.android.ui.ResourceModel
import com.passbolt.mobile.android.ui.ResourcePermission
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.flow.single
import kotlinx.coroutines.launch
import org.koin.core.component.get
import org.koin.core.parameter.parametersOf
import timber.log.Timber
import java.util.UUID

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
    private val resourceTypeIdToSlugMappingProvider: ResourceTypeIdToSlugMappingProvider,
    private val getLocalMetadataKeysUseCase: GetLocalMetadataKeysUseCase,
    coroutineLaunchContext: CoroutineLaunchContext
) : PermissionsContract.Presenter,
    DataRefreshViewReactivePresenter<PermissionsContract.View>(coroutineLaunchContext) {

    override var view: PermissionsContract.View? = null
    private val job = SupervisorJob()
    private val scope = CoroutineScope(job + coroutineLaunchContext.ui)
    private val missingItemHandler = CoroutineExceptionHandler { _, throwable ->
        if (throwable is NullPointerException) {
            view?.showContentNotAvailable()
            view?.navigateToHome()
        }
    }

    private lateinit var mode: PermissionsMode
    private lateinit var permissionsItem: PermissionsItem
    private lateinit var id: String
    private lateinit var recipients: MutableList<PermissionModelUi>

    override fun argsReceived(permissionsItem: PermissionsItem, id: String, mode: PermissionsMode) {
        this.mode = mode
        this.id = id
        this.permissionsItem = permissionsItem
    }

    override fun refreshSuccessAction() {
        refreshPermissionsList()
    }

    override fun refreshPermissionsList() {
        processItemsVisibility(mode)
        getPermissions(permissionsItem, id)
    }

    override fun refreshFailureAction() {
        view?.showDataRefreshError()
    }

    private fun getPermissions(permissionsItem: PermissionsItem, id: String) {
        scope.launch(missingItemHandler) {
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
                scope.launch(missingItemHandler) {
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
                updateIfNeededAndShareResource()
            }
        }
    }

    private fun shouldReEncryptUserResourceMetadataWithSharedKey(
        contentType: ContentType,
        resource: ResourceModel
    ) =
        contentType.isV5() && resource.metadataKeyType == MetadataKeyTypeModel.PERSONAL && recipients.size > 1

    private fun updateIfNeededAndShareResource() {
        view?.showProgress()
        scope.launch {
            val resource = getLocalResourceUseCase.execute(GetLocalResourceUseCase.Input(id)).resource
            val contentType = ContentType.fromSlug(
                resourceTypeIdToSlugMappingProvider.provideMappingForSelectedAccount()[
                    UUID.fromString(resource.resourceTypeId)
                ]!!
            )

            if (shouldReEncryptUserResourceMetadataWithSharedKey(contentType, resource)) {
                reEncryptUserResourceMetadataWithSharedKey(resource)
            }
            shareResource()

            view?.hideProgress()
        }
    }

    private suspend fun reEncryptUserResourceMetadataWithSharedKey(resource: ResourceModel) {
        val metadataKeyId = getLocalMetadataKeysUseCase.execute(GetLocalMetadataKeysUseCase.Input(ENCRYPT))
            .firstOrNull()
            ?.id

        val resourceUpdateActionsInteractor = get<ResourceUpdateActionsInteractor> {
            parametersOf(resource, needSessionRefreshFlow, sessionRefreshedFlow)
        }
        if (metadataKeyId == null) {
            Timber.e(
                "Resource metadata should be re-encrypted with shared key " +
                        "but no valid ENCRYPT metadata shared key found "
            )
            view?.showReEncyptMetadataFailure()
            return
        }
        when (resourceUpdateActionsInteractor.reEncryptResourceMetadata(
            metadataKeyId = metadataKeyId.toString(),
            metadataKeyType = MetadataKeyTypeModel.SHARED
        ).single()) {
            is ResourceUpdateActionResult.Success -> shareResource()
            else -> view?.showReEncyptMetadataFailure()
        }
    }

    private suspend fun shareResource() {
        when (runAuthenticatedOperation(needSessionRefreshFlow, sessionRefreshedFlow) {
            resourceShareInteractor.simulateAndShareResource(id, recipients)
        }) {
            is Output.SecretDecryptFailure -> view?.showSecretDecryptFailure()
            is Output.SecretEncryptFailure -> view?.showSecretEncryptFailure()
            is Output.SecretFetchFailure -> view?.showSecretFetchFailure()
            is Output.ShareFailure -> view?.showShareFailure()
            is Output.SimulateShareFailure -> view?.showShareSimulationFailure()
            is Output.Success -> shareSuccess()
            is Output.Unauthorized -> {
                /* not interested */
            }
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
        super<DataRefreshViewReactivePresenter>.detach()
    }
}
