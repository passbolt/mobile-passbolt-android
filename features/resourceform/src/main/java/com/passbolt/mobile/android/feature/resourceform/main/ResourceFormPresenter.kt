package com.passbolt.mobile.android.feature.resourceform.main

import com.passbolt.mobile.android.core.fulldatarefresh.DataRefreshStatus
import com.passbolt.mobile.android.core.fulldatarefresh.FullDataRefreshExecutor
import com.passbolt.mobile.android.core.mvp.authentication.BaseAuthenticatedPresenter
import com.passbolt.mobile.android.core.mvp.coroutinecontext.CoroutineLaunchContext
import com.passbolt.mobile.android.core.passwordgenerator.SecretGenerator
import com.passbolt.mobile.android.core.passwordgenerator.codepoints.toCodepoints
import com.passbolt.mobile.android.core.passwordgenerator.entropy.EntropyCalculator
import com.passbolt.mobile.android.core.policies.usecase.GetPasswordPoliciesUseCase
import com.passbolt.mobile.android.core.resources.actions.ResourceCreateActionsInteractor
import com.passbolt.mobile.android.core.resources.actions.ResourceUpdateActionsInteractor
import com.passbolt.mobile.android.core.resources.actions.performResourceCreateAction
import com.passbolt.mobile.android.core.resources.actions.performResourceUpdateAction
import com.passbolt.mobile.android.core.resources.usecase.db.GetLocalResourceUseCase
import com.passbolt.mobile.android.core.resourcetypes.graph.redesigned.UpdateAction.ADD_METADATA_DESCRIPTION
import com.passbolt.mobile.android.core.resourcetypes.graph.redesigned.UpdateAction.ADD_PASSWORD
import com.passbolt.mobile.android.core.resourcetypes.graph.redesigned.UpdateAction.ADD_NOTE
import com.passbolt.mobile.android.core.resourcetypes.graph.redesigned.UpdateAction.ADD_TOTP
import com.passbolt.mobile.android.core.resourcetypes.graph.redesigned.UpdateAction.EDIT_METADATA
import com.passbolt.mobile.android.core.resourcetypes.graph.redesigned.UpdateAction.REMOVE_METADATA_DESCRIPTION
import com.passbolt.mobile.android.core.resourcetypes.graph.redesigned.UpdateAction.REMOVE_PASSWORD
import com.passbolt.mobile.android.core.resourcetypes.graph.redesigned.UpdateAction.REMOVE_NOTE
import com.passbolt.mobile.android.core.resourcetypes.graph.redesigned.UpdateAction.REMOVE_TOTP
import com.passbolt.mobile.android.core.secrets.usecase.decrypt.parser.SecretJsonModel
import com.passbolt.mobile.android.jsonmodel.delegates.TotpSecret
import com.passbolt.mobile.android.mappers.EntropyViewMapper
import com.passbolt.mobile.android.mappers.ResourceFormMapper
import com.passbolt.mobile.android.serializers.jsonschema.SchemaEntity
import com.passbolt.mobile.android.supportedresourceTypes.ContentType
import com.passbolt.mobile.android.ui.Entropy
import com.passbolt.mobile.android.ui.LeadingContentType
import com.passbolt.mobile.android.ui.LeadingContentType.PASSWORD
import com.passbolt.mobile.android.ui.LeadingContentType.TOTP
import com.passbolt.mobile.android.ui.MetadataJsonModel
import com.passbolt.mobile.android.ui.MetadataTypeModel
import com.passbolt.mobile.android.ui.OtpParseResult
import com.passbolt.mobile.android.ui.PasswordGeneratorTypeModel
import com.passbolt.mobile.android.ui.PasswordUiModel
import com.passbolt.mobile.android.ui.ResourceFormMode
import com.passbolt.mobile.android.ui.ResourceFormMode.Create
import com.passbolt.mobile.android.ui.ResourceFormMode.Edit
import com.passbolt.mobile.android.ui.ResourceFormUiModel
import com.passbolt.mobile.android.ui.TotpUiModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.core.component.get
import org.koin.core.parameter.parametersOf
import timber.log.Timber

/**
 * Passbolt - Open source password manager for teams
 * Copyright (c) 2021 Passbolt SA
 *
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General
 * Public License (AGPL) as published by the Free Software Foundation version 3.
 *
 *
 * The name "Passbolt" is a registered trademark of Passbolt SA, and Passbolt SA hereby declines to grant a trademark
 * license to "Passbolt" pursuant to the GNU Affero General Public License version 3 Section 7(e), without a separate
 * agreement with Passbolt SA.
 *
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See GNU Affero General Public License for more details.
 *
 *
 * You should have received a copy of the GNU Affero General Public License along with this program. If not,
 * see GNU Affero General Public License v3 (http://www.gnu.org/licenses/agpl-3.0.html).
 *
 * @copyright Copyright (c) Passbolt SA (https://www.passbolt.com)
 * @license https://opensource.org/licenses/AGPL-3.0 AGPL License
 * @link https://www.passbolt.com Passbolt (tm)
 * @since v1.0
 */

