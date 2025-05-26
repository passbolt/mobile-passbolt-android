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

package com.passbolt.mobile.android.feature.otp.scanotp.scanotpsuccess

import com.passbolt.mobile.android.core.mvp.authentication.BaseAuthenticatedPresenter
import com.passbolt.mobile.android.core.mvp.coroutinecontext.CoroutineLaunchContext
import com.passbolt.mobile.android.core.resources.actions.ResourceCreateActionsInteractor
import com.passbolt.mobile.android.core.resources.actions.ResourceUpdateActionsInteractor
import com.passbolt.mobile.android.core.resources.actions.performResourceCreateAction
import com.passbolt.mobile.android.core.resources.actions.performResourceUpdateAction
import com.passbolt.mobile.android.core.resources.usecase.GetDefaultCreateContentTypeUseCase
import com.passbolt.mobile.android.core.resourcetypes.graph.redesigned.UpdateAction
import com.passbolt.mobile.android.core.resourcetypes.usecase.db.ResourceTypeIdToSlugMappingProvider
import com.passbolt.mobile.android.core.secrets.usecase.decrypt.parser.SecretJsonModel
import com.passbolt.mobile.android.feature.authentication.session.runAuthenticatedOperation
import com.passbolt.mobile.android.jsonmodel.delegates.TotpSecret
import com.passbolt.mobile.android.metadata.interactor.MetadataPrivateKeysHelperInteractor
import com.passbolt.mobile.android.resourcepicker.model.PickResourceAction
import com.passbolt.mobile.android.serializers.jsonschema.SchemaEntity
import com.passbolt.mobile.android.supportedresourceTypes.ContentType
import com.passbolt.mobile.android.supportedresourceTypes.ContentType.PasswordAndDescription
import com.passbolt.mobile.android.supportedresourceTypes.ContentType.PasswordDescriptionTotp
import com.passbolt.mobile.android.supportedresourceTypes.ContentType.V5Default
import com.passbolt.mobile.android.supportedresourceTypes.ContentType.V5DefaultWithTotp
import com.passbolt.mobile.android.ui.LeadingContentType
import com.passbolt.mobile.android.ui.MetadataJsonModel
import com.passbolt.mobile.android.ui.NewMetadataKeyToTrustModel
import com.passbolt.mobile.android.ui.OtpParseResult
import com.passbolt.mobile.android.ui.ResourceModel
import com.passbolt.mobile.android.ui.TrustedKeyDeletedModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koin.core.parameter.parametersOf
import timber.log.Timber
import java.util.UUID

