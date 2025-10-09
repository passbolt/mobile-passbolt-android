package com.passbolt.mobile.android.createresourcemenu

import com.passbolt.mobile.android.createresourcemenu.usecase.CreateCreateResourceMenuModelUseCase
import com.passbolt.mobile.android.createresourcemenu.view.CreateResourceMenuContract
import com.passbolt.mobile.android.ui.CreateResourceMenuModel
import com.passbolt.mobile.android.ui.HomeDisplayViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Rule
import org.junit.Test
import org.koin.core.logger.Level
import org.koin.test.KoinTest
import org.koin.test.KoinTestRule
import org.koin.test.inject
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.stub
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

class CreateResourceMenuPresenterTest : KoinTest {
    private val presenter: CreateResourceMenuContract.Presenter by inject()
    private val view: CreateResourceMenuContract.View = mock()

    @ExperimentalCoroutinesApi
    @get:Rule
    val koinTestRule =
        KoinTestRule.create {
            printLogger(Level.ERROR)
            modules(testCreateResourceMenuModule)
        }

    @Test
    fun `totp and folders items should be visible based on settings`() {
        mockCreateCreateResourceMenuModelUseCase.stub {
            onBlocking { execute(any()) }.doReturn(
                CreateCreateResourceMenuModelUseCase.Output(
                    CreateResourceMenuModel(
                        isTotpEnabled = true,
                        isFolderEnabled = true,
                        isPasswordEnabled = true,
                        isNoteEnabled = true,
                    ),
                ),
            )
        }

        presenter.attach(view)
        presenter.argsRetrieved(HomeDisplayViewModel.folderRoot())

        verify(view).showTotpButton()
        verify(view).showFoldersButton()
        verify(view).showPasswordButton()
        verify(view).showNoteButton()
    }

    @Test
    fun `totp and folders items should not be visible based on settings`() {
        mockCreateCreateResourceMenuModelUseCase.stub {
            onBlocking { execute(any()) }.doReturn(
                CreateCreateResourceMenuModelUseCase.Output(
                    CreateResourceMenuModel(
                        isTotpEnabled = false,
                        isFolderEnabled = false,
                        isPasswordEnabled = false,
                        isNoteEnabled = false,
                    ),
                ),
            )
        }

        presenter.attach(view)

        verify(view, never()).showTotpButton()
        verify(view, never()).showFoldersButton()
        verify(view, never()).showPasswordButton()
        verify(view, never()).showNoteButton()
    }

    @Test
    fun `note item should be visible when enabled`() {
        mockCreateCreateResourceMenuModelUseCase.stub {
            onBlocking { execute(any()) }.doReturn(
                CreateCreateResourceMenuModelUseCase.Output(
                    CreateResourceMenuModel(
                        isTotpEnabled = false,
                        isFolderEnabled = false,
                        isPasswordEnabled = false,
                        isNoteEnabled = true,
                    ),
                ),
            )
        }

        presenter.attach(view)
        presenter.argsRetrieved(HomeDisplayViewModel.folderRoot())

        verify(view, never()).showTotpButton()
        verify(view, never()).showFoldersButton()
        verify(view, never()).showPasswordButton()
        verify(view).showNoteButton()
    }
}
