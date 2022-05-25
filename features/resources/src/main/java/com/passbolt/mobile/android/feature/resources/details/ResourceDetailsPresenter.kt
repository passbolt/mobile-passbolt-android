package com.passbolt.mobile.android.feature.resources.details

import com.passbolt.mobile.android.core.commonresource.ResourceTypeFactory
import com.passbolt.mobile.android.core.commonresource.usecase.DeleteResourceUseCase
import com.passbolt.mobile.android.core.mvp.authentication.BaseAuthenticatedPresenter
import com.passbolt.mobile.android.core.mvp.coroutinecontext.CoroutineLaunchContext
import com.passbolt.mobile.android.database.DatabaseProvider
import com.passbolt.mobile.android.database.impl.resources.GetLocalResourcePermissionsUseCase
import com.passbolt.mobile.android.database.impl.resources.GetLocalResourceUseCase
import com.passbolt.mobile.android.feature.authentication.session.runAuthenticatedOperation
import com.passbolt.mobile.android.feature.resources.permissionavatarlist.PermissionsDatasetCreator
import com.passbolt.mobile.android.feature.resources.permissions.ResourcePermissionsMode
import com.passbolt.mobile.android.feature.secrets.usecase.decrypt.SecretInteractor
import com.passbolt.mobile.android.feature.secrets.usecase.decrypt.parser.SecretParser
import com.passbolt.mobile.android.featureflags.usecase.GetFeatureFlagsUseCase
import com.passbolt.mobile.android.mappers.ResourceMenuModelMapper
import com.passbolt.mobile.android.storage.usecase.selectedaccount.GetSelectedAccountUseCase
import com.passbolt.mobile.android.ui.ResourceModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import timber.log.Timber

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
    private val secretInteractor: SecretInteractor,
    private val databaseProvider: DatabaseProvider,
    private val getSelectedAccountUseCase: GetSelectedAccountUseCase,
    private val resourceTypeFactory: ResourceTypeFactory,
    private val secretParser: SecretParser,
    private val getFeatureFlagsUseCase: GetFeatureFlagsUseCase,
    private val resourceMenuModelMapper: ResourceMenuModelMapper,
    private val deleteResourceUseCase: DeleteResourceUseCase,
    private val getLocalResourceUseCase: GetLocalResourceUseCase,
    private val getLocalResourcePermissionsUseCase: GetLocalResourcePermissionsUseCase,
    coroutineLaunchContext: CoroutineLaunchContext
) : BaseAuthenticatedPresenter<ResourceDetailsContract.View>(coroutineLaunchContext),
    ResourceDetailsContract.Presenter {

    override var view: ResourceDetailsContract.View? = null
    private val job = SupervisorJob()
    private val scope = CoroutineScope(job + coroutineLaunchContext.ui)

    private lateinit var resourceModel: ResourceModel
    private var isPasswordVisible = false
    private var permissionsListWidth: Int = -1
    private var permissionItemWidth: Float = -1f

    // TODO consider resource types - for now only description can be both encrypted and unencrypted
    // TODO for future draw and set encrypted properties dynamically based on database input

    override fun argsReceived(resourceId: String, permissionsListWidth: Int, permissionItemWidth: Float) {
        this.permissionsListWidth = permissionsListWidth
        this.permissionItemWidth = permissionItemWidth
        getResourcesAndPermissions(resourceId)
    }

    private fun getResourcesAndPermissions(resourceId: String) {
        scope.launch {
            launch { // get and display resource
                resourceModel = getLocalResourceUseCase.execute(GetLocalResourceUseCase.Input(resourceId)).resource
                view?.apply {
                    displayTitle(resourceModel.name)
                    displayUsername(resourceModel.username.orEmpty())
                    displayInitialsIcon(resourceModel.name, resourceModel.initials)
                    displayUrl(resourceModel.url.orEmpty())
                    showPasswordHidden()
                    showPasswordHiddenIcon()
                    handleDescriptionField(resourceModel)
                    handleFeatureFlags()
                }
            }
            launch { // get and display permissions
                val permissions = getLocalResourcePermissionsUseCase
                    .execute(GetLocalResourcePermissionsUseCase.Input(resourceId)).permissions

                val permissionsDisplayDataset = PermissionsDatasetCreator(permissionsListWidth, permissionItemWidth)
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

    private suspend fun handleFeatureFlags() {
        val isPasswordEyeIconVisible = getFeatureFlagsUseCase.execute(Unit).featureFlags.isPreviewPasswordAvailable
        if (!isPasswordEyeIconVisible) {
            view?.hidePasswordEyeIcon()
        }
    }

    private fun handleDescriptionField(resourceModel: ResourceModel) {
        scope.launch {
            val userId = requireNotNull(getSelectedAccountUseCase.execute(Unit).selectedAccount)
            val resourceWithFields = databaseProvider
                .get(userId)
                .resourceTypesDao()
                .getResourceTypeWithFieldsById(resourceModel.resourceTypeId)
            val isDescriptionSecret = resourceWithFields.resourceFields
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

    override fun usernameCopyClick() {
        view?.addToClipboard(USERNAME_LABEL, resourceModel.username.orEmpty())
    }

    override fun urlCopyClick() {
        view?.addToClipboard(WEBSITE_LABEL, resourceModel.url.orEmpty())
    }

    override fun moreClick() {
        view?.navigateToMore(resourceMenuModelMapper.map(resourceModel))
    }

    override fun backArrowClick() {
        view?.navigateBack()
    }

    override fun secretIconClick() {
        if (!isPasswordVisible) {
            scope.launch {
                val resourceTypeEnum = resourceTypeFactory.getResourceTypeEnum(resourceModel.resourceTypeId)
                doAfterFetchAndDecrypt { decryptedSecret ->
                    view?.apply {
                        showPasswordVisibleIcon()
                        val password = secretParser.extractPassword(resourceTypeEnum, decryptedSecret)
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
        scope.launch {
            val resourceTypeEnum = resourceTypeFactory.getResourceTypeEnum(resourceModel.resourceTypeId)
            doAfterFetchAndDecrypt { decryptedSecret ->
                view?.apply {
                    val description = secretParser.extractDescription(resourceTypeEnum, decryptedSecret)
                    showDescription(description, useSecretFont = true)
                }
            }
        }
    }

    private fun doAfterFetchAndDecrypt(action: (ByteArray) -> Unit) {
        scope.launch {
            when (val output =
                runAuthenticatedOperation(needSessionRefreshFlow, sessionRefreshedFlow) {
                    secretInteractor.fetchAndDecrypt(
                        resourceModel.resourceId
                    )
                }
            ) {
                is SecretInteractor.Output.DecryptFailure -> view?.showDecryptionFailure()
                is SecretInteractor.Output.FetchFailure -> view?.showFetchFailure()
                is SecretInteractor.Output.Success -> {
                    action(output.decryptedSecret)
                }
            }
        }
    }

    override fun menuCopyPasswordClick() {
        scope.launch {
            val resourceTypeEnum = resourceTypeFactory.getResourceTypeEnum(resourceModel.resourceTypeId)
            doAfterFetchAndDecrypt { decryptedSecret ->
                val password = secretParser.extractPassword(resourceTypeEnum, decryptedSecret)
                view?.addToClipboard(SECRET_LABEL, password)
            }
        }
    }

    override fun menuCopyDescriptionClick() {
        scope.launch {
            when (val resourceTypeEnum = resourceTypeFactory.getResourceTypeEnum(resourceModel.resourceTypeId)) {
                ResourceTypeFactory.ResourceTypeEnum.SIMPLE_PASSWORD -> {
                    view?.addToClipboard(DESCRIPTION_LABEL, resourceModel.description.orEmpty())
                }
                ResourceTypeFactory.ResourceTypeEnum.PASSWORD_WITH_DESCRIPTION -> {
                    doAfterFetchAndDecrypt { decryptedSecret ->
                        val description = secretParser.extractDescription(resourceTypeEnum, decryptedSecret)
                        view?.addToClipboard(DESCRIPTION_LABEL, description)
                    }
                }
            }
        }
    }

    override fun menuCopyUrlClick() {
        view?.addToClipboard(URL_LABEL, resourceModel.url.orEmpty())
    }

    override fun menuCopyUsernameClick() {
        view?.addToClipboard(USERNAME_LABEL, resourceModel.username.orEmpty())
    }

    override fun menuLaunchWebsiteClick() {
        if (resourceModel.url.isNullOrEmpty()) {
            view?.openWebsite(resourceModel.url.orEmpty())
        }
    }

    override fun menuDeleteClick() {
        view?.showDeleteConfirmationDialog()
    }

    override fun deleteResourceConfirmed() {
        runWhileShowingListProgress {
            when (val response = deleteResourceUseCase
                .execute(DeleteResourceUseCase.Input(resourceModel.resourceId))) {
                is DeleteResourceUseCase.Output.Success -> {
                    view?.closeWithDeleteSuccessResult(resourceModel.name)
                }
                is DeleteResourceUseCase.Output.Failure<*> -> {
                    Timber.e(response.response.exception)
                    view?.showGeneralError()
                }
            }
        }
    }

    override fun menuEditClick() {
        view?.navigateToEditResource(resourceModel)
    }

    override fun menuShareClick() {
        view?.navigateToResourcePermissions(resourceModel.resourceId, ResourcePermissionsMode.EDIT)
    }

    override fun resourceEdited(resourceName: String) {
        getResourcesAndPermissions(resourceModel.resourceId)
        view?.showResourceEditedSnackbar(resourceName)
    }

    private fun runWhileShowingListProgress(action: suspend () -> Unit) {
        scope.launch {
            view?.showProgress()
            action()
            view?.hideProgress()
        }
    }

    override fun sharedWithClick() {
        view?.navigateToResourcePermissions(resourceModel.resourceId, ResourcePermissionsMode.VIEW)
    }

    override fun detach() {
        scope.coroutineContext.cancelChildren()
        super<BaseAuthenticatedPresenter>.detach()
    }

    companion object {
        private const val WEBSITE_LABEL = "Website"
        private const val USERNAME_LABEL = "Username"
        private const val SECRET_LABEL = "Secret"
        private const val DESCRIPTION_LABEL = "Description"
        private const val URL_LABEL = "Url"
    }
}
