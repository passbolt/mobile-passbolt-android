package com.passbolt.mobile.android.feature.otp.scanotp.scanotpsuccess

import com.passbolt.mobile.android.core.resources.actions.ResourceCreateActionResult
import com.passbolt.mobile.android.core.resources.actions.ResourceUpdateActionResult
import com.passbolt.mobile.android.core.resources.usecase.GetDefaultCreateContentTypeUseCase
import com.passbolt.mobile.android.core.resourcetypes.graph.redesigned.UpdateAction
import com.passbolt.mobile.android.resourcepicker.model.PickResourceAction
import com.passbolt.mobile.android.supportedresourceTypes.ContentType
import com.passbolt.mobile.android.ui.MetadataJsonModel
import com.passbolt.mobile.android.ui.MetadataTypeModel
import com.passbolt.mobile.android.ui.OtpParseResult
import com.passbolt.mobile.android.ui.ResourceModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.koin.core.logger.Level
import org.koin.test.KoinTest
import org.koin.test.KoinTestRule
import org.koin.test.inject
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
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
    val koinTestRule =
        KoinTestRule.create {
            printLogger(Level.ERROR)
            modules(testScanOtpSuccessModule)
        }

    @Test
    fun `create standalone totp should create totp and navigate back`() =
        runBlocking {
            mockGetDefaultCreateContentTypeUseCase.stub {
                onBlocking { execute(any()) }.doReturn(
                    GetDefaultCreateContentTypeUseCase.Output.CreationContentType(
                        ContentType.V5TotpStandalone,
                        MetadataTypeModel.V5,
                    ),
                )
            }
            val mockScannedTotp =
                OtpParseResult.OtpQr.TotpQr(
                    label = "label",
                    secret = "secret",
                    issuer = "issuer",
                    algorithm = OtpParseResult.OtpQr.Algorithm.SHA1,
                    digits = 6,
                    period = 30,
                )
            val mockResourceName = "mockResourceName"
            val mockResourceId = UUID.randomUUID()
            mockResourceCreateActionsInteractor.stub {
                onBlocking {
                    createGenericResource(any(), anyOrNull(), any(), any())
                }.doReturn(flowOf(ResourceCreateActionResult.Success(mockResourceId.toString(), mockResourceName)))
            }

            presenter.attach(view)
            presenter.argsRetrieved(mockScannedTotp, null)
            presenter.createStandaloneOtpClick()

            verify(view).showProgress()
            verify(view).hideProgress()
            verify(view).navigateToOtpList(mockScannedTotp, otpCreated = true, resourceId = mockResourceId.toString())
        }

    @Test
    fun `link totp to linked resource should update fields and navigate back`() =
        runBlocking {
            val mockScannedTotp =
                OtpParseResult.OtpQr.TotpQr(
                    label = "label",
                    secret = "secret",
                    issuer = "issuer",
                    algorithm = OtpParseResult.OtpQr.Algorithm.SHA1,
                    digits = 6,
                    period = 30,
                )
            val mockResourceName = "mockResourceName"
            val mockResourceId = UUID.randomUUID()
            val mockResourceTypeId = UUID.randomUUID()
            val mockMetadataJsonModel =
                mock<MetadataJsonModel> {
                    on { name } doReturn mockResourceName
                }
            val mockLinkResourceModel =
                mock<ResourceModel> {
                    on { resourceTypeId } doReturn mockResourceTypeId.toString()
                    on { metadataJsonModel } doReturn mockMetadataJsonModel
                }

            mockResourceUpdateActionsInteractor.stub {
                onBlocking {
                    updateGenericResource(eq(UpdateAction.ADD_TOTP), any(), any())
                }.doReturn(flowOf(ResourceUpdateActionResult.Success(mockResourceId.toString(), mockResourceName)))
            }
            mockIdToSlugMappingProvider.stub {
                onBlocking { provideMappingForSelectedAccount() }.doReturn(
                    mapOf(mockResourceTypeId to ContentType.V5DefaultWithTotp.slug),
                )
            }

            presenter.attach(view)
            presenter.argsRetrieved(mockScannedTotp, null)
            presenter.linkToResourceClick()
            presenter.linkedResourceReceived(PickResourceAction.TOTP_REPLACE, mockLinkResourceModel)

            verify(view).showProgress()
            verify(view).hideProgress()
            verify(view).navigateToOtpList(mockScannedTotp, otpCreated = true, resourceId = mockResourceId.toString())
        }

    @Test
    fun `link totp to password resource should update fields and navigate back`() =
        runBlocking {
            val mockScannedTotp =
                OtpParseResult.OtpQr.TotpQr(
                    label = "label",
                    secret = "secret",
                    issuer = "issuer",
                    algorithm = OtpParseResult.OtpQr.Algorithm.SHA1,
                    digits = 6,
                    period = 30,
                )
            val mockResourceName = "mockResourceName"
            val mockResourceId = UUID.randomUUID()
            val mockResourceTypeId = UUID.randomUUID()
            val mockMetadataJsonModel =
                mock<MetadataJsonModel> {
                    on { name } doReturn mockResourceName
                }
            val mockLinkResourceModel =
                mock<ResourceModel> {
                    on { resourceTypeId } doReturn mockResourceTypeId.toString()
                    on { metadataJsonModel } doReturn mockMetadataJsonModel
                }

            mockResourceUpdateActionsInteractor.stub {
                onBlocking {
                    updateGenericResource(eq(UpdateAction.ADD_TOTP), any(), any())
                }.doReturn(flowOf(ResourceUpdateActionResult.Success(mockResourceId.toString(), mockResourceName)))
            }
            mockIdToSlugMappingProvider.stub {
                onBlocking { provideMappingForSelectedAccount() }.doReturn(
                    mapOf(mockResourceTypeId to ContentType.V5Default.slug),
                )
            }

            presenter.attach(view)
            presenter.argsRetrieved(mockScannedTotp, null)
            presenter.linkToResourceClick()
            presenter.linkedResourceReceived(PickResourceAction.TOTP_LINK, mockLinkResourceModel)

            verify(view).showProgress()
            verify(view).hideProgress()
            verify(view).navigateToOtpList(mockScannedTotp, otpCreated = true, resourceId = mockResourceId.toString())
        }
}
