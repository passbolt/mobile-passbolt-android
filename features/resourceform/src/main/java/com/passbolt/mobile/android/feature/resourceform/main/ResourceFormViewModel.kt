package com.passbolt.mobile.android.feature.resourceform.main

import com.passbolt.mobile.android.common.datarefresh.DataRefreshTrackingFlow
import com.passbolt.mobile.android.common.validation.StringIsBase32
import com.passbolt.mobile.android.common.validation.StringMaxLength
import com.passbolt.mobile.android.core.idlingresource.CreateResourceIdlingResource
import com.passbolt.mobile.android.core.idlingresource.UpdateResourceIdlingResource
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
import com.passbolt.mobile.android.core.resourcetypes.graph.redesigned.UpdateAction.ADD_NOTE
import com.passbolt.mobile.android.core.resourcetypes.graph.redesigned.UpdateAction.ADD_PASSWORD
import com.passbolt.mobile.android.core.resourcetypes.graph.redesigned.UpdateAction.ADD_TOTP
import com.passbolt.mobile.android.core.resourcetypes.graph.redesigned.UpdateAction.EDIT_ADDITIONAL_URIS
import com.passbolt.mobile.android.core.resourcetypes.graph.redesigned.UpdateAction.EDIT_APPEARANCE
import com.passbolt.mobile.android.core.resourcetypes.graph.redesigned.UpdateAction.EDIT_METADATA
import com.passbolt.mobile.android.core.resourcetypes.graph.redesigned.UpdateAction.REMOVE_METADATA_DESCRIPTION
import com.passbolt.mobile.android.core.resourcetypes.graph.redesigned.UpdateAction.REMOVE_NOTE
import com.passbolt.mobile.android.core.resourcetypes.graph.redesigned.UpdateAction.REMOVE_PASSWORD
import com.passbolt.mobile.android.core.resourcetypes.graph.redesigned.UpdateAction.REMOVE_TOTP
import com.passbolt.mobile.android.feature.authentication.compose.AuthenticatedViewModel
import com.passbolt.mobile.android.feature.authentication.session.runAuthenticatedOperation
import com.passbolt.mobile.android.feature.resourceform.additionalsecrets.note.NoteFormViewModel
import com.passbolt.mobile.android.feature.resourceform.additionalsecrets.note.NoteValidationError.MaxLengthExceeded
import com.passbolt.mobile.android.feature.resourceform.additionalsecrets.totp.TotpSecretValidationError.MustBeBase32
import com.passbolt.mobile.android.feature.resourceform.additionalsecrets.totp.TotpSecretValidationError.MustNotBeEmpty
import com.passbolt.mobile.android.feature.resourceform.main.ResourceFormIntent.AdditionalUrisResult
import com.passbolt.mobile.android.feature.resourceform.main.ResourceFormIntent.AppearanceResult
import com.passbolt.mobile.android.feature.resourceform.main.ResourceFormIntent.CreateResource
import com.passbolt.mobile.android.feature.resourceform.main.ResourceFormIntent.CustomFieldsResult
import com.passbolt.mobile.android.feature.resourceform.main.ResourceFormIntent.DescriptionResult
import com.passbolt.mobile.android.feature.resourceform.main.ResourceFormIntent.DismissMetadataKeyDialog
import com.passbolt.mobile.android.feature.resourceform.main.ResourceFormIntent.ExpandAdvancedSettings
import com.passbolt.mobile.android.feature.resourceform.main.ResourceFormIntent.GeneratePassword
import com.passbolt.mobile.android.feature.resourceform.main.ResourceFormIntent.GoBack
import com.passbolt.mobile.android.feature.resourceform.main.ResourceFormIntent.GoToAdditionalNote
import com.passbolt.mobile.android.feature.resourceform.main.ResourceFormIntent.GoToAdditionalPassword
import com.passbolt.mobile.android.feature.resourceform.main.ResourceFormIntent.GoToAdditionalUris
import com.passbolt.mobile.android.feature.resourceform.main.ResourceFormIntent.GoToAppearance
import com.passbolt.mobile.android.feature.resourceform.main.ResourceFormIntent.GoToCustomFields
import com.passbolt.mobile.android.feature.resourceform.main.ResourceFormIntent.GoToMetadataDescription
import com.passbolt.mobile.android.feature.resourceform.main.ResourceFormIntent.NameTextChanged
import com.passbolt.mobile.android.feature.resourceform.main.ResourceFormIntent.NoteResult
import com.passbolt.mobile.android.feature.resourceform.main.ResourceFormIntent.PasswordMainUriTextChanged
import com.passbolt.mobile.android.feature.resourceform.main.ResourceFormIntent.PasswordResult
import com.passbolt.mobile.android.feature.resourceform.main.ResourceFormIntent.PasswordTextChanged
import com.passbolt.mobile.android.feature.resourceform.main.ResourceFormIntent.PasswordUsernameTextChanged
import com.passbolt.mobile.android.feature.resourceform.main.ResourceFormIntent.ScanOtpResult
import com.passbolt.mobile.android.feature.resourceform.main.ResourceFormIntent.TotpAdvancedSettingsResult
import com.passbolt.mobile.android.feature.resourceform.main.ResourceFormIntent.TotpResult
import com.passbolt.mobile.android.feature.resourceform.main.ResourceFormIntent.TotpSecretChanged
import com.passbolt.mobile.android.feature.resourceform.main.ResourceFormIntent.TotpUrlChanged
import com.passbolt.mobile.android.feature.resourceform.main.ResourceFormIntent.TrustNewMetadataKey
import com.passbolt.mobile.android.feature.resourceform.main.ResourceFormIntent.TrustedMetadataKeyDeleted
import com.passbolt.mobile.android.feature.resourceform.main.ResourceFormIntent.UpdateResource
import com.passbolt.mobile.android.feature.resourceform.main.ResourceFormSideEffect.NavigateBack
import com.passbolt.mobile.android.feature.resourceform.main.ResourceFormSideEffect.NavigateBackWithCreateSuccess
import com.passbolt.mobile.android.feature.resourceform.main.ResourceFormSideEffect.NavigateBackWithEditSuccess
import com.passbolt.mobile.android.feature.resourceform.main.ResourceFormSideEffect.NavigateToAdditionalUris
import com.passbolt.mobile.android.feature.resourceform.main.ResourceFormSideEffect.NavigateToAppearance
import com.passbolt.mobile.android.feature.resourceform.main.ResourceFormSideEffect.NavigateToCustomFields
import com.passbolt.mobile.android.feature.resourceform.main.ResourceFormSideEffect.NavigateToDescription
import com.passbolt.mobile.android.feature.resourceform.main.ResourceFormSideEffect.NavigateToNote
import com.passbolt.mobile.android.feature.resourceform.main.ResourceFormSideEffect.NavigateToPassword
import com.passbolt.mobile.android.feature.resourceform.main.ResourceFormSideEffect.NavigateToScanOtp
import com.passbolt.mobile.android.feature.resourceform.main.ResourceFormSideEffect.NavigateToTotp
import com.passbolt.mobile.android.feature.resourceform.main.ResourceFormSideEffect.NavigateToTotpAdvancedSettings
import com.passbolt.mobile.android.feature.resourceform.main.ResourceFormSideEffect.ShowSnackbar
import com.passbolt.mobile.android.feature.resourceform.main.ResourceFormSideEffect.ShowToast
import com.passbolt.mobile.android.jsonmodel.delegates.TotpSecret
import com.passbolt.mobile.android.mappers.EntropyViewMapper
import com.passbolt.mobile.android.mappers.ResourceFormMapper
import com.passbolt.mobile.android.metadata.interactor.MetadataPrivateKeysHelperInteractor
import com.passbolt.mobile.android.serializers.jsonschema.SchemaEntity
import com.passbolt.mobile.android.ui.AdditionalUrisUiModel
import com.passbolt.mobile.android.ui.Entropy
import com.passbolt.mobile.android.ui.LeadingContentType
import com.passbolt.mobile.android.ui.LeadingContentType.CUSTOM_FIELDS
import com.passbolt.mobile.android.ui.LeadingContentType.PASSWORD
import com.passbolt.mobile.android.ui.LeadingContentType.STANDALONE_NOTE
import com.passbolt.mobile.android.ui.LeadingContentType.TOTP
import com.passbolt.mobile.android.ui.MetadataIconModel
import com.passbolt.mobile.android.ui.NewMetadataKeyToTrustModel
import com.passbolt.mobile.android.ui.OtpParseResult
import com.passbolt.mobile.android.ui.PasswordGeneratorTypeModel
import com.passbolt.mobile.android.ui.ResourceAppearanceModel.Companion.DEFAULT_BACKGROUND_COLOR_HEX_STRING
import com.passbolt.mobile.android.ui.ResourceAppearanceModel.Companion.ICON_TYPE_KEEPASS
import com.passbolt.mobile.android.ui.ResourceAppearanceModel.Companion.ICON_TYPE_PASSBOLT
import com.passbolt.mobile.android.ui.ResourceFormMode
import com.passbolt.mobile.android.ui.ResourceFormMode.Create
import com.passbolt.mobile.android.ui.ResourceFormMode.Edit
import com.passbolt.mobile.android.ui.ResourceFormUiModel
import com.passbolt.mobile.android.ui.TotpUiModel
import org.koin.core.component.get
import org.koin.core.parameter.parametersOf
import timber.log.Timber

