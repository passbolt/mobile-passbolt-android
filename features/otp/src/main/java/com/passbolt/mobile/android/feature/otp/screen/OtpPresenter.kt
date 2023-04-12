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
import com.passbolt.mobile.android.core.resources.actions.ResourceAuthenticatedActionsInteractor
import com.passbolt.mobile.android.database.impl.resources.GetLocalOtpResourcesUseCase
import com.passbolt.mobile.android.database.impl.resources.GetLocalResourceUseCase
import com.passbolt.mobile.android.feature.home.screen.model.SearchInputEndIconMode
import com.passbolt.mobile.android.feature.home.switchaccount.SwitchAccountBottomSheetFragment
import com.passbolt.mobile.android.feature.otp.otpmoremenu.OtpMoreMenuFragment
import com.passbolt.mobile.android.mappers.OtpModelMapper
import com.passbolt.mobile.android.storage.usecase.accountdata.GetSelectedAccountDataUseCase
import com.passbolt.mobile.android.ui.OtpListItemWrapper
import com.passbolt.mobile.android.ui.OtpMoreMenuModel
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
import kotlin.time.Duration.Companion.seconds

class OtpPresenter(
    private val getSelectedAccountDataUseCase: GetSelectedAccountDataUseCase,
    private val searchableMatcher: SearchableMatcher,
    private val getLocalOtpResourcesUseCase: GetLocalOtpResourcesUseCase,
    private val otpModelMapper: OtpModelMapper,
    private val getLocalResourceUseCase: GetLocalResourceUseCase,
    private val totpParametersProvider: TotpParametersProvider,
    coroutineLaunchContext: CoroutineLaunchContext
) : DataRefreshViewReactivePresenter<OtpContract.View>(coroutineLaunchContext), OtpContract.Presenter,
    SwitchAccountBottomSheetFragment.Listener, OtpMoreMenuFragment.Listener, KoinScopeComponent {

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
    private var currentOtpItemForMenu: OtpListItemWrapper? = null

    private var otpList = mutableListOf<OtpListItemWrapper>()
    private val visibleOtpIds = mutableListOf<String>()

    override fun attach(view: OtpContract.View) {
        super<DataRefreshViewReactivePresenter>.attach(view)
        updateOtpsCounterTime(view)
        loadUserAvatar()
        collectFilteringRefreshes()

        presenterScope.launch {
            getAndShowOtpResources()
        }
    }

    override fun refreshAction() {
        refreshInProgress = false
        presenterScope.launch {
            getAndShowOtpResources()
        }
    }

    private suspend fun getAndShowOtpResources() {
        getLocalOtpResourcesUseCase.execute(Unit).otps
            .map(otpModelMapper::map)
            .let {
                otpList = it.toMutableList()
                view?.hideFullScreenError()
                if (otpList.isEmpty()) {
                    view?.showEmptyView()
                } else {
                    view?.hideEmptyView()
                    view?.showOtpList(it)
                }
            }
    }

    private fun loadUserAvatar() {
        userAvatarUrl = getSelectedAccountDataUseCase.execute(Unit).avatarUrl
            .also { view?.displaySearchAvatar(it) }
    }

    private fun updateOtpsCounterTime(view: OtpContract.View) {
        tickerScope.launch {
            infiniteTimer(tickDuration = 1.seconds).collectLatest {
                var replaced = false
                otpList.replaceAll {
                    if (!visibleOtpIds.contains(it.otp.resourceId)) {
                        it
                    } else {
                        replaced = true
                        it.copy(remainingSecondsCounter = it.remainingSecondsCounter!! - 1)
                    }
                }
                if (replaced) {
                    view.showOtpList(otpList)
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
        view?.showOtpList(filtered)
    }

    private fun processSearchIconChange() {
        when (searchInputEndIconMode) {
            SearchInputEndIconMode.AVATAR -> view?.displaySearchAvatar(userAvatarUrl)
            SearchInputEndIconMode.CLEAR -> view?.displaySearchClearIcon()
        }
    }

    override fun otpItemClick(otpListItemWrapper: OtpListItemWrapper) {
        // TODO get from API
        presenterScope.launch {
            val otpResource = getLocalResourceUseCase.execute(
                GetLocalResourceUseCase.Input(otpListItemWrapper.otp.resourceId)
            ).resource
            val resourceAuthenticatedActionsInteractor = get<ResourceAuthenticatedActionsInteractor> {
                parametersOf(otpResource, needSessionRefreshFlow, sessionRefreshedFlow)
            }
            resourceAuthenticatedActionsInteractor.provideOtp(
                decryptionFailure = { view?.showDecryptionFailure() },
                fetchFailure = { view?.showFetchFailure() }
            ) { _, otp ->
                view?.apply {
                    val otpParameters = totpParametersProvider.provideOtpParameters(
                        secretKey = otp.totp.key,
                        digits = otp.totp.digits,
                        period = otp.totp.period,
                        algorithm = otp.totp.algorithm
                    )

                    val newOtp = otpListItemWrapper.copy(
                        otpValue = otpParameters.otpValue,
                        isVisible = true,
                        otpExpirySeconds = otp.totp.period,
                        remainingSecondsCounter = otpParameters.secondsValid
                    )
                    with(newOtp) {
                        otpList[otpList.indexOf(otpListItemWrapper)] = this
                        visibleOtpIds.add(otpListItemWrapper.otp.resourceId)
                    }
                    view?.showOtpList(otpList)
                }
            }
        }
    }

    override fun otpItemMoreClick(otpListWrapper: OtpListItemWrapper) {
        currentOtpItemForMenu = otpListWrapper
        view?.showOtmMoreMenu(
            OtpMoreMenuModel(otpListWrapper.otp.name, canDelete = true, canEdit = true)
        )
    }

    override fun refreshFailureAction() {
        view?.showFullscreenError()
    }

    override fun refreshClick() {
        view?.performRefreshUsingRefreshExecutor()
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
        tickerScope.coroutineContext.cancelChildren()
        filteringScope.coroutineContext.cancelChildren()
        presenterScope.coroutineContext.cancelChildren()
        scope.close()
        super<DataRefreshViewReactivePresenter>.detach()
    }

    override fun menuCopyOtpClick() {
        // TODO
    }

    override fun menuShowOtpClick() {
        otpItemClick(requireNotNull(currentOtpItemForMenu))
    }

    override fun menuDeleteOtpClick() {
        view?.showConfirmDeleteDialog()
    }

    override fun menuEditOtpClick() {
        // TODO navigate to edit otp
    }

    override fun scanOtpQrCodeClick() {
        view?.navigateToScanOtpQrCode()
    }

    override fun createOtpManuallyClick() {
        view?.navigateToCreateOtpManually()
    }

    override fun totpDeletetionConfirmed() {
        presenterScope.launch {
            val otpResource = getLocalResourceUseCase.execute(
                GetLocalResourceUseCase.Input(requireNotNull(currentOtpItemForMenu).otp.resourceId)
            ).resource
            val resourceAuthenticatedActionsInteractor = get<ResourceAuthenticatedActionsInteractor> {
                parametersOf(otpResource, needSessionRefreshFlow, sessionRefreshedFlow)
            }
            resourceAuthenticatedActionsInteractor.deleteResource(
                failure = { view?.showFailedToDeleteResource() },
                success = {
                    view?.initRefresh()
                    view?.showResourceDeleted()
                }
            )
        }
    }

    override fun otpCreated() {
        view?.showNewOtpCreated()
        view?.initRefresh()
    }
}
