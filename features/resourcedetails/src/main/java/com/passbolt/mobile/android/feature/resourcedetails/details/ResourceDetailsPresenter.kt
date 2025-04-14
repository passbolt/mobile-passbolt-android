package com.passbolt.mobile.android.feature.resourcedetails.details

import com.passbolt.mobile.android.common.coroutinetimer.infiniteTimer
import com.passbolt.mobile.android.common.extension.isBeforeNow
import com.passbolt.mobile.android.common.types.ClipboardLabel
import com.passbolt.mobile.android.core.commonfolders.usecase.db.GetLocalFolderLocationUseCase
import com.passbolt.mobile.android.core.fulldatarefresh.DataRefreshStatus
import com.passbolt.mobile.android.core.fulldatarefresh.base.DataRefreshViewReactivePresenter
import com.passbolt.mobile.android.core.idlingresource.ResourceDetailActionIdlingResource
import com.passbolt.mobile.android.core.mvp.coroutinecontext.CoroutineLaunchContext
import com.passbolt.mobile.android.core.otpcore.TotpParametersProvider
import com.passbolt.mobile.android.core.rbac.usecase.GetRbacRulesUseCase
import com.passbolt.mobile.android.core.resources.actions.ResourceCommonActionsInteractor
import com.passbolt.mobile.android.core.resources.actions.ResourcePropertiesActionsInteractor
import com.passbolt.mobile.android.core.resources.actions.SecretPropertiesActionsInteractor
import com.passbolt.mobile.android.core.resources.actions.performCommonResourceAction
import com.passbolt.mobile.android.core.resources.actions.performResourcePropertyAction
import com.passbolt.mobile.android.core.resources.actions.performSecretPropertyAction
import com.passbolt.mobile.android.core.resources.usecase.db.GetLocalResourcePermissionsUseCase
import com.passbolt.mobile.android.core.resources.usecase.db.GetLocalResourceTagsUseCase
import com.passbolt.mobile.android.core.resources.usecase.db.GetLocalResourceUseCase
import com.passbolt.mobile.android.core.resourcetypes.usecase.db.ResourceTypeIdToSlugMappingProvider
import com.passbolt.mobile.android.featureflags.usecase.GetFeatureFlagsUseCase
import com.passbolt.mobile.android.jsonmodel.delegates.TotpSecret
import com.passbolt.mobile.android.mappers.OtpModelMapper
import com.passbolt.mobile.android.permissions.permissions.PermissionsMode
import com.passbolt.mobile.android.permissions.recycler.PermissionsDatasetCreator
import com.passbolt.mobile.android.supportedresourceTypes.ContentType
import com.passbolt.mobile.android.ui.OtpItemWrapper
import com.passbolt.mobile.android.ui.RbacModel
import com.passbolt.mobile.android.ui.RbacRuleModel.ALLOW
import com.passbolt.mobile.android.ui.ResourceModel
import com.passbolt.mobile.android.ui.ResourceMoreMenuModel
import com.passbolt.mobile.android.ui.isFavourite
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koin.core.parameter.parametersOf
import timber.log.Timber
import java.util.UUID
import kotlin.time.Duration.Companion.seconds

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
@Suppress("TooManyFunctions")
class ResourceDetailsPresenter(
    private val getFeatureFlagsUseCase: GetFeatureFlagsUseCase,
    private val getLocalResourceUseCase: GetLocalResourceUseCase,
    private val getLocalResourcePermissionsUseCase: GetLocalResourcePermissionsUseCase,
    private val getLocalResourceTagsUseCase: GetLocalResourceTagsUseCase,
    private val getLocalFolderLocation: GetLocalFolderLocationUseCase,
    private val totpParametersProvider: TotpParametersProvider,
    private val otpModelMapper: OtpModelMapper,
    private val idToSlugMappingProvider: ResourceTypeIdToSlugMappingProvider,
    private val getRbacRulesUseCase: GetRbacRulesUseCase,
    private val resourceDetailActionIdlingResource: ResourceDetailActionIdlingResource,
    coroutineLaunchContext: CoroutineLaunchContext
) : DataRefreshViewReactivePresenter<ResourceDetailsContract.View>(coroutineLaunchContext),
    ResourceDetailsContract.Presenter, KoinComponent {

    override var view: ResourceDetailsContract.View? = null
    private val job = SupervisorJob()
    private val coroutineScope = CoroutineScope(job + coroutineLaunchContext.ui)
    private lateinit var resourceModel: ResourceModel

    private var isPasswordVisible = false
    private var isNoteVisible = false
    private var permissionsListWidth: Int = -1
    private var permissionItemWidth: Float = -1f
    private val missingItemExceptionHandler = CoroutineExceptionHandler { _, throwable ->
        if (throwable is NullPointerException) {
            view?.showContentNotAvailable()
            view?.navigateBack()
        }
    }
    private val tickerJob = SupervisorJob()
    private val tickerScope = CoroutineScope(tickerJob + coroutineLaunchContext.ui)
    private var otpModel: OtpItemWrapper? = null

    private val resourcePropertiesActionsInteractor: ResourcePropertiesActionsInteractor
        get() = get { parametersOf(resourceModel) }
    private val secretPropertiesActionsInteractor: SecretPropertiesActionsInteractor
        get() = get {
            parametersOf(resourceModel, needSessionRefreshFlow, sessionRefreshedFlow)
        }
    private val resourceCommonActionsInteractor: ResourceCommonActionsInteractor
        get() = get {
            parametersOf(resourceModel, needSessionRefreshFlow, sessionRefreshedFlow)
        }

    override fun argsReceived(resourceModel: ResourceModel, permissionsListWidth: Int, permissionItemWidth: Float) {
        this.permissionsListWidth = permissionsListWidth
        this.permissionItemWidth = permissionItemWidth
        this.resourceModel = resourceModel

        getResourcesAndPermissions()
        updateOtpsCounterTime()
    }

    override fun refreshSuccessAction() {
        getResourcesAndPermissions()
    }

    override fun refreshFailureAction() {
        view?.showDataRefreshError()
    }

    private fun getResourcesAndPermissions() {
        coroutineScope.launch(missingItemExceptionHandler) {
            // show data from fragment arguments while refresh is in progress
            getAndDisplayResource()

            // wait for full refresh to finish to perform db operations
            if (fullDataRefreshExecutor.dataRefreshStatusFlow.first() is DataRefreshStatus.InProgress) {
                fullDataRefreshExecutor.dataRefreshStatusFlow.first { it is DataRefreshStatus.Finished }

                // refresh resource data based on latest db state
                resourceModel = getLocalResourceUseCase.execute(GetLocalResourceUseCase.Input(resourceModel.resourceId))
                    .resource
                getAndDisplayResource()
            }

            // fetch remaining data from db
            val rbac = getRbacRulesUseCase.execute(Unit).rbacModel
            launch { // get and display permissions
                getAndDisplayPermissions(rbac)
            }
            launch { // get and display tags
                getAndDisplayTags(rbac)
            }
            launch { // get and show location
                getAndDisplayLocation(rbac)
            }
        }
    }

    private suspend fun getAndDisplayLocation(rbac: RbacModel) {
        if (rbac.foldersUseRule == ALLOW) {
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

    private suspend fun getAndDisplayTags(rbac: RbacModel) {
        val areTagsAvailable = getFeatureFlagsUseCase.execute(Unit).featureFlags.areTagsAvailable
        if (areTagsAvailable && rbac.tagsUseRule == ALLOW) {
            getLocalResourceTagsUseCase.execute(GetLocalResourceTagsUseCase.Input(resourceModel.resourceId))
                .tags
                .map { it.slug }
                .let { view?.showTags(it) }
        }
    }

    private suspend fun getAndDisplayPermissions(rbac: RbacModel) {
        if (rbac.shareViewRule == ALLOW) {
            val permissions = getLocalResourcePermissionsUseCase.execute(
                GetLocalResourcePermissionsUseCase.Input(resourceModel.resourceId)
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
    }

    private suspend fun getAndDisplayResource() {
        performResourcePropertyAction(
            action = { resourcePropertiesActionsInteractor.provideWebsiteUrl() },
            doOnResult = { view?.displayUrl(it.result) }
        )
        view?.apply {
            displayUsername(resourceModel.metadataJsonModel.username.orEmpty())
            displayInitialsIcon(resourceModel.metadataJsonModel.name, resourceModel.initials)
            handleExpiry()
            handleFavourite()
            handlePassword(resourceModel)
            handleTotpField(resourceModel)
            handleDescriptionAndNoteField(resourceModel)
            handleFeatureFlagsAndRbac()
        }
    }

    private fun handleFavourite() {
        if (resourceModel.isFavourite()) {
            view?.showFavouriteStar()
        } else {
            view?.hideFavouriteStar()
        }
    }

    private fun handleExpiry() {
        resourceModel.expiry.let { expiry ->
            if (expiry == null) {
                view?.displayTitle(resourceModel.metadataJsonModel.name)
                view?.hideExpirySection()
            } else {
                if (expiry.isBeforeNow()) {
                    view?.displayExpiryTitle(resourceModel.metadataJsonModel.name)
                    view?.showExpiryIndicator()
                } else {
                    view?.displayTitle(resourceModel.metadataJsonModel.name)
                }
                view?.displayExpirySection(resourceModel.expiry!!)
            }
        }
    }

    private fun handleTotpField(resourceModel: ResourceModel) {
        coroutineScope.launch {
            val slug = idToSlugMappingProvider.provideMappingForSelectedAccount()[
                UUID.fromString(resourceModel.resourceTypeId)
            ]
            if (ContentType.fromSlug(slug!!).hasTotp()) {
                view?.showTotpSection()
            } else {
                view?.hideTotpSection()
            }
        }
    }

    private fun handlePassword(resourceModel: ResourceModel) {
        view?.hidePassword()
        coroutineScope.launch {
            val slug = idToSlugMappingProvider.provideMappingForSelectedAccount()[
                UUID.fromString(resourceModel.resourceTypeId)
            ]
            if (ContentType.fromSlug(slug!!).hasPassword()) {
                view?.showPasswordSection()
            } else {
                view?.hidePasswordSection()
            }
        }
    }

    private suspend fun handleFeatureFlagsAndRbac() {
        val passwordPreviewFeatureFlag = getFeatureFlagsUseCase.execute(Unit).featureFlags.isPreviewPasswordAvailable
        val passwordPreviewRbac = getRbacRulesUseCase.execute(Unit).rbacModel.passwordPreviewRule
        if (passwordPreviewFeatureFlag && passwordPreviewRbac == ALLOW) {
            view?.showPasswordEyeIcon()
        } else {
            view?.hidePasswordEyeIcon()
        }
    }

    private fun handleDescriptionAndNoteField(resourceModel: ResourceModel) {
        coroutineScope.launch {
            val slug = idToSlugMappingProvider.provideMappingForSelectedAccount()[
                UUID.fromString(resourceModel.resourceTypeId)
            ]
            val contentType = ContentType.fromSlug(slug!!)
            if (!contentType.hasNote()) {
                view?.disableNote()
            }
            if (contentType.hasMetadataDescription()) {
                view?.showMetadataDescription(resourceModel.metadataJsonModel.description.orEmpty())
            } else {
                view?.disableMetadataDescription()
            }
        }
    }

    override fun viewStopped() {
        view?.apply {
            clearNoteInput()
            hideNote()
            clearPasswordInput()
            hidePassword()
        }
    }

    override fun detach() {
        tickerScope.coroutineContext.cancelChildren()
        coroutineScope.coroutineContext.cancelChildren()
        super<DataRefreshViewReactivePresenter>.detach()
    }

    override fun moreClick() {
        hideTotp()
        view?.navigateToMore(resourceModel.resourceId, resourceModel.metadataJsonModel.name)
    }

    override fun backArrowClick() {
        view?.navigateBack()
    }

    override fun passwordActionClick() {
        if (!isPasswordVisible) {
            resourceDetailActionIdlingResource.setIdle(false)
            coroutineScope.launch {
                performSecretPropertyAction(
                    action = { secretPropertiesActionsInteractor.providePassword() },
                    doOnDecryptionFailure = { view?.showDecryptionFailure() },
                    doOnFetchFailure = { view?.showFetchFailure() },
                    doOnSuccess = {
                        view?.showPassword(it.result.orEmpty())
                        isPasswordVisible = true
                    }
                )
                resourceDetailActionIdlingResource.setIdle(true)
            }
        } else {
            view?.apply {
                clearPasswordInput()
                hidePassword()
            }
            isPasswordVisible = false
        }
    }

    override fun metadataDescriptionActionClick() {
        coroutineScope.launch {
            resourceDetailActionIdlingResource.setIdle(false)
            performResourcePropertyAction(
                action = { resourcePropertiesActionsInteractor.provideDescription() },
                doOnResult = { view?.showMetadataDescription(it.result) }
            )
            resourceDetailActionIdlingResource.setIdle(true)
        }
    }

    override fun noteActionClick() {
        if (isNoteVisible) {
            view?.hideNote()
            isNoteVisible = false
        } else {
            coroutineScope.launch {
                performSecretPropertyAction(
                    action = { secretPropertiesActionsInteractor.provideNote() },
                    doOnDecryptionFailure = { view?.showDecryptionFailure() },
                    doOnFetchFailure = { view?.showFetchFailure() },
                    doOnSuccess = {
                        view?.showNote(it.result)
                        isNoteVisible = true
                    }
                )
            }
        }
    }

    override fun usernameCopyClick() {
        coroutineScope.launch {
            performResourcePropertyAction(
                action = { resourcePropertiesActionsInteractor.provideUsername() },
                doOnResult = { view?.addToClipboard(it.label, it.result, it.isSecret) }
            )
        }
    }

    override fun urlCopyClick() {
        coroutineScope.launch {
            performResourcePropertyAction(
                action = { resourcePropertiesActionsInteractor.provideWebsiteUrl() },
                doOnResult = { view?.addToClipboard(it.label, it.result, it.isSecret) }
            )
        }
    }

    override fun copyPasswordClick() {
        resourceDetailActionIdlingResource.setIdle(false)
        coroutineScope.launch {
            if (getRbacRulesUseCase.execute(Unit).rbacModel.passwordCopyRule == ALLOW) {
                performSecretPropertyAction(
                    action = { secretPropertiesActionsInteractor.providePassword() },
                    doOnFetchFailure = { view?.showFetchFailure() },
                    doOnDecryptionFailure = { view?.showDecryptionFailure() },
                    doOnSuccess = { view?.addToClipboard(it.label, it.result.orEmpty(), it.isSecret) }
                )
            }
            resourceDetailActionIdlingResource.setIdle(true)
        }
    }

    override fun copyMetadataDescriptionClick() {
        resourceDetailActionIdlingResource.setIdle(false)
        coroutineScope.launch {
            performResourcePropertyAction(
                action = { resourcePropertiesActionsInteractor.provideDescription() },
                doOnResult = { view?.addToClipboard(it.label, it.result, it.isSecret) }
            )
        }
        resourceDetailActionIdlingResource.setIdle(true)
    }

    override fun copyNoteClick() {
        resourceDetailActionIdlingResource.setIdle(false)
        coroutineScope.launch {
            performSecretPropertyAction(
                action = { secretPropertiesActionsInteractor.provideNote() },
                doOnFetchFailure = { view?.showFetchFailure() },
                doOnDecryptionFailure = { view?.showDecryptionFailure() },
                doOnSuccess = { view?.addToClipboard(it.label, it.result, it.isSecret) }
            )
        }
        resourceDetailActionIdlingResource.setIdle(true)
    }

    override fun launchWebsiteClick() {
        coroutineScope.launch {
            performResourcePropertyAction(
                action = { resourcePropertiesActionsInteractor.provideWebsiteUrl() },
                doOnResult = { view?.openWebsite(it.result) }
            )
        }
    }

    override fun deleteClick() {
        view?.showDeleteConfirmationDialog()
    }

    override fun deleteResourceConfirmed() {
        runWhileShowingProgress {
            performCommonResourceAction(
                action = { resourceCommonActionsInteractor.deleteResource() },
                doOnFailure = { view?.showGeneralError() },
                doOnSuccess = { view?.closeWithDeleteSuccessResult(it.resourceName) }
            )
        }
    }

    override fun resourceEdited(resourceName: String?) {
        coroutineScope.launch {
            resourceModel = getLocalResourceUseCase.execute(GetLocalResourceUseCase.Input(resourceModel.resourceId))
                .resource
            getResourcesAndPermissions()
            view?.apply {
                showResourceEditedSnackbar(resourceName.orEmpty())
                setResourceEditedResult(resourceName.orEmpty())
            }
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
        resourceDetailActionIdlingResource.setIdle(false)
        runWhileShowingProgress {
            performCommonResourceAction(
                action = { resourceCommonActionsInteractor.toggleFavourite(option) },
                doOnFailure = { view?.showToggleFavouriteFailure() },
                doOnSuccess = { view?.setResourceEditedResult(resourceModel.metadataJsonModel.name) }
            )
            getResourcesAndPermissions()
            resourceDetailActionIdlingResource.setIdle(true)
        }
    }

    override fun locationClick() {
        view?.navigateToResourceLocation(resourceModel.resourceId)
    }

    override fun copyTotpClick() {
        resourceDetailActionIdlingResource.setIdle(false)
        doAfterOtpFetchAndDecrypt { label, _, otpParameters ->
            view?.addToClipboard(label, otpParameters.otpValue, isSecret = true)
            resourceDetailActionIdlingResource.setIdle(true)
        }
    }

    override fun totpIconClick() {
        if (otpModel == null) {
            showTotp()
        } else {
            hideTotp()
        }
    }

    private fun showTotp() {
        resourceDetailActionIdlingResource.setIdle(false)
        otpModel = otpModelMapper.map(resourceModel)
            .copy(isRefreshing = true)
            .also {
                view?.showTotp(it)
            }

        doAfterOtpFetchAndDecrypt { _, otp, otpParameters ->
            otpModel = otpModelMapper.map(resourceModel)
                .copy(
                    otpValue = otpParameters.otpValue,
                    isVisible = true,
                    otpExpirySeconds = otp.period,
                    remainingSecondsCounter = otpParameters.secondsValid,
                    isRefreshing = false
                )
            resourceDetailActionIdlingResource.setIdle(true)
        }
    }

    private fun hideTotp() {
        view?.showTotp(otpModelMapper.map(resourceModel))
        otpModel = null
    }

    private fun updateOtpsCounterTime() {
        tickerScope.launch {
            infiniteTimer(tickDuration = 1.seconds).collectLatest {
                if (otpModel != null && otpModel?.remainingSecondsCounter != null) {
                    if (otpModel?.remainingSecondsCounter!! > 0) {
                        otpModel = otpModel?.copy(remainingSecondsCounter = otpModel?.remainingSecondsCounter!! - 1)
                        view?.showTotp(otpModel)
                    } else {
                        // restart if expired
                        showTotp()
                    }
                }
            }
        }
    }

    private fun doAfterOtpFetchAndDecrypt(
        action: (
            ClipboardLabel,
            TotpSecret,
            TotpParametersProvider.OtpParameters
        ) -> Unit
    ) {
        coroutineScope.launch {
            performSecretPropertyAction(
                action = { secretPropertiesActionsInteractor.provideOtp() },
                doOnFetchFailure = { view?.showFetchFailure() },
                doOnDecryptionFailure = { view?.showDecryptionFailure() },
                doOnSuccess = {
                    if (it.result.key.isNotBlank()) {
                        val otpParameters = totpParametersProvider.provideOtpParameters(
                            secretKey = it.result.key,
                            digits = it.result.digits,
                            period = it.result.period,
                            algorithm = it.result.algorithm
                        )
                        action(it.label, it.result, otpParameters)
                    } else {
                        val error = "Fetched totp key is empty"
                        Timber.e(error)
                        view?.showGeneralError(error)
                    }
                }
            )
        }
    }
}
