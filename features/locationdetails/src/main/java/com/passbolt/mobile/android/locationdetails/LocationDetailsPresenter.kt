package com.passbolt.mobile.android.locationdetails

import com.passbolt.mobile.android.core.commonfolders.usecase.db.GetLocalFolderDetailsUseCase
import com.passbolt.mobile.android.core.commonfolders.usecase.db.GetLocalFolderLocationUseCase
import com.passbolt.mobile.android.core.fulldatarefresh.base.DataRefreshViewReactivePresenter
import com.passbolt.mobile.android.core.mvp.coroutinecontext.CoroutineLaunchContext
import com.passbolt.mobile.android.core.resources.usecase.db.GetLocalResourceUseCase
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
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

class LocationDetailsPresenter(
    private val getLocalFolderDetailsUseCase: GetLocalFolderDetailsUseCase,
    private val getLocalFolderLocation: GetLocalFolderLocationUseCase,
    private val getLocalResourceDetailsUseCase: GetLocalResourceUseCase,
    coroutineLaunchContext: CoroutineLaunchContext
) : DataRefreshViewReactivePresenter<LocationDetailsContract.View>(coroutineLaunchContext),
    LocationDetailsContract.Presenter {

    override var view: LocationDetailsContract.View? = null
    private val job = SupervisorJob()
    private val scope = CoroutineScope(job + coroutineLaunchContext.ui)
    private val missingItemHandler = CoroutineExceptionHandler { _, throwable ->
        if (throwable is NullPointerException) {
            view?.showContentNotAvailable()
            view?.navigateToHome()
        }
    }

    private lateinit var locationItem: LocationItem
    private lateinit var itemId: String

    override fun argsRetrieved(locationItem: LocationItem, id: String) {
        this.locationItem = locationItem
        this.itemId = id
        showItemLocation()
    }

    private fun showItemLocation() {
        when (locationItem) {
            LocationItem.RESOURCE -> resourceLocation(itemId)
            LocationItem.FOLDER -> folderLocation(itemId)
        }
    }

    override fun refreshSuccessAction() {
        showItemLocation()
    }

    override fun refreshFailureAction() {
        view?.showDataRefreshError()
    }

    private fun resourceLocation(resourceId: String) {
        val resourceModelDeferred = scope.async(missingItemHandler) { // get and show resource details
            getLocalResourceDetailsUseCase.execute(GetLocalResourceUseCase.Input(resourceId))
                .resource
                .let {
                    view?.showFolderName(it.name)
                    view?.displayInitialsIcon(it.name, it.initials)
                    it
                }
        }
        scope.launch(missingItemHandler) { // get and show resource location
            val resourceFolderId = resourceModelDeferred.await().folderId
            val locationSegments = if (resourceFolderId != null) {
                getLocalFolderLocation.execute(GetLocalFolderLocationUseCase.Input(resourceFolderId))
                    .parentFolders
            } else {
                emptyList()
            }
            view?.showFolderLocation(locationSegments)
        }
    }

    private fun folderLocation(folderId: String) {
        scope.launch(missingItemHandler) { // get and show folder details
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
        scope.launch(missingItemHandler) { // get and show folder location
            getLocalFolderLocation.execute(GetLocalFolderLocationUseCase.Input(folderId))
                .parentFolders
                .let {
                    view?.showFolderLocation(it)
                }
        }
    }

    override fun detach() {
        scope.coroutineContext.cancelChildren()
        super<DataRefreshViewReactivePresenter>.detach()
    }
}