@Suppress("TooManyFunctions", "LargeClass")
class ResourceFormViewModel(
    private val mode: ResourceFormMode,
    private val getPasswordPoliciesUseCase: GetPasswordPoliciesUseCase,
    private val secretGenerator: SecretGenerator,
    private val entropyViewMapper: EntropyViewMapper,
    private val entropyCalculator: EntropyCalculator,
    private val resourceFormMapper: ResourceFormMapper,
    private val resourceModelHandler: ResourceModelHandler,
    private val getLocalResourceUseCase: GetLocalResourceUseCase,
    private val dataRefreshTrackingFlow: DataRefreshTrackingFlow,
    private val metadataPrivateKeysHelperInteractor: MetadataPrivateKeysHelperInteractor,
    private val createResourceIdlingResource: CreateResourceIdlingResource,
    private val updateResourceIdlingResource: UpdateResourceIdlingResource,
) : AuthenticatedViewModel<ResourceFormState, ResourceFormSideEffect>(ResourceFormState(mode = mode)) {
    private val uiModel: ResourceFormUiModel by lazy {
        resourceModelHandler.getUiModel(mode)
    }
    private var parentFolderId: String? = null

    init {
        initialize()
    }

    @Suppress("CyclomaticComplexMethod")
    fun onIntent(intent: ResourceFormIntent) {
        when (intent) {
            is NameTextChanged -> nameTextChanged(intent.name)
            ExpandAdvancedSettings -> expandAdvancedSettings()
            CreateResource -> createResource()
            UpdateResource -> updateResource()
            is PasswordTextChanged -> passwordTextChanged(intent.password)
            GeneratePassword -> generatePassword()
            is PasswordMainUriTextChanged -> passwordMainUriTextChanged(intent.mainUri)
            is PasswordUsernameTextChanged -> passwordUsernameTextChanged(intent.username)
            is TotpSecretChanged -> totpSecretChanged(intent.totpSecret)
            is TotpUrlChanged -> totpUrlChanged(intent.url)
            is ResourceFormIntent.GoToTotpMoreSettings -> goToTotpMoreSettings()
            is ResourceFormIntent.ScanTotp -> scanTotp()
            is ResourceFormIntent.NoteChanged -> noteChanged(intent.note)
            GoToAdditionalNote -> goToAdditionalNote()
            GoToAdditionalPassword -> goToAdditionalPassword()
            is ResourceFormIntent.GoToAdditionalTotp -> goToAdditionalTotp()
            GoToCustomFields -> goToCustomFields()
            GoToMetadataDescription -> goToMetadataDescription()
            GoToAppearance -> goToAppearance()
            GoToAdditionalUris -> goToAdditionalUris()
            is PasswordResult -> passwordResult(intent.passwordUiModel)
            is TotpResult -> totpResult(intent.totpUiModel)
            is TotpAdvancedSettingsResult -> totpAdvancedSettingsResult(intent.totpAdvancedSettings)
            is NoteResult -> noteResult(intent.note)
            is DescriptionResult -> descriptionResult(intent.metadataDescription)
            is AppearanceResult -> appearanceResult(intent.model)
            is AdditionalUrisResult -> additionalUrisResult(intent.urisUiModel)
            is CustomFieldsResult -> customFieldsResult()
            is ScanOtpResult -> scanOtpResult(intent.isManualCreationChosen, intent.scannedTotp)
            is TrustNewMetadataKey -> trustNewMetadataKey(intent.model)
            is TrustedMetadataKeyDeleted -> trustedMetadataKeyDeleted()
            DismissMetadataKeyDialog -> dismissMetadataKeyDialog()
            GoBack -> goBack()
        }
    }

    private fun scanTotp() {
        emitSideEffect(NavigateToScanOtp)
    }

    private fun dismissMetadataKeyDialog() {
        updateViewState {
            copy(metadataKeyModifiedDialog = null, metadataKeyDeletedDialog = null)
        }
    }

    private fun goBack() {
        emitSideEffect(NavigateBack)
    }

    private fun initialize() {
        launch {
            updateViewState { copy(shouldShowScreenProgress = true) }
            dataRefreshTrackingFlow.awaitIdle()
            when (mode) {
                is Create -> {
                    try {
                        Timber.d("Initializing model with leading content type: ${mode.leadingContentType}")
                        parentFolderId = mode.parentFolderId
                        resourceModelHandler.initializeModelForCreation(mode.leadingContentType)
                    } catch (_: Exception) {
                        emitSideEffect(ShowToast(ToastMessage.CREATE_INITIALIZATION_ERROR))
                        emitSideEffect(NavigateBack)
                        return@launch
                    }
                }
                is Edit -> {
                    Timber.d("Initializing model for edition")
                    try {
                        resourceModelHandler.initializeModelForEdition(mode.resourceId)
                    } catch (_: Exception) {
                        emitSideEffect(ShowToast(ToastMessage.EDIT_INITIALIZATION_ERROR))
                        emitSideEffect(NavigateBack)
                        return@launch
                    }
                }
            }

            setupState()
            updateViewState { copy(shouldShowScreenProgress = false) }
        }
    }

    private suspend fun setupState() {
        val leadingContentType = uiModel.leadingContentType
        val areAdvancedSettingsExpanded = viewState.value.areAdvancedSettingsExpanded

        updateViewState {
            copy(
                name = resourceModelHandler.resourceMetadata.name,
                leadingContentType = leadingContentType,
                isPrimaryButtonVisible = true,
            )
        }

        setupLeadingContentType(leadingContentType)

        if (areAdvancedSettingsExpanded) {
            updateViewState {
                copy(
                    supportedAdditionalSecrets = uiModel.supportedAdditionalSecrets,
                    supportedMetadata = uiModel.supportedMetadata,
                    areAdvancedSettingsVisible = false,
                )
            }
        }
    }

    private suspend fun setupLeadingContentType(leadingContentType: LeadingContentType) {
        val resourceMetadata = resourceModelHandler.resourceMetadata
        val resourceSecret = resourceModelHandler.resourceSecret
        val contentType = resourceModelHandler.contentType
        when (leadingContentType) {
            TOTP -> {
                val totpUiModel =
                    resourceFormMapper.mapToUiModel(resourceSecret.totp, resourceMetadata.name)
                updateViewState {
                    copy(
                        totpData =
                            totpData.copy(
                                totpUiModel = totpUiModel,
                                totpIssuer = resourceMetadata.getMainUri(contentType),
                                totpSecret = resourceSecret.totp?.key.orEmpty(),
                            ),
                    )
                }
            }
            PASSWORD -> {
                val password = resourceSecret.getPassword(contentType).orEmpty()
                val entropy = entropyCalculator.getSecretEntropy(password)
                val passwordStrength = entropyViewMapper.map(Entropy.parse(entropy))
                updateViewState {
                    copy(
                        passwordData =
                            passwordData.copy(
                                mainUri = resourceMetadata.getMainUri(contentType),
                                username = resourceMetadata.username.orEmpty(),
                                password =
                                    password
                                        .toCodepoints()
                                        .map { Character.toChars(it.value) }
                                        .joinToString("") { String(it) },
                                passwordStrength = passwordStrength,
                                passwordEntropyBits = entropy,
                            ),
                    )
                }
            }
            CUSTOM_FIELDS -> {
                // no leading form to setup
            }
            STANDALONE_NOTE -> {
                updateViewState {
                    copy(noteData = noteData.copy(note = resourceSecret.description.orEmpty()))
                }
            }
        }
    }

    private fun expandAdvancedSettings() {
        updateViewState {
            copy(
                supportedAdditionalSecrets = uiModel.supportedAdditionalSecrets,
                supportedMetadata = uiModel.supportedMetadata,
                areAdvancedSettingsVisible = false,
                areAdvancedSettingsExpanded = true,
            )
        }
    }

    private fun nameTextChanged(name: String) {
        resourceModelHandler.applyModelChange(EDIT_METADATA) { metadata, _ ->
            metadata.name = name
        }
        updateViewState { copy(name = name) }
    }

    private fun passwordTextChanged(password: String) {
        resourceModelHandler.applyModelChange(
            if (password.isBlank()) REMOVE_PASSWORD else ADD_PASSWORD,
        ) { _, secret ->
            secret.setPassword(resourceModelHandler.contentType, password)
        }
        updateViewState { copy(passwordData = passwordData.copy(password = password)) }
        launch {
            val entropy = entropyCalculator.getSecretEntropy(password)
            updateViewState {
                copy(
                    passwordData =
                        passwordData.copy(
                            passwordStrength = entropyViewMapper.map(Entropy.parse(entropy)),
                            passwordEntropyBits = entropy,
                        ),
                )
            }
        }
    }

    private fun generatePassword() {
        launch {
            val passwordPolicies = getPasswordPoliciesUseCase.execute(Unit)
            val secretGenerationResult =
                when (passwordPolicies.defaultGenerator) {
                    PasswordGeneratorTypeModel.PASSWORD ->
                        secretGenerator.generatePassword(passwordPolicies.passwordGeneratorSettings)
                    PasswordGeneratorTypeModel.PASSPHRASE ->
                        secretGenerator.generatePassphrase(passwordPolicies.passphraseGeneratorSettings)
                }
            when (secretGenerationResult) {
                is SecretGenerator.SecretGenerationResult.FailedToGenerateLowEntropy ->
                    emitSideEffect(
                        ShowToast(
                            ToastMessage.UNABLE_TO_GENERATE_PASSWORD,
                            listOf(secretGenerationResult.minimumEntropyBits),
                        ),
                    )
                is SecretGenerator.SecretGenerationResult.Success -> {
                    val password = secretGenerationResult.password
                    val entropy = secretGenerationResult.entropy
                    val passwordStr = password.map { Character.toChars(it.value) }.joinToString("") { String(it) }
                    resourceModelHandler.applyModelChange(ADD_PASSWORD) { _, secret ->
                        secret.setPassword(resourceModelHandler.contentType, passwordStr)
                    }
                    updateViewState {
                        copy(
                            passwordData =
                                passwordData.copy(
                                    password = passwordStr,
                                    passwordStrength = entropyViewMapper.map(Entropy.parse(entropy)),
                                    passwordEntropyBits = entropy,
                                ),
                        )
                    }
                }
            }
        }
    }

    private fun passwordMainUriTextChanged(mainUri: String) {
        resourceModelHandler.applyModelChange(EDIT_METADATA) { metadata, _ ->
            metadata.setMainUri(resourceModelHandler.contentType, mainUri)
        }
        updateViewState { copy(passwordData = passwordData.copy(mainUri = mainUri)) }
    }

    private fun passwordUsernameTextChanged(username: String) {
        resourceModelHandler.applyModelChange(EDIT_METADATA) { metadata, _ ->
            metadata.username = username
        }
        updateViewState { copy(passwordData = passwordData.copy(username = username)) }
    }

    private fun totpSecretChanged(totpSecret: String) {
        resourceModelHandler.applyModelChange(
            if (totpSecret.isBlank()) REMOVE_TOTP else ADD_TOTP,
        ) { _, secret ->
            secret.totp = requireNotNull(secret.totp).copy(key = totpSecret)
        }
        updateViewState { copy(totpData = totpData.copy(totpSecret = totpSecret, totpSecretError = null)) }
    }

    private fun totpUrlChanged(url: String) {
        resourceModelHandler.applyModelChange(EDIT_METADATA) { metadata, _ ->
            metadata.setMainUri(resourceModelHandler.contentType, url)
        }
        updateViewState { copy(totpData = totpData.copy(totpIssuer = url)) }
    }

    private fun goToTotpMoreSettings() {
        val totpUiModel =
            resourceFormMapper.mapToUiModel(
                resourceModelHandler.resourceSecret.totp,
                resourceModelHandler.resourceMetadata.getMainUri(resourceModelHandler.contentType),
            )
        emitSideEffect(NavigateToTotpAdvancedSettings(mode, totpUiModel))
    }

    private fun noteChanged(note: String) {
        resourceModelHandler.applyModelChange(
            if (note.isBlank()) REMOVE_NOTE else ADD_NOTE,
        ) { _, secret ->
            secret.description = note
        }
        updateViewState { copy(noteData = noteData.copy(note = note, noteError = null)) }
    }

    private fun goToAdditionalNote() {
        emitSideEffect(NavigateToNote(mode, resourceModelHandler.resourceSecret.description.orEmpty()))
    }

    private fun goToAdditionalTotp() {
        emitSideEffect(
            NavigateToTotp(
                mode,
                resourceFormMapper.mapToUiModel(
                    resourceModelHandler.resourceSecret.totp,
                    resourceModelHandler.resourceMetadata.getMainUri(resourceModelHandler.contentType),
                ),
            ),
        )
    }

    private fun goToAdditionalPassword() {
        val resourceMetadata = resourceModelHandler.resourceMetadata
        val contentType = resourceModelHandler.contentType
        emitSideEffect(
            NavigateToPassword(
                mode,
                resourceFormMapper.mapToUiModel(
                    resourceModelHandler.resourceSecret.getPassword(contentType).orEmpty(),
                    resourceMetadata.getMainUri(contentType),
                    resourceMetadata.username.orEmpty(),
                ),
            ),
        )
    }

    private fun goToCustomFields() {
        val customFieldsModel =
            resourceFormMapper.mapToUiModel(
                resourceModelHandler.resourceMetadata.customFields,
                resourceModelHandler.resourceSecret.customFields,
            )
        emitSideEffect(
            NavigateToCustomFields(
                mode,
                resourceFormMapper.mapToCustomFieldsUiModel(customFieldsModel),
            ),
        )
    }

    private fun goToMetadataDescription() {
        emitSideEffect(NavigateToDescription(mode, resourceModelHandler.resourceMetadata.description.orEmpty()))
    }

    private fun goToAppearance() {
        val appearanceModel = resourceFormMapper.mapToUiModel(resourceModelHandler.resourceMetadata.icon)
        emitSideEffect(NavigateToAppearance(mode, appearanceModel))
    }

    private fun goToAdditionalUris() {
        val resourceMetadata = resourceModelHandler.resourceMetadata
        val mainUri = resourceMetadata.getMainUri(resourceModelHandler.contentType)
        val additionalUris = resourceMetadata.uris.orEmpty().filter { it != mainUri }
        emitSideEffect(
            NavigateToAdditionalUris(
                mode,
                AdditionalUrisUiModel(mainUri = mainUri, additionalUris = additionalUris),
            ),
        )
    }

    private fun passwordResult(passwordUiModel: com.passbolt.mobile.android.ui.PasswordUiModel?) {
        val contentType = resourceModelHandler.contentType
        passwordUiModel?.let {
            val passwordEvent = if (passwordUiModel.password.isBlank()) REMOVE_PASSWORD else ADD_PASSWORD
            resourceModelHandler.applyModelChange(passwordEvent) { _, secret ->
                secret.setPassword(contentType, passwordUiModel.password)
            }
            resourceModelHandler.applyModelChange(EDIT_METADATA) { metadata, _ ->
                metadata.username = passwordUiModel.username
                metadata.setMainUri(contentType, passwordUiModel.mainUri)
            }
        }
    }

    private fun totpResult(totpUiModel: TotpUiModel?) {
        val totpAction = if (totpUiModel == null || totpUiModel.secret.isBlank()) REMOVE_TOTP else ADD_TOTP
        resourceModelHandler.applyModelChange(totpAction) { _, secret ->
            secret.totp = resourceFormMapper.mapToJsonModel(totpUiModel)
        }
        if (totpUiModel != null) {
            resourceModelHandler.applyModelChange(EDIT_METADATA) { metadata, _ ->
                metadata.setMainUri(resourceModelHandler.contentType, totpUiModel.issuer)
            }
        }
    }

    private fun totpAdvancedSettingsResult(totpAdvancedSettings: TotpUiModel?) {
        resourceModelHandler.applyModelChange(ADD_TOTP) { _, secret ->
            val settings =
                totpAdvancedSettings ?: TotpUiModel.emptyWithDefaults(
                    resourceModelHandler.resourceMetadata.getMainUri(resourceModelHandler.contentType),
                )
            secret.totp =
                requireNotNull(resourceModelHandler.resourceSecret.totp).copy(
                    algorithm = settings.algorithm,
                    digits = settings.length.toInt(),
                    period = settings.expiry.toLong(),
                )
        }
    }

    private fun noteResult(note: String?) {
        resourceModelHandler.applyModelChange(
            if (note.isNullOrBlank()) REMOVE_NOTE else ADD_NOTE,
        ) { _, secret ->
            secret.description = note
        }
    }

    private fun descriptionResult(metadataDescription: String?) {
        resourceModelHandler.applyModelChange(
            if (metadataDescription.isNullOrBlank()) REMOVE_METADATA_DESCRIPTION else ADD_METADATA_DESCRIPTION,
        ) { metadata, _ ->
            metadata.description = metadataDescription
        }
    }

    private fun appearanceResult(model: com.passbolt.mobile.android.ui.ResourceAppearanceModel?) {
        resourceModelHandler.applyModelChange(EDIT_APPEARANCE) { metadata, _ ->
            val iconType = model?.iconType ?: ICON_TYPE_PASSBOLT
            val iconValue =
                when (iconType) {
                    ICON_TYPE_KEEPASS -> model?.iconValue
                    else -> null
                }
            val iconBackgroundColorHex = model?.iconBackgroundHexColor ?: DEFAULT_BACKGROUND_COLOR_HEX_STRING
            metadata.icon =
                MetadataIconModel(
                    type = iconType,
                    value = iconValue,
                    backgroundColorHexString = iconBackgroundColorHex,
                )
        }
    }

    private fun additionalUrisResult(urisUiModel: AdditionalUrisUiModel?) {
        urisUiModel?.let {
            val uris = listOf(urisUiModel.mainUri) + urisUiModel.additionalUris
            resourceModelHandler.applyModelChange(EDIT_ADDITIONAL_URIS) { metadata, _ ->
                metadata.uris =
                    if (uris.all { it.isBlank() }) {
                        emptyList()
                    } else {
                        uris.map { it.trim() }.filter { it.isNotBlank() }
                    }
            }
        }
    }

    private fun customFieldsResult() {
        // TODO not supported for now
    }

    private fun scanOtpResult(
        isManualCreationChosen: Boolean,
        scannedTotp: OtpParseResult.OtpQr.TotpQr?,
    ) {
        if (isManualCreationChosen) return

        scannedTotp?.let {
            resourceModelHandler.applyModelChange(EDIT_METADATA) { metadata, _ ->
                metadata.setMainUri(resourceModelHandler.contentType, it.issuer.orEmpty())
                metadata.name = it.label
            }
            resourceModelHandler.applyModelChange(ADD_TOTP) { _, secret ->
                secret.totp =
                    TotpSecret(
                        key = it.secret,
                        algorithm = it.algorithm.name,
                        digits = it.digits,
                        period = it.period,
                    )
            }
            updateViewState {
                copy(
                    name = it.label,
                    totpData =
                        totpData.copy(
                            totpSecret = it.secret,
                            totpIssuer = it.issuer.orEmpty(),
                        ),
                )
            }
        }
    }

    private fun createResource() {
        onValid {
            launch {
                createResourceIdlingResource.setIdle(false)
                updateViewState { copy(shouldShowDialogProgress = true) }
                val resourceCreateActionsInteractor = get<ResourceCreateActionsInteractor>()
                performResourceCreateAction(
                    action = {
                        resourceCreateActionsInteractor.createGenericResource(
                            resourceModelHandler.contentType,
                            parentFolderId,
                            resourceModelHandler.getResourceMetadataWithRequiredFields(),
                            resourceModelHandler.getResourceSecretWithRequiredFields(),
                        )
                    },
                    doOnFailure = { emitSideEffect(ShowSnackbar(SnackbarMessage.COMMON_FAILURE)) },
                    doOnCryptoFailure = {
                        emitSideEffect(ShowSnackbar(SnackbarMessage.ENCRYPTION_FAILURE))
                    },
                    doOnSchemaValidationFailure = ::handleSchemaValidationFailure,
                    doOnSuccess = {
                        emitSideEffect(NavigateBackWithCreateSuccess(it.resourceName, it.resourceId))
                    },
                    doOnCannotCreateWithCurrentConfig = {
                        emitSideEffect(
                            ShowSnackbar(SnackbarMessage.CANNOT_CREATE_RESOURCE_WITH_CURRENT_CONFIG),
                        )
                    },
                    doOnMetadataKeyModified = {
                        updateViewState { copy(metadataKeyModifiedDialog = it) }
                    },
                    doOnMetadataKeyDeleted = {
                        updateViewState { copy(metadataKeyDeletedDialog = it) }
                    },
                    doOnMetadataKeyVerificationFailure = {
                        emitSideEffect(
                            ShowSnackbar(SnackbarMessage.METADATA_KEY_VERIFICATION_FAILURE),
                        )
                    },
                )
                updateViewState { copy(shouldShowDialogProgress = false) }
                createResourceIdlingResource.setIdle(true)
            }
        }
    }

    private fun updateResource() {
        onValid {
            launch {
                updateResourceIdlingResource.setIdle(false)
                updateViewState { copy(shouldShowDialogProgress = true) }
                val editedResource =
                    getLocalResourceUseCase
                        .execute(
                            GetLocalResourceUseCase.Input((mode as Edit).resourceId),
                        ).resource
                val resourceUpdateActionsInteractor =
                    get<ResourceUpdateActionsInteractor> { parametersOf(editedResource) }
                performResourceUpdateAction(
                    action = {
                        resourceUpdateActionsInteractor.updateGenericResource(
                            resourceModelHandler.contentType,
                            { resourceModelHandler.getResourceMetadataWithRequiredFields() },
                            { resourceModelHandler.getResourceSecretWithRequiredFields() },
                        )
                    },
                    doOnFailure = { emitSideEffect(ShowSnackbar(SnackbarMessage.COMMON_FAILURE)) },
                    doOnCryptoFailure = {
                        emitSideEffect(ShowSnackbar(SnackbarMessage.ENCRYPTION_FAILURE))
                    },
                    doOnSchemaValidationFailure = ::handleSchemaValidationFailure,
                    doOnSuccess = { emitSideEffect(NavigateBackWithEditSuccess(resourceModelHandler.resourceMetadata.name)) },
                    doOnCannotEditWithCurrentConfig = {
                        emitSideEffect(
                            ShowSnackbar(SnackbarMessage.CANNOT_CREATE_RESOURCE_WITH_CURRENT_CONFIG),
                        )
                    },
                    doOnMetadataKeyModified = {
                        updateViewState { copy(metadataKeyModifiedDialog = it) }
                    },
                    doOnMetadataKeyDeleted = {
                        updateViewState { copy(metadataKeyDeletedDialog = it) }
                    },
                    doOnMetadataKeyVerificationFailure = {
                        emitSideEffect(
                            ShowSnackbar(SnackbarMessage.METADATA_KEY_VERIFICATION_FAILURE),
                        )
                    },
                )
                updateViewState { copy(shouldShowDialogProgress = false) }
                updateResourceIdlingResource.setIdle(true)
            }
        }
    }

    private fun handleSchemaValidationFailure(entity: SchemaEntity) {
        when (entity) {
            SchemaEntity.RESOURCE ->
                emitSideEffect(ShowSnackbar(SnackbarMessage.JSON_SCHEMA_RESOURCE_VALIDATION_ERROR))
            SchemaEntity.SECRET ->
                emitSideEffect(ShowSnackbar(SnackbarMessage.JSON_SCHEMA_SECRET_VALIDATION_ERROR))
        }
    }

    private fun onValid(action: () -> Unit) {
        val resourceSecret = resourceModelHandler.resourceSecret
        when (uiModel.leadingContentType) {
            TOTP -> {
                val totpKey = resourceSecret.totp?.key
                if (totpKey.isNullOrBlank()) {
                    updateViewState { copy(totpData = totpData.copy(totpSecretError = MustNotBeEmpty)) }
                    return
                }
                if (!StringIsBase32.condition(totpKey)) {
                    updateViewState { copy(totpData = totpData.copy(totpSecretError = MustBeBase32)) }
                    return
                }
                action()
            }
            STANDALONE_NOTE -> {
                if (!StringMaxLength(NoteFormViewModel.NOTE_MAX_LENGTH)
                        .condition(resourceSecret.description.orEmpty())
                ) {
                    updateViewState {
                        copy(noteData = noteData.copy(noteError = MaxLengthExceeded(NoteFormViewModel.NOTE_MAX_LENGTH)))
                    }
                    return
                }
                action()
            }
            else -> action()
        }
    }

    private fun trustedMetadataKeyDeleted() {
        launch {
            metadataPrivateKeysHelperInteractor.deletedTrustedMetadataPrivateKey()
        }
    }

    private fun trustNewMetadataKey(model: NewMetadataKeyToTrustModel) {
        launch {
            updateViewState { copy(shouldShowDialogProgress = true) }
            when (
                val output =
                    runAuthenticatedOperation {
                        metadataPrivateKeysHelperInteractor.trustNewKey(model)
                    }
            ) {
                is MetadataPrivateKeysHelperInteractor.Output.Success ->
                    emitSideEffect(ShowSnackbar(SnackbarMessage.METADATA_KEY_IS_TRUSTED))
                else -> {
                    Timber.e("Failed to trust new metadata key: $output")
                    emitSideEffect(ShowSnackbar(SnackbarMessage.METADATA_KEY_TRUST_FAILED))
                }
            }
            updateViewState { copy(shouldShowDialogProgress = false) }
        }
    }
}
