package com.passbolt.mobile.android.feature.resourcedetails.details

import com.passbolt.mobile.android.common.coroutinetimer.infiniteTimer
import com.passbolt.mobile.android.common.extension.isBeforeNow
import com.passbolt.mobile.android.common.types.ClipboardLabel
import com.passbolt.mobile.android.core.commonfolders.usecase.db.GetLocalFolderLocationUseCase
import com.passbolt.mobile.android.core.fulldatarefresh.base.DataRefreshViewReactivePresenter
import com.passbolt.mobile.android.core.idlingresource.ResourceDetailActionIdlingResource
import com.passbolt.mobile.android.core.mvp.coroutinecontext.CoroutineLaunchContext
import com.passbolt.mobile.android.core.otpcore.TotpParametersProvider
import com.passbolt.mobile.android.core.resources.actions.ResourceCommonActionsInteractor
import com.passbolt.mobile.android.core.resources.actions.ResourcePropertiesActionsInteractor
import com.passbolt.mobile.android.core.resources.actions.ResourceUpdateActionsInteractor
import com.passbolt.mobile.android.core.resources.actions.SecretPropertiesActionsInteractor
import com.passbolt.mobile.android.core.resources.actions.performCommonResourceAction
import com.passbolt.mobile.android.core.resources.actions.performResourcePropertyAction
import com.passbolt.mobile.android.core.resources.actions.performResourceUpdateAction
import com.passbolt.mobile.android.core.resources.actions.performSecretPropertyAction
import com.passbolt.mobile.android.core.resources.usecase.db.GetLocalResourcePermissionsUseCase
import com.passbolt.mobile.android.core.resources.usecase.db.GetLocalResourceTagsUseCase
import com.passbolt.mobile.android.core.resources.usecase.db.GetLocalResourceUseCase
import com.passbolt.mobile.android.core.resourcetypes.ResourceTypeFactory
import com.passbolt.mobile.android.core.resourcetypes.ResourceTypeFactory.ResourceTypeEnum.SIMPLE_PASSWORD
import com.passbolt.mobile.android.core.resourcetypes.usecase.db.GetResourceTypeIdToSlugMappingUseCase
import com.passbolt.mobile.android.core.resourcetypes.usecase.db.GetResourceTypeWithFieldsByIdUseCase
import com.passbolt.mobile.android.core.secrets.usecase.decrypt.parser.TotpSecret
import com.passbolt.mobile.android.feature.otp.scanotp.parser.OtpParseResult
import com.passbolt.mobile.android.mappers.OtpModelMapper
import com.passbolt.mobile.android.permissions.permissions.PermissionsMode
import com.passbolt.mobile.android.permissions.recycler.PermissionsDatasetCreator
import com.passbolt.mobile.android.serializers.jsonschema.SchemaEntity
import com.passbolt.mobile.android.storage.usecase.featureflags.GetFeatureFlagsUseCase
import com.passbolt.mobile.android.storage.usecase.rbac.GetRbacRulesUseCase
import com.passbolt.mobile.android.supportedresourceTypes.SupportedContentTypes.PASSWORD_DESCRIPTION_TOTP_SLUG
import com.passbolt.mobile.android.ui.ManageTotpAction
import com.passbolt.mobile.android.ui.ManageTotpAction.ADD_TOTP
import com.passbolt.mobile.android.ui.ManageTotpAction.EDIT_TOTP
import com.passbolt.mobile.android.ui.OtpItemWrapper
import com.passbolt.mobile.android.ui.RbacModel
import com.passbolt.mobile.android.ui.RbacRuleModel.ALLOW
import com.passbolt.mobile.android.ui.ResourceModel
import com.passbolt.mobile.android.ui.ResourceMoreMenuModel
import com.passbolt.mobile.android.ui.isFavourite
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koin.core.parameter.parametersOf
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
    private val getResourceTypeWithFieldsByIdUseCase: GetResourceTypeWithFieldsByIdUseCase,
    private val totpParametersProvider: TotpParametersProvider,
    private val otpModelMapper: OtpModelMapper,
    private val getResourceTypeIdToSlugMappingUseCase: GetResourceTypeIdToSlugMappingUseCase,
    private val resourceTypeFactory: ResourceTypeFactory,
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
    private var isDescriptionVisible = false
    private var permissionsListWidth: Int = -1
    private var permissionItemWidth: Float = -1f
    private lateinit var resourceId: String
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
    private lateinit var otpAction: ManageTotpAction

    override fun argsReceived(resourceId: String, permissionsListWidth: Int, permissionItemWidth: Float) {
        this.permissionsListWidth = permissionsListWidth
        this.permissionItemWidth = permissionItemWidth
        this.resourceId = resourceId
        getResourcesAndPermissions(resourceId)
        updateOtpsCounterTime()
    }

    override fun refreshAction() {
        getResourcesAndPermissions(resourceId)
    }

    override fun refreshFailureAction() {
        view?.showDataRefreshError()
    }

    private fun getResourcesAndPermissions(resourceId: String) {
        coroutineScope.launch(missingItemExceptionHandler) {
            val rbac = getRbacRulesUseCase.execute(Unit).rbacModel
            val resourceInitializationDeferred = async {
                getAndDisplayResource(resourceId)
            }
            launch { // get and display permissions
                getAndDisplayPermissions(rbac, resourceId)
            }
            launch { // get and display tags
                getAndDisplayTags(rbac, resourceId)
            }
            launch { // get and show location
                getAndDisplayLocation(rbac, resourceInitializationDeferred)
            }
        }
    }

    private suspend fun getAndDisplayLocation(
        rbac: RbacModel,
        resourceInitializationDeferred: Deferred<Unit>
    ) {
        if (rbac.foldersUseRule == ALLOW) {
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

    private suspend fun getAndDisplayTags(rbac: RbacModel, resourceId: String) {
        val areTagsAvailable = getFeatureFlagsUseCase.execute(Unit).featureFlags.areTagsAvailable
        if (areTagsAvailable && rbac.tagsUseRule == ALLOW) {
            getLocalResourceTagsUseCase.execute(GetLocalResourceTagsUseCase.Input(resourceId))
                .tags
                .map { it.slug }
                .let { view?.showTags(it) }
        }
    }

    private suspend fun getAndDisplayPermissions(rbac: RbacModel, resourceId: String) {
        if (rbac.shareViewRule == ALLOW) {
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
    }

    private suspend fun getAndDisplayResource(
        resourceId: String
    ) {
        resourceModel = getLocalResourceUseCase.execute(GetLocalResourceUseCase.Input(resourceId)).resource
        view?.apply {
            displayUsername(resourceModel.username.orEmpty())
            displayInitialsIcon(resourceModel.name, resourceModel.initials)
            displayUrl(resourceModel.url.orEmpty())
            handleExpiry()
            handleFavourite()
            hidePassword()
            handleDescriptionField(resourceModel)
            handleTotpField(resourceModel)
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
                view?.displayTitle(resourceModel.name)
                view?.hideExpirySection()
            } else {
                if (expiry.isBeforeNow()) {
                    view?.displayExpiryTitle(resourceModel.name)
                    view?.showExpiryIndicator()
                } else {
                    view?.displayTitle(resourceModel.name)
                }
                view?.displayExpirySection(resourceModel.expiry!!)
            }
        }
    }

    private fun handleTotpField(resourceModel: ResourceModel) {
        coroutineScope.launch {
            getResourceTypeIdToSlugMappingUseCase.execute(Unit)
                .idToSlugMapping[UUID.fromString(resourceModel.resourceTypeId)]
                .let { slug ->
                    if (slug == PASSWORD_DESCRIPTION_TOTP_SLUG) {
                        view?.showTotpSection()
                    } else {
                        view?.hideTotpSection()
                    }
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

    private fun handleDescriptionField(resourceModel: ResourceModel) {
        coroutineScope.launch {
            val resourceTypeFields = getResourceTypeWithFieldsByIdUseCase.execute(
                GetResourceTypeWithFieldsByIdUseCase.Input(resourceModel.resourceTypeId)
            ).fields
            val isDescriptionSecret = resourceTypeFields
                .find { it.name == "description" }
                ?.isSecret ?: false
            if (isDescriptionSecret) {
                view?.hideDescription()
            } else {
                view?.showDescription(resourceModel.description.orEmpty(), isSecret = false)
            }
        }
    }

    override fun viewStopped() {
        view?.apply {
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
        view?.navigateToMore(resourceModel.resourceId, resourceModel.name)
    }

    override fun manageTotpClick() {
        view?.navigateToOtpMoreMenu(resourceModel.resourceId, resourceModel.name)
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
                        view?.showPassword(it.result)
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

    override fun descriptionActionClick() {
        if (!isDescriptionVisible) {
            resourceDetailActionIdlingResource.setIdle(false)
            coroutineScope.launch {
                when (resourceTypeFactory.getResourceTypeEnum(resourceModel.resourceTypeId)) {
                    SIMPLE_PASSWORD -> {
                        performResourcePropertyAction(
                            action = { resourcePropertiesActionsInteractor.provideDescription() },
                            doOnResult = { view?.showDescription(it.result, isSecret = it.isSecret) }
                        )
                    }
                    else -> {
                        performSecretPropertyAction(
                            action = { secretPropertiesActionsInteractor.provideDescription() },
                            doOnDecryptionFailure = { view?.showDecryptionFailure() },
                            doOnFetchFailure = { view?.showFetchFailure() },
                            doOnSuccess = { view?.showDescription(it.result, isSecret = it.isSecret) }
                        )
                    }
                }
                isDescriptionVisible = true
                resourceDetailActionIdlingResource.setIdle(true)
            }
        } else {
            view?.apply {
                clearDescriptionInput()
                hideDescription()
            }
            isDescriptionVisible = false
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
                    doOnSuccess = { view?.addToClipboard(it.label, it.result, it.isSecret) }
                )
            }
            resourceDetailActionIdlingResource.setIdle(true)
        }
    }

    override fun copyDescriptionClick() {
        resourceDetailActionIdlingResource.setIdle(false)
        coroutineScope.launch {
            when (resourceTypeFactory.getResourceTypeEnum(resourceModel.resourceTypeId)) {
                SIMPLE_PASSWORD -> {
                    performResourcePropertyAction(
                        action = { resourcePropertiesActionsInteractor.provideDescription() },
                        doOnResult = { view?.addToClipboard(it.label, it.result, it.isSecret) }
                    )
                }
                else -> {
                    performSecretPropertyAction(
                        action = { secretPropertiesActionsInteractor.provideDescription() },
                        doOnDecryptionFailure = { view?.showDecryptionFailure() },
                        doOnFetchFailure = { view?.showFetchFailure() },
                        doOnSuccess = { view?.addToClipboard(it.label, it.result, it.isSecret) }
                    )
                }
            }
            resourceDetailActionIdlingResource.setIdle(true)
        }
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
        resourceDetailActionIdlingResource.setIdle(false)
        runWhileShowingProgress {
            performCommonResourceAction(
                action = { resourceCommonActionsInteractor.toggleFavourite(option) },
                doOnFailure = { view?.showToggleFavouriteFailure() },
                doOnSuccess = { view?.setResourceEditedResult(resourceModel.name) }
            )
            getResourcesAndPermissions(resourceModel.resourceId)
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

    override fun otpScanned(totpQr: OtpParseResult.OtpQr.TotpQr?) {
        if (totpQr == null) {
            view?.showInvalidTotpScanned()
            return
        }
        coroutineScope.launch {
            runWhileShowingProgress {
                val resourceUpdateActionsInteractor = get<ResourceUpdateActionsInteractor> {
                    parametersOf(resourceModel, needSessionRefreshFlow, sessionRefreshedFlow)
                }
                val updateAction = when (otpAction) {
                    ADD_TOTP -> suspend {
                        resourceUpdateActionsInteractor.addTotpToResource(
                            overrideName = resourceModel.name,
                            overrideUri = resourceModel.url,
                            period = totpQr.period,
                            digits = totpQr.digits,
                            algorithm = totpQr.algorithm.name,
                            secretKey = totpQr.secret
                        )
                    }
                    EDIT_TOTP -> suspend {
                        resourceUpdateActionsInteractor.updateLinkedTotpResourceTotpFields(
                            label = resourceModel.name,
                            issuer = resourceModel.url,
                            period = totpQr.period,
                            digits = totpQr.digits,
                            algorithm = totpQr.algorithm.name,
                            secretKey = totpQr.secret
                        )
                    }
                }
                performResourceUpdateAction(
                    action = updateAction,
                    doOnFetchFailure = { view?.showFetchFailure() },
                    doOnFailure = { view?.showGeneralError(it) },
                    doOnCryptoFailure = { view?.showEncryptionError(it) },
                    doOnSchemaValidationFailure = ::handleSchemaValidationFailure,
                    doOnSuccess = { resourceEdited(it.resourceName) }
                )
            }
        }
    }

    private fun handleSchemaValidationFailure(entity: SchemaEntity) {
        when (entity) {
            SchemaEntity.RESOURCE -> view?.showJsonResourceSchemaValidationError()
            SchemaEntity.SECRET -> view?.showJsonSecretSchemaValidationError()
        }
    }

    override fun addTotpManuallyClick() {
        view?.navigateToOtpCreate(resourceModel.resourceId)
    }

    override fun menuCopyOtpClick() {
        doAfterOtpFetchAndDecrypt { label, _, otpParameters ->
            view?.addToClipboard(label, otpParameters.otpValue, isSecret = true)
        }
    }

    override fun menuShowOtpClick() {
        showTotp()
    }

    override fun menuAddTotpClick() {
        otpAction = ADD_TOTP
        view?.navigateToOtpCreateMenu()
    }

    override fun menuEditOtpClick() {
        otpAction = EDIT_TOTP
        view?.navigateToOtpEdit()
    }

    override fun menuDeleteOtpClick() {
        view?.showTotpDeleteConfirmationDialog()
    }

    override fun totpDeleteConfirmed() {
        resourceDetailActionIdlingResource.setIdle(false)
        coroutineScope.launch {
            view?.showProgress()

            val resourceUpdateActionInteractor = get<ResourceUpdateActionsInteractor> {
                parametersOf(resourceModel, needSessionRefreshFlow, sessionRefreshedFlow)
            }
            performResourceUpdateAction(
                action = {
                    resourceUpdateActionInteractor.deleteTotpFromResource()
                },
                doOnCryptoFailure = { view?.showEncryptionError(it) },
                doOnFailure = { view?.showGeneralError(it) },
                doOnSuccess = {
                    view?.showTotpDeleted()
                    getResourcesAndPermissions(it.resourceId)
                    view?.setResourceEditedResult(it.resourceName)
                },
                doOnSchemaValidationFailure = ::handleSchemaValidationFailure,
                doOnFetchFailure = { view?.showFetchFailure() }
            )

            view?.hideProgress()
            resourceDetailActionIdlingResource.setIdle(true)
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
                    val otpParameters = totpParametersProvider.provideOtpParameters(
                        secretKey = it.result.key,
                        digits = it.result.digits,
                        period = it.result.period,
                        algorithm = it.result.algorithm
                    )
                    action(it.label, it.result, otpParameters)
                }
            )
        }
    }

    override fun editOtpManuallyClick() {
        view?.navigateToOtpCreate(resourceModel.resourceId)
    }
}
