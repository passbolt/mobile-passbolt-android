package com.passbolt.mobile.android.feature.otp.scanotp.scanotpsuccess

import com.passbolt.mobile.android.core.resources.actions.ResourceCreateActionsInteractor
import com.passbolt.mobile.android.core.resources.actions.ResourceUpdateActionResult
import com.passbolt.mobile.android.core.resources.actions.ResourceUpdateActionsInteractor
import com.passbolt.mobile.android.core.resources.actions.performResourceCreateAction
import com.passbolt.mobile.android.core.resources.actions.performResourceUpdateAction
import com.passbolt.mobile.android.core.resources.usecase.GetDefaultCreateContentTypeUseCase
import com.passbolt.mobile.android.core.resourcetypes.graph.redesigned.UpdateAction
import com.passbolt.mobile.android.core.resourcetypes.usecase.db.ResourceTypeIdToSlugMappingProvider
import com.passbolt.mobile.android.core.secrets.usecase.decrypt.parser.SecretJsonModel
import com.passbolt.mobile.android.feature.authentication.compose.AuthenticatedViewModel
import com.passbolt.mobile.android.feature.authentication.session.runAuthenticatedOperation
import com.passbolt.mobile.android.feature.otp.scanotp.scanotpsuccess.ScanOtpSuccessIntent.CreateStandaloneOtpClick
import com.passbolt.mobile.android.feature.otp.scanotp.scanotpsuccess.ScanOtpSuccessIntent.DismissNewMetadataTrustDialog
import com.passbolt.mobile.android.feature.otp.scanotp.scanotpsuccess.ScanOtpSuccessIntent.DismissTrustedMetadataKeyDeletedDialog
import com.passbolt.mobile.android.feature.otp.scanotp.scanotpsuccess.ScanOtpSuccessIntent.LinkToResourceClick
import com.passbolt.mobile.android.feature.otp.scanotp.scanotpsuccess.ScanOtpSuccessIntent.LinkedResourceReceived
import com.passbolt.mobile.android.feature.otp.scanotp.scanotpsuccess.ScanOtpSuccessIntent.TrustNewMetadataKey
import com.passbolt.mobile.android.feature.otp.scanotp.scanotpsuccess.ScanOtpSuccessIntent.TrustedMetadataKeyDeleted
import com.passbolt.mobile.android.feature.otp.scanotp.scanotpsuccess.ScanOtpSuccessSideEffect.NavigateToOtpList
import com.passbolt.mobile.android.feature.otp.scanotp.scanotpsuccess.ScanOtpSuccessSideEffect.NavigateToResourcePicker
import com.passbolt.mobile.android.feature.otp.scanotp.scanotpsuccess.ScanOtpSuccessSideEffect.ShowErrorSnackbar
import com.passbolt.mobile.android.feature.otp.scanotp.scanotpsuccess.ScanOtpSuccessSideEffect.ShowSuccessSnackbar
import com.passbolt.mobile.android.jsonmodel.delegates.TotpSecret
import com.passbolt.mobile.android.metadata.interactor.MetadataPrivateKeysHelperInteractor
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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koin.core.parameter.parametersOf
import timber.log.Timber
import java.util.UUID

