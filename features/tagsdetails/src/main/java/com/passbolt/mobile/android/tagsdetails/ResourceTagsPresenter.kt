package com.passbolt.mobile.android.tagsdetails

import com.passbolt.mobile.android.core.fulldatarefresh.base.DataRefreshViewReactivePresenter
import com.passbolt.mobile.android.core.mvp.coroutinecontext.CoroutineLaunchContext
import com.passbolt.mobile.android.database.impl.resources.GetLocalResourceTagsUseCase
import com.passbolt.mobile.android.database.impl.resources.GetLocalResourceUseCase
import com.passbolt.mobile.android.permissions.permissions.PermissionsMode
import com.passbolt.mobile.android.ui.isFavourite
import kotlinx.coroutines.CoroutineExceptionHandler
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
class ResourceTagsPresenter(
    private val getLocalResourceUseCase: GetLocalResourceUseCase,
    val getLocalResourceTags: GetLocalResourceTagsUseCase,
    coroutineLaunchContext: CoroutineLaunchContext
) : DataRefreshViewReactivePresenter<ResourceTagsContract.View>(coroutineLaunchContext),
    ResourceTagsContract.Presenter {

    override var view: ResourceTagsContract.View? = null
    private val job = SupervisorJob()
    private val scope = CoroutineScope(job + coroutineLaunchContext.ui)
    private val missingItemHandler = CoroutineExceptionHandler { _, throwable ->
        if (throwable is NullPointerException) {
            view?.showContentNotAvailable()
            view?.navigateToHome()
        }
    }

    private lateinit var resourceId: String

    override fun argsRetrieved(resourceId: String, mode: PermissionsMode) {
        this.resourceId = resourceId
        showTags()
    }

    private fun showTags() {
        scope.launch(missingItemHandler) {
            launch { // get and display resource
                val resourceModel = getLocalResourceUseCase.execute(GetLocalResourceUseCase.Input(resourceId)).resource
                view?.apply {
                    displayTitle(resourceModel.name)
                    displayInitialsIcon(resourceModel.name, resourceModel.initials)
                    if (resourceModel.isFavourite()) {
                        showFavouriteStar()
                    }
                }
            }
            launch { // get and display tags
                getLocalResourceTags.execute(GetLocalResourceTagsUseCase.Input(resourceId))
                    .tags
                    .let { view?.showTags(it) }
            }
        }
    }

    override fun refreshAction() {
        showTags()
    }

    override fun refreshFailureAction() {
        view?.showDataRefreshError()
    }

    override fun detach() {
        scope.coroutineContext.cancelChildren()
        super<DataRefreshViewReactivePresenter>.detach()
    }
}