class ResourceFormPresenter(
    private val getPasswordPoliciesUseCase: GetPasswordPoliciesUseCase,
    private val secretGenerator: SecretGenerator,
    private val entropyViewMapper: EntropyViewMapper,
    private val entropyCalculator: EntropyCalculator,
    private val resourceFormMapper: ResourceFormMapper,
    private val resourceModelHandler: ResourceModelHandler,
    private val getLocalResourceUseCase: GetLocalResourceUseCase,
    private val fullDataRefreshExecutor: FullDataRefreshExecutor,
    coroutineLaunchContext: CoroutineLaunchContext
) : BaseAuthenticatedPresenter<ResourceFormContract.View>(coroutineLaunchContext),
    ResourceFormContract.Presenter {

    override var view: ResourceFormContract.View? = null
    private val job = SupervisorJob()
    private val scope = CoroutineScope(job + coroutineLaunchContext.ui)

    private lateinit var mode: ResourceFormMode
    private lateinit var uiModel: ResourceFormUiModel
    private var parentFolderId: String? = null

    private val resourceMetadata: MetadataJsonModel
        get() = resourceModelHandler.resourceMetadata
    private val resourceSecret: SecretJsonModel
        get() = resourceModelHandler.resourceSecret
    private val metadataType: MetadataTypeModel
        get() = resourceModelHandler.metadataType
    private val contentType: ContentType
        get() = resourceModelHandler.contentType

    private var argsConsumed = false
    private var areAdvancedSettingsExpanded = false

    override fun argsRetrieved(mode: ResourceFormMode) {
        this.mode = mode

        scope.launch {
            view?.showInitializationProgress()
            fullDataRefreshExecutor.dataRefreshStatusFlow.first { it is DataRefreshStatus.Finished }
            if (!argsConsumed) {
                when (mode) {
                    is Create -> {
                        Timber.d("Initializing model with leading content type: ${mode.leadingContentType}")
                        parentFolderId = mode.parentFolderId
                        resourceModelHandler.initializeModelForCreation(mode.leadingContentType)
                    }
                    is Edit -> {
                        Timber.d("Initializing model for edition")
                        try {
                            resourceModelHandler.initializeModelForEdition(
                                mode.resourceId,
                                needSessionRefreshFlow,
                                sessionRefreshedFlow
                            )
                        } catch (e: Exception) {
                            view?.showEditResourceInitializationError()
                            view?.navigateBack()
                            return@launch
                        }
                    }
                }
                argsConsumed = true
            }

            uiModel = resourceModelHandler.getUiModel()
            view?.showName(resourceMetadata.name)
            setupAdvancedSettings()
            setupLeadingContentType(uiModel.leadingContentType)
            setupPrimaryButton(mode)
            setupTitle()
            view?.hideInitializationProgress()
        }
    }

    private fun setupTitle() {
        mode.let {
            when (it) {
                is Create -> when (uiModel.leadingContentType) {
                    TOTP -> view?.showCreateTotpTitle()
                    PASSWORD -> view?.showCreatePasswordTitle()
                }
                is Edit -> view?.showEditTitle(it.resourceName)
            }
        }
    }

    private fun setupAdvancedSettings() {
        if (areAdvancedSettingsExpanded) {
            view?.setupAdditionalSecrets(uiModel.supportedAdditionalSecrets)
            view?.setupMetadata(uiModel.supportedMetadata)
            view?.hideAdvancedSettings()
        }
    }

    override fun detach() {
        scope.coroutineContext.cancelChildren()
        super<BaseAuthenticatedPresenter>.detach()
    }

    override fun advancedSettingsClick() {
        view?.setupAdditionalSecrets(uiModel.supportedAdditionalSecrets)
        view?.setupMetadata(uiModel.supportedMetadata)
        view?.hideAdvancedSettings()
        areAdvancedSettingsExpanded = true
    }

    private fun setupPrimaryButton(mode: ResourceFormMode) {
        when (mode) {
            is Create -> view?.showCreateButton()
            is Edit -> view?.showSaveButton()
        }
    }

    private suspend fun setupLeadingContentType(leadingContentType: LeadingContentType) {
        when (leadingContentType) {
            TOTP -> {
                view?.addTotpLeadingForm(
                    resourceFormMapper.mapToUiModel(resourceSecret.totp, resourceMetadata.name)
                )
                view?.showTotpIssuer(resourceMetadata.getMainUri(metadataType))
                view?.showTotpSecret(resourceSecret.totp?.key.orEmpty())
            }
            PASSWORD -> {
                view?.addPasswordLeadingForm()
                view?.showPasswordUsername(resourceMetadata.username.orEmpty())
                view?.showPasswordMainUri(resourceMetadata.getMainUri(metadataType))
                showPassword(resourceSecret.getPassword(contentType).orEmpty())
            }
        }
    }

    override fun passwordGenerateClick() {
        scope.launch {
            val passwordPolicies = getPasswordPoliciesUseCase.execute(Unit)
            val secretGenerationResult = when (passwordPolicies.defaultGenerator) {
                PasswordGeneratorTypeModel.PASSWORD ->
                    secretGenerator.generatePassword(passwordPolicies.passwordGeneratorSettings)
                PasswordGeneratorTypeModel.PASSPHRASE ->
                    secretGenerator.generatePassphrase(passwordPolicies.passphraseGeneratorSettings)
            }
            when (secretGenerationResult) {
                is SecretGenerator.SecretGenerationResult.FailedToGenerateLowEntropy ->
                    view?.showUnableToGeneratePassword(secretGenerationResult.minimumEntropyBits)
                is SecretGenerator.SecretGenerationResult.Success -> view?.showPassword(
                    secretGenerationResult.password,
                    secretGenerationResult.entropy,
                    entropyViewMapper.map(Entropy.parse(secretGenerationResult.entropy))
                )
            }
        }
    }

    override fun nameTextChanged(name: String) {
        resourceModelHandler.applyModelChange(EDIT_METADATA) { metadata, _ ->
            metadata.name = name
        }
    }

    override fun passwordTextChanged(password: String) {
        resourceModelHandler.applyModelChange(if (password.isBlank()) REMOVE_PASSWORD else ADD_PASSWORD) { _, secret ->
            secret.setPassword(contentType, password)

            scope.launch {
                val entropy = entropyCalculator.getSecretEntropy(password)
                view?.showPasswordStrength(entropyViewMapper.map(Entropy.parse(entropy)), entropy)
            }
        }
    }

    private suspend fun showPassword(password: String) {
        val entropy = entropyCalculator.getSecretEntropy(password)
        view?.showPassword(password.toCodepoints(), entropy, entropyViewMapper.map(Entropy.parse(entropy)))
    }

    override fun passwordMainUriTextChanged(mainUri: String) {
        resourceModelHandler.applyModelChange(EDIT_METADATA) { metadata, _ ->
            metadata.setMainUri(metadataType, mainUri)
        }
    }

    override fun passwordUsernameTextChanged(username: String) {
        resourceModelHandler.applyModelChange(EDIT_METADATA) { metadata, _ ->
            metadata.username = username
        }
    }

    override fun metadataDescriptionChanged(metadataDescription: String?) {
        resourceModelHandler.applyModelChange(
            if (metadataDescription.isNullOrBlank()) REMOVE_METADATA_DESCRIPTION else ADD_METADATA_DESCRIPTION
        ) { metadata, _ ->
            metadata.description = metadataDescription
        }
    }

    override fun additionalNoteClick() {
        view?.navigateToNote(resourceSecret.description.orEmpty())
    }

    override fun totpSecretChanged(totpSecret: String) {
        resourceModelHandler.applyModelChange(
            if (totpSecret.isBlank()) REMOVE_TOTP else ADD_TOTP
        ) { _, secret ->
            secret.totp = requireNotNull(secret.totp).copy(key = totpSecret)
        }
    }

    override fun totpUrlChanged(url: String) {
        resourceModelHandler.applyModelChange(EDIT_METADATA) { metadata, _ ->
            metadata.setMainUri(metadataType, url)
        }
    }

    override fun totpAdvancedSettingsChanged(totpAdvancedSettings: TotpUiModel?) {
        resourceModelHandler.applyModelChange(ADD_TOTP) { _, secret ->
            val settings =
                totpAdvancedSettings ?: TotpUiModel.emptyWithDefaults(resourceMetadata.getMainUri(metadataType))
            secret.totp = requireNotNull(resourceSecret.totp).copy(
                algorithm = settings.algorithm,
                digits = settings.length.toInt(),
                period = settings.expiry.toLong()
            )
        }
    }

    override fun totpScanned(isManualCreationChosen: Boolean, scannedTotp: OtpParseResult.OtpQr.TotpQr?) {
        // just stay on totp screen and allow manual input
        if (isManualCreationChosen) return

        scannedTotp?.let {
            resourceModelHandler.applyModelChange(EDIT_METADATA) { metadata, _ ->
                metadata.setMainUri(metadataType, it.issuer.orEmpty())
                metadata.name = it.label
            }
            resourceModelHandler.applyModelChange(ADD_TOTP) { _, secret ->
                secret.totp = TotpSecret(
                    key = it.secret,
                    algorithm = it.algorithm.name,
                    digits = it.digits,
                    period = it.period
                )
            }
        }
    }

    override fun additionalTotpClick() {
        view?.navigateToTotp(
            resourceFormMapper.mapToUiModel(resourceSecret.totp, resourceMetadata.getMainUri(metadataType))
        )
    }

    override fun additionalPasswordClick() {
        view?.navigateToPassword(
            resourceFormMapper.mapToUiModel(
                resourceSecret.getPassword(contentType).orEmpty(),
                resourceMetadata.getMainUri(metadataType),
                resourceMetadata.username.orEmpty()
            )
        )
    }

    override fun metadataDescriptionClick() {
        view?.navigateToMetadataDescription(resourceMetadata.description.orEmpty())
    }

    override fun noteChanged(note: String?) {
        resourceModelHandler.applyModelChange(
            if (note.isNullOrBlank()) REMOVE_NOTE else ADD_NOTE
        ) { _, secret ->
            secret.description = note
        }
    }

    override fun totpChanged(totpUiModel: TotpUiModel?) {
        val totpAction = if (totpUiModel == null || totpUiModel.secret.isBlank()) REMOVE_TOTP else ADD_TOTP

        resourceModelHandler.applyModelChange(totpAction) { _, secret ->
            secret.totp = resourceFormMapper.mapToJsonModel(totpUiModel)
        }
        if (totpUiModel != null) {
            resourceModelHandler.applyModelChange(EDIT_METADATA) { metadata, _ ->
                metadata.setMainUri(metadataType, totpUiModel.issuer)
            }
        }
    }

    override fun passwordChanged(passwordUiModel: PasswordUiModel?) {
        passwordUiModel?.let {
            val passwordEvent = if (passwordUiModel.password.isBlank()) REMOVE_PASSWORD else ADD_PASSWORD

            resourceModelHandler.applyModelChange(passwordEvent) { _, secret ->
                secret.setPassword(contentType, passwordUiModel.password)
            }
            resourceModelHandler.applyModelChange(EDIT_METADATA) { metadata, _ ->
                metadata.username = passwordUiModel.username
                metadata.setMainUri(metadataType, passwordUiModel.mainUri)
            }
        }
    }

    override fun createResourceClick() {
        onValid {
            scope.launch {
                view?.showProgress()
                val resourceCreateActionsInteractor = get<ResourceCreateActionsInteractor> {
                    parametersOf(needSessionRefreshFlow, sessionRefreshedFlow)
                }
                performResourceCreateAction(
                    action = {
                        resourceCreateActionsInteractor.createGenericResourceResource(
                            contentType,
                            parentFolderId,
                            resourceModelHandler.getResourceMetadataWithRequiredFields(),
                            resourceModelHandler.getResourceSecretWithRequiredFields()
                        )
                    },
                    doOnFailure = { view?.showGenericError() },
                    doOnCryptoFailure = { view?.showEncryptionError(it) },
                    doOnSchemaValidationFailure = ::handleSchemaValidationFailure,
                    doOnSuccess = { view?.navigateBackWithCreateSuccess(resourceMetadata.name) }
                )
                view?.hideProgress()
            }
        }
    }

    private fun handleSchemaValidationFailure(entity: SchemaEntity) {
        when (entity) {
            SchemaEntity.RESOURCE -> view?.showJsonResourceSchemaValidationError()
            SchemaEntity.SECRET -> view?.showJsonSecretSchemaValidationError()
        }
    }

    override fun updateResourceClick() {
        onValid {
            scope.launch {
                view?.showProgress()
                val editedResource = getLocalResourceUseCase.execute(
                    GetLocalResourceUseCase.Input((mode as Edit).resourceId)
                ).resource
                val resourceUpdateActionsInteractor = get<ResourceUpdateActionsInteractor> {
                    parametersOf(editedResource, needSessionRefreshFlow, sessionRefreshedFlow)
                }
                performResourceUpdateAction(
                    action = {
                        resourceUpdateActionsInteractor.updateGenericResource(
                            contentType,
                            editedResource,
                            resourceModelHandler.getResourceMetadataWithRequiredFields(),
                            resourceModelHandler.getResourceSecretWithRequiredFields()
                        )
                    },
                    doOnFailure = { view?.showGenericError() },
                    doOnCryptoFailure = { view?.showEncryptionError(it) },
                    doOnSchemaValidationFailure = ::handleSchemaValidationFailure,
                    doOnSuccess = { view?.navigateBackWithEditSuccess(resourceMetadata.name) }
                )
                view?.hideProgress()
            }
        }
    }

    private fun onValid(action: () -> Unit) {
        if (uiModel.leadingContentType == TOTP && resourceSecret.totp?.key.isNullOrBlank()) {
            view?.showTotpRequired()
        } else {
            action()
        }
    }
}
