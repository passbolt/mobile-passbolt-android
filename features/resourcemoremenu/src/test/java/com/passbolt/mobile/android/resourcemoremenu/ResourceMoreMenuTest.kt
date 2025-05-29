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

package com.passbolt.mobile.android.resourcemoremenu

import com.passbolt.mobile.android.resourcemoremenu.usecase.CreateResourceMoreMenuModelUseCase
import com.passbolt.mobile.android.ui.ResourceMoreMenuModel
import com.passbolt.mobile.android.ui.ResourceMoreMenuModel.DescriptionOption.HAS_METADATA_DESCRIPTION
import com.passbolt.mobile.android.ui.ResourceMoreMenuModel.DescriptionOption.HAS_NOTE
import com.passbolt.mobile.android.ui.ResourceMoreMenuModel.FavouriteOption.ADD_TO_FAVOURITES
import com.passbolt.mobile.android.ui.ResourceMoreMenuModel.FavouriteOption.REMOVE_FROM_FAVOURITES
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
import org.mockito.kotlin.verifyNoMoreInteractions

@ExperimentalCoroutinesApi
class ResourceMoreMenuTest : KoinTest {
    private val presenter: ResourceMoreMenuContract.Presenter by inject()
    private val view = mock<ResourceMoreMenuContract.View>()

    @get:Rule
    val koinTestRule =
        KoinTestRule.create {
            printLogger(Level.ERROR)
            modules(testModule)
        }

    @Test
    fun `all enabled items should be displayed`() {
        mockCreateResourceMoreMenuModelUseCase.stub {
            onBlocking { execute(any()) } doReturn
                CreateResourceMoreMenuModelUseCase.Output(
                    ResourceMoreMenuModel(
                        title = "title",
                        canCopy = true,
                        canDelete = true,
                        canEdit = true,
                        canShare = true,
                        favouriteOption = ADD_TO_FAVOURITES,
                        descriptionOptions = listOf(HAS_NOTE, HAS_METADATA_DESCRIPTION),
                    ),
                )
        }

        presenter.apply {
            attach(this@ResourceMoreMenuTest.view)
            argsRetrieved("resourceId")
            refreshSuccessAction()
        }

        verify(view).showTitle("title")
        verify(view).showSeparator()
        verify(view).showCopyButton()
        verify(view).showDeleteButton()
        verify(view).showEditButton()
        verify(view).showShareButton()
        verify(view).showAddToFavouritesButton()
        verify(view).showCopyNoteButton()
        verify(view).showCopyMetadataDescriptionButton()
        verifyNoMoreInteractions(view)
    }

    @Test
    fun `disabled items should be not be displayed`() {
        mockCreateResourceMoreMenuModelUseCase.stub {
            onBlocking { execute(any()) }.thenReturn(
                CreateResourceMoreMenuModelUseCase.Output(
                    ResourceMoreMenuModel(
                        title = "title",
                        canCopy = false,
                        canDelete = false,
                        canEdit = false,
                        canShare = false,
                        favouriteOption = REMOVE_FROM_FAVOURITES,
                        descriptionOptions = emptyList(),
                    ),
                ),
            )
        }

        presenter.apply {
            attach(this@ResourceMoreMenuTest.view)
            argsRetrieved("resourceId")
            refreshSuccessAction()
        }

        verify(view).showTitle("title")
        verify(view, never()).showCopyButton()
        verify(view, never()).showDeleteButton()
        verify(view, never()).showEditButton()
        verify(view, never()).showShareButton()
        verify(view, never()).showAddToFavouritesButton()
        verify(view, never()).showCopyNoteButton()
        verify(view, never()).showCopyMetadataDescriptionButton()
        verify(view).showRemoveFromFavouritesButton()
        verifyNoMoreInteractions(view)
    }
}
