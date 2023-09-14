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
import com.passbolt.mobile.android.core.fulldatarefresh.base.DataRefreshViewReactivePresenter
import com.passbolt.mobile.android.core.mvp.coroutinecontext.CoroutineLaunchContext
import com.passbolt.mobile.android.core.navigation.AppContext
import com.passbolt.mobile.android.core.otpcore.TotpParametersProvider
import com.passbolt.mobile.android.core.resources.actions.ResourceCommonActionsInteractor
import com.passbolt.mobile.android.core.resources.actions.ResourceUpdateActionsInteractor
import com.passbolt.mobile.android.core.resources.actions.SecretPropertiesActionsInteractor
import com.passbolt.mobile.android.core.resources.actions.performCommonResourceAction
import com.passbolt.mobile.android.core.resources.actions.performResourceUpdateAction
import com.passbolt.mobile.android.core.resources.actions.performSecretPropertyAction
import com.passbolt.mobile.android.core.resources.usecase.db.GetLocalResourcesUseCase
import com.passbolt.mobile.android.core.resourcetypes.ResourceTypeFactory
import com.passbolt.mobile.android.core.resourcetypes.ResourceTypeFactory.ResourceTypeEnum.PASSWORD_DESCRIPTION_TOTP
import com.passbolt.mobile.android.core.resourcetypes.ResourceTypeFactory.ResourceTypeEnum.PASSWORD_WITH_DESCRIPTION
import com.passbolt.mobile.android.core.resourcetypes.ResourceTypeFactory.ResourceTypeEnum.SIMPLE_PASSWORD
import com.passbolt.mobile.android.core.resourcetypes.ResourceTypeFactory.ResourceTypeEnum.STANDALONE_TOTP
import com.passbolt.mobile.android.feature.home.screen.model.SearchInputEndIconMode
import com.passbolt.mobile.android.feature.otp.scanotp.parser.OtpParseResult
import com.passbolt.mobile.android.mappers.OtpModelMapper
import com.passbolt.mobile.android.storage.usecase.accountdata.GetSelectedAccountDataUseCase
import com.passbolt.mobile.android.supportedresourceTypes.SupportedContentTypes.totpSlugs
import com.passbolt.mobile.android.ui.OtpItemWrapper
import com.passbolt.mobile.android.ui.ResourceModel
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
import kotlin.time.Duration.Companion.seconds

