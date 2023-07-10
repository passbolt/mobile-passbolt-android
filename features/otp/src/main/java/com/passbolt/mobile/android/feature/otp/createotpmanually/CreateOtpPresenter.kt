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

package com.passbolt.mobile.android.feature.otp.createotpmanually

import com.passbolt.mobile.android.common.validation.StringMaxLength
import com.passbolt.mobile.android.common.validation.StringNotBlank
import com.passbolt.mobile.android.common.validation.validation
import com.passbolt.mobile.android.core.mvp.authentication.BaseAuthenticatedPresenter
import com.passbolt.mobile.android.core.mvp.coroutinecontext.CoroutineLaunchContext
import com.passbolt.mobile.android.core.resources.interactor.UpdateResourceInteractor
import com.passbolt.mobile.android.core.resources.interactor.UpdateStandaloneTotpResourceInteractor
import com.passbolt.mobile.android.core.resources.interactor.UpdateToLinkedTotpResourceInteractor
import com.passbolt.mobile.android.core.resources.usecase.CreateStandaloneTotpResourceUseCase
import com.passbolt.mobile.android.core.resourcetypes.ResourceTypeFactory
import com.passbolt.mobile.android.core.secrets.usecase.decrypt.SecretInteractor
import com.passbolt.mobile.android.database.impl.resources.UpdateLocalResourceUseCase
import com.passbolt.mobile.android.database.impl.resourcetypes.GetResourceTypeWithFieldsBySlugUseCase
import com.passbolt.mobile.android.feature.authentication.session.runAuthenticatedOperation
import com.passbolt.mobile.android.feature.otp.scanotp.parser.OtpParseResult
import com.passbolt.mobile.android.resourcepicker.model.PickResourceAction
import com.passbolt.mobile.android.ui.OtpAdvancedSettingsModel
import com.passbolt.mobile.android.ui.OtpResourceModel
import com.passbolt.mobile.android.ui.ResourceModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber

