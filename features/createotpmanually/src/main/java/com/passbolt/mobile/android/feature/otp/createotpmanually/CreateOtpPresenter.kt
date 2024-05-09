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
import com.passbolt.mobile.android.core.resources.actions.ResourceUpdateActionResult
import com.passbolt.mobile.android.core.resources.actions.ResourceUpdateActionsInteractor
import com.passbolt.mobile.android.core.resources.actions.SecretPropertiesActionsInteractor
import com.passbolt.mobile.android.core.resources.actions.performResourceUpdateAction
import com.passbolt.mobile.android.core.resources.actions.performSecretPropertyAction
import com.passbolt.mobile.android.core.resources.interactor.create.CreateResourceInteractor
import com.passbolt.mobile.android.core.resources.interactor.create.CreateResourceInteractor.Output.JsonSchemaValidationFailure
import com.passbolt.mobile.android.core.resources.interactor.create.CreateStandaloneTotpResourceInteractor
import com.passbolt.mobile.android.core.resources.usecase.db.GetLocalResourceUseCase
import com.passbolt.mobile.android.core.resourcetypes.ResourceTypeFactory
import com.passbolt.mobile.android.core.resourcetypes.ResourceTypeFactory.ResourceTypeEnum.PASSWORD_DESCRIPTION_TOTP
import com.passbolt.mobile.android.core.resourcetypes.ResourceTypeFactory.ResourceTypeEnum.PASSWORD_WITH_DESCRIPTION
import com.passbolt.mobile.android.core.resourcetypes.ResourceTypeFactory.ResourceTypeEnum.SIMPLE_PASSWORD
import com.passbolt.mobile.android.core.resourcetypes.ResourceTypeFactory.ResourceTypeEnum.STANDALONE_TOTP
import com.passbolt.mobile.android.feature.authentication.session.runAuthenticatedOperation
import com.passbolt.mobile.android.feature.otp.scanotp.parser.OtpParseResult
import com.passbolt.mobile.android.resourcepicker.model.PickResourceAction
import com.passbolt.mobile.android.serializers.jsonschema.SchemaEntity
import com.passbolt.mobile.android.ui.OtpAdvancedSettingsModel
import com.passbolt.mobile.android.ui.OtpResourceModel
import com.passbolt.mobile.android.ui.ResourceModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koin.core.parameter.parametersOf

