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

package com.passbolt.mobile.android.feature.otp.screen

import androidx.lifecycle.viewModelScope
import com.passbolt.mobile.android.common.coroutinetimer.TimerFactory
import com.passbolt.mobile.android.common.search.SearchableMatcher
import com.passbolt.mobile.android.core.accounts.usecase.accountdata.GetSelectedAccountDataUseCase
import com.passbolt.mobile.android.core.fulldatarefresh.DataRefreshStatus.Finished
import com.passbolt.mobile.android.core.fulldatarefresh.DataRefreshStatus.InProgress
import com.passbolt.mobile.android.core.fulldatarefresh.FullDataRefreshExecutor
import com.passbolt.mobile.android.core.mvp.coroutinecontext.CoroutineLaunchContext
import com.passbolt.mobile.android.core.otpcore.TotpParametersProvider
import com.passbolt.mobile.android.core.otpcore.TotpParametersProvider.OtpParametersResult.InvalidTotpInput
import com.passbolt.mobile.android.core.otpcore.TotpParametersProvider.OtpParametersResult.OtpParameters
import com.passbolt.mobile.android.core.resources.actions.ResourceCommonActionsInteractor
import com.passbolt.mobile.android.core.resources.actions.ResourceUpdateActionsInteractor
import com.passbolt.mobile.android.core.resources.actions.SecretPropertiesActionsInteractor
import com.passbolt.mobile.android.core.resources.actions.SecretPropertyActionResult
import com.passbolt.mobile.android.core.resources.actions.performCommonResourceAction
import com.passbolt.mobile.android.core.resources.actions.performResourceUpdateAction
import com.passbolt.mobile.android.core.resources.actions.performSecretPropertyAction
import com.passbolt.mobile.android.core.resources.usecase.db.GetLocalResourcesUseCase
import com.passbolt.mobile.android.core.resourcetypes.graph.redesigned.UpdateAction
import com.passbolt.mobile.android.core.resourcetypes.usecase.db.ResourceTypeIdToSlugMappingProvider
import com.passbolt.mobile.android.core.ui.compose.search.SearchInputEndIconMode.AVATAR
import com.passbolt.mobile.android.core.ui.compose.search.SearchInputEndIconMode.CLEAR
import com.passbolt.mobile.android.feature.authentication.compose.AuthenticatedViewModel
import com.passbolt.mobile.android.feature.authentication.session.runAuthenticatedOperation
import com.passbolt.mobile.android.feature.otp.screen.OtpIntent.CloseCreateResourceMenu
import com.passbolt.mobile.android.feature.otp.screen.OtpIntent.CloseDeleteConfirmationDialog
import com.passbolt.mobile.android.feature.otp.screen.OtpIntent.CloseOtpMoreMenu
import com.passbolt.mobile.android.feature.otp.screen.OtpIntent.CloseSwitchAccount
import com.passbolt.mobile.android.feature.otp.screen.OtpIntent.CloseTrustNewKeyDialog
import com.passbolt.mobile.android.feature.otp.screen.OtpIntent.CloseTrustedKeyDeletedDialog
import com.passbolt.mobile.android.feature.otp.screen.OtpIntent.ConfirmDeleteTotp
import com.passbolt.mobile.android.feature.otp.screen.OtpIntent.CopyOtp
import com.passbolt.mobile.android.feature.otp.screen.OtpIntent.CreatePassword
import com.passbolt.mobile.android.feature.otp.screen.OtpIntent.CreateTotp
import com.passbolt.mobile.android.feature.otp.screen.OtpIntent.DeleteOtp
import com.passbolt.mobile.android.feature.otp.screen.OtpIntent.EditOtp
import com.passbolt.mobile.android.feature.otp.screen.OtpIntent.InitiateFullDataRefresh
import com.passbolt.mobile.android.feature.otp.screen.OtpIntent.OpenCreateResourceMenu
import com.passbolt.mobile.android.feature.otp.screen.OtpIntent.OpenOtpMoreMenu
import com.passbolt.mobile.android.feature.otp.screen.OtpIntent.OtpQRScanReturned
import com.passbolt.mobile.android.feature.otp.screen.OtpIntent.ResourceFormReturned
import com.passbolt.mobile.android.feature.otp.screen.OtpIntent.RevealOtp
import com.passbolt.mobile.android.feature.otp.screen.OtpIntent.Search
import com.passbolt.mobile.android.feature.otp.screen.OtpIntent.SearchEndIconAction
import com.passbolt.mobile.android.feature.otp.screen.OtpIntent.TrustMetadataKeyDeletion
import com.passbolt.mobile.android.feature.otp.screen.OtpIntent.TrustNewMetadataKey
import com.passbolt.mobile.android.feature.otp.screen.OtpSideEffect.CopyToClipboard
import com.passbolt.mobile.android.feature.otp.screen.OtpSideEffect.NavigateToCreateResourceForm
import com.passbolt.mobile.android.feature.otp.screen.OtpSideEffect.NavigateToCreateTotp
import com.passbolt.mobile.android.feature.otp.screen.OtpSideEffect.NavigateToEditResourceForm
import com.passbolt.mobile.android.feature.otp.screen.OtpSideEffect.ShowErrorSnackbar
import com.passbolt.mobile.android.feature.otp.screen.OtpSideEffect.ShowSuccessSnackbar
import com.passbolt.mobile.android.feature.otp.screen.SnackbarErrorType.CANNOT_UPDATE_WITH_CURRENT_CONFIGURATION
import com.passbolt.mobile.android.feature.otp.screen.SnackbarErrorType.DECRYPTION_FAILURE
import com.passbolt.mobile.android.feature.otp.screen.SnackbarErrorType.ERROR
import com.passbolt.mobile.android.feature.otp.screen.SnackbarErrorType.FAILED_TO_DELETE_RESOURCE
import com.passbolt.mobile.android.feature.otp.screen.SnackbarErrorType.FAILED_TO_TRUST_METADATA_KEY
import com.passbolt.mobile.android.feature.otp.screen.SnackbarErrorType.FAILED_TO_VERIFY_METADATA_KEYS
import com.passbolt.mobile.android.feature.otp.screen.SnackbarErrorType.FETCH_FAILURE
import com.passbolt.mobile.android.feature.otp.screen.SnackbarErrorType.RESOURCE_SCHEMA_INVALID
import com.passbolt.mobile.android.feature.otp.screen.SnackbarErrorType.SECRET_SCHEMA_INVALID
import com.passbolt.mobile.android.feature.otp.screen.SnackbarSuccessType.METADATA_KEY_IS_TRUSTED
import com.passbolt.mobile.android.feature.otp.screen.SnackbarSuccessType.RESOURCE_CREATED
import com.passbolt.mobile.android.feature.otp.screen.SnackbarSuccessType.RESOURCE_DELETED
import com.passbolt.mobile.android.feature.otp.screen.SnackbarSuccessType.RESOURCE_EDITED
import com.passbolt.mobile.android.jsonmodel.delegates.TotpSecret
import com.passbolt.mobile.android.mappers.OtpModelMapper
import com.passbolt.mobile.android.metadata.interactor.MetadataPrivateKeysHelperInteractor
import com.passbolt.mobile.android.serializers.jsonschema.SchemaEntity.RESOURCE
import com.passbolt.mobile.android.serializers.jsonschema.SchemaEntity.SECRET
import com.passbolt.mobile.android.supportedresourceTypes.ContentType
import com.passbolt.mobile.android.supportedresourceTypes.ContentType.PasswordDescriptionTotp
import com.passbolt.mobile.android.supportedresourceTypes.ContentType.Totp
import com.passbolt.mobile.android.supportedresourceTypes.ContentType.V5DefaultWithTotp
import com.passbolt.mobile.android.supportedresourceTypes.ContentType.V5TotpStandalone
import com.passbolt.mobile.android.supportedresourceTypes.SupportedContentTypes.totpSlugs
import com.passbolt.mobile.android.ui.LeadingContentType.PASSWORD
import com.passbolt.mobile.android.ui.LeadingContentType.TOTP
import com.passbolt.mobile.android.ui.NewMetadataKeyToTrustModel
import com.passbolt.mobile.android.ui.OtpItemWrapper
import com.passbolt.mobile.android.ui.ResourceModel
import com.passbolt.mobile.android.ui.allReset
import com.passbolt.mobile.android.ui.findVisible
import com.passbolt.mobile.android.ui.isExpired
import com.passbolt.mobile.android.ui.refreshingNone
import com.passbolt.mobile.android.ui.refreshingOnly
import com.passbolt.mobile.android.ui.replaceOnId
import com.passbolt.mobile.android.ui.revealed
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koin.core.parameter.parametersOf
import timber.log.Timber
import java.util.UUID
import kotlin.time.Duration.Companion.seconds

