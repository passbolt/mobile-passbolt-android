package com.passbolt.mobile.android.feature.resourcedetails.details

import com.passbolt.mobile.android.common.coroutinetimer.infiniteTimer
import com.passbolt.mobile.android.common.types.ClipboardLabel
import com.passbolt.mobile.android.common.types.Description
import com.passbolt.mobile.android.common.types.Password
import com.passbolt.mobile.android.core.commonfolders.usecase.db.GetLocalFolderLocationUseCase
import com.passbolt.mobile.android.core.fulldatarefresh.base.DataRefreshViewReactivePresenter
import com.passbolt.mobile.android.core.mvp.coroutinecontext.CoroutineLaunchContext
import com.passbolt.mobile.android.core.otpcore.TotpParametersProvider
import com.passbolt.mobile.android.core.resources.actions.ResourceActionsInteractor
import com.passbolt.mobile.android.core.resources.actions.ResourceAuthenticatedActionsInteractor
import com.passbolt.mobile.android.core.resources.interactor.update.UpdateLinkedTotpResourceInteractor
import com.passbolt.mobile.android.core.resources.interactor.update.UpdatePasswordAndDescriptionResourceInteractor
import com.passbolt.mobile.android.core.resources.interactor.update.UpdateResourceInteractor
import com.passbolt.mobile.android.core.resources.usecase.db.GetLocalResourcePermissionsUseCase
import com.passbolt.mobile.android.core.resources.usecase.db.GetLocalResourceTagsUseCase
import com.passbolt.mobile.android.core.resources.usecase.db.GetLocalResourceUseCase
import com.passbolt.mobile.android.core.resources.usecase.db.UpdateLocalResourceUseCase
import com.passbolt.mobile.android.core.resourcetypes.ResourceTypeFactory
import com.passbolt.mobile.android.core.resourcetypes.ResourceTypeFactory.ResourceTypeEnum.PASSWORD_DESCRIPTION_TOTP
import com.passbolt.mobile.android.core.resourcetypes.ResourceTypeFactory.ResourceTypeEnum.PASSWORD_WITH_DESCRIPTION
import com.passbolt.mobile.android.core.resourcetypes.ResourceTypeFactory.ResourceTypeEnum.SIMPLE_PASSWORD
import com.passbolt.mobile.android.core.resourcetypes.ResourceTypeFactory.ResourceTypeEnum.STANDALONE_TOTP
import com.passbolt.mobile.android.core.resourcetypes.usecase.db.GetResourceTypeIdToSlugMappingUseCase
import com.passbolt.mobile.android.core.resourcetypes.usecase.db.GetResourceTypeWithFieldsByIdUseCase
import com.passbolt.mobile.android.core.secrets.usecase.decrypt.SecretInteractor
import com.passbolt.mobile.android.core.secrets.usecase.decrypt.parser.DecryptedSecret
import com.passbolt.mobile.android.feature.authentication.session.runAuthenticatedOperation
import com.passbolt.mobile.android.feature.otp.scanotp.parser.OtpParseResult
import com.passbolt.mobile.android.mappers.OtpModelMapper
import com.passbolt.mobile.android.otpmoremenu.usecase.CreateOtpMoreMenuModelUseCase
import com.passbolt.mobile.android.permissions.permissions.PermissionsMode
import com.passbolt.mobile.android.permissions.recycler.PermissionsDatasetCreator
import com.passbolt.mobile.android.resourcemoremenu.usecase.CreateResourceMoreMenuModelUseCase
import com.passbolt.mobile.android.storage.usecase.featureflags.GetFeatureFlagsUseCase
import com.passbolt.mobile.android.supportedresourceTypes.SupportedContentTypes.PASSWORD_DESCRIPTION_TOTP_SLUG
import com.passbolt.mobile.android.ui.OtpItemWrapper
import com.passbolt.mobile.android.ui.ResourceModel
import com.passbolt.mobile.android.ui.ResourceMoreMenuModel
import com.passbolt.mobile.android.ui.isFavourite
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.koin.core.component.KoinScopeComponent
import org.koin.core.component.get
import org.koin.core.component.getOrCreateScope
import org.koin.core.parameter.parametersOf
import org.koin.core.scope.Scope
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
    private val createResourceMenuModelUseCase: CreateResourceMoreMenuModelUseCase,
    private val getLocalResourceUseCase: GetLocalResourceUseCase,
    private val getLocalResourcePermissionsUseCase: GetLocalResourcePermissionsUseCase,
    private val getLocalResourceTagsUseCase: GetLocalResourceTagsUseCase,
    private val getLocalFolderLocation: GetLocalFolderLocationUseCase,
    private val getResourceTypeWithFieldsByIdUseCase: GetResourceTypeWithFieldsByIdUseCase,
    private val totpParametersProvider: TotpParametersProvider,
    private val otpModelMapper: OtpModelMapper,
    private val getResourceTypeIdToSlugMappingUseCase: GetResourceTypeIdToSlugMappingUseCase,
    private val updateLinkedTotpResourceInteractor: UpdateLinkedTotpResourceInteractor,
    private val updatePasswordAndDescriptionResourceInteractor: UpdatePasswordAndDescriptionResourceInteractor,
    private val secretInteractor: SecretInteractor,
    private val updateLocalResourceUseCase: UpdateLocalResourceUseCase,
    private val resourceTypeFactory: ResourceTypeFactory,
    private val createOtpMoreMenuModelUseCase: CreateOtpMoreMenuModelUseCase,
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
    private val tickerJob = SupervisorJob()
    private val tickerScope = CoroutineScope(tickerJob + coroutineLaunchContext.ui)
    private var otpModel: OtpItemWrapper? = null

    // TODO consider resource types - for now only description can be both encrypted and unencrypted
    // TODO for future draw and set encrypted properties dynamically based on database input

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
                    handleTotpField(resourceModel)
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
        tickerScope.coroutineContext.cancelChildren()
        coroutineScope.coroutineContext.cancelChildren()
        scope.close()
        super<DataRefreshViewReactivePresenter>.detach()
    }

    override fun moreClick() {
        hideTotp()
        coroutineScope.launch {
            createResourceMenuModelUseCase.execute(
                CreateResourceMoreMenuModelUseCase.Input(resourceModel.resourceId)
            )
                .resourceMenuModel
                .let { view?.navigateToMore(it) }
        }
    }

    override fun manageTotpClick() {
        coroutineScope.launch {
            createOtpMoreMenuModelUseCase.execute(
                CreateOtpMoreMenuModelUseCase.Input(resourceModel.resourceId, canShowOtp = true)
            )
                .otpMoreMenuModel
                .let { view?.navigateToOtpMoreMenu(it) }
        }
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

    override fun copyTotpClick() {
        doAfterOtpFetchAndDecrypt { label, _, otpParameters ->
            view?.addToClipboard(label, otpParameters.otpValue, isSecret = true)
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
                when (val fetchedSecret = runAuthenticatedOperation(needSessionRefreshFlow, sessionRefreshedFlow) {
                    secretInteractor.fetchAndDecrypt(resourceModel.resourceId)
                }) {
                    is SecretInteractor.Output.DecryptFailure -> {
                        Timber.e("Failed to decrypt secret during linking totp resource")
                        view?.showEncryptionError(fetchedSecret.error.message)
                    }
                    is SecretInteractor.Output.FetchFailure -> {
                        Timber.e("Failed to fetch secret during linking totp resource")
                        view?.showGeneralError()
                    }
                    is SecretInteractor.Output.Success -> {
                        when (val editResourceResult =
                            runAuthenticatedOperation(needSessionRefreshFlow, sessionRefreshedFlow) {
                                updateLinkedTotpResourceInteractor.execute(
                                    createTotpInputAfterScanning(totpQr),
                                    createUpdateToLinkedTotpInput(
                                        totpQr,
                                        fetchedSecret.decryptedSecret,
                                        resourceModel.resourceTypeId
                                    )
                                )
                            }) {
                            is UpdateResourceInteractor.Output.Success -> {
                                updateLocalResourceUseCase.execute(
                                    UpdateLocalResourceUseCase.Input(editResourceResult.resource)
                                )
                                resourceEdited(editResourceResult.resource.name)
                            }
                            is UpdateResourceInteractor.Output.Failure<*> ->
                                view?.showGeneralError(editResourceResult.response.exception.message.orEmpty())
                            is UpdateResourceInteractor.Output.PasswordExpired -> {
                                /* will not happen in BaseAuthenticatedPresenter */
                            }
                            is UpdateResourceInteractor.Output.OpenPgpError ->
                                view?.showEncryptionError(editResourceResult.message)
                        }
                    }
                    is SecretInteractor.Output.Unauthorized -> {
                        /* will not happen in BaseAuthenticatedPresenter */
                    }
                }
            }
        }
    }

    private suspend fun createTotpInputAfterScanning(totpQr: OtpParseResult.OtpQr.TotpQr) =
        when (resourceTypeFactory.getResourceTypeEnum(resourceModel.resourceTypeId)) {
            SIMPLE_PASSWORD, STANDALONE_TOTP -> throw IllegalArgumentException(
                "Cannot edit simple password or standalone totp by scanning qr code on resource details form"
            )
            PASSWORD_WITH_DESCRIPTION, PASSWORD_DESCRIPTION_TOTP -> createCommonUpdateInput()
        }

    // updates existing resource to linked totp resource
    private fun createCommonUpdateInput() =
        UpdateResourceInteractor.CommonInput(
            resourceId = resourceModel.resourceId,
            resourceName = resourceModel.name,
            resourceUsername = resourceModel.username,
            resourceUri = resourceModel.url,
            resourceParentFolderId = resourceModel.folderId
        )

    private fun createUpdateToLinkedTotpInput(
        totpQr: OtpParseResult.OtpQr.TotpQr,
        fetchedSecret: ByteArray,
        existingResourceTypeId: String
    ) =
        UpdateLinkedTotpResourceInteractor.UpdateToLinkedTotpInput(
            period = totpQr.period,
            digits = totpQr.digits,
            algorithm = totpQr.algorithm.name,
            secretKey = totpQr.secret,
            existingSecret = fetchedSecret,
            existingResourceTypeId = existingResourceTypeId,
            password = null,
            description = null
        )

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

    override fun menuEditOtpClick() {
        view?.navigateToOtpEdit()
    }

    override fun menuDeleteOtpClick() {
        view?.showTotpDeleteConfirmationDialog()
    }

    override fun totpDeleteConfirmed() {
        coroutineScope.launch {
            view?.showProgress()
            resourceAuthenticatedActionsInteractor.providePasswordAndDescription(
                decryptionFailure = { view?.showDecryptionFailure() },
                fetchFailure = { view?.showFetchFailure() }
            ) { password: Password, description: Description ->
                when (val editResourceResult = runAuthenticatedOperation(needSessionRefreshFlow, sessionRefreshedFlow) {
                    updatePasswordAndDescriptionResourceInteractor.execute(
                        createCommonUpdateInput(),
                        UpdatePasswordAndDescriptionResourceInteractor.UpdatePasswordAndDescriptionInput(
                            password = password,
                            description = description
                        )
                    )
                }) {
                    is UpdateResourceInteractor.Output.Success -> {
                        updateLocalResourceUseCase.execute(
                            UpdateLocalResourceUseCase.Input(editResourceResult.resource)
                        )
                        getResourcesAndPermissions(editResourceResult.resource.resourceId)
                        view?.showTotpDeleted()
                        view?.setResourceEditedResult(editResourceResult.resource.name)
                    }
                    is UpdateResourceInteractor.Output.Failure<*> ->
                        view?.showGeneralError(editResourceResult.response.exception.message.orEmpty())
                    is UpdateResourceInteractor.Output.PasswordExpired -> {
                        /* will not happen in BaseAuthenticatedPresenter */
                    }
                    is UpdateResourceInteractor.Output.OpenPgpError ->
                        view?.showEncryptionError(editResourceResult.message)
                }
            }
            view?.hideProgress()
        }
    }

    private fun doAfterOtpFetchAndDecrypt(
        action: (
            ClipboardLabel,
            DecryptedSecret.StandaloneTotp.Totp,
            TotpParametersProvider.OtpParameters
        ) -> Unit
    ) {
        coroutineScope.launch {
            resourceAuthenticatedActionsInteractor.provideOtp(
                decryptionFailure = { view?.showDecryptionFailure() },
                fetchFailure = { view?.showFetchFailure() }
            ) { label, otp ->
                val otpParameters = totpParametersProvider.provideOtpParameters(
                    secretKey = otp.key,
                    digits = otp.digits,
                    period = otp.period,
                    algorithm = otp.algorithm
                )
                action(label, otp, otpParameters)
            }
        }
    }

    override fun editOtpManuallyClick() {
        view?.navigateToOtpCreate(resourceModel.resourceId)
    }
}
