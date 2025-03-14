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

import com.google.common.truth.Truth.assertThat
import com.google.gson.JsonObject
import com.passbolt.mobile.android.core.accounts.usecase.accountdata.GetSelectedAccountDataUseCase
import com.passbolt.mobile.android.core.fulldatarefresh.DataRefreshStatus
import com.passbolt.mobile.android.core.fulldatarefresh.FullDataRefreshExecutor
import com.passbolt.mobile.android.core.fulldatarefresh.HomeDataInteractor
import com.passbolt.mobile.android.core.mvp.coroutinecontext.CoroutineLaunchContext
import com.passbolt.mobile.android.core.otpcore.TotpParametersProvider
import com.passbolt.mobile.android.core.resources.actions.SecretPropertiesActionsInteractor
import com.passbolt.mobile.android.core.resources.actions.SecretPropertyActionResult
import com.passbolt.mobile.android.core.resources.usecase.db.GetLocalResourcesUseCase
import com.passbolt.mobile.android.jsonmodel.delegates.TotpSecret
import com.passbolt.mobile.android.mappers.OtpModelMapper
import com.passbolt.mobile.android.ui.OtpItemWrapper
import com.passbolt.mobile.android.ui.OtpParseResult
import com.passbolt.mobile.android.ui.ResourceModel
import com.passbolt.mobile.android.ui.ResourcePermission
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.koin.core.logger.Level
import org.koin.test.KoinTest
import org.koin.test.KoinTestRule
import org.koin.test.get
import org.koin.test.inject
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.ZonedDateTime

@OptIn(ExperimentalCoroutinesApi::class)
class OtpPresenterTest : KoinTest {

    private val presenter: OtpContract.Presenter by inject()
    private val view: OtpContract.View = mock()
    private val mockFullDataRefreshExecutor: FullDataRefreshExecutor by inject()

    @OptIn(ExperimentalCoroutinesApi::class)
    @get:Rule
    val koinTestRule = KoinTestRule.create {
        printLogger(Level.ERROR)
        modules(testOtpModule)
    }

    @Before
    fun setup() {
        whenever(mockSelectedAccountDataCase.execute(anyOrNull())).thenReturn(
            GetSelectedAccountDataUseCase.Output(
                firstName = "",
                lastName = "",
                email = "",
                avatarUrl = SEARCH_AVATAR_URL,
                url = "",
                serverId = "",
                label = "label",
                role = "user"
            )
        )
        whenever(mockFullDataRefreshExecutor.dataRefreshStatusFlow).doReturn(
            flowOf(DataRefreshStatus.Finished(HomeDataInteractor.Output.Success))
        )
        mockTotpResources = listOf(
            ResourceModel(
                resourceId = "resId",
                resourceTypeId = "resTypeId",
                folderId = null,
                permission = ResourcePermission.READ,
                favouriteId = null,
                modified = ZonedDateTime.now(),
                expiry = null,
                json = JsonObject().apply {
                    addProperty("name", "")
                    addProperty("username", "")
                    addProperty("uri", "")
                    addProperty("description", "")
                }.toString(),
                metadataKeyId = null,
                metadataKeyType = null
            )
        )
    }

    @Test
    fun `view should show user avatar after attach`() {
        presenter.attach(view)
        presenter.resume(view)

        verify(view).displaySearchAvatar(SEARCH_AVATAR_URL)
    }

    @Test
    fun `view should show empty state if otp list is empty`() {
        mockGetLocalResourcesUseCase.stub {
            onBlocking { execute(any()) } doReturn GetLocalResourcesUseCase.Output(emptyList())
        }

        presenter.attach(view)
        presenter.resume(view)

        verify(view, times(2)).showEmptyView()
    }

    @Test
    fun `view should show otp resources`() {
        mockGetLocalResourcesUseCase.stub {
            onBlocking { execute(any()) } doReturn GetLocalResourcesUseCase.Output(mockTotpResources)
        }
        val mapper = get<OtpModelMapper>()

        presenter.attach(view)
        presenter.resume(view)

        verify(view, times(2)).hideEmptyView()
        argumentCaptor<List<OtpItemWrapper>> {
            verify(view, times(3)).showOtpList(capture())
            assertThat(firstValue).apply {
                hasSize(mockTotpResources.size)
                containsExactly(*(mockTotpResources.map(mapper::map)).toTypedArray())
            }
        }
    }

    @Test
    fun `view should show empty list when search term not found`() {
        mockGetLocalResourcesUseCase.stub {
            onBlocking { execute(any()) } doReturn GetLocalResourcesUseCase.Output(mockTotpResources)
        }

        presenter.attach(view)
        presenter.resume(view)
        presenter.searchTextChanged("show empty result")

        verify(view).showEmptyView()
    }

    @Test
    fun `view should reveal selected otp`() = runTest(get<CoroutineLaunchContext>().ui) {
        val mapper = get<OtpModelMapper>()
        val clickedItem = mapper.map(mockTotpResources[0])
        mockGetLocalResourcesUseCase.stub {
            onBlocking { execute(any()) } doReturn GetLocalResourcesUseCase.Output(mockTotpResources)
        }
        mockSecretPropertiesActionsInteractor.stub {
            onBlocking { provideOtp() } doReturn flowOf(
                SecretPropertyActionResult.Success(
                    SecretPropertiesActionsInteractor.OTP_LABEL,
                    isSecret = true,
                    TotpSecret(
                        algorithm = OtpParseResult.OtpQr.Algorithm.SHA1.name,
                        key = "aaa",
                        digits = 6,
                        period = 100
                    )
                )
            )
        }
        val otpValue = "111111"
        val otpSecondsValid = 30L
        whenever(mockTotpParametersProvider.provideOtpParameters(any(), any(), any(), any())).doReturn(
            TotpParametersProvider.OtpParameters(otpValue, otpSecondsValid)
        )

        presenter.attach(view)
        presenter.resume(view)
        presenter.otpItemClick(clickedItem)

        argumentCaptor<List<OtpItemWrapper>> {
            verify(view, times(5)).showOtpList(capture())
            assertThat(lastValue.any { it.otpValue == otpValue }).isTrue()
        }
    }

    private companion object {
        private const val SEARCH_AVATAR_URL = "url"
        private lateinit var mockTotpResources: List<ResourceModel>
    }
}
