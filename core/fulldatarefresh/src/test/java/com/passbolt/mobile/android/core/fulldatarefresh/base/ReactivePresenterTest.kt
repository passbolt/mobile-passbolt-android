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

package com.passbolt.mobile.android.core.fulldatarefresh.base

import com.passbolt.mobile.android.common.datarefresh.DataRefreshStatus.Idle.FinishedWithFailure
import com.passbolt.mobile.android.common.datarefresh.DataRefreshStatus.Idle.FinishedWithSuccess
import com.passbolt.mobile.android.common.datarefresh.DataRefreshStatus.InProgress
import com.passbolt.mobile.android.common.datarefresh.DataRefreshTrackingFlow
import com.passbolt.mobile.android.commontest.TestCoroutineLaunchContext
import com.passbolt.mobile.android.core.mvp.authentication.SessionRefreshTrackingFlow
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.koin.core.logger.Level
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.koin.test.KoinTestRule
import org.koin.test.get
import org.mockito.Mockito.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify

@OptIn(ExperimentalCoroutinesApi::class)
class ReactivePresenterTest : KoinTest {
    private val coroutineLaunchContext = TestCoroutineLaunchContext()
    private val presenter = DummyReactPresenter(coroutineLaunchContext)
    private val view = mock<DummyReactContract.View>()

    @get:Rule
    val koinTestRule =
        KoinTestRule.create {
            printLogger(Level.ERROR)
            modules(
                module {
                    singleOf(::DataRefreshTrackingFlow)
                    singleOf(::SessionRefreshTrackingFlow)
                },
            )
        }

    @Test
    fun `ui should execute refresh action after data refresh finishes`() =
        runTest {
            val dataRefreshFlow = get<DataRefreshTrackingFlow>()

            presenter.attach(view)
            presenter.resume(view)
            dataRefreshFlow.updateStatus(InProgress)
            dataRefreshFlow.updateStatus(FinishedWithSuccess)
            presenter.pause()
            presenter.detach()

            verify(view).showRefreshProgress()
            verify(view).refreshActionUiEffect()
            verify(view, times(2)).hideRefreshProgress()
        }

    @Test
    fun `ui should execute failure action after data refresh fails`() =
        runTest {
            val dataRefreshFlow = get<DataRefreshTrackingFlow>()

            presenter.attach(view)
            presenter.resume(view)
            dataRefreshFlow.updateStatus(InProgress)
            dataRefreshFlow.updateStatus(FinishedWithFailure)

            verify(view).showRefreshProgress()
            verify(view).refreshFailureActionUiEffect()
            verify(view).hideRefreshProgress()
        }
}
