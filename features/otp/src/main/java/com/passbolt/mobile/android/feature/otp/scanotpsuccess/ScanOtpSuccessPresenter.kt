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

package com.passbolt.mobile.android.feature.otp.scanotpsuccess

import com.passbolt.mobile.android.core.mvp.authentication.BaseAuthenticatedPresenter
import com.passbolt.mobile.android.core.mvp.coroutinecontext.CoroutineLaunchContext
import com.passbolt.mobile.android.core.resources.interactor.create.CreateResourceInteractor
import com.passbolt.mobile.android.core.resources.interactor.create.CreateStandaloneTotpResourceInteractor
import com.passbolt.mobile.android.core.resources.interactor.update.UpdateResourceInteractor
import com.passbolt.mobile.android.core.resources.interactor.update.UpdateToLinkedTotpResourceInteractor
import com.passbolt.mobile.android.core.resources.usecase.db.UpdateLocalResourceUseCase
import com.passbolt.mobile.android.core.secrets.usecase.decrypt.SecretInteractor
import com.passbolt.mobile.android.feature.authentication.session.runAuthenticatedOperation
import com.passbolt.mobile.android.feature.otp.scanotp.parser.OtpParseResult
import com.passbolt.mobile.android.resourcepicker.model.PickResourceAction
import com.passbolt.mobile.android.ui.ResourceModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber

class ScanOtpSuccessPresenter(
    private val createStandaloneTotpResourceInteractor: CreateStandaloneTotpResourceInteractor,
    private val secretInteractor: SecretInteractor,
    private val updateToLinkedTotpResourceInteractor: UpdateToLinkedTotpResourceInteractor,
    private val updateLocalResourceUseCase: UpdateLocalResourceUseCase,
    coroutineLaunchContext: CoroutineLaunchContext
) : BaseAuthenticatedPresenter<ScanOtpSuccessContract.View>(coroutineLaunchContext),
    ScanOtpSuccessContract.Presenter {

    override var view: ScanOtpSuccessContract.View? = null
    private lateinit var scannedTotp: OtpParseResult.OtpQr.TotpQr
    private val job = SupervisorJob()
    private val scope = CoroutineScope(job + coroutineLaunchContext.ui)

    override fun argsRetrieved(scannedTotp: OtpParseResult.OtpQr.TotpQr) {
        this.scannedTotp = scannedTotp
    }

    override fun createStandaloneOtpClick() {
        scope.launch {
            view?.showProgress()
            when (val result = runAuthenticatedOperation(needSessionRefreshFlow, sessionRefreshedFlow) {
                createStandaloneTotpResourceInteractor.execute(
                    createTotpResourceCommonCreateInput(),
                    createTotpResourceCustomCreateInput()
                )
            }) {
                is CreateResourceInteractor.Output.Failure<*> -> view?.showGenericError()
                is CreateResourceInteractor.Output.OpenPgpError -> view?.showEncryptionError(result.message)
                is CreateResourceInteractor.Output.PasswordExpired -> {
                    /* will not happen in BaseAuthenticatedPresenter */
                }
                is CreateResourceInteractor.Output.Success -> {
                    view?.navigateToOtpList(otpCreated = true)
                }
            }
            view?.hideProgress()
        }
    }

    override fun linkedResourceReceived(action: PickResourceAction, resource: ResourceModel) {
        scope.launch {
            view?.showProgress()

            when (val fetchedSecret = runAuthenticatedOperation(needSessionRefreshFlow, sessionRefreshedFlow) {
                secretInteractor.fetchAndDecrypt(resource.resourceId)
            }) {
                is SecretInteractor.Output.DecryptFailure -> {
                    Timber.e("Failed to decrypt secret during linking totp resource")
                    view?.showEncryptionError(fetchedSecret.error.message)
                }
                is SecretInteractor.Output.FetchFailure -> {
                    Timber.e("Failed to fetch secret during linking totp resource")
                    view?.showGenericError()
                }
                is SecretInteractor.Output.Success -> {
                    when (val editResourceResult =
                        runAuthenticatedOperation(needSessionRefreshFlow, sessionRefreshedFlow) {
                            updateToLinkedTotpResourceInteractor.execute(
                                createCommonLinkToTotpUpdateInput(resource),
                                createUpdateToLinkedTotpInput(resource.resourceTypeId, fetchedSecret.decryptedSecret)
                            )
                        }) {
                        is UpdateResourceInteractor.Output.Success -> {
                            updateLocalResourceUseCase.execute(
                                UpdateLocalResourceUseCase.Input(editResourceResult.resource)
                            )
                            view?.navigateToOtpList(otpCreated = true)
                        }
                        is UpdateResourceInteractor.Output.Failure<*> ->
                            view?.showError(editResourceResult.response.exception.message.orEmpty())
                        is UpdateResourceInteractor.Output.PasswordExpired -> {
                            /* will not happen in BaseAuthenticatedPresenter */
                        }
                        is UpdateResourceInteractor.Output.OpenPgpError ->
                            view?.showEncryptionError(editResourceResult.message)
                    }
                }
                is SecretInteractor.Output.Unauthorized -> {
                    /* will not happen in BaseAuthenticatedPresenter */
                }
            }
        }
        view?.hideProgress()
    }

    private fun createUpdateToLinkedTotpInput(
        resourceTypeId: String,
        decryptedSecret: ByteArray
    ) =
        UpdateToLinkedTotpResourceInteractor.UpdateToLinkedTotpInput(
            period = scannedTotp.period,
            digits = scannedTotp.digits,
            algorithm = scannedTotp.algorithm.name,
            secretKey = scannedTotp.secret,
            existingSecret = decryptedSecret,
            existingResourceTypeId = resourceTypeId,
            password = null,
            description = null
        )

    private fun createCommonLinkToTotpUpdateInput(resource: ResourceModel) =
        UpdateResourceInteractor.CommonInput(
            resourceId = resource.resourceId,
            resourceName = resource.name,
            resourceUsername = resource.username,
            resourceUri = resource.url,
            resourceParentFolderId = resource.folderId
        )

    override fun linkToResourceClick() {
        view?.navigateToResourcePicker()
    }

    private fun createTotpResourceCommonCreateInput() =
        CreateResourceInteractor.CommonInput(
            resourceName = scannedTotp.label,
            resourceUsername = null,
            resourceUri = scannedTotp.issuer,
            resourceParentFolderId = null
        )

    private fun createTotpResourceCustomCreateInput() =
        CreateStandaloneTotpResourceInteractor.CreateStandaloneTotpInput(
            period = scannedTotp.period,
            digits = scannedTotp.digits,
            algorithm = scannedTotp.algorithm.name,
            secretKey = scannedTotp.secret
        )
}
