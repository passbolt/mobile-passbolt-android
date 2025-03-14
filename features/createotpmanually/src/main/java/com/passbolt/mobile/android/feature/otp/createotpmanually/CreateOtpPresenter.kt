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
import com.passbolt.mobile.android.core.resources.actions.ResourceCreateActionsInteractor
import com.passbolt.mobile.android.core.resources.actions.ResourceUpdateActionResult
import com.passbolt.mobile.android.core.resources.actions.ResourceUpdateActionsInteractor
import com.passbolt.mobile.android.core.resources.actions.SecretPropertiesActionsInteractor
import com.passbolt.mobile.android.core.resources.actions.performResourceCreateAction
import com.passbolt.mobile.android.core.resources.actions.performResourceUpdateAction
import com.passbolt.mobile.android.core.resources.actions.performSecretPropertyAction
import com.passbolt.mobile.android.core.resources.usecase.db.GetLocalResourceUseCase
import com.passbolt.mobile.android.core.resourcetypes.usecase.db.ResourceTypeIdToSlugMappingProvider
import com.passbolt.mobile.android.resourcepicker.model.PickResourceAction
import com.passbolt.mobile.android.serializers.jsonschema.SchemaEntity
import com.passbolt.mobile.android.supportedresourceTypes.ContentType
import com.passbolt.mobile.android.supportedresourceTypes.ContentType.PasswordAndDescription
import com.passbolt.mobile.android.supportedresourceTypes.ContentType.PasswordDescriptionTotp
import com.passbolt.mobile.android.supportedresourceTypes.ContentType.PasswordString
import com.passbolt.mobile.android.supportedresourceTypes.ContentType.Totp
import com.passbolt.mobile.android.supportedresourceTypes.ContentType.V5Default
import com.passbolt.mobile.android.supportedresourceTypes.ContentType.V5DefaultWithTotp
import com.passbolt.mobile.android.supportedresourceTypes.ContentType.V5PasswordString
import com.passbolt.mobile.android.supportedresourceTypes.ContentType.V5TotpStandalone
import com.passbolt.mobile.android.ui.OtpAdvancedSettingsModel
import com.passbolt.mobile.android.ui.OtpParseResult
import com.passbolt.mobile.android.ui.OtpResourceModel
import com.passbolt.mobile.android.ui.ResourceModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koin.core.parameter.parametersOf
import java.util.UUID