class ScanOtpSuccessPresenter(
    private val idToSlugMappingProvider: ResourceTypeIdToSlugMappingProvider,
    private val getDefaultCreateContentTypeUseCase: GetDefaultCreateContentTypeUseCase,
    private val metadataPrivateKeysHelperInteractor: MetadataPrivateKeysHelperInteractor,
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
            val resourceCreateActionsInteractor = get<ResourceCreateActionsInteractor> {
                parametersOf(needSessionRefreshFlow, sessionRefreshedFlow)
            }
            val defaultType = getDefaultCreateContentTypeUseCase.execute(
                GetDefaultCreateContentTypeUseCase.Input(LeadingContentType.TOTP)
            )

            performResourceCreateAction(
                action = {
                    resourceCreateActionsInteractor.createGenericResource(
                        resourceParentFolderId = null,
                        contentType = defaultType.contentType,
                        metadataJsonModel = MetadataJsonModel.empty().apply {
                            name = scannedTotp.label
                            scannedTotp.issuer?.let {
                                setMainUri(defaultType.contentType, it)
                            }
                        },
                        secretJsonModel = SecretJsonModel.emptyTotp().apply {
                            totp = TotpSecret(
                                algorithm = scannedTotp.algorithm.name,
                                key = scannedTotp.secret,
                                period = scannedTotp.period,
                                digits = scannedTotp.digits
                            )
                        }
                    )
                },
                doOnFailure = { view?.showGenericError() },
                doOnCryptoFailure = { view?.showEncryptionError(it) },
                doOnSchemaValidationFailure = ::handleSchemaValidationFailure,
                doOnSuccess = { view?.navigateToOtpList(scannedTotp, otpCreated = true) },
                doOnCannotCreateWithCurrentConfig = { view?.showCannotUpdateTotpWithCurrentConfig() },
                doOnMetadataKeyModified = { view?.showMetadataKeyModifiedDialog(it) },
                doOnMetadataKeyDeleted = { view?.showMetadataKeyDeletedDialog(it) },
                doOnMetadataKeyVerificationFailure = { view?.showFailedToVerifyMetadataKey() }
            )
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

            val slug = idToSlugMappingProvider.provideMappingForSelectedAccount()[
                UUID.fromString(resource.resourceTypeId)
            ]
            val resourceUpdateActionsInteractor = get<ResourceUpdateActionsInteractor> {
                parametersOf(resource, needSessionRefreshFlow, sessionRefreshedFlow)
            }
            val updateOperation =
                when (ContentType.fromSlug(slug!!)) {
                    is PasswordAndDescription, V5Default -> suspend {
                        resourceUpdateActionsInteractor.updateGenericResource(
                            UpdateAction.ADD_TOTP,
                            secretModification = {
                                it.apply {
                                    totp = TotpSecret(
                                        algorithm = scannedTotp.algorithm.name,
                                        key = scannedTotp.secret,
                                        period = scannedTotp.period,
                                        digits = scannedTotp.digits
                                    )
                                }
                            }
                        )
                    }
                    is PasswordDescriptionTotp, V5DefaultWithTotp -> suspend {
                        resourceUpdateActionsInteractor.updateGenericResource(
                            UpdateAction.ADD_TOTP,
                            secretModification = {
                                it.apply {
                                    totp = TotpSecret(
                                        algorithm = scannedTotp.algorithm.name,
                                        key = scannedTotp.secret,
                                        period = scannedTotp.period,
                                        digits = scannedTotp.digits
                                    )
                                }
                            }
                        )
                    }
                    else ->
                        throw IllegalArgumentException("$slug resource type is not possible to link")
                }
            performResourceUpdateAction(
                action = updateOperation,
                doOnFailure = { view?.showGenericError() },
                doOnFetchFailure = { view?.showGenericError() },
                doOnCryptoFailure = { view?.showEncryptionError(it) },
                doOnSchemaValidationFailure = ::handleSchemaValidationFailure,
                doOnSuccess = { view?.navigateToOtpList(totp = scannedTotp, otpCreated = true) },
                doOnCannotEditWithCurrentConfig = { view?.showCannotUpdateTotpWithCurrentConfig() },
                doOnMetadataKeyModified = { view?.showMetadataKeyModifiedDialog(it) },
                doOnMetadataKeyDeleted = { view?.showMetadataKeyDeletedDialog(it) },
                doOnMetadataKeyVerificationFailure = { view?.showFailedToVerifyMetadataKey() }
            )

            view?.hideProgress()
        }
    }

    override fun linkToResourceClick() {
        view?.navigateToResourcePicker()
    }

    override fun detach() {
        scope.coroutineContext.cancelChildren()
        super<BaseAuthenticatedPresenter>.detach()
    }

    override fun trustedMetadataKeyDeleted(model: TrustedKeyDeletedModel) {
        scope.launch {
            metadataPrivateKeysHelperInteractor.deletedTrustedMetadataPrivateKey()
        }
    }

    override fun trustNewMetadataKey(model: NewMetadataKeyToTrustModel) {
        scope.launch {
            view?.showProgress()
            when (val output = runAuthenticatedOperation(needSessionRefreshFlow, sessionRefreshedFlow) {
                metadataPrivateKeysHelperInteractor.trustNewKey(model)
            }) {
                is MetadataPrivateKeysHelperInteractor.Output.Success ->
                    view?.showNewMetadataKeyIsTrusted()
                else -> {
                    Timber.e("Failed to trust new metadata key: $output")
                    view?.showFailedToTrustMetadataKey()
                }
            }
            view?.hideProgress()
        }
    }
}
