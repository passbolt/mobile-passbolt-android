package com.passbolt.mobile.android.feature.resourcedetails.details

import com.passbolt.mobile.android.core.fulldatarefresh.base.DataRefreshViewReactivePresenter
import com.passbolt.mobile.android.core.mvp.coroutinecontext.CoroutineLaunchContext
import com.passbolt.mobile.android.database.impl.folders.GetLocalFolderLocationUseCase
import com.passbolt.mobile.android.database.impl.resources.GetLocalResourcePermissionsUseCase
import com.passbolt.mobile.android.database.impl.resources.GetLocalResourceTagsUseCase
import com.passbolt.mobile.android.database.impl.resources.GetLocalResourceUseCase
import com.passbolt.mobile.android.database.impl.resourcetypes.GetResourceTypeWithFieldsByIdUseCase
import com.passbolt.mobile.android.feature.resourcedetails.actions.ResourceActionsInteractor
import com.passbolt.mobile.android.feature.resourcedetails.actions.ResourceAuthenticatedActionsInteractor
import com.passbolt.mobile.android.mappers.ResourceMenuModelMapper
import com.passbolt.mobile.android.permissions.permissions.PermissionsMode
import com.passbolt.mobile.android.permissions.recycler.PermissionsDatasetCreator
import com.passbolt.mobile.android.storage.usecase.featureflags.GetFeatureFlagsUseCase
import com.passbolt.mobile.android.ui.ResourceModel
import com.passbolt.mobile.android.ui.ResourceMoreMenuModel
import com.passbolt.mobile.android.ui.isFavourite
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import org.koin.core.component.KoinScopeComponent
import org.koin.core.component.get
import org.koin.core.component.getOrCreateScope
import org.koin.core.parameter.parametersOf
import org.koin.core.scope.Scope

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
class ResourceDetailsPresenter(
    private val getFeatureFlagsUseCase: GetFeatureFlagsUseCase,
    private val resourceMenuModelMapper: ResourceMenuModelMapper,
    private val getLocalResourceUseCase: GetLocalResourceUseCase,
    private val getLocalResourcePermissionsUseCase: GetLocalResourcePermissionsUseCase,
    private val getLocalResourceTagsUseCase: GetLocalResourceTagsUseCase,
    private val getLocalFolderLocation: GetLocalFolderLocationUseCase,
    private val getResourceTypeWithFieldsByIdUseCase: GetResourceTypeWithFieldsByIdUseCase,
    coroutineLaunchContext: CoroutineLaunchContext
) : DataRefreshViewReactivePresenter<ResourceDetailsContract.View>(coroutineLaunchContext),
    ResourceDetailsContract.Presenter, KoinScopeComponent {

    override var view: ResourceDetailsContract.View? = null
    private val job = SupervisorJob()
    private val coroutineScope = CoroutineScope(job + coroutineLaunchContext.ui)
    override val scope: Scope
        get() = getOrCreateScope().value
    private lateinit var resourceModel: ResourceModel

    private lateinit var resourceActionsInteractor: ResourceActionsInteractor
    private lateinit var resourceAuthenticatedActionsInteractor: ResourceAuthenticatedActionsInteractor
    private var isPasswordVisible = false
    private var permissionsListWidth: Int = -1
    private var permissionItemWidth: Float = -1f
    private lateinit var resourceId: String
    private val missingItemExceptionHandler = CoroutineExceptionHandler { _, throwable ->
        if (throwable is NullPointerException) {
            view?.showContentNotAvailable()
            view?.navigateBack()
        }
    }

    // TODO consider resource types - for now only description can be both encrypted and unencrypted
    // TODO for future draw and set encrypted properties dynamically based on database input

    override fun argsReceived(resourceId: String, permissionsListWidth: Int, permissionItemWidth: Float) {
        this.permissionsListWidth = permissionsListWidth
        this.permissionItemWidth = permissionItemWidth
        this.resourceId = resourceId
        getResourcesAndPermissions(resourceId)
    }

    override fun refreshAction() {
        getResourcesAndPermissions(resourceId)
    }

    override fun refreshFailureAction() {
        view?.showDataRefresError()
    }

    private fun getResourcesAndPermissions(resourceId: String) {
        coroutineScope.launch(missingItemExceptionHandler) {
            val resourceInitializationDeferred = async { // get and display resource
                resourceModel = getLocalResourceUseCase.execute(GetLocalResourceUseCase.Input(resourceId)).resource
                resourceActionsInteractor = get { parametersOf(resourceModel) }
                resourceAuthenticatedActionsInteractor = get {
                    parametersOf(resourceModel, needSessionRefreshFlow, sessionRefreshedFlow)
                }
                view?.apply {
                    displayTitle(resourceModel.name)
                    displayUsername(resourceModel.username.orEmpty())
                    displayInitialsIcon(resourceModel.name, resourceModel.initials)
                    displayUrl(resourceModel.url.orEmpty())
                    if (resourceModel.isFavourite()) {
                        view?.showFavouriteStar()
                    } else {
                        view?.hideFavouriteStar()
                    }
                    showPasswordHidden()
                    showPasswordHiddenIcon()
                    handleDescriptionField(resourceModel)
                    handleFeatureFlags()
                }
            }
            launch { // get and display permissions
                val permissions = getLocalResourcePermissionsUseCase.execute(
                    GetLocalResourcePermissionsUseCase.Input(resourceId)
                ).permissions

                val permissionsDisplayDataset = PermissionsDatasetCreator(permissionsListWidth, permissionItemWidth)
                    .prepareDataset(permissions)

                view?.showPermissions(
                    permissionsDisplayDataset.groupPermissions,
                    permissionsDisplayDataset.userPermissions,
                    permissionsDisplayDataset.counterValue,
                    permissionsDisplayDataset.overlap
                )
            }
            launch { // get and display tags
                getLocalResourceTagsUseCase.execute(GetLocalResourceTagsUseCase.Input(resourceId))
                    .tags
                    .map { it.slug }
                    .let { view?.showTags(it) }
            }
            launch { // get and show location
                resourceInitializationDeferred.await()
                val resourceLocationPathSegments = resourceModel.folderId.let {
                    if (it != null) {
                        getLocalFolderLocation.execute(GetLocalFolderLocationUseCase.Input(it))
                            .parentFolders
                            .map { folder -> folder.name }
                    } else {
                        emptyList()
                    }
                }
                view?.showFolderLocation(resourceLocationPathSegments)
            }
        }
    }

    private suspend fun handleFeatureFlags() {
        val isPasswordEyeIconVisible = getFeatureFlagsUseCase.execute(Unit).featureFlags.isPreviewPasswordAvailable
        if (!isPasswordEyeIconVisible) {
            view?.hidePasswordEyeIcon()
        }
    }

    private fun handleDescriptionField(resourceModel: ResourceModel) {
        coroutineScope.launch {
            val resourceTypeFields = getResourceTypeWithFieldsByIdUseCase.execute(
                GetResourceTypeWithFieldsByIdUseCase.Input(resourceModel.resourceTypeId)
            ).fields
            val isDescriptionSecret = resourceTypeFields
                .find { it.name == "description" }
                ?.isSecret ?: false
            if (isDescriptionSecret) {
                view?.showDescriptionIsEncrypted()
            } else {
                view?.showDescription(resourceModel.description.orEmpty(), useSecretFont = false)
            }
        }
    }

    override fun viewStopped() {
        view?.apply {
            clearPasswordInput()
            showPasswordHidden()
            showPasswordHiddenIcon()
        }
    }

    override fun detach() {
        coroutineScope.coroutineContext.cancelChildren()
        scope.close()
        super<DataRefreshViewReactivePresenter>.detach()
    }

    override fun moreClick() {
        view?.navigateToMore(resourceMenuModelMapper.map(resourceModel))
    }

    override fun backArrowClick() {
        view?.navigateBack()
    }

    override fun secretIconClick() {
        if (!isPasswordVisible) {
            coroutineScope.launch {
                resourceAuthenticatedActionsInteractor.providePassword(
                    decryptionFailure = { view?.showDecryptionFailure() },
                    fetchFailure = { view?.showFetchFailure() }
                ) { _, password ->
                    view?.apply {
                        showPasswordVisibleIcon()
                        showPassword(password)
                    }
                }
            }
            isPasswordVisible = true
        } else {
            view?.apply {
                clearPasswordInput()
                showPasswordHidden()
                showPasswordHiddenIcon()
            }
            isPasswordVisible = false
        }
    }

    override fun seeDescriptionButtonClick() {
        coroutineScope.launch {
            resourceAuthenticatedActionsInteractor.provideDescription(
                decryptionFailure = { view?.showDecryptionFailure() },
                fetchFailure = { view?.showFetchFailure() }
            ) { _, description, isSecret ->
                view?.showDescription(description, useSecretFont = isSecret)
            }
        }
    }

    override fun usernameCopyClick() {
        resourceActionsInteractor.provideUsername { label, username ->
            view?.addToClipboard(label, username, isSecret = false)
        }
    }

    override fun urlCopyClick() {
        resourceActionsInteractor.provideWebsiteUrl { label, url ->
            view?.addToClipboard(label, url, isSecret = false)
        }
    }

    override fun copyPasswordClick() {
        coroutineScope.launch {
            resourceAuthenticatedActionsInteractor.providePassword(
                decryptionFailure = { view?.showDecryptionFailure() },
                fetchFailure = { view?.showFetchFailure() }
            ) { label, password ->
                view?.addToClipboard(label, password, isSecret = true)
            }
        }
    }

    override fun copyDescriptionClick() {
        coroutineScope.launch {
            resourceAuthenticatedActionsInteractor.provideDescription(
                decryptionFailure = { view?.showDecryptionFailure() },
                fetchFailure = { view?.showFetchFailure() }
            ) { label, description, isSecret ->
                view?.addToClipboard(label, description, isSecret = isSecret)
            }
        }
    }

    override fun launchWebsiteClick() {
        resourceActionsInteractor.provideWebsiteUrl { _, url ->
            view?.openWebsite(url)
        }
    }

    override fun deleteClick() {
        view?.showDeleteConfirmationDialog()
    }

    override fun deleteResourceConfirmed() {
        runWhileShowingProgress {
            resourceAuthenticatedActionsInteractor.deleteResource(
                failure = { view?.showGeneralError() }
            ) {
                view?.closeWithDeleteSuccessResult(resourceModel.name)
            }
        }
    }

    override fun resourceEdited(resourceName: String) {
        getResourcesAndPermissions(resourceModel.resourceId)
        view?.apply {
            showResourceEditedSnackbar(resourceName)
            setResourceEditedResult(resourceName)
        }
    }

    private fun runWhileShowingProgress(action: suspend () -> Unit) {
        coroutineScope.launch {
            view?.showProgress()
            action()
            view?.hideProgress()
        }
    }

    override fun resourceShared() {
        view?.showResourceSharedSnackbar()
    }

    override fun editClick() {
        view?.navigateToEditResource(resourceModel)
    }

    override fun shareClick() {
        view?.navigateToResourcePermissions(resourceModel.resourceId, PermissionsMode.EDIT)
    }

    override fun sharedWithClick() {
        view?.navigateToResourcePermissions(resourceModel.resourceId, PermissionsMode.VIEW)
    }

    override fun tagsClick() {
        view?.navigateToResourceTags(resourceModel.resourceId, PermissionsMode.VIEW)
    }

    override fun favouriteClick(option: ResourceMoreMenuModel.FavouriteOption) {
        runWhileShowingProgress {
            resourceAuthenticatedActionsInteractor.toggleFavourite(
                favouriteOption = option,
                failure = {
                    view?.showToggleFavouriteFailure()
                }) {
                view?.setResourceEditedResult(resourceModel.name)
            }
            getResourcesAndPermissions(resourceModel.resourceId)
        }
    }

    override fun locationClick() {
        view?.navigateToResourceLocation(resourceModel.resourceId)
    }
}
