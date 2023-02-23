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
import com.passbolt.mobile.android.core.fulldatarefresh.base.DataRefreshViewReactivePresenter
import com.passbolt.mobile.android.core.mvp.coroutinecontext.CoroutineLaunchContext
import com.passbolt.mobile.android.ui.Otp
import com.passbolt.mobile.android.ui.OtpListItemWrapper
import com.passbolt.mobile.android.ui.ResourcePermission
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

class OtpPresenter(
    coroutineLaunchContext: CoroutineLaunchContext
) : DataRefreshViewReactivePresenter<OtpContract.View>(coroutineLaunchContext), OtpContract.Presenter {

    override var view: OtpContract.View? = null

    private val job = SupervisorJob()
    private val scope = CoroutineScope(job + coroutineLaunchContext.ui)
    private val tickerJob = SupervisorJob()
    private val tickerScope = CoroutineScope(tickerJob + coroutineLaunchContext.ui)

    // TODO get from API
    @Suppress("MagicNumber")
    private val otpList = mutableListOf(
        OtpListItemWrapper(
            Otp("demotywatory", "w", ResourcePermission.OWNER, 15, null), 1L, false
        ),
        OtpListItemWrapper(
            Otp("wykop", "d", ResourcePermission.OWNER, 60, null), 2L, false
        ),
        OtpListItemWrapper(
            Otp("android", "a", ResourcePermission.OWNER, 20, null), 3L, false
        )
    )
    private val visibleOtpListIds = mutableListOf<Long>()

    override fun attach(view: OtpContract.View) {
        super<DataRefreshViewReactivePresenter>.attach(view)
        updateOtpsCounterTime(view)
    }

    private fun updateOtpsCounterTime(view: OtpContract.View) {
        tickerScope.launch {
            infiniteTimer(tickDuration = 1.seconds).collectLatest {
                var replaced = false
                otpList.replaceAll {
                    if (!visibleOtpListIds.contains(it.listId)) {
                        it
                    } else {
                        replaced = true
                        it.copy(remainingSecondsCounter = it.remainingSecondsCounter - 1)
                    }
                }
                if (replaced) {
                    view.showOtpList(otpList)
                }
            }
        }
    }

    override fun otpItemClick(otp: OtpListItemWrapper) {
        // TODO get from API
        val otpValue = "123 456"

        val newOtp = otp.copy(otp = otp.otp.copy(otpValue = otpValue), isVisible = true)
        with(newOtp) {
            otpList[otpList.indexOf(otp)] = this
            visibleOtpListIds.add(listId)
        }
        view?.showOtpList(otpList)
    }

    override fun otpItemMoreClick(otp: OtpListItemWrapper) {
//        TODO("Not yet implemented")
    }

    override fun refreshAction() {
        if (otpList.isEmpty()) {
            view?.showEmptyView()
        } else {
            view?.hideEmptyView()
        }
        view?.showOtpList(otpList)
    }

    override fun refreshFailureAction() {
//        TODO("Not yet implemented")
    }

    override fun refreshClick() {
        view?.performRefreshUsingRefreshExecutor()
    }

    override fun detach() {
        tickerScope.coroutineContext.cancelChildren()
        scope.coroutineContext.cancelChildren()
        super<DataRefreshViewReactivePresenter>.detach()
    }
}