class CreateOtpPresenter(
    private val createStandaloneTotpResourceUseCase: CreateStandaloneTotpResourceUseCase,
    private val updateStandaloneTotpResourceInteractor: UpdateStandaloneTotpResourceInteractor,
    private val getResourceTypeWithFieldsBySlugUseCase: GetResourceTypeWithFieldsBySlugUseCase,
    private val updateLocalResourceUseCase: UpdateLocalResourceUseCase,
    private val updateToLinkedTotpResourceInteractor: UpdateToLinkedTotpResourceInteractor,
    private val secretInteractor: SecretInteractor,
    coroutineLaunchContext: CoroutineLaunchContext
) : BaseAuthenticatedPresenter<CreateOtpContract.View>(coroutineLaunchContext), CreateOtpContract.Presenter {

    override var view: CreateOtpContract.View? = null
    private val job = SupervisorJob()
    private val scope = CoroutineScope(job + coroutineLaunchContext.ui)
    private var algorithm = OtpParseResult.OtpQr.Algorithm.DEFAULT.name

    private var period = OtpParseResult.OtpQr.TotpQr.DEFAULT_PERIOD_SECONDS
    private var digits = OtpParseResult.OtpQr.TotpQr.DEFAULT_DIGITS
    private var secret = ""
    private var label = ""
    private var issuer = ""

    private var editedOtpData: OtpResourceModel? = null

    // prevents restoring data from editedOtpData when restored->edited->navigated to advanced->navigated back
    private var argsConsumed = false

    override fun attach(view: CreateOtpContract.View) {
        super<BaseAuthenticatedPresenter>.attach(view)
        view.setValues(label, issuer, secret)
    }

    override fun argsRetrieved(editedOtpData: OtpResourceModel?) {
        this.editedOtpData = editedOtpData
        if (editedOtpData != null && !argsConsumed) {
            algorithm = editedOtpData.algorithm
            period = editedOtpData.period
            digits = editedOtpData.digits
            secret = editedOtpData.secret
            label = editedOtpData.label
            issuer = editedOtpData.issuer.orEmpty()
            argsConsumed = true
        }
        if (editedOtpData == null) {
            view?.setupCreateUi()
        } else {
            view?.setupEditUi()
        }
        view?.setFormValues(label, issuer, secret)
    }

    override fun otpSettingsModified(algorithm: String, period: Long, digits: Int) {
        this.algorithm = algorithm
        this.period = period
        this.digits = digits
    }

    override fun totpLabelChanged(label: String) {
        this.label = label
    }

    override fun totpSecretChanged(secret: String) {
        this.secret = secret
    }

    override fun totpIssuerChanged(issuer: String) {
        this.issuer = issuer
    }

    override fun mainButtonClick() {
        validateFields {
            if (editedOtpData == null) {
                createTotp()
            } else {
                editTotp()
            }
        }
    }

    private fun validateFields(onValid: () -> Unit) {
        validation {
            of(label) {
                withRules(StringNotBlank, StringMaxLength(LABEL_MAX_LENGTH)) {
                    onInvalid { view?.showLabelValidationError(LABEL_MAX_LENGTH) }
                }
            }
            of(issuer) {
                withRules(StringMaxLength(ISSUER_MAX_LENGTH)) {
                    onInvalid { view?.showIssuerValidationError(ISSUER_MAX_LENGTH) }
                }
            }
            of(secret) {
                withRules(StringNotBlank, StringMaxLength(SECRET_MAX_LENGTH)) {
                    onInvalid { view?.showSecretValidationError(SECRET_MAX_LENGTH) }
                }
            }
            onValid(onValid)
        }
    }

    private fun createTotp() {
        scope.launch {
            view?.showProgress()
            when (val result = runAuthenticatedOperation(needSessionRefreshFlow, sessionRefreshedFlow) {
                createStandaloneTotpResourceUseCase.execute(createStandaloneTotpResourceInput())
            }) {
                is CreateStandaloneTotpResourceUseCase.Output.Failure<*> -> view?.showGenericError()
                is CreateStandaloneTotpResourceUseCase.Output.OpenPgpError -> view?.showEncryptionError(result.message)
                is CreateStandaloneTotpResourceUseCase.Output.PasswordExpired -> {
                    /* will not happen in BaseAuthenticatedPresenter */
                }
                is CreateStandaloneTotpResourceUseCase.Output.Success -> {
                    view?.navigateToOtpListInCreateFlow(otpCreated = true)
                }
            }
            view?.hideProgress()
        }
    }

    private fun editTotp() {
        scope.launch {
            view?.showProgress()
            val existingResource = requireNotNull(editedOtpData) { "In edit mode but existing resource not present" }

            when (val editResourceResult =
                runAuthenticatedOperation(needSessionRefreshFlow, sessionRefreshedFlow) {
                    updateStandaloneTotpResourceInteractor.execute(
                        createCommonTotpUpdateInput(existingResource.resourceId, existingResource.parentFolderId),
                        createStandaloneTotpUpdateInput()
                    )
                }) {
                is UpdateResourceInteractor.Output.Failure<*> -> {
                    view?.showError(editResourceResult.response.exception.message.orEmpty())
                }
                is UpdateResourceInteractor.Output.OpenPgpError -> {
                    view?.showEncryptionError(editResourceResult.message)
                }
                is UpdateResourceInteractor.Output.PasswordExpired -> {
                    /* will not happen in BaseAuthenticatedPresenter */
                }
                is UpdateResourceInteractor.Output.Success -> {
                    updateLocalResourceUseCase.execute(
                        UpdateLocalResourceUseCase.Input(editResourceResult.resource)
                    )
                    view?.navigateToOtpListInUpdateFlow(otpUpdated = true)
                }
            }

            view?.hideProgress()
        }
    }

    private fun createStandaloneTotpUpdateInput() =
        UpdateStandaloneTotpResourceInteractor.UpdateStandaloneTotpInput(
            period = period,
            digits = digits,
            algorithm = algorithm,
            secretKey = secret
        )

    // updates standalone totp
    private fun createCommonTotpUpdateInput(resourceId: String, resourceParentFolderId: String?) =
        UpdateResourceInteractor.CommonInput(
            resourceId = resourceId,
            resourceName = label,
            resourceUsername = null,
            resourceUri = issuer,
            resourceParentFolderId = resourceParentFolderId
        )

    // updates existing resource to linked totp resource
    private fun createCommonLinkToTotpUpdateInput(resource: ResourceModel) =
        UpdateResourceInteractor.CommonInput(
            resourceId = resource.resourceId,
            resourceName = resource.name,
            resourceUsername = resource.username,
            resourceUri = resource.url,
            resourceParentFolderId = resource.folderId
        )

    private suspend fun createStandaloneTotpResourceInput(): CreateStandaloneTotpResourceUseCase.Input {
        val totpResourceType = getResourceTypeWithFieldsBySlugUseCase.execute(
            GetResourceTypeWithFieldsBySlugUseCase.Input(ResourceTypeFactory.SLUG_TOTP)
        )
        return CreateStandaloneTotpResourceUseCase.Input(
            resourceTypeId = totpResourceType.resourceTypeId,
            issuer = issuer,
            label = label,
            period = period,
            digits = digits,
            algorithm = algorithm,
            secretKey = secret
        )
    }

    override fun advancedSettingsClick() {
        view?.navigateToCreateOtpAdvancedSettings(
            OtpAdvancedSettingsModel(
                period = period,
                algorithm = algorithm,
                digits = digits
            )
        )
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
                            view?.navigateToOtpListInCreateFlow(otpCreated = true)
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

    private fun createUpdateToLinkedTotpInput(existingResourceTypeId: String, fetchedSecret: ByteArray) =
        UpdateToLinkedTotpResourceInteractor.UpdateToLinkedTotpInput(
            period = period,
            digits = digits,
            algorithm = algorithm,
            secretKey = secret,
            existingSecret = fetchedSecret,
            existingResourceTypeId = existingResourceTypeId
        )

    override fun linkToResourceClick() {
        validateFields {
            view?.navigateToResourcePicker(label)
        }
    }

    private companion object {
        private const val LABEL_MAX_LENGTH = 255
        private const val ISSUER_MAX_LENGTH = 255
        private const val SECRET_MAX_LENGTH = 1024
    }
}