@Suppress("TooManyFunctions")
class OtpPresenter(
    private val getSelectedAccountDataUseCase: GetSelectedAccountDataUseCase,
    private val searchableMatcher: SearchableMatcher,
    private val getLocalResourcesUseCase: GetLocalResourcesUseCase,
    private val otpModelMapper: OtpModelMapper,
    private val totpParametersProvider: TotpParametersProvider,
    private val resourceTypeFactory: ResourceTypeFactory,
    coroutineLaunchContext: CoroutineLaunchContext
) : DataRefreshViewReactivePresenter<OtpContract.View>(coroutineLaunchContext), OtpContract.Presenter,
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

    private lateinit var otpResultAction: OtpResultAction

    override fun attach(view: OtpContract.View) {
        super<DataRefreshViewReactivePresenter>.attach(view)
        updateOtpsCounterTime()
        loadUserAvatar()
        collectFilteringRefreshes()

        presenterScope.launch {
            getAndShowOtpResources()
        }
    }

    override fun refreshAction() {
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
                showOtps()
            }
        }
    }

    private suspend fun getAndShowOtpResources() {
        getLocalResourcesUseCase.execute(GetLocalResourcesUseCase.Input(totpSlugs)).resources
            .map(otpModelMapper::map)
            .let {
                otpList = it.toMutableList()
                if (otpList.isEmpty()) {
                    view?.showEmptyView()
                } else {
                    view?.hideEmptyView()
                    showOtps()
                }
            }
    }

    private fun loadUserAvatar() {
        userAvatarUrl = getSelectedAccountDataUseCase.execute(Unit).avatarUrl
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
                                remainingSecondsCounter = null
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
                        showOtps()
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
                    filterOtps()
                }
        }
    }

    private fun filterOtps() {
        val filtered = otpList.filter {
            searchableMatcher.matches(it, currentSearchText.value)
        }
        if (filtered.isEmpty()) {
            view?.showEmptyView()
        } else {
            view?.hideEmptyView()
            view?.showOtpList(filtered)
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
                    isRefreshing = isCurrentItemRefreshing
                )
            } else {
                it
            }
        }
        visibleOtpId = ""
        showOtps()
    }

    override fun otpItemClick(otpItemWrapper: OtpItemWrapper) {
        visibleOtpId = otpItemWrapper.resource.resourceId
        hideCurrentlyVisibleItem(isCurrentItemRefreshing = true)
        showTotp(otpItemWrapper, copyToClipboard = true)
    }

    private fun showTotp(otpItemWrapper: OtpItemWrapper, copyToClipboard: Boolean) {
        presenterScope.launch {
            val secretPropertiesActionsInteractor = get<SecretPropertiesActionsInteractor> {
                parametersOf(otpItemWrapper.resource, needSessionRefreshFlow, sessionRefreshedFlow)
            }
            performSecretPropertyAction(
                action = { secretPropertiesActionsInteractor.provideOtp() },
                doOnDecryptionFailure = { view?.showDecryptionFailure() },
                doOnFetchFailure = { view?.showFetchFailure() },
                doOnSuccess = {
                    view?.apply {
                        val otpParameters = totpParametersProvider.provideOtpParameters(
                            secretKey = it.result.key,
                            digits = it.result.digits,
                            period = it.result.period,
                            algorithm = it.result.algorithm
                        )

                        val newOtp = otpItemWrapper.copy(
                            otpValue = otpParameters.otpValue,
                            isVisible = true,
                            otpExpirySeconds = it.result.period,
                            remainingSecondsCounter = otpParameters.secondsValid,
                            isRefreshing = false
                        )
                        with(newOtp) {
                            val indexOfOld = otpList
                                .indexOfFirst { it.resource.resourceId == otpItemWrapper.resource.resourceId }
                            otpList[indexOfOld] = this
                            visibleOtpId = otpItemWrapper.resource.resourceId
                        }
                        showOtps()
                        if (copyToClipboard) {
                            view?.copySecretToClipBoard(it.label, otpParameters.otpValue)
                        }
                    }
                }
            )
        }
    }

    private fun showOtps() {
        if (currentSearchText.value.isEmpty()) {
            view?.showOtpList(otpList)
        } else {
            filterOtps()
        }
    }

    override fun otpItemMoreClick(otpListWrapper: OtpItemWrapper) {
        hideCurrentlyVisibleItem()
        currentOtpItemForMenu = otpListWrapper
        view?.showOtmMoreMenu(otpListWrapper.resource.resourceId, otpListWrapper.resource.name)
    }

    override fun refreshFailureAction() {
        view?.showDataRefreshError()
    }

    override fun refreshClick() {
        refreshData()
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
            val secretPropertiesActionsInteractor = get<SecretPropertiesActionsInteractor> {
                parametersOf(currentOtpItemForMenu!!.resource, needSessionRefreshFlow, sessionRefreshedFlow)
            }
            performSecretPropertyAction(
                action = { secretPropertiesActionsInteractor.provideOtp() },
                doOnDecryptionFailure = { view?.showDecryptionFailure() },
                doOnFetchFailure = { view?.showFetchFailure() },
                doOnSuccess = {
                    val otpParameters = totpParametersProvider.provideOtpParameters(
                        secretKey = it.result.key,
                        digits = it.result.digits,
                        period = it.result.period,
                        algorithm = it.result.algorithm
                    )
                    view?.copySecretToClipBoard(it.label, otpParameters.otpValue)
                }
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
        view?.navigateToEditOtpMenu()
    }

    override fun scanOtpQrCodeClick() {
        otpResultAction = OtpResultAction.CREATE
        view?.navigateToScanOtpCodeForResult()
    }

    override fun createOtpManuallyClick() {
        view?.navigateToCreateOtpManually()
    }

    override fun totpDeletionConfirmed() {
        presenterScope.launch {
            view?.showProgress()
            val otpResource = currentOtpItemForMenu!!.resource
            when (val resourceTypeEnum = resourceTypeFactory.getResourceTypeEnum(otpResource.resourceTypeId)) {
                SIMPLE_PASSWORD, PASSWORD_WITH_DESCRIPTION ->
                    throw IllegalArgumentException("${resourceTypeEnum.name} type should not be presented on totp list")
                STANDALONE_TOTP ->
                    deleteStandaloneTotpResource(otpResource)
                PASSWORD_DESCRIPTION_TOTP ->
                    downgradeToPasswordAndDescriptionResource(otpResource)
            }
            view?.hideProgress()
        }
    }

    private suspend fun downgradeToPasswordAndDescriptionResource(otpResource: ResourceModel) {
        val resourceUpdateActionInteractor = get<ResourceUpdateActionsInteractor> {
            parametersOf(otpResource, needSessionRefreshFlow, sessionRefreshedFlow)
        }

        performResourceUpdateAction(
            action = {
                resourceUpdateActionInteractor.downgradeToPasswordAndDescriptionResource()
            },
            doOnCryptoFailure = { view?.showEncryptionError(it) },
            doOnFailure = { view?.showError(it) },
            doOnSuccess = {
                view?.showResourceDeleted()
                refreshData()
            },
            doOnFetchFailure = { view?.showFetchFailure() }
        )
    }

    private suspend fun deleteStandaloneTotpResource(otpResource: ResourceModel) {
        val resourceCommonActionsInteractor = get<ResourceCommonActionsInteractor> {
            parametersOf(otpResource, needSessionRefreshFlow, sessionRefreshedFlow)
        }
        performCommonResourceAction(
            action = { resourceCommonActionsInteractor.deleteResource() },
            doOnFailure = { view?.showFailedToDeleteResource() },
            doOnSuccess = {
                view?.showResourceDeleted()
                refreshData()
            }
        )
    }

    override fun otpCreated() {
        view?.showNewOtpCreated()
        refreshData()
    }

    private fun refreshData() {
        fullDataRefreshExecutor.performFullDataRefresh()
        refreshInProgress = true
        view?.hideCreateButton()
    }

    override fun otpUpdated() {
        view?.showOtpUpdate()
        refreshData()
    }

    override fun menuEditByQrScanClick() {
        otpResultAction = OtpResultAction.EDIT
        view?.navigateToScanOtpCodeForResult()
    }

    override fun menuEditOtpManuallyClick() {
        view?.navigateToEditOtpManually(currentOtpItemForMenu!!.resource.resourceId)
    }

    override fun otpQrScanned(totpQr: OtpParseResult.OtpQr.TotpQr?) {
        when (otpResultAction) {
            OtpResultAction.EDIT -> {
                if (totpQr == null) {
                    Timber.e("No data scanned in the QR code")
                    view?.showInvalidQrCodeDataScanned()
                    return
                }
                presenterScope.launch {
                    view?.showProgress()
                    val resource = currentOtpItemForMenu!!.resource
                    val resourceUpdateActionsInteractor = get<ResourceUpdateActionsInteractor> {
                        parametersOf(resource, needSessionRefreshFlow, sessionRefreshedFlow)
                    }
                    val updateOperation =
                        when (resourceTypeFactory.getResourceTypeEnum(resource.resourceTypeId)) {
                            SIMPLE_PASSWORD, PASSWORD_WITH_DESCRIPTION ->
                                throw IllegalArgumentException("These resource types are not shown on TOTP tab")
                            STANDALONE_TOTP ->
                                resourceUpdateActionsInteractor.updateStandaloneTotpResource(
                                    label = resource.name,
                                    issuer = resource.url,
                                    period = totpQr.period,
                                    digits = totpQr.digits,
                                    algorithm = totpQr.algorithm.name,
                                    secretKey = totpQr.secret
                                )
                            PASSWORD_DESCRIPTION_TOTP ->
                                resourceUpdateActionsInteractor.updateLinkedTotpResourceTotpFields(
                                    label = resource.name,
                                    issuer = resource.url,
                                    period = totpQr.period,
                                    digits = totpQr.digits,
                                    algorithm = totpQr.algorithm.name,
                                    secretKey = totpQr.secret
                                )
                        }
                    performResourceUpdateAction(
                        action = { updateOperation },
                        doOnCryptoFailure = { view?.showEncryptionError(it) },
                        doOnFailure = { view?.showError(it) },
                        doOnSuccess = {
                            view?.showOtpUpdate()
                            refreshData()
                        }
                    )

                    view?.hideProgress()
                }
            }
            OtpResultAction.CREATE -> {
                totpQr
                    ?.let { view?.navigateToScanOtpSuccess(totpQr) }
                    ?: run {
                        Timber.e("Invalid totp data scanned")
                        view?.showInvalidQrCodeDataScanned()
                    }
            }
        }
    }

    private enum class OtpResultAction {
        EDIT,
        CREATE
    }
}
