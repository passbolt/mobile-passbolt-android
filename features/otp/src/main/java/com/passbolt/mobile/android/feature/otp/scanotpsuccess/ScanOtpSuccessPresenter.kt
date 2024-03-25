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
import com.passbolt.mobile.android.core.resources.actions.ResourceUpdateActionsInteractor
import com.passbolt.mobile.android.core.resources.actions.performResourceUpdateAction
import com.passbolt.mobile.android.core.resources.interactor.create.CreateResourceInteractor
import com.passbolt.mobile.android.core.resources.interactor.create.CreateStandaloneTotpResourceInteractor
import com.passbolt.mobile.android.core.resourcetypes.ResourceTypeFactory
import com.passbolt.mobile.android.core.resourcetypes.ResourceTypeFactory.ResourceTypeEnum.PASSWORD_DESCRIPTION_TOTP
import com.passbolt.mobile.android.core.resourcetypes.ResourceTypeFactory.ResourceTypeEnum.PASSWORD_WITH_DESCRIPTION
import com.passbolt.mobile.android.core.resourcetypes.ResourceTypeFactory.ResourceTypeEnum.SIMPLE_PASSWORD
import com.passbolt.mobile.android.core.resourcetypes.ResourceTypeFactory.ResourceTypeEnum.STANDALONE_TOTP
import com.passbolt.mobile.android.feature.authentication.session.runAuthenticatedOperation
import com.passbolt.mobile.android.feature.otp.scanotp.parser.OtpParseResult
import com.passbolt.mobile.android.resourcepicker.model.PickResourceAction
import com.passbolt.mobile.android.serializers.jsonschema.SchemaEntity
import com.passbolt.mobile.android.ui.ResourceModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koin.core.parameter.parametersOf

class ScanOtpSuccessPresenter(
    private val createStandaloneTotpResourceInteractor: CreateStandaloneTotpResourceInteractor,
    private val resourceTypeFactory: ResourceTypeFactory,
    coroutineLaunchContext: CoroutineLaunchContext
) : BaseAuthenticatedPresenter<ScanOtpSuccessContract.View>(coroutineLaunchContext),
    ScanOtpSuccessContract.Presenter, KoinComponent {

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
                is CreateResourceInteractor.Output.Failure<*> ->
                    view?.showGenericError()
                is CreateResourceInteractor.Output.OpenPgpError ->
                    view?.showEncryptionError(result.message)
                is CreateResourceInteractor.Output.PasswordExpired -> {
                    /* will not happen in BaseAuthenticatedPresenter */
                }
                is CreateResourceInteractor.Output.Success -> {
                    view?.navigateToOtpList(otpCreated = true)
                }
                is CreateResourceInteractor.Output.JsonSchemaValidationFailure ->
                    handleSchemaValidationFailure(result.entity)
            }
            view?.hideProgress()
        }
    }

    private fun handleSchemaValidationFailure(entity: SchemaEntity) {
        when (entity) {
            SchemaEntity.RESOURCE -> view?.showJsonResourceSchemaValidationError()
            SchemaEntity.SECRET -> view?.showJsonSecretSchemaValidationError()
        }
    }

    override fun linkedResourceReceived(action: PickResourceAction, resource: ResourceModel) {
        scope.launch {
            view?.showProgress()

            val resourceUpdateActionsInteractor = get<ResourceUpdateActionsInteractor> {
                parametersOf(resource, needSessionRefreshFlow, sessionRefreshedFlow)
            }
            val updateOperation =
                when (resourceTypeFactory.getResourceTypeEnum(resource.resourceTypeId)) {
                    SIMPLE_PASSWORD, STANDALONE_TOTP ->
                        throw IllegalArgumentException("These resource types are not possible to link")
                    PASSWORD_WITH_DESCRIPTION -> suspend {
                        resourceUpdateActionsInteractor.addTotpToResource(
                            overrideName = resource.name,
                            overrideUri = resource.url,
                            period = scannedTotp.period,
                            digits = scannedTotp.digits,
                            algorithm = scannedTotp.algorithm.name,
                            secretKey = scannedTotp.secret
                        )
                    }
                    PASSWORD_DESCRIPTION_TOTP -> suspend {
                        resourceUpdateActionsInteractor.updateLinkedTotpResourceTotpFields(
                            label = resource.name,
                            issuer = resource.url,
                            period = scannedTotp.period,
                            digits = scannedTotp.digits,
                            algorithm = scannedTotp.algorithm.name,
                            secretKey = scannedTotp.secret
                        )
                    }
                }
            performResourceUpdateAction(
                action = updateOperation,
                doOnFailure = { view?.showGenericError() },
                doOnFetchFailure = { view?.showGenericError() },
                doOnCryptoFailure = { view?.showEncryptionError(it) },
                doOnSchemaValidationFailure = ::handleSchemaValidationFailure,
                doOnSuccess = { view?.navigateToOtpList(otpCreated = true) }
            )

            view?.hideProgress()
        }
    }

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
