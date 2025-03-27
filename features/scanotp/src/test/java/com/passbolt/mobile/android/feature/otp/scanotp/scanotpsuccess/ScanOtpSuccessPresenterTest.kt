package com.passbolt.mobile.android.feature.otp.scanotp.scanotpsuccess

import com.passbolt.mobile.android.core.qrscan.analyzer.BarcodeScanResult
import com.passbolt.mobile.android.core.resources.actions.ResourceCreateActionResult
import com.passbolt.mobile.android.core.resources.actions.ResourceCreateActionsInteractor
import com.passbolt.mobile.android.core.resources.actions.ResourceUpdateActionResult
import com.passbolt.mobile.android.resourcepicker.model.PickResourceAction
import com.passbolt.mobile.android.supportedresourceTypes.ContentType
import com.passbolt.mobile.android.ui.MetadataJsonModel
import com.passbolt.mobile.android.ui.OtpParseResult
import com.passbolt.mobile.android.ui.OtpParseResult.UserResolvableError.ErrorType.MULTIPLE_BARCODES
import com.passbolt.mobile.android.ui.OtpParseResult.UserResolvableError.ErrorType.NOT_A_OTP_QR
import com.passbolt.mobile.android.ui.OtpParseResult.UserResolvableError.ErrorType.NO_BARCODES_IN_RANGE
import com.passbolt.mobile.android.ui.ResourceModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.koin.core.logger.Level
import org.koin.test.KoinTest
import org.koin.test.KoinTestRule
import org.koin.test.inject
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
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
class ScanOtpSuccessPresenterTest : KoinTest {

    private val presenter: ScanOtpSuccessContract.Presenter by inject()
    private var view: ScanOtpSuccessContract.View = mock()

    @ExperimentalCoroutinesApi
    @get:Rule
    val koinTestRule = KoinTestRule.create {
        printLogger(Level.ERROR)
        modules(testScanOtpSuccessModule)
    }

    @Test
    fun `create standalone totp should create totp and navigate back`() = runBlocking {
        val mockScannedTotp = OtpParseResult.OtpQr.TotpQr(
            label = "label",
            secret = "secret",
            issuer = "issuer",
            algorithm = OtpParseResult.OtpQr.Algorithm.SHA1,
            digits = 6,
            period = 30
        )
        val mockResourceName = "mockResourceName"
        val mockResourceId = UUID.randomUUID()
        mockResourceCreateActionsInteractor.stub {
            onBlocking {
                createStandaloneTotpResource(anyOrNull(), anyOrNull(), any(), anyOrNull(), any(), any(), any(), any())
            }
                .doReturn(flowOf(ResourceCreateActionResult.Success(mockResourceId.toString(), mockResourceName)))
        }

        presenter.attach(view)
        presenter.argsRetrieved(mockScannedTotp)
        presenter.createStandaloneOtpClick()

        verify(view).showProgress()
        verify(view).hideProgress()
        verify(view).navigateToOtpList(mockScannedTotp, otpCreated = true)
    }

    @Test
    fun `link totp to linked resource should update fields and navigate back`() = runBlocking {
        val mockScannedTotp = OtpParseResult.OtpQr.TotpQr(
            label = "label",
            secret = "secret",
            issuer = "issuer",
            algorithm = OtpParseResult.OtpQr.Algorithm.SHA1,
            digits = 6,
            period = 30
        )
        val mockResourceName = "mockResourceName"
        val mockResourceId = UUID.randomUUID()
        val mockResourceTypeId = UUID.randomUUID()
        val mockMetadataJsonModel = mock<MetadataJsonModel> {
            on { name } doReturn mockResourceName
        }
        val mockLinkResourceModel = mock<ResourceModel> {
            on { resourceTypeId } doReturn mockResourceTypeId.toString()
            on { metadataJsonModel } doReturn mockMetadataJsonModel
        }

        mockResourceUpdateActionsInteractor.stub {
            onBlocking {
                updateLinkedTotpResourceTotpFields(any(), anyOrNull(), any(), any(), any(), any())
            }
                .doReturn(flowOf(ResourceUpdateActionResult.Success(mockResourceId.toString(), mockResourceName)))
        }
        mockIdToSlugMappingProvider.stub {
            onBlocking { provideMappingForSelectedAccount() }.doReturn(
                mapOf(mockResourceTypeId to ContentType.V5DefaultWithTotp.slug)
            )
        }

        presenter.attach(view)
        presenter.argsRetrieved(mockScannedTotp)
        presenter.linkToResourceClick()
        presenter.linkedResourceReceived(PickResourceAction.TOTP_REPLACE, mockLinkResourceModel)

        verify(view).showProgress()
        verify(view).hideProgress()
        verify(view).navigateToOtpList(mockScannedTotp, otpCreated = true)
    }

    @Test
    fun `link totp to password resource should update fields and navigate back`() = runBlocking {
        val mockScannedTotp = OtpParseResult.OtpQr.TotpQr(
            label = "label",
            secret = "secret",
            issuer = "issuer",
            algorithm = OtpParseResult.OtpQr.Algorithm.SHA1,
            digits = 6,
            period = 30
        )
        val mockResourceName = "mockResourceName"
        val mockResourceId = UUID.randomUUID()
        val mockResourceTypeId = UUID.randomUUID()
        val mockMetadataJsonModel = mock<MetadataJsonModel> {
            on { name } doReturn mockResourceName
        }
        val mockLinkResourceModel = mock<ResourceModel> {
            on { resourceTypeId } doReturn mockResourceTypeId.toString()
            on { metadataJsonModel } doReturn mockMetadataJsonModel
        }

        mockResourceUpdateActionsInteractor.stub {
            onBlocking {
                addTotpToResource(anyOrNull(), anyOrNull(), any(), any(), any(), any())
            }
                .doReturn(flowOf(ResourceUpdateActionResult.Success(mockResourceId.toString(), mockResourceName)))
        }
        mockIdToSlugMappingProvider.stub {
            onBlocking { provideMappingForSelectedAccount() }.doReturn(
                mapOf(mockResourceTypeId to ContentType.V5Default.slug)
            )
        }

        presenter.attach(view)
        presenter.argsRetrieved(mockScannedTotp)
        presenter.linkToResourceClick()
        presenter.linkedResourceReceived(PickResourceAction.TOTP_LINK, mockLinkResourceModel)

        verify(view).showProgress()
        verify(view).hideProgress()
        verify(view).navigateToOtpList(mockScannedTotp, otpCreated = true)
    }
}
