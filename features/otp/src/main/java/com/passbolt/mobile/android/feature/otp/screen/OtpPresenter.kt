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

package com.passbolt.mobile.android.feature.otp.screen

import com.passbolt.mobile.android.common.coroutinetimer.infiniteTimer
import com.passbolt.mobile.android.common.search.SearchableMatcher
import com.passbolt.mobile.android.core.accounts.usecase.accountdata.GetSelectedAccountDataUseCase
import com.passbolt.mobile.android.core.fulldatarefresh.base.DataRefreshViewReactivePresenter
import com.passbolt.mobile.android.core.mvp.coroutinecontext.CoroutineLaunchContext
import com.passbolt.mobile.android.core.navigation.AppContext
import com.passbolt.mobile.android.core.otpcore.TotpParametersProvider
import com.passbolt.mobile.android.core.otpcore.TotpParametersProvider.OtpParametersResult.InvalidTotpInput
import com.passbolt.mobile.android.core.otpcore.TotpParametersProvider.OtpParametersResult.OtpParameters
import com.passbolt.mobile.android.core.resources.actions.ResourceCommonActionsInteractor
import com.passbolt.mobile.android.core.resources.actions.ResourceUpdateActionsInteractor
import com.passbolt.mobile.android.core.resources.actions.SecretPropertiesActionsInteractor
import com.passbolt.mobile.android.core.resources.actions.performCommonResourceAction
import com.passbolt.mobile.android.core.resources.actions.performResourceUpdateAction
import com.passbolt.mobile.android.core.resources.actions.performSecretPropertyAction
import com.passbolt.mobile.android.core.resources.usecase.db.GetLocalResourcesUseCase
import com.passbolt.mobile.android.core.resourcetypes.graph.redesigned.UpdateAction
import com.passbolt.mobile.android.core.resourcetypes.usecase.db.ResourceTypeIdToSlugMappingProvider
import com.passbolt.mobile.android.feature.authentication.session.runAuthenticatedOperation
import com.passbolt.mobile.android.feature.home.screen.model.SearchInputEndIconMode
import com.passbolt.mobile.android.feature.otp.screen.recycler.OtpItemUpdatePayload
import com.passbolt.mobile.android.mappers.OtpModelMapper
import com.passbolt.mobile.android.metadata.interactor.MetadataPrivateKeysHelperInteractor
import com.passbolt.mobile.android.serializers.jsonschema.SchemaEntity
import com.passbolt.mobile.android.supportedresourceTypes.ContentType
import com.passbolt.mobile.android.supportedresourceTypes.ContentType.PasswordDescriptionTotp
import com.passbolt.mobile.android.supportedresourceTypes.ContentType.Totp
import com.passbolt.mobile.android.supportedresourceTypes.ContentType.V5DefaultWithTotp
import com.passbolt.mobile.android.supportedresourceTypes.ContentType.V5TotpStandalone
import com.passbolt.mobile.android.supportedresourceTypes.SupportedContentTypes.totpSlugs
import com.passbolt.mobile.android.ui.NewMetadataKeyToTrustModel
import com.passbolt.mobile.android.ui.OtpItemWrapper
import com.passbolt.mobile.android.ui.ResourceModel
import com.passbolt.mobile.android.ui.TrustedKeyDeletedModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import org.koin.core.component.KoinScopeComponent
import org.koin.core.component.get
import org.koin.core.component.getOrCreateScope
import org.koin.core.parameter.parametersOf
import org.koin.core.scope.Scope
import timber.log.Timber
import java.util.UUID
import kotlin.time.Duration.Companion.seconds