internal class OtpViewModel(
    private val getSelectedAccountDataUseCase: GetSelectedAccountDataUseCase,
    private val searchableMatcher: SearchableMatcher,
    private val getLocalResourcesUseCase: GetLocalResourcesUseCase,
    private val otpModelMapper: OtpModelMapper,
    private val totpParametersProvider: TotpParametersProvider,
    private val coroutineLaunchContext: CoroutineLaunchContext,
    private val fullDataRefreshExecutor: FullDataRefreshExecutor,
    private val idToSlugMappingProvider: ResourceTypeIdToSlugMappingProvider,
    private val metadataPrivateKeysHelperInteractor: MetadataPrivateKeysHelperInteractor,
    private val timerFactory: TimerFactory,
) : AuthenticatedViewModel<OtpState, OtpSideEffect>(OtpState()),
    KoinComponent {
    init {
        fullDataRefreshExecutor.attach(this)
        loadUserAvatar()
        viewModelScope.launch(coroutineLaunchContext.io) {
            synchronizeWithDataRefresh()
        }
        viewModelScope.launch(coroutineLaunchContext.io) {
            val otps = getOtpResources()
            updateViewState { copy(otps = otps) }
            updateOtpsCounterTime()
        }
    }

    override fun onCleared() {
        fullDataRefreshExecutor.detach()
        super.onCleared()
    }

    // TODO refactor after feature completion
    @Suppress("CyclomaticComplexMethod")
    fun onIntent(intent: OtpIntent) {
        when (intent) {
            OpenCreateResourceMenu -> updateViewState { copy(showCreateResourceBottomSheet = true) }
            CloseCreateResourceMenu -> updateViewState { copy(showCreateResourceBottomSheet = false) }
            InitiateFullDataRefresh -> fullDataRefreshExecutor.performFullDataRefresh()
            is Search -> searchQueryChanged(intent.searchQuery)
            is RevealOtp -> otpClick(intent.otpItemWrapper)
            is OpenOtpMoreMenu -> updateViewState { copy(showOtpMoreBottomSheet = true, moreMenuResource = intent.otpItemWrapper) }
            is CloseOtpMoreMenu -> updateViewState { copy(showOtpMoreBottomSheet = false) }
            CreatePassword -> emitSideEffect(NavigateToCreateResourceForm(leadingContentType = PASSWORD))
            CreateTotp -> emitSideEffect(NavigateToCreateTotp)
            is OtpQRScanReturned -> processOtpScanResult(intent)
            is ResourceFormReturned -> processResourceFormResult(intent)
            is CopyOtp -> copyTotp(intent.otpItemWrapper)
            is DeleteOtp -> updateViewState { copy(showDeleteTotpConfirmationDialog = true) }
            is EditOtp ->
                emitSideEffect(
                    NavigateToEditResourceForm(
                        resourceId = intent.otpItemWrapper.resource.resourceId,
                        resourceName = intent.otpItemWrapper.resource.metadataJsonModel.name,
                    ),
                )
            CloseDeleteConfirmationDialog -> updateViewState { copy(showDeleteTotpConfirmationDialog = false) }
            ConfirmDeleteTotp -> deleteTotp(viewState.value.moreMenuResource)
            CloseTrustedKeyDeletedDialog ->
                updateViewState {
                    copy(
                        showMetadataTrustedKeyDeletedDialog = false,
                        metadataDeletedKeyModel = null,
                    )
                }
            is TrustMetadataKeyDeletion -> deleteTrustedMetadataKeyConfirmed()
            CloseTrustNewKeyDialog ->
                updateViewState {
                    copy(
                        showNewMetadataTrustDialog = false,
                        newMetadataKeyTrustModel = null,
                    )
                }
            is TrustNewMetadataKey -> trustNewMetadataKeyConfirmed(intent.model)
            CloseSwitchAccount -> updateViewState { copy(showAccountSwitchBottomSheet = false) }
            SearchEndIconAction -> searchEndIconAction()
        }
    }

    private fun searchEndIconAction() {
        when (viewState.value.searchInputEndIconMode) {
            AVATAR -> updateViewState { copy(showAccountSwitchBottomSheet = true) }
            CLEAR ->
                updateViewState {
                    copy(
                        searchQuery = "",
                        searchInputEndIconMode = AVATAR,
                    )
                }
        }
    }

    private fun trustNewMetadataKeyConfirmed(model: NewMetadataKeyToTrustModel) {
        updateViewState { copy(showProgress = true) }
        viewModelScope.launch(coroutineLaunchContext.io) {
            when (
                val output =
                    runAuthenticatedOperation(this@OtpViewModel) {
                        metadataPrivateKeysHelperInteractor.trustNewKey(model)
                    }
            ) {
                is MetadataPrivateKeysHelperInteractor.Output.Success ->
                    emitSideEffect(ShowSuccessSnackbar(METADATA_KEY_IS_TRUSTED))
                else -> {
                    Timber.e("Failed to trust new metadata key: $output")
                    emitSideEffect(ShowErrorSnackbar(FAILED_TO_TRUST_METADATA_KEY))
                }
            }
            updateViewState {
                copy(
                    showNewMetadataTrustDialog = false,
                    newMetadataKeyTrustModel = null,
                )
            }
        }
    }

    private fun deleteTrustedMetadataKeyConfirmed() {
        viewModelScope.launch(coroutineLaunchContext.io) {
            metadataPrivateKeysHelperInteractor.deletedTrustedMetadataPrivateKey()
            updateViewState {
                copy(
                    showMetadataTrustedKeyDeletedDialog = false,
                    metadataDeletedKeyModel = null,
                )
            }
        }
    }

    private fun deleteTotp(moreMenuResource: OtpItemWrapper?) {
        updateViewState { copy(showProgress = true) }
        viewModelScope.launch(coroutineLaunchContext.io) {
            val otpResource = requireNotNull(moreMenuResource)
            val slug =
                idToSlugMappingProvider.provideMappingForSelectedAccount()[
                    UUID.fromString(otpResource.resource.resourceTypeId),
                ]
            when (val contentType = ContentType.fromSlug(slug!!)) {
                is Totp, V5TotpStandalone ->
                    deleteStandaloneTotpResource(otpResource.resource)
                is PasswordDescriptionTotp, V5DefaultWithTotp ->
                    downgradeToPasswordAndDescriptionResource(otpResource.resource)
                else ->
                    error("$contentType type should not be presented on totp list")
            }
            updateViewState { copy(showProgress = false) }
        }
    }

    private suspend fun deleteStandaloneTotpResource(otpResource: ResourceModel) {
        val resourceCommonActionsInteractor =
            get<ResourceCommonActionsInteractor> {
                parametersOf(otpResource, needSessionRefreshFlow, sessionRefreshedFlow)
            }
        performCommonResourceAction(
            action = { resourceCommonActionsInteractor.deleteResource() },
            doOnFailure = { emitSideEffect(ShowErrorSnackbar(FAILED_TO_DELETE_RESOURCE)) },
            doOnSuccess = {
                emitSideEffect(ShowSuccessSnackbar(RESOURCE_DELETED))
                fullDataRefreshExecutor.performFullDataRefresh()
            },
        )
    }

    private suspend fun downgradeToPasswordAndDescriptionResource(otpResource: ResourceModel) {
        val resourceUpdateActionInteractor =
            get<ResourceUpdateActionsInteractor> {
                parametersOf(otpResource, needSessionRefreshFlow, sessionRefreshedFlow)
            }

        performResourceUpdateAction(
            action = {
                resourceUpdateActionInteractor.updateGenericResource(
                    UpdateAction.REMOVE_TOTP,
                    secretModification = { it.apply { totp = null } },
                )
            },
            doOnCryptoFailure = { emitSideEffect(ShowErrorSnackbar(SnackbarErrorType.ENCRYPTION_FAILURE)) },
            doOnFailure = { emitSideEffect(ShowErrorSnackbar(ERROR)) },
            doOnSuccess = {
                emitSideEffect(ShowSuccessSnackbar(RESOURCE_DELETED))
                fullDataRefreshExecutor.performFullDataRefresh()
            },
            doOnSchemaValidationFailure = {
                when (it) {
                    RESOURCE -> emitSideEffect(ShowErrorSnackbar(RESOURCE_SCHEMA_INVALID))
                    SECRET -> emitSideEffect(ShowErrorSnackbar(SECRET_SCHEMA_INVALID))
                }
            },
            doOnFetchFailure = { emitSideEffect(ShowErrorSnackbar(FETCH_FAILURE)) },
            doOnCannotEditWithCurrentConfig = { emitSideEffect(ShowErrorSnackbar(CANNOT_UPDATE_WITH_CURRENT_CONFIGURATION)) },
            doOnMetadataKeyModified = {
                updateViewState { copy(showNewMetadataTrustDialog = true, newMetadataKeyTrustModel = it) }
            },
            doOnMetadataKeyDeleted = {
                updateViewState {
                    copy(
                        showMetadataTrustedKeyDeletedDialog = true,
                        metadataDeletedKeyModel = it,
                    )
                }
            },
            doOnMetadataKeyVerificationFailure = { emitSideEffect(ShowErrorSnackbar(FAILED_TO_VERIFY_METADATA_KEYS)) },
        )
    }

    private fun copyTotp(otpItemWrapper: OtpItemWrapper) {
        fetchTotp(otpItemWrapper) { totp ->
            val otpParameters =
                totpParametersProvider.provideOtpParameters(
                    secretKey = totp.result.key,
                    digits = totp.result.digits,
                    period = totp.result.period,
                    algorithm = totp.result.algorithm,
                )

            when (otpParameters) {
                InvalidTotpInput -> stopRefreshingAndShowError("Failed to generate totp parameters")
                is OtpParameters -> {
                    emitSideEffect(
                        CopyToClipboard(
                            label = totp.label,
                            value = otpParameters.otpValue,
                            isSensitive = true,
                        ),
                    )
                }
            }
        }
    }

    private fun processResourceFormResult(intent: ResourceFormReturned) {
        if (intent.resourceCreated) {
            fullDataRefreshExecutor.performFullDataRefresh()
            emitSideEffect(ShowSuccessSnackbar(RESOURCE_CREATED, intent.resourceName))
        }
        if (intent.resourceEdited) {
            fullDataRefreshExecutor.performFullDataRefresh()
            emitSideEffect(ShowSuccessSnackbar(RESOURCE_EDITED))
        }
    }

    private fun processOtpScanResult(intent: OtpQRScanReturned) {
        if (intent.otpCreated) {
            fullDataRefreshExecutor.performFullDataRefresh()
        } else {
            if (intent.otpManualCreationChosen) {
                emitSideEffect(NavigateToCreateResourceForm(leadingContentType = TOTP))
            }
        }
    }

    private fun searchQueryChanged(searchQuery: String) {
        val searchEndIcon = if (searchQuery.isNotBlank()) CLEAR else AVATAR
        val filteredOtps =
            viewState.value.otps.filter {
                searchableMatcher.matches(it, searchQuery)
            }
        updateViewState {
            copy(
                searchInputEndIconMode = searchEndIcon,
                searchQuery = searchQuery,
                filteredOtps = filteredOtps,
            )
        }
    }

    private fun otpClick(otpItemWrapper: OtpItemWrapper) {
        fetchTotp(otpItemWrapper) {
            showTotp(it, otpItemWrapper.resource.resourceId)
        }
    }

    private fun fetchTotp(
        otpItemWrapper: OtpItemWrapper,
        afterFetchAction: (SecretPropertyActionResult.Success<TotpSecret>) -> Unit,
    ) {
        viewModelScope.launch(coroutineLaunchContext.io) {
            updateViewState {
                copy(otps = otps.refreshingOnly(otpItemWrapper.resource.resourceId))
            }

            val secretPropertiesActionsInteractor =
                get<SecretPropertiesActionsInteractor> {
                    parametersOf(otpItemWrapper.resource, needSessionRefreshFlow, sessionRefreshedFlow)
                }

            performSecretPropertyAction(
                action = { secretPropertiesActionsInteractor.provideOtp() },
                doOnDecryptionFailure = {
                    emitSideEffect(ShowErrorSnackbar(DECRYPTION_FAILURE))
                    updateViewState { copy(otps = otps.refreshingNone()) }
                },
                doOnFetchFailure = {
                    emitSideEffect(ShowErrorSnackbar(FETCH_FAILURE))
                    updateViewState { copy(otps = otps.refreshingNone()) }
                },
                doOnSuccess = { result ->
                    afterFetchAction(result)
                },
            )
        }
    }

    private fun showTotp(
        totp: SecretPropertyActionResult.Success<TotpSecret>,
        resourceId: String,
    ) {
        if (totp.result.key.isBlank()) {
            stopRefreshingAndShowError("Fetched totp key is empty")
        }

        val otpParameters =
            totpParametersProvider.provideOtpParameters(
                secretKey = totp.result.key,
                digits = totp.result.digits,
                period = totp.result.period,
                algorithm = totp.result.algorithm,
            )

        when (otpParameters) {
            InvalidTotpInput -> stopRefreshingAndShowError("Failed to generate totp parameters")
            is OtpParameters -> {
                updateViewState {
                    copy(
                        otps =
                            otps.revealed(
                                resourceId,
                                otpParameters.otpValue,
                                totp.result.period,
                                otpParameters.secondsValid,
                            ),
                    )
                }

                emitSideEffect(
                    CopyToClipboard(
                        label = totp.label,
                        value = otpParameters.otpValue,
                        isSensitive = true,
                    ),
                )
            }
        }
    }

    private suspend fun updateOtpsCounterTime() {
        timerFactory.createInfiniteTimer(tickDuration = 1.seconds).collectLatest {
            val visibleTotp = viewState.value.otps.findVisible()
            if (visibleTotp != null) {
                val updated = visibleTotp.copy(remainingSecondsCounter = (visibleTotp.remainingSecondsCounter!!) - 1)

                if (updated.isExpired()) {
                    updateViewState { copy(otps = otps.allReset()) }
                    fetchTotp(updated) {
                        showTotp(it, updated.resource.resourceId)
                    }
                } else {
                    updateViewState { copy(otps = otps.replaceOnId(updated)) }
                }
            }
        }
    }

    private suspend fun synchronizeWithDataRefresh() {
        fullDataRefreshExecutor.dataRefreshStatusFlow.collect {
            when (it) {
                InProgress -> updateViewState { copy(isRefreshing = true) }
                is Finished -> {
                    val otps = getOtpResources()
                    updateViewState { copy(isRefreshing = false, otps = otps) }
                }
            }
        }
    }

    private fun loadUserAvatar() {
        val avatarUrl =
            getSelectedAccountDataUseCase
                .execute(Unit)
                .avatarUrl

        updateViewState { copy(userAvatar = avatarUrl) }
    }

    private suspend fun getOtpResources(): List<OtpItemWrapper> =
        getLocalResourcesUseCase
            .execute(GetLocalResourcesUseCase.Input(totpSlugs))
            .resources
            .map(otpModelMapper::map)

    private fun stopRefreshingAndShowError(message: String) {
        Timber.e(message)
        emitSideEffect(ShowErrorSnackbar(ERROR, message))
        updateViewState { copy(otps = otps.refreshingNone()) }
    }
}
