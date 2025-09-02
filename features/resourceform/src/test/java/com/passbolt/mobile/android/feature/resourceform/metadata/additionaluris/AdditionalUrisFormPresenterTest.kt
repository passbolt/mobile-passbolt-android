package com.passbolt.mobile.android.feature.resourceform.metadata.additionaluris

import com.google.common.truth.Truth.assertThat
import com.passbolt.mobile.android.ui.AdditionalUrisUiModel
import com.passbolt.mobile.android.ui.LeadingContentType
import com.passbolt.mobile.android.ui.ResourceFormMode
import org.junit.Rule
import org.junit.Test
import org.koin.core.logger.Level
import org.koin.test.KoinTest
import org.koin.test.KoinTestRule
import org.koin.test.inject
import org.mockito.Mockito.mockStatic
import org.mockito.Mockito.never
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import java.util.UUID

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

class AdditionalUrisFormPresenterTest : KoinTest {
    private val presenter: AdditionalUrisFormContract.Presenter by inject()
    private val view: AdditionalUrisFormContract.View = mock()

    @get:Rule
    val koinTestRule =
        KoinTestRule.create {
            printLogger(Level.ERROR)
            modules(additionalUrisFormModule)
        }

    @Test
    fun `view should show correct create title and uris on attach`() {
        val mockUrisModel =
            AdditionalUrisUiModel(
                mainUri = "main",
                additionalUris = listOf("uri1", "uri2"),
            )

        presenter.attach(view)
        presenter.argsRetrieved(
            ResourceFormMode.Create(
                leadingContentType = LeadingContentType.PASSWORD,
                parentFolderId = null,
            ),
            mockUrisModel,
        )

        verify(view).showCreateTitle()
        verify(view).showMainUri(mockUrisModel.mainUri)
        argumentCaptor<LinkedHashMap<UUID, String>> {
            verify(view).showAdditionalUris(capture())
            assertThat(firstValue.size).isEqualTo(mockUrisModel.additionalUris.size)
            mockUrisModel.additionalUris.forEachIndexed { index, uri ->
                assertThat(firstValue.values.elementAt(index)).isEqualTo(uri)
            }
        }
        verifyNoMoreInteractions(view)
    }

    @Test
    fun `main uri changes should be applied`() {
        val mockUrisModel =
            AdditionalUrisUiModel(
                mainUri = "main",
                additionalUris = listOf("uri1", "uri2"),
            )
        val changedMainUri = "changed uri"

        presenter.attach(view)
        presenter.argsRetrieved(
            ResourceFormMode.Create(
                leadingContentType = LeadingContentType.PASSWORD,
                parentFolderId = null,
            ),
            mockUrisModel,
        )
        presenter.mainUriChanged(changedMainUri)
        presenter.applyClick()

        argumentCaptor<AdditionalUrisUiModel> {
            verify(view).goBackWithResult(capture())
            assertThat(firstValue.mainUri).isEqualTo(changedMainUri)
        }
    }

    @Test
    fun `additional uri changes should be applied`() {
        val mockUrisModel =
            AdditionalUrisUiModel(
                mainUri = "main",
                additionalUris = listOf("uri1"),
            )
        val changedUri1 = "changed uri1"
        val fixedUUID = UUID.fromString("123e4567-e89b-12d3-a456-426614174000")

        mockStatic(UUID::class.java).use { mockedUUID ->
            mockedUUID.`when`<Any?> { UUID.randomUUID() }.thenReturn(fixedUUID)

            presenter.attach(view)
            presenter.argsRetrieved(
                ResourceFormMode.Create(
                    leadingContentType = LeadingContentType.PASSWORD,
                    parentFolderId = null,
                ),
                mockUrisModel,
            )
            presenter.additionalUriChanged(fixedUUID, changedUri1)
            presenter.applyClick()

            argumentCaptor<AdditionalUrisUiModel> {
                verify(view).goBackWithResult(capture())
                assertThat(firstValue.additionalUris).isNotEmpty()
                assertThat(firstValue.additionalUris.first()).isEqualTo(changedUri1)
            }
        }
    }

    @Test
    fun `additional uri removal should be applied`() {
        val mockUrisModel =
            AdditionalUrisUiModel(
                mainUri = "main",
                additionalUris = listOf("uri1"),
            )
        val fixedUUID = UUID.fromString("123e4567-e89b-12d3-a456-426614174000")

        mockStatic(UUID::class.java).use { mockedUUID ->
            mockedUUID.`when`<Any?> { UUID.randomUUID() }.thenReturn(fixedUUID)

            presenter.attach(view)
            presenter.argsRetrieved(
                ResourceFormMode.Create(
                    leadingContentType = LeadingContentType.PASSWORD,
                    parentFolderId = null,
                ),
                mockUrisModel,
            )
            presenter.additionalUriRemoved(fixedUUID)
            presenter.applyClick()

            argumentCaptor<AdditionalUrisUiModel> {
                verify(view).goBackWithResult(capture())
                assertThat(firstValue.additionalUris).isEmpty()
            }
        }
    }

    @Test
    fun `validation should fail when main uri exceeds max length`() {
        val mockUrisModel =
            AdditionalUrisUiModel(
                mainUri = "main",
                additionalUris = emptyList(),
            )

        presenter.attach(view)
        presenter.argsRetrieved(
            ResourceFormMode.Create(
                leadingContentType = LeadingContentType.PASSWORD,
                parentFolderId = null,
            ),
            mockUrisModel,
        )

        val tooLongUri = "a".repeat(AdditionalUrisFormPresenter.URI_MAX_LENGTH + 1)
        presenter.mainUriChanged(tooLongUri)
        presenter.applyClick()

        verify(view).clearValidationErrors()
        verify(view).showMainUriMaxLengthError(AdditionalUrisFormPresenter.URI_MAX_LENGTH)
        verify(view, never()).goBackWithResult(org.mockito.kotlin.any())
    }

    @Test
    fun `validation should fail when additional uri exceeds max length`() {
        val mockUrisModel =
            AdditionalUrisUiModel(
                mainUri = "main",
                additionalUris = listOf("uri1"),
            )
        val fixedUUID = UUID.fromString("123e4567-e89b-12d3-a456-426614174000")

        mockStatic(UUID::class.java).use { mockedUUID ->
            mockedUUID.`when`<Any?> { UUID.randomUUID() }.thenReturn(fixedUUID)

            presenter.attach(view)
            presenter.argsRetrieved(
                ResourceFormMode.Create(
                    leadingContentType = LeadingContentType.PASSWORD,
                    parentFolderId = null,
                ),
                mockUrisModel,
            )

            val tooLongUri = "a".repeat(AdditionalUrisFormPresenter.URI_MAX_LENGTH + 1)
            presenter.additionalUriChanged(fixedUUID, tooLongUri)
            presenter.applyClick()

            verify(view).clearValidationErrors()
            verify(view).showAdditionalUriMaxLengthError(fixedUUID, AdditionalUrisFormPresenter.URI_MAX_LENGTH)
            verify(view).scrollToAdditionalUriWithError(fixedUUID)
            verify(view, never()).goBackWithResult(org.mockito.kotlin.any())
        }
    }
}
