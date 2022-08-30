package com.passbolt.mobile.android.core.mvp.progress

import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.reset
import org.mockito.kotlin.times
import org.mockito.kotlin.verify

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
class SynchronizedProgressViewTest {

    private val view: SynchronizedProgressView = mock()
    private val progressStackSynchronizer = ProgressStackSynchronizer()

    @Before
    fun setup() {
        progressStackSynchronizer.attach(view)
    }

    @After
    fun tearDown() {
        progressStackSynchronizer.detach()
    }

    @Test
    fun `one to one show-hide progress should work correct`() {
        progressStackSynchronizer.showProgress()
        progressStackSynchronizer.hideProgress()

        verify(view).showProgress()
        verify(view).hideProgress()
    }

    @Test
    fun `progress should be hidden once all operations complete`() {
        progressStackSynchronizer.showProgress()
        progressStackSynchronizer.showProgress()
        progressStackSynchronizer.hideProgress()

        verify(view, times(2)).showProgress()
        verify(view, never()).hideProgress()

        reset(view)
        progressStackSynchronizer.hideProgress()
        verify(view).hideProgress()
    }

    @Test
    fun `one to many show-hide progress should work correct`() {
        progressStackSynchronizer.showProgress()
        progressStackSynchronizer.hideProgress()
        progressStackSynchronizer.hideProgress()

        verify(view).showProgress()
        verify(view, times(2)).hideProgress()
    }
}