@Suppress("TooManyFunctions")
class OtpPresenter(
    private val getSelectedAccountDataUseCase: GetSelectedAccountDataUseCase,
    private val searchableMatcher: SearchableMatcher,
    private val getLocalResourcesUseCase: GetLocalResourcesUseCase,
    private val otpModelMapper: OtpModelMapper,
    private val totpParametersProvider: TotpParametersProvider,
    private val idToSlugMappingProvider: ResourceTypeIdToSlugMappingProvider,
    private val metadataPrivateKeysHelperInteractor: MetadataPrivateKeysHelperInteractor,
    coroutineLaunchContext: CoroutineLaunchContext,
) : DataRefreshViewReactivePresenter<OtpContract.View>(coroutineLaunchContext),
    OtpContract.Presenter,
    KoinScopeComponent {
    override var view: OtpContract.View? = null
    private val job = SupervisorJob()

    override val scope: Scope
        get() = getOrCreateScope().value
    private val presenterScope = CoroutineScope(job + coroutineLaunchContext.ui)
    private val tickerJob = SupervisorJob()
    private val tickerScope = CoroutineScope(tickerJob + coroutineLaunchContext.ui)
    private val filteringJob = SupervisorJob()
    private val filteringScope = CoroutineScope(filteringJob + coroutineLaunchContext.ui)

    private var userAvatarUrl: String? = null
    private var refreshInProgress: Boolean = true
    private var currentSearchText = MutableStateFlow("")
    private val searchInputEndIconMode
        get() = if (currentSearchText.value.isBlank()) SearchInputEndIconMode.AVATAR else SearchInputEndIconMode.CLEAR
    private var currentOtpItemForMenu: OtpItemWrapper? = null

    private var otpList = mutableListOf<OtpItemWrapper>()
    private var visibleOtpId: String = ""

    override fun attach(view: OtpContract.View) {
        super<DataRefreshViewReactivePresenter>.attach(view)
        updateOtpsCounterTime()
        loadUserAvatar()
        collectFilteringRefreshes()

        presenterScope.launch {
            getAndShowOtpResources()
        }
    }

    override fun refreshSuccessAction() {
        refreshInProgress = false
        view?.showCreateButton()
        presenterScope.launch {
            val itemVisibleBeforeRefresh = visibleOtpId
            hideCurrentlyVisibleItem()
            getAndShowOtpResources()
            if (itemVisibleBeforeRefresh.isNotBlank()) {
                otpList
                    .find { it.resource.resourceId == itemVisibleBeforeRefresh }
                    ?.let {
                        hideCurrentlyVisibleItem()
                        showTotp(it, copyToClipboard = true)
                    }
                showOtps(OtpItemUpdatePayload.ALL)
            }
        }
    }

    override fun refreshStartAction() {
        view?.hideCreateButton()
    }

    private suspend fun getAndShowOtpResources() {
        getLocalResourcesUseCase
            .execute(GetLocalResourcesUseCase.Input(totpSlugs))
            .resources
            .map(otpModelMapper::map)
            .let {
                otpList = it.toMutableList()
                if (otpList.isEmpty()) {
                    view?.showEmptyView()
                } else {
                    view?.hideEmptyView()
                    showOtps(OtpItemUpdatePayload.ALL)
                }
            }
    }

    private fun loadUserAvatar() {
        userAvatarUrl =
            getSelectedAccountDataUseCase
                .execute(Unit)
                .avatarUrl
                .also { view?.displaySearchAvatar(it) }
    }

    private fun updateOtpsCounterTime() {
        tickerScope.launch {
            infiniteTimer(tickDuration = 1.seconds).collectLatest {
                if (visibleOtpId.isNotEmpty()) {
                    var replaced = false
                    otpList.replaceAll {
                        if (it.resource.resourceId != visibleOtpId) {
                            it.copy(
                                resource = it.resource,
                                isVisible = false,
                                otpExpirySeconds = null,
                                otpValue = null,
                                remainingSecondsCounter = null,
                            )
                        } else {
                            replaced = true
                            it.copy(remainingSecondsCounter = it.remainingSecondsCounter!! - 1)
                        }
                    }
                    if (replaced) {
                        // restart expired ones
                        otpList
                            .find { it.isVisible && (it.remainingSecondsCounter ?: Long.MAX_VALUE) < 0 }
                            ?.let {
                                hideCurrentlyVisibleItem(isCurrentItemRefreshing = true)
                                showTotp(it, copyToClipboard = true)
                            }
                        showOtps(OtpItemUpdatePayload.DATA)
                    }
                }
            }
        }
    }

    override fun searchTextChanged(text: String) {
        currentSearchText.value = text
    }

    private fun collectFilteringRefreshes() {
        filteringScope.launch {
            currentSearchText
                .drop(1) // initial empty value
                .collectLatest {
                    processSearchIconChange()
                    filterOtps(OtpItemUpdatePayload.ALL)
                }
        }
    }

    private fun filterOtps(updatePayload: OtpItemUpdatePayload) {
        val filtered =
            otpList.filter {
                searchableMatcher.matches(it, currentSearchText.value)
            }
        if (filtered.isEmpty()) {
            view?.showEmptyView()
        } else {
            view?.hideEmptyView()
            view?.showOtpList(filtered, updatePayload)
        }
    }

    private fun processSearchIconChange() {
        when (searchInputEndIconMode) {
            SearchInputEndIconMode.AVATAR -> view?.displaySearchAvatar(userAvatarUrl)
            SearchInputEndIconMode.CLEAR -> view?.displaySearchClearIcon()
        }
    }

    private fun hideCurrentlyVisibleItem(isCurrentItemRefreshing: Boolean = false) {
        otpList.replaceAll {
            if (it.resource.resourceId == visibleOtpId) {
                it.copy(
                    resource = it.resource,
                    isVisible = false,
                    otpExpirySeconds = null,
                    otpValue = null,
                    remainingSecondsCounter = null,
                    isRefreshing = isCurrentItemRefreshing,
                )
            } else {
                it
            }
        }
        visibleOtpId = ""
        showOtps(OtpItemUpdatePayload.ALL)
    }

    override fun otpItemClick(otpItemWrapper: OtpItemWrapper) {
        visibleOtpId = otpItemWrapper.resource.resourceId
        hideCurrentlyVisibleItem(isCurrentItemRefreshing = true)
        showTotp(otpItemWrapper, copyToClipboard = true)
    }

    private fun showTotp(
        otpItemWrapper: OtpItemWrapper,
        copyToClipboard: Boolean,
    ) {
        presenterScope.launch {
            val secretPropertiesActionsInteractor =
                get<SecretPropertiesActionsInteractor> {
                    parametersOf(otpItemWrapper.resource, needSessionRefreshFlow, sessionRefreshedFlow)
                }
            performSecretPropertyAction(
                action = { secretPropertiesActionsInteractor.provideOtp() },
                doOnDecryptionFailure = { view?.showDecryptionFailure() },
                doOnFetchFailure = { view?.showFetchFailure() },
                doOnSuccess = {
                    if (it.result.key.isNotBlank()) {
                        view?.apply {
                            val otpParametersResult =
                                totpParametersProvider.provideOtpParameters(
                                    secretKey = it.result.key,
                                    digits = it.result.digits,
                                    period = it.result.period,
                                    algorithm = it.result.algorithm,
                                )
                            when (otpParametersResult) {
                                is InvalidTotpInput -> {
                                    showGeneralError("Invalid TOTP input")
                                    return@apply
                                }

                                is OtpParameters -> {
                                    val newOtp =
                                        otpItemWrapper.copy(
                                            otpValue = otpParametersResult.otpValue,
                                            isVisible = true,
                                            otpExpirySeconds = it.result.period,
                                            remainingSecondsCounter = otpParametersResult.secondsValid,
                                            isRefreshing = false,
                                        )
                                    with(newOtp) {
                                        val indexOfOld =
                                            otpList
                                                .indexOfFirst { it.resource.resourceId == otpItemWrapper.resource.resourceId }
                                        otpList[indexOfOld] = this
                                        visibleOtpId = otpItemWrapper.resource.resourceId
                                    }
                                    showOtps(OtpItemUpdatePayload.DATA)
                                    if (copyToClipboard) {
                                        view?.copySecretToClipBoard(it.label, otpParametersResult.otpValue)
                                    }
                                }
                            }
                        }
                    } else {
                        val error = "Fetched totp key is empty"
                        Timber.e(error)
                        view?.showGeneralError(error)
                    }
                },
            )
        }
    }

    private fun showOtps(otpItemUpdatePayload: OtpItemUpdatePayload) {
        if (currentSearchText.value.isEmpty()) {
            view?.showOtpList(otpList, otpItemUpdatePayload)
        } else {
            filterOtps(otpItemUpdatePayload)
        }
    }

    override fun otpItemMoreClick(otpListWrapper: OtpItemWrapper) {
        hideCurrentlyVisibleItem()
        currentOtpItemForMenu = otpListWrapper
        view?.showOtmMoreMenu(otpListWrapper.resource.resourceId, otpListWrapper.resource.metadataJsonModel.name)
    }

    override fun refreshFailureAction() {
        view?.showDataRefreshError()
    }

    override fun refreshClick() {
        initRefresh()
    }

    override fun searchAvatarClick() {
        // TODO add support during OTP from Autofill
        if (refreshInProgress) {
            view?.showPleaseWaitForDataRefresh()
        } else {
            view?.navigateToSwitchAccount(AppContext.APP)
        }
    }

    override fun switchAccountManageAccountClick() {
        view?.navigateToManageAccounts()
    }

    override fun switchAccountClick() {
        // TODO add support during OTP from Autofill
        view?.navigateToSwitchedAccountAuth(AppContext.APP)
    }

    override fun searchClearClick() {
        view?.clearSearchInput()
    }

    override fun detach() {
        hideCurrentlyVisibleItem()
        tickerScope.coroutineContext.cancelChildren()
        filteringScope.coroutineContext.cancelChildren()
        presenterScope.coroutineContext.cancelChildren()
        scope.close()
        super<DataRefreshViewReactivePresenter>.detach()
    }

    override fun menuCopyOtpClick() {
        presenterScope.launch {
            val secretPropertiesActionsInteractor =
                get<SecretPropertiesActionsInteractor> {
                    parametersOf(currentOtpItemForMenu!!.resource, needSessionRefreshFlow, sessionRefreshedFlow)
                }
            performSecretPropertyAction(
                action = { secretPropertiesActionsInteractor.provideOtp() },
                doOnDecryptionFailure = { view?.showDecryptionFailure() },
                doOnFetchFailure = { view?.showFetchFailure() },
                doOnSuccess = {
                    if (it.result.key.isNotBlank()) {
                        val otpParametersResult =
                            totpParametersProvider.provideOtpParameters(
                                secretKey = it.result.key,
                                digits = it.result.digits,
                                period = it.result.period,
                                algorithm = it.result.algorithm,
                            )
                        when (otpParametersResult) {
                            is OtpParameters -> view?.copySecretToClipBoard(it.label, otpParametersResult.otpValue)
                            InvalidTotpInput -> view?.showGeneralError("Invalid TOTP input")
                        }
                    } else {
                        val error = "Fetched totp key is empty"
                        Timber.e(error)
                        view?.showGeneralError(error)
                    }
                },
            )
        }
    }

    override fun menuShowOtpClick() {
        showTotp(requireNotNull(currentOtpItemForMenu), copyToClipboard = false)
    }

    override fun menuDeleteOtpClick() {
        view?.showConfirmDeleteDialog()
    }

    override fun menuEditOtpClick() {
        view?.navigateToEditResource(currentOtpItemForMenu!!.resource)
    }

    override fun totpDeletionConfirmed() {
        presenterScope.launch {
            view?.showProgress()
            val otpResource = currentOtpItemForMenu!!.resource
            val slug =
                idToSlugMappingProvider.provideMappingForSelectedAccount()[
                    UUID.fromString(otpResource.resourceTypeId),
                ]
            when (val contentType = ContentType.fromSlug(slug!!)) {
                is Totp, V5TotpStandalone ->
                    deleteStandaloneTotpResource(otpResource)
                is PasswordDescriptionTotp, V5DefaultWithTotp ->
                    downgradeToPasswordAndDescriptionResource(otpResource)
                else ->
                    throw IllegalArgumentException("$contentType type should not be presented on totp list")
            }
            view?.hideProgress()
        }
    }

    private suspend fun downgradeToPasswordAndDescriptionResource(otpResource: ResourceModel) {
        val resourceUpdateActionInteractor =
            get<ResourceUpdateActionsInteractor> {
                parametersOf(otpResource, needSessionRefreshFlow, sessionRefreshedFlow)
            }

        performResourceUpdateAction(
            action = {
                resourceUpdateActionInteractor.updateGenericResource(
                    UpdateAction.REMOVE_TOTP,
                    secretModification = { it.apply { totp = null } },
                )
            },
            doOnCryptoFailure = { view?.showEncryptionError(it) },
            doOnFailure = { view?.showGeneralError(it) },
            doOnSuccess = {
                view?.showResourceDeleted()
                initRefresh()
            },
            doOnSchemaValidationFailure = ::handleSchemaValidationFailure,
            doOnFetchFailure = { view?.showFetchFailure() },
            doOnCannotEditWithCurrentConfig = { view?.showCannotUpdateTotpWithCurrentConfig() },
            doOnMetadataKeyModified = { view?.showMetadataKeyModifiedDialog(it) },
            doOnMetadataKeyDeleted = { view?.showMetadataKeyDeletedDialog(it) },
            doOnMetadataKeyVerificationFailure = { view?.showFailedToVerifyMetadataKey() },
        )
    }

    private fun handleSchemaValidationFailure(entity: SchemaEntity) {
        when (entity) {
            SchemaEntity.RESOURCE -> view?.showJsonResourceSchemaValidationError()
            SchemaEntity.SECRET -> view?.showJsonSecretSchemaValidationError()
        }
    }

    private suspend fun deleteStandaloneTotpResource(otpResource: ResourceModel) {
        val resourceCommonActionsInteractor =
            get<ResourceCommonActionsInteractor> {
                parametersOf(otpResource, needSessionRefreshFlow, sessionRefreshedFlow)
            }
        performCommonResourceAction(
            action = { resourceCommonActionsInteractor.deleteResource() },
            doOnFailure = { view?.showFailedToDeleteResource() },
            doOnSuccess = {
                view?.showResourceDeleted()
                initRefresh()
            },
        )
    }

    override fun otpCreated() {
        view?.showNewOtpCreated()
        initRefresh()
    }

    private fun initRefresh() {
        fullDataRefreshExecutor.performFullDataRefresh()
        refreshInProgress = true
        view?.hideCreateButton()
    }

    override fun otpUpdated() {
        view?.showOtpUpdate()
        initRefresh()
    }

    override fun otpQrScanReturned(
        isTotpCreated: Boolean,
        isManualCreationChosen: Boolean,
    ) {
        if (isTotpCreated) {
            initRefresh()
        } else {
            if (isManualCreationChosen) {
                view?.navigateToCreateTotpManually()
            }
        }
    }

    override fun resourceFormReturned(
        isResourceCreated: Boolean,
        isResourceEdited: Boolean,
        resourceName: String?,
    ) {
        if (isResourceCreated) {
            initRefresh()
            view?.showResourceCreatedSnackbar()
        }
        if (isResourceEdited) {
            initRefresh()
            view?.showResourceEditedSnackbar(resourceName.orEmpty())
        }
    }

    override fun trustedMetadataKeyDeleted(model: TrustedKeyDeletedModel) {
        presenterScope.launch {
            metadataPrivateKeysHelperInteractor.deletedTrustedMetadataPrivateKey()
        }
    }

    override fun trustNewMetadataKey(model: NewMetadataKeyToTrustModel) {
        presenterScope.launch {
            view?.showProgress()
            when (
                val output =
                    runAuthenticatedOperation(needSessionRefreshFlow, sessionRefreshedFlow) {
                        metadataPrivateKeysHelperInteractor.trustNewKey(model)
                    }
            ) {
                is MetadataPrivateKeysHelperInteractor.Output.Success ->
                    view?.showNewMetadataKeyIsTrusted()
                else -> {
                    Timber.e("Failed to trust new metadata key: $output")
                    view?.showFailedToTrustMetadataKey()
                }
            }
            view?.hideProgress()
        }
    }
}