class CreateOtpPresenter(
    private val getLocalResourceUseCase: GetLocalResourceUseCase,
    private val idToSlugMappingProvider: ResourceTypeIdToSlugMappingProvider,
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
                val slug = idToSlugMappingProvider.provideMappingForSelectedAccount()[
                    UUID.fromString(resource.resourceTypeId)
                ]

                when (val contentType = ContentType.fromSlug(slug!!)) {
                    is PasswordString, V5PasswordString -> {
                        setupEditForResourceWithoutTotp(resource, contentType)
                    }
                    is PasswordAndDescription, V5Default -> {
                        setupEditForResourceWithoutTotp(resource, contentType)
                        view?.showEditingValuesAlsoEditsResourceValuesWarning()
                    }
                    is Totp, V5TotpStandalone -> {
                        setupEditForResourceContainingTotp(resource, contentType)
                    }
                    is PasswordDescriptionTotp, V5DefaultWithTotp -> {
                        setupEditForResourceContainingTotp(resource, contentType)
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

    private suspend fun setupEditForResourceContainingTotp(resource: ResourceModel, contentType: ContentType) {
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
                        issuer = if (contentType.isV5()) {
                            resource.uris?.firstOrNull()
                        } else {
                            resource.uri
                        },
                        algorithm = it.result.algorithm,
                        digits = it.result.digits,
                        period = it.result.period
                    )
                )
            }
        )
    }

    private fun setupEditForResourceWithoutTotp(resource: ResourceModel, contentType: ContentType) {
        initScreenData(
            OtpResourceModel(
                resourceId = resource.resourceId,
                parentFolderId = resource.folderId,
                label = resource.name,
                secret = "",
                issuer = if (contentType.isV5()) {
                    resource.uris?.firstOrNull()
                } else {
                    resource.uri
                },
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
            val resourceCreateActionsInteractor = get<ResourceCreateActionsInteractor> {
                parametersOf(needSessionRefreshFlow, sessionRefreshedFlow)
            }
            performResourceCreateAction(
                action = {
                    resourceCreateActionsInteractor.createStandaloneTotpResource(
                        label = label,
                        resourceUsername = null,
                        issuer = issuer,
                        period = period,
                        digits = digits,
                        algorithm = algorithm,
                        secretKey = secret,
                        resourceParentFolderId = null
                    )
                },
                doOnFailure = { view?.showGenericError() },
                doOnCryptoFailure = { view?.showEncryptionError(it) },
                doOnSchemaValidationFailure = ::handleSchemaValidationFailure,
                doOnSuccess = { (_, resourceName) ->
                    view?.navigateBackInCreateFlow(resourceName, otpCreated = true)
                }
            )
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
        val slug = idToSlugMappingProvider.provideMappingForSelectedAccount()[
            UUID.fromString(resource.resourceTypeId)
        ]
        return when (val contentType = ContentType.fromSlug(slug!!)) {
            is PasswordAndDescription, V5Default ->
                resourceUpdateActionsInteractor.addTotpToResource(
                    overrideUri = issuer,
                    overrideName = label,
                    period = period,
                    digits = digits,
                    algorithm = algorithm,
                    secretKey = secret
                )
            is PasswordDescriptionTotp, V5DefaultWithTotp ->
                resourceUpdateActionsInteractor.updateLinkedTotpResourceTotpFields(
                    label = label,
                    issuer = issuer,
                    period = period,
                    digits = digits,
                    algorithm = algorithm,
                    secretKey = secret
                )
            is Totp, V5TotpStandalone ->
                resourceUpdateActionsInteractor.updateStandaloneTotpResource(
                    label = label,
                    issuer = issuer,
                    period = period,
                    digits = digits,
                    algorithm = algorithm,
                    secretKey = secret
                )
            else ->
                throw IllegalArgumentException("Unsupported resource type on manual totp form: $contentType")
        }
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
        coroutineScope.launch {
            view?.showProgress()

            val resourceUpdateActionsInteractor = get<ResourceUpdateActionsInteractor> {
                parametersOf(resource, needSessionRefreshFlow, sessionRefreshedFlow)
            }
            val updateOperation = idToSlugMappingProvider.provideMappingForSelectedAccount()[
                UUID.fromString(resource.resourceTypeId)
            ].let {
                when (val contentType = ContentType.fromSlug(it!!)) {
                    is PasswordString, V5PasswordString, Totp, V5TotpStandalone ->
                        throw IllegalArgumentException("These resource types are not possible to link")
                    is PasswordAndDescription, V5Default -> suspend {
                        resourceUpdateActionsInteractor.addTotpToResource(
                            overrideName = resource.name,
                            overrideUri = if (contentType.isV5()) {
                                resource.uris?.firstOrNull()
                            } else {
                                resource.uri
                            },
                            period = period,
                            digits = digits,
                            algorithm = algorithm,
                            secretKey = secret
                        )
                    }
                    is PasswordDescriptionTotp, V5DefaultWithTotp -> suspend {
                        resourceUpdateActionsInteractor.updateLinkedTotpResourceTotpFields(
                            label = resource.name,
                            issuer = if (contentType.isV5()) {
                                resource.uris?.firstOrNull()
                            } else {
                                resource.uri
                            },
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

    override fun detach() {
        coroutineScope.coroutineContext.cancelChildren()
        super<BaseAuthenticatedPresenter>.detach()
    }

    private companion object {
        private const val LABEL_MAX_LENGTH = 255
        private const val ISSUER_MAX_LENGTH = 255
        private const val SECRET_MAX_LENGTH = 1024
    }
}