class CreateOtpPresenter(
    private val createStandaloneTotpResourceInteractor: CreateStandaloneTotpResourceInteractor,
    private val getLocalResourceUseCase: GetLocalResourceUseCase,
    private val resourceTypeFactory: ResourceTypeFactory,
    coroutineLaunchContext: CoroutineLaunchContext
) : BaseAuthenticatedPresenter<CreateOtpContract.View>(coroutineLaunchContext), CreateOtpContract.Presenter,
    KoinComponent {

    override var view: CreateOtpContract.View? = null
    private val job = SupervisorJob()
    private val coroutineScope = CoroutineScope(job + coroutineLaunchContext.ui)

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

    override fun argsRetrieved(editedOtpResourceId: String?) {
        coroutineScope.launch {
            view?.showProgress()
            if (editedOtpResourceId != null && !argsConsumed) {
                val resource = getLocalResourceUseCase.execute(GetLocalResourceUseCase.Input(editedOtpResourceId))
                    .resource

                when (resourceTypeFactory.getResourceTypeEnum(resource.resourceTypeId)) {
                    SIMPLE_PASSWORD -> {
                        setupEditForResourceWithoutTotp(resource)
                    }
                    PASSWORD_WITH_DESCRIPTION -> {
                        setupEditForResourceWithoutTotp(resource)
                        view?.showEditingValuesAlsoEditsResourceValuesWarning()
                    }
                    STANDALONE_TOTP -> {
                        setupEditForResourceContainingTotp(resource)
                    }
                    PASSWORD_DESCRIPTION_TOTP -> {
                        setupEditForResourceContainingTotp(resource)
                        view?.showEditingValuesAlsoEditsResourceValuesWarning()
                    }
                }
                argsConsumed = true
            }

            if (editedOtpResourceId == null) {
                view?.setupCreateUi()
            } else {
                view?.setupEditUi()
            }
            view?.setFormValues(label, issuer, secret)
            view?.hideProgress()
        }
    }

    private suspend fun setupEditForResourceContainingTotp(resource: ResourceModel) {
        val secretPropertiesActionsInteractor = get<SecretPropertiesActionsInteractor> {
            parametersOf(resource, needSessionRefreshFlow, sessionRefreshedFlow)
        }
        performSecretPropertyAction(
            action = { secretPropertiesActionsInteractor.provideOtp() },
            doOnDecryptionFailure = { view?.showDecryptionError() },
            doOnFetchFailure = { view?.showFetchError() },
            doOnSuccess = {
                initScreenData(
                    OtpResourceModel(
                        resourceId = resource.resourceId,
                        parentFolderId = resource.folderId,
                        label = resource.name,
                        secret = it.result.key,
                        issuer = resource.url,
                        algorithm = it.result.algorithm,
                        digits = it.result.digits,
                        period = it.result.period
                    )
                )
            }
        )
    }

    private fun setupEditForResourceWithoutTotp(resource: ResourceModel) {
        initScreenData(
            OtpResourceModel(
                resourceId = resource.resourceId,
                parentFolderId = resource.folderId,
                label = resource.name,
                secret = "",
                issuer = resource.url,
                algorithm = OtpParseResult.OtpQr.Algorithm.DEFAULT.name,
                digits = OtpParseResult.OtpQr.TotpQr.DEFAULT_DIGITS,
                period = OtpParseResult.OtpQr.TotpQr.DEFAULT_PERIOD_SECONDS
            )
        )
    }

    private fun initScreenData(model: OtpResourceModel) {
        algorithm = model.algorithm
        period = model.period
        digits = model.digits
        secret = model.secret
        label = model.label
        issuer = model.issuer.orEmpty()
        editedOtpData = model
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
        coroutineScope.launch {
            view?.showProgress()
            when (val result = runAuthenticatedOperation(needSessionRefreshFlow, sessionRefreshedFlow) {
                createStandaloneTotpResourceInteractor.execute(
                    createCommonStandaloneTotpResourceCreateInput(),
                    createStandaloneTotpResourceCreateInput()
                )
            }) {
                is CreateResourceInteractor.Output.Failure<*> -> view?.showGenericError()
                is CreateResourceInteractor.Output.OpenPgpError -> view?.showEncryptionError(result.message)
                is CreateResourceInteractor.Output.PasswordExpired -> {
                    /* will not happen in BaseAuthenticatedPresenter */
                }
                is CreateResourceInteractor.Output.Success -> {
                    view?.navigateBackInCreateFlow(
                        result.resource.resourceModel.name,
                        otpCreated = true
                    )
                }
                is JsonSchemaValidationFailure -> handleSchemaValidationFailure(result.entity)
            }
            view?.hideProgress()
        }
    }

    private fun editTotp() {
        coroutineScope.launch {
            view?.showProgress()

            val resource = getLocalResourceUseCase.execute(GetLocalResourceUseCase.Input(editedOtpData!!.resourceId))
                .resource
            performResourceUpdateAction(
                action = { getEditOperation(resource) },
                doOnCryptoFailure = { view?.showEncryptionError(it) },
                doOnFetchFailure = { view?.showFetchError() },
                doOnFailure = { view?.showError(it) },
                doOnSchemaValidationFailure = ::handleSchemaValidationFailure,
                doOnSuccess = {
                    view?.navigateBackInUpdateFlow(it.resourceName, otpUpdated = true)
                }
            )

            view?.hideProgress()
        }
    }

    private suspend fun getEditOperation(
        resource: ResourceModel
    ): Flow<ResourceUpdateActionResult> {
        val resourceUpdateActionsInteractor = get<ResourceUpdateActionsInteractor> {
            parametersOf(resource, needSessionRefreshFlow, sessionRefreshedFlow)
        }
        return when (val resourceTypeEnum = resourceTypeFactory.getResourceTypeEnum(resource.resourceTypeId)) {
            SIMPLE_PASSWORD -> throw IllegalArgumentException(
                "Unsupported resource type on manual totp form:" +
                        " $resourceTypeEnum"
            )
            PASSWORD_WITH_DESCRIPTION ->
                resourceUpdateActionsInteractor.addTotpToResource(
                    overrideUri = issuer,
                    overrideName = label,
                    period = period,
                    digits = digits,
                    algorithm = algorithm,
                    secretKey = secret
                )
            PASSWORD_DESCRIPTION_TOTP ->
                resourceUpdateActionsInteractor.updateLinkedTotpResourceTotpFields(
                    label = label,
                    issuer = issuer,
                    period = period,
                    digits = digits,
                    algorithm = algorithm,
                    secretKey = secret
                )
            STANDALONE_TOTP ->
                resourceUpdateActionsInteractor.updateStandaloneTotpResource(
                    label = label,
                    issuer = issuer,
                    period = period,
                    digits = digits,
                    algorithm = algorithm,
                    secretKey = secret
                )
        }
    }

    // creates standalone totp resource input
    private fun createStandaloneTotpResourceCreateInput() =
        CreateStandaloneTotpResourceInteractor.CreateStandaloneTotpInput(
            period = period,
            digits = digits,
            algorithm = algorithm,
            secretKey = secret
        )

    // creates standalone totp common input
    private fun createCommonStandaloneTotpResourceCreateInput() =
        CreateResourceInteractor.CommonInput(
            resourceName = label,
            resourceUsername = null,
            resourceUri = issuer,
            resourceParentFolderId = null
        )

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
        coroutineScope.launch {
            view?.showProgress()

            val resourceUpdateActionsInteractor = get<ResourceUpdateActionsInteractor> {
                parametersOf(resource, needSessionRefreshFlow, sessionRefreshedFlow)
            }
            val updateOperation = resourceTypeFactory.getResourceTypeEnum(resource.resourceTypeId).let {
                when (it) {
                    SIMPLE_PASSWORD, STANDALONE_TOTP ->
                        throw IllegalArgumentException("These resource types are not possible to link")
                    PASSWORD_WITH_DESCRIPTION -> suspend {
                        resourceUpdateActionsInteractor.addTotpToResource(
                            overrideName = resource.name,
                            overrideUri = resource.url,
                            period = period,
                            digits = digits,
                            algorithm = algorithm,
                            secretKey = secret
                        )
                    }
                    PASSWORD_DESCRIPTION_TOTP -> suspend {
                        resourceUpdateActionsInteractor.updateLinkedTotpResourceTotpFields(
                            label = resource.name,
                            issuer = resource.url,
                            period = period,
                            digits = digits,
                            algorithm = algorithm,
                            secretKey = secret
                        )
                    }
                }
            }
            performResourceUpdateAction(
                action = updateOperation,
                doOnCryptoFailure = { view?.showEncryptionError(it) },
                doOnFetchFailure = { view?.showGenericError() },
                doOnFailure = { view?.showError(it) },
                doOnSchemaValidationFailure = ::handleSchemaValidationFailure,
                doOnSuccess = {
                    view?.navigateBackInCreateFlow(
                        resourceName = it.resourceName,
                        otpCreated = true
                    )
                }
            )
        }
        view?.hideProgress()
    }

    private fun handleSchemaValidationFailure(entity: SchemaEntity) {
        when (entity) {
            SchemaEntity.RESOURCE -> view?.showJsonResourceSchemaValidationError()
            SchemaEntity.SECRET -> view?.showJsonSecretSchemaValidationError()
        }
    }

    override fun linkToResourceClick() {
        validateFields {
            view?.navigateToResourcePicker(issuer)
        }
    }

    private companion object {
        private const val LABEL_MAX_LENGTH = 255
        private const val ISSUER_MAX_LENGTH = 255
        private const val SECRET_MAX_LENGTH = 1024
    }
}