internal class ScanOtpSuccessViewModel(
    private val scannedTotp: OtpParseResult.OtpQr.TotpQr,
    private val parentFolderId: String?,
    private val idToSlugMappingProvider: ResourceTypeIdToSlugMappingProvider,
    private val getDefaultCreateContentTypeUseCase: GetDefaultCreateContentTypeUseCase,
    private val metadataPrivateKeysHelperInteractor: MetadataPrivateKeysHelperInteractor,
) : AuthenticatedViewModel<ScanOtpSuccessState, ScanOtpSuccessSideEffect>(ScanOtpSuccessState()),
    KoinComponent {
    fun onIntent(intent: ScanOtpSuccessIntent) {
        when (intent) {
            CreateStandaloneOtpClick -> createStandaloneOtp()
            LinkToResourceClick -> emitSideEffect(NavigateToResourcePicker(scannedTotp.issuer))
            is LinkedResourceReceived -> linkedResourceReceived(intent.resource)
            TrustNewMetadataKey -> viewState.value.metadataKeyToTrust?.let { trustNewMetadataKey(it) }
            TrustedMetadataKeyDeleted -> trustedMetadataKeyDeleted()
            DismissNewMetadataTrustDialog -> updateViewState { copy(showNewMetadataTrustDialog = false) }
            DismissTrustedMetadataKeyDeletedDialog -> updateViewState { copy(showTrustedMetadataKeyDeletedDialog = false) }
        }
    }

    private fun createStandaloneOtp() {
        launch {
            updateViewState { copy(showProgress = true) }
            val resourceCreateActionsInteractor = get<ResourceCreateActionsInteractor>()
            val defaultType =
                getDefaultCreateContentTypeUseCase.execute(
                    GetDefaultCreateContentTypeUseCase.Input(LeadingContentType.TOTP),
                )

            if (defaultType is GetDefaultCreateContentTypeUseCase.Output.CreationContentType) {
                performResourceCreateAction(
                    action = {
                        resourceCreateActionsInteractor.createGenericResource(
                            resourceParentFolderId = parentFolderId,
                            contentType = defaultType.contentType,
                            metadataJsonModel =
                                MetadataJsonModel.empty().apply {
                                    name = scannedTotp.label
                                    scannedTotp.issuer?.let {
                                        setMainUri(defaultType.contentType, it)
                                    }
                                },
                            secretJsonModel =
                                SecretJsonModel.emptyTotp().apply {
                                    totp = scannedTotp.toTotpSecret()
                                },
                        )
                    },
                    doOnFailure = { emitSideEffect(ShowErrorSnackbar(ErrorSnackbarType.GENERIC_ERROR)) },
                    doOnCryptoFailure = { emitSideEffect(ShowErrorSnackbar(ErrorSnackbarType.ENCRYPTION_ERROR, it)) },
                    doOnSchemaValidationFailure = ::handleSchemaValidationFailure,
                    doOnSuccess = {
                        emitSideEffect(NavigateToOtpList(scannedTotp, otpCreated = true, it.resourceId))
                    },
                    doOnCannotCreateWithCurrentConfig = {
                        emitSideEffect(ShowErrorSnackbar(ErrorSnackbarType.CANNOT_CREATE_WITH_CURRENT_CONFIG))
                    },
                    doOnMetadataKeyModified = {
                        updateViewState { copy(metadataKeyToTrust = it, showNewMetadataTrustDialog = true) }
                    },
                    doOnMetadataKeyDeleted = {
                        updateViewState { copy(metadataKeyDeleted = it, showTrustedMetadataKeyDeletedDialog = true) }
                    },
                    doOnMetadataKeyVerificationFailure = {
                        emitSideEffect(ShowErrorSnackbar(ErrorSnackbarType.FAILED_TO_VERIFY_METADATA_KEY))
                    },
                )
            } else {
                Timber.e("Could not determine default content type for TOTP")
            }
            updateViewState { copy(showProgress = false) }
        }
    }

    private fun handleSchemaValidationFailure(entity: SchemaEntity) {
        when (entity) {
            SchemaEntity.RESOURCE ->
                emitSideEffect(ShowErrorSnackbar(ErrorSnackbarType.JSON_RESOURCE_SCHEMA_VALIDATION_ERROR))
            SchemaEntity.SECRET ->
                emitSideEffect(ShowErrorSnackbar(ErrorSnackbarType.JSON_SECRET_SCHEMA_VALIDATION_ERROR))
        }
    }

    private fun linkedResourceReceived(resource: ResourceModel) {
        launch {
            updateViewState { copy(showProgress = true) }
            val updateOperation = createLinkTotpOperation(resource)
            performLinkTotpUpdate(updateOperation)
            updateViewState { copy(showProgress = false) }
        }
    }

    private suspend fun createLinkTotpOperation(resource: ResourceModel): suspend () -> Flow<ResourceUpdateActionResult> {
        val slug =
            idToSlugMappingProvider.provideMappingForSelectedAccount()[UUID.fromString(resource.resourceTypeId)]
                ?: return { emptyFlow() }

        val resourceUpdateActionsInteractor = get<ResourceUpdateActionsInteractor> { parametersOf(resource) }

        return when (ContentType.fromSlug(slug)) {
            is PasswordAndDescription, V5Default, is PasswordDescriptionTotp, V5DefaultWithTotp ->
                suspend {
                    resourceUpdateActionsInteractor.updateGenericResource(
                        updateAction = UpdateAction.ADD_TOTP,
                        secretModification = {
                            it.apply { totp = scannedTotp.toTotpSecret() }
                        },
                    )
                }
            else -> throw IllegalArgumentException("$slug resource type is not possible to link")
        }
    }

    private suspend fun performLinkTotpUpdate(updateOperation: suspend () -> Flow<ResourceUpdateActionResult>) {
        performResourceUpdateAction(
            action = updateOperation,
            doOnFailure = { emitSideEffect(ShowErrorSnackbar(ErrorSnackbarType.GENERIC_ERROR)) },
            doOnFetchFailure = { emitSideEffect(ShowErrorSnackbar(ErrorSnackbarType.GENERIC_ERROR)) },
            doOnCryptoFailure = { emitSideEffect(ShowErrorSnackbar(ErrorSnackbarType.ENCRYPTION_ERROR, it)) },
            doOnSchemaValidationFailure = ::handleSchemaValidationFailure,
            doOnSuccess = {
                emitSideEffect(NavigateToOtpList(totp = scannedTotp, otpCreated = true, resourceId = it.resourceId))
            },
            doOnCannotEditWithCurrentConfig = {
                emitSideEffect(ShowErrorSnackbar(ErrorSnackbarType.CANNOT_CREATE_WITH_CURRENT_CONFIG))
            },
            doOnMetadataKeyModified = {
                updateViewState { copy(metadataKeyToTrust = it, showNewMetadataTrustDialog = true) }
            },
            doOnMetadataKeyDeleted = {
                updateViewState { copy(metadataKeyDeleted = it, showTrustedMetadataKeyDeletedDialog = true) }
            },
            doOnMetadataKeyVerificationFailure = {
                emitSideEffect(ShowErrorSnackbar(ErrorSnackbarType.FAILED_TO_VERIFY_METADATA_KEY))
            },
        )
    }

    private fun trustNewMetadataKey(model: NewMetadataKeyToTrustModel) {
        launch {
            updateViewState { copy(showProgress = true, showNewMetadataTrustDialog = false, metadataKeyToTrust = null) }
            when (
                val output =
                    runAuthenticatedOperation {
                        metadataPrivateKeysHelperInteractor.trustNewKey(model)
                    }
            ) {
                is MetadataPrivateKeysHelperInteractor.Output.Success ->
                    emitSideEffect(ShowSuccessSnackbar(SuccessSnackbarType.NEW_METADATA_KEY_IS_TRUSTED))
                else -> {
                    Timber.e("Failed to trust new metadata key: $output")
                    emitSideEffect(ShowErrorSnackbar(ErrorSnackbarType.FAILED_TO_TRUST_METADATA_KEY))
                }
            }
            updateViewState { copy(showProgress = false) }
        }
    }

    private fun trustedMetadataKeyDeleted() {
        launch {
            updateViewState {
                copy(metadataKeyDeleted = null, showTrustedMetadataKeyDeletedDialog = false)
            }
            metadataPrivateKeysHelperInteractor.deletedTrustedMetadataPrivateKey()
        }
    }

    private fun OtpParseResult.OtpQr.TotpQr.toTotpSecret() =
        TotpSecret(
            algorithm = algorithm.name,
            key = secret,
            period = period,
            digits = digits,
        )
}
