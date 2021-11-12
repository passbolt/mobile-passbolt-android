package com.passbolt.mobile.android.feature.resources.details

import com.passbolt.mobile.android.core.commonresource.ResourceTypeFactory
import com.passbolt.mobile.android.core.mvp.authentication.BaseAuthenticatedPresenter
import com.passbolt.mobile.android.core.mvp.coroutinecontext.CoroutineLaunchContext
import com.passbolt.mobile.android.core.mvp.session.runAuthenticatedOperation
import com.passbolt.mobile.android.database.DatabaseProvider
import com.passbolt.mobile.android.feature.resources.details.more.ResourceDetailsMenuModel
import com.passbolt.mobile.android.feature.secrets.usecase.decrypt.SecretInteractor
import com.passbolt.mobile.android.feature.secrets.usecase.decrypt.parser.SecretParser
import com.passbolt.mobile.android.featureflags.usecase.GetFeatureFlagsUseCase
import com.passbolt.mobile.android.storage.usecase.selectedaccount.GetSelectedAccountUseCase
import com.passbolt.mobile.android.ui.ResourceModel
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
class ResourceDetailsPresenter(
    private val secretInteractor: SecretInteractor,
    private val databaseProvider: DatabaseProvider,
    private val getSelectedAccountUseCase: GetSelectedAccountUseCase,
    private val resourceTypeFactory: ResourceTypeFactory,
    private val secretParser: SecretParser,
    private val getFeatureFlagsUseCase: GetFeatureFlagsUseCase,
    coroutineLaunchContext: CoroutineLaunchContext
) : BaseAuthenticatedPresenter<ResourceDetailsContract.View>(coroutineLaunchContext),
    ResourceDetailsContract.Presenter {

    override var view: ResourceDetailsContract.View? = null
    private lateinit var resourceModel: ResourceModel
    private val job = SupervisorJob()
    private val scope = CoroutineScope(job + coroutineLaunchContext.ui)
    private var isPasswordVisible = false

    // TODO consider resource types - for now only description can be both encrypted and unencrypted
    // TODO for future draw and set encrypted properties dynamically based on database input
    override fun argsReceived(resourceModel: ResourceModel) {
        this.resourceModel = resourceModel
        view?.apply {
            displayTitle(resourceModel.name)
            displayUsername(resourceModel.username)
            displayInitialsIcon(resourceModel.name, resourceModel.initials)
            if (!resourceModel.url.isNullOrEmpty()) {
                displayUrl(resourceModel.url!!)
            }
            showPasswordHidden()
            showPasswordHiddenIcon()
            handleDescriptionField(resourceModel)
            handleFeatureFlags()
        }
    }

    private fun handleFeatureFlags() {
        scope.launch {
            val isPasswordEyeIconVisible = getFeatureFlagsUseCase.execute(Unit).featureFlags.isPreviewPasswordAvailable
            if (!isPasswordEyeIconVisible) {
                view?.hidePasswordEyeIcon()
            }
        }
    }

    private fun handleDescriptionField(resourceModel: ResourceModel) {
        scope.launch {
            val userId = requireNotNull(getSelectedAccountUseCase.execute(Unit).selectedAccount)
            val resourceWithFields = databaseProvider
                .get(userId)
                .resourceTypesDao()
                .getResourceTypeWithFields(resourceModel.resourceTypeId)
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
        view?.navigateToMore(ResourceDetailsMenuModel(resourceModel.name))
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

    override fun detach() {
        scope.coroutineContext.cancelChildren()
        super<BaseAuthenticatedPresenter>.detach()
    }

    companion object {
        private const val WEBSITE_LABEL = "Website"
        private const val USERNAME_LABEL = "Username"
        private const val SECRET_LABEL = "Secret"
        private const val DESCRIPTION_LABEL = "Description"
    }
}
