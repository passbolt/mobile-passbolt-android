package com.passbolt.mobile.android.folderdetails

import com.passbolt.mobile.android.core.commonfolders.usecase.db.GetLocalFolderDetailsUseCase
import com.passbolt.mobile.android.core.commonfolders.usecase.db.GetLocalFolderLocationUseCase
import com.passbolt.mobile.android.core.commonfolders.usecase.db.GetLocalFolderPermissionsUseCase
import com.passbolt.mobile.android.core.fulldatarefresh.base.DataRefreshViewReactivePresenter
import com.passbolt.mobile.android.core.mvp.coroutinecontext.CoroutineLaunchContext
import com.passbolt.mobile.android.core.rbac.usecase.GetRbacRulesUseCase
import com.passbolt.mobile.android.permissions.permissions.PermissionsMode
import com.passbolt.mobile.android.permissions.recycler.PermissionsDatasetCreator
import com.passbolt.mobile.android.ui.RbacRuleModel.ALLOW
import kotlinx.coroutines.CoroutineExceptionHandler
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

class FolderDetailsPresenter(
    private val getLocalFolderDetailsUseCase: GetLocalFolderDetailsUseCase,
    private val getLocalFolderLocation: GetLocalFolderLocationUseCase,
    private val getLocalFolderPermissionsUseCase: GetLocalFolderPermissionsUseCase,
    private val getRbacRulesUseCase: GetRbacRulesUseCase,
    coroutineLaunchContext: CoroutineLaunchContext
) : DataRefreshViewReactivePresenter<FolderDetailsContract.View>(coroutineLaunchContext),
    FolderDetailsContract.Presenter {

    override var view: FolderDetailsContract.View? = null
    private val job = SupervisorJob()
    private val scope = CoroutineScope(job + coroutineLaunchContext.ui)
    private var permissionsListWidth: Int = -1
    private var permissionItemWidth: Float = -1f
    private lateinit var folderId: String

    private val missingItemHandler = CoroutineExceptionHandler { _, throwable ->
        if (throwable is NullPointerException) {
            view?.showContentNotAvailable()
            view?.navigateToHome()
        }
    }

    override fun argsRetrieved(folderId: String, permissionsListWidth: Int, permissionItemWidth: Float) {
        this.permissionsListWidth = permissionsListWidth
        this.permissionItemWidth = permissionItemWidth
        this.folderId = folderId
        showFolderDetails()
    }

    override fun refreshAction() {
        showFolderDetails()
    }

    override fun refreshFailureAction() {
        view?.showDataRefreshError()
    }

    private fun showFolderDetails() {
        scope.launch(missingItemHandler) { // get and show details
            getLocalFolderDetailsUseCase.execute(GetLocalFolderDetailsUseCase.Input(folderId))
                .folder
                .let {
                    view?.showFolderName(it.name)
                    if (it.isShared) {
                        view?.showFolderSharedIcon()
                    } else {
                        view?.showFolderIcon()
                    }
                }
        }
        scope.launch(missingItemHandler) { // get and show location
            getLocalFolderLocation.execute(GetLocalFolderLocationUseCase.Input(folderId))
                .parentFolders
                .let { view?.showFolderLocation(it.map { folder -> folder.name }) }
        }
        scope.launch(missingItemHandler) { // get and display permissions
            if (getRbacRulesUseCase.execute(Unit).rbacModel.shareViewRule == ALLOW) {
                val permissions = getLocalFolderPermissionsUseCase.execute(
                    GetLocalFolderPermissionsUseCase.Input(folderId)
                ).permissions

                val permissionsDisplayDataset = PermissionsDatasetCreator(
                    permissionsListWidth,
                    permissionItemWidth
                )
                    .prepareDataset(permissions)

                view?.showPermissions(
                    permissionsDisplayDataset.groupPermissions,
                    permissionsDisplayDataset.userPermissions,
                    permissionsDisplayDataset.counterValue,
                    permissionsDisplayDataset.overlap
                )
            }
        }
    }

    override fun sharedWithClick() {
        view?.navigateToFolderPermissions(folderId, PermissionsMode.VIEW)
    }

    override fun locationClick() {
        view?.navigateToFolderLocation(folderId)
    }
}
