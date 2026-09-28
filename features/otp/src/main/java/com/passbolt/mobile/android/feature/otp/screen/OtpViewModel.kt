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
import com.passbolt.mobile.android.common.datarefresh.DataRefreshStatus.Idle.FinishedWithFailure
import com.passbolt.mobile.android.common.datarefresh.DataRefreshStatus.Idle.FinishedWithSuccess
import com.passbolt.mobile.android.common.datarefresh.DataRefreshStatus.Idle.NotCompleted
import com.passbolt.mobile.android.common.datarefresh.DataRefreshStatus.InProgress
import com.passbolt.mobile.android.common.datarefresh.DataRefreshTrackingFlow
import com.passbolt.mobile.android.common.time.TimeProvider
import com.passbolt.mobile.android.common.urimatcher.AutofillUriMatcher
import com.passbolt.mobile.android.core.compose.SideEffectViewModel
import com.passbolt.mobile.android.core.mvp.coroutinecontext.CoroutineLaunchContext
import com.passbolt.mobile.android.core.otpcore.TotpParametersProvider
import com.passbolt.mobile.android.core.otpcore.TotpParametersProvider.OtpParametersResult.InvalidTotpInput
import com.passbolt.mobile.android.core.otpcore.TotpParametersProvider.OtpParametersResult.OtpParameters
import com.passbolt.mobile.android.core.resourcetypes.graph.redesigned.UpdateAction
import com.passbolt.mobile.android.core.ui.search.SearchInputEndIconMode.AVATAR
import com.passbolt.mobile.android.core.ui.search.SearchInputEndIconMode.CLEAR
import com.passbolt.mobile.android.core.ui.search.SearchInputEndIconMode.NONE
import com.passbolt.mobile.android.domain.accounts.usecase.GetSelectedAccountDataUseCase
import com.passbolt.mobile.android.domain.metadata.interactor.MetadataPrivateKeysHelperInteractor
import com.passbolt.mobile.android.domain.metadata.interactor.ResourceAccessInteractor
import com.passbolt.mobile.android.domain.resources.actions.ResourceCommonActionsInteractor
import com.passbolt.mobile.android.domain.resources.actions.ResourceUpdateActionsInteractorFactory
import com.passbolt.mobile.android.domain.resources.actions.SecretPropertiesActionsInteractorFactory
import com.passbolt.mobile.android.domain.resources.actions.SecretPropertyActionResult
import com.passbolt.mobile.android.domain.resources.actions.performCommonResourceAction
import com.passbolt.mobile.android.domain.resources.actions.performResourceUpdateAction
import com.passbolt.mobile.android.domain.resources.actions.performSecretPropertyAction
import com.passbolt.mobile.android.domain.resources.mapper.toOtpItemWrapper
import com.passbolt.mobile.android.domain.resources.usecase.EditPermissionsConfirmationInteractor
import com.passbolt.mobile.android.domain.resources.usecase.db.GetLocalResourcesUseCase
import com.passbolt.mobile.android.feature.authentication.session.runAuthenticatedOperation
import com.passbolt.mobile.android.feature.home.screen.ShowSuggestedModel
import com.passbolt.mobile.android.feature.otp.screen.OtpIntent.CloseDeleteConfirmationDialog
import com.passbolt.mobile.android.feature.otp.screen.OtpIntent.CloseOtpMoreMenu
import com.passbolt.mobile.android.feature.otp.screen.OtpIntent.CloseSwitchAccount
import com.passbolt.mobile.android.feature.otp.screen.OtpIntent.CloseTrustNewKeyDialog
import com.passbolt.mobile.android.feature.otp.screen.OtpIntent.CloseTrustedKeyDeletedDialog
import com.passbolt.mobile.android.feature.otp.screen.OtpIntent.ConfirmDeleteTotp
import com.passbolt.mobile.android.feature.otp.screen.OtpIntent.ConfirmedPermissionsResult
import com.passbolt.mobile.android.feature.otp.screen.OtpIntent.CopyOtp
import com.passbolt.mobile.android.feature.otp.screen.OtpIntent.CreateTotp
import com.passbolt.mobile.android.feature.otp.screen.OtpIntent.DeleteOtp
import com.passbolt.mobile.android.feature.otp.screen.OtpIntent.Dispose
import com.passbolt.mobile.android.feature.otp.screen.OtpIntent.EditOtp
import com.passbolt.mobile.android.feature.otp.screen.OtpIntent.OpenOtpMoreMenu
import com.passbolt.mobile.android.feature.otp.screen.OtpIntent.OtpQRScanReturned
import com.passbolt.mobile.android.feature.otp.screen.OtpIntent.ResourceFormReturned
import com.passbolt.mobile.android.feature.otp.screen.OtpIntent.RevealOtp
import com.passbolt.mobile.android.feature.otp.screen.OtpIntent.Search
import com.passbolt.mobile.android.feature.otp.screen.OtpIntent.SearchEndIconAction
import com.passbolt.mobile.android.feature.otp.screen.OtpIntent.TrustMetadataKeyDeletion
import com.passbolt.mobile.android.feature.otp.screen.OtpIntent.TrustNewMetadataKey
import com.passbolt.mobile.android.feature.otp.screen.OtpSideEffect.CopyToClipboard
import com.passbolt.mobile.android.feature.otp.screen.OtpSideEffect.InitiateDataRefresh
import com.passbolt.mobile.android.feature.otp.screen.OtpSideEffect.NavigateToConfirmPermissions
import com.passbolt.mobile.android.feature.otp.screen.OtpSideEffect.NavigateToCreateResourceForm
import com.passbolt.mobile.android.feature.otp.screen.OtpSideEffect.NavigateToCreateTotp
import com.passbolt.mobile.android.feature.otp.screen.OtpSideEffect.NavigateToEditResourceForm
import com.passbolt.mobile.android.feature.otp.screen.OtpSideEffect.ShowErrorSnackbar
import com.passbolt.mobile.android.feature.otp.screen.OtpSideEffect.ShowSuccessSnackbar
import com.passbolt.mobile.android.feature.otp.screen.OtpSideEffect.ShowToast
import com.passbolt.mobile.android.feature.otp.screen.SnackbarErrorType.CANNOT_UPDATE_WITH_CURRENT_CONFIGURATION
import com.passbolt.mobile.android.feature.otp.screen.SnackbarErrorType.DECRYPTION_FAILURE
import com.passbolt.mobile.android.feature.otp.screen.SnackbarErrorType.ERROR
import com.passbolt.mobile.android.feature.otp.screen.SnackbarErrorType.FAILED_TO_DELETE_RESOURCE
import com.passbolt.mobile.android.feature.otp.screen.SnackbarErrorType.FAILED_TO_REFRESH_DATA
import com.passbolt.mobile.android.feature.otp.screen.SnackbarErrorType.FAILED_TO_TRUST_METADATA_KEY
import com.passbolt.mobile.android.feature.otp.screen.SnackbarErrorType.FAILED_TO_VERIFY_METADATA_KEYS
import com.passbolt.mobile.android.feature.otp.screen.SnackbarErrorType.FETCH_FAILURE
import com.passbolt.mobile.android.feature.otp.screen.SnackbarErrorType.INVALID_TOTP_PARAMETERS
import com.passbolt.mobile.android.feature.otp.screen.SnackbarErrorType.NO_SHARED_KEY_ACCESS
import com.passbolt.mobile.android.feature.otp.screen.SnackbarErrorType.RESOURCE_SCHEMA_INVALID
import com.passbolt.mobile.android.feature.otp.screen.SnackbarErrorType.SECRET_SCHEMA_INVALID
import com.passbolt.mobile.android.feature.otp.screen.SnackbarSuccessType.METADATA_KEY_IS_TRUSTED
import com.passbolt.mobile.android.feature.otp.screen.SnackbarSuccessType.OTP_CREATED
import com.passbolt.mobile.android.feature.otp.screen.SnackbarSuccessType.RESOURCE_CREATED
import com.passbolt.mobile.android.feature.otp.screen.SnackbarSuccessType.RESOURCE_DELETED
import com.passbolt.mobile.android.feature.otp.screen.SnackbarSuccessType.RESOURCE_EDITED
import com.passbolt.mobile.android.feature.otp.screen.ToastType.WAIT_FOR_DATA_REFRESH_FINISH
import com.passbolt.mobile.android.jsonmodel.delegates.TotpSecret
import com.passbolt.mobile.android.serializers.jsonschema.SchemaEntity.RESOURCE
import com.passbolt.mobile.android.serializers.jsonschema.SchemaEntity.SECRET
import com.passbolt.mobile.android.supportedresourceTypes.ContentType.PasswordDescriptionTotp
import com.passbolt.mobile.android.supportedresourceTypes.ContentType.Totp
import com.passbolt.mobile.android.supportedresourceTypes.ContentType.V5DefaultWithTotp
import com.passbolt.mobile.android.supportedresourceTypes.ContentType.V5TotpStandalone
import com.passbolt.mobile.android.supportedresourceTypes.SupportedContentTypes.totpSlugs
import com.passbolt.mobile.android.ui.LeadingContentType.TOTP
import com.passbolt.mobile.android.ui.NewMetadataKeyToTrustModel
import com.passbolt.mobile.android.ui.OtpItemWrapper
import com.passbolt.mobile.android.ui.PermissionModelUi
import com.passbolt.mobile.android.ui.ResourceUiModel
import com.passbolt.mobile.android.ui.allReset
import com.passbolt.mobile.android.ui.contentType
import com.passbolt.mobile.android.ui.findVisible
import com.passbolt.mobile.android.ui.isExpired
import com.passbolt.mobile.android.ui.refreshingNone
import com.passbolt.mobile.android.ui.refreshingOnly
import com.passbolt.mobile.android.ui.replaceOnId
import com.passbolt.mobile.android.ui.revealed
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koin.core.parameter.parametersOf
import timber.log.Timber
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

internal class OtpViewModel(
    private val showSuggestedModel: ShowSuggestedModel,
    private val getSelectedAccountDataUseCase: GetSelectedAccountDataUseCase,
    private val getLocalResourcesUseCase: GetLocalResourcesUseCase,
    private val totpParametersProvider: TotpParametersProvider,
    private val coroutineLaunchContext: CoroutineLaunchContext,
    private val dataRefreshTrackingFlow: DataRefreshTrackingFlow,
    private val metadataPrivateKeysHelperInteractor: MetadataPrivateKeysHelperInteractor,
    private val timerFactory: TimerFactory,
    private val resourceAccessInteractor: ResourceAccessInteractor,
    private val resourceUpdateActionsInteractorFactory: ResourceUpdateActionsInteractorFactory,
    private val editPermissionsConfirmationInteractor: EditPermissionsConfirmationInteractor,
    private val secretPropertiesActionsInteractorFactory: SecretPropertiesActionsInteractorFactory,
    private val autofillUriMatcher: AutofillUriMatcher,
    private val timeProvider: TimeProvider,
) : SideEffectViewModel<OtpState, OtpSideEffect>(OtpState()),
    KoinComponent {
    private var dataRefreshJob: Job? = null
    private var otpsCounterJob: Job? = null
    private var universalCountdownJob: Job? = null
    private var fetchTotpJob: Job? = null

    private val searchQueryFlow = MutableStateFlow("")

    init {
        loadUserAvatar()
        observeSearchQuery()
        updateViewState { copy(universalCountdownSeconds = currentRemainingCountdownSeconds()) }
        dataRefreshJob?.cancel()
        dataRefreshJob =
            viewModelScope.launch(coroutineLaunchContext.io) {
                synchronizeWithDataRefresh()
            }
        otpsCounterJob?.cancel()
        otpsCounterJob =
            viewModelScope.launch(coroutineLaunchContext.io) {
                val otps = getOtpResources()
                updateViewState { copy(otps = otps, suggestedOtps = getSuggestedOtps(otps)) }
                updateOtpsCounterTime()
            }
        universalCountdownJob?.cancel()
        universalCountdownJob =
            viewModelScope.launch(coroutineLaunchContext.io) {
                updateUniversalCountdown()
            }
    }

    private fun currentRemainingCountdownSeconds(): Long =
        DEFAULT_TOTP_PERIOD - (timeProvider.getCurrentEpochSeconds() % DEFAULT_TOTP_PERIOD)

    override fun onCleared() {
        dataRefreshJob?.cancel()
        otpsCounterJob?.cancel()
        universalCountdownJob?.cancel()
        fetchTotpJob?.cancel()
    }

    private fun onCanCreateResource(function: () -> Unit) {
        viewModelScope.launch {
            if (resourceAccessInteractor.canCreateResource()) {
                function()
            } else {
                emitSideEffect(ShowErrorSnackbar(NO_SHARED_KEY_ACCESS))
            }
        }
    }

    // TODO refactor after feature completion
    @Suppress("CyclomaticComplexMethod", "LongMethod")
    fun onIntent(intent: OtpIntent) {
        when (intent) {
            is Search -> searchQueryChanged(intent.searchQuery)
            is RevealOtp -> {
                updateViewState { copy(showOtpMoreBottomSheet = false) }
                otpClick(intent.resource)
            }
            is OpenOtpMoreMenu -> updateViewState { copy(showOtpMoreBottomSheet = true, moreMenuResource = intent.otpItemWrapper) }
            is CloseOtpMoreMenu -> updateViewState { copy(showOtpMoreBottomSheet = false) }
            CreateTotp -> onCanCreateResource { emitSideEffect(NavigateToCreateTotp) }
            is OtpQRScanReturned -> processOtpScanResult(intent)
            is ResourceFormReturned -> processResourceFormResult(intent)
            is CopyOtp -> {
                updateViewState { copy(showOtpMoreBottomSheet = false) }
                copyTotp(intent.otpItemWrapper)
            }
            is DeleteOtp -> updateViewState { copy(showOtpMoreBottomSheet = false, showDeleteTotpConfirmationDialog = true) }
            is EditOtp -> {
                updateViewState { copy(showOtpMoreBottomSheet = false) }
                emitSideEffect(
                    NavigateToEditResourceForm(
                        resourceId = intent.otpItemWrapper.resource.resourceId,
                        resourceName = intent.otpItemWrapper.resource.metadataJsonModel.name,
                    ),
                )
            }
            CloseDeleteConfirmationDialog -> updateViewState { copy(showDeleteTotpConfirmationDialog = false) }
            is ConfirmedPermissionsResult -> confirmedPermissionsReceived(intent.permissions)
            ConfirmDeleteTotp -> {
                updateViewState { copy(showProgress = true, showDeleteTotpConfirmationDialog = false) }
                deleteTotp(viewState.value.moreMenuResource)
            }
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
            Dispose -> dispose()
        }
    }

    private fun dispose() {
        fetchTotpJob?.cancel()
        updateOtpLists { allReset() }
    }

    private fun updateOtpLists(transform: List<OtpItemWrapper>.() -> List<OtpItemWrapper>) {
        updateViewState { copy(otps = otps.transform(), filteredOtps = filteredOtps.transform()) }
    }

    private fun searchEndIconAction() {
        when (viewState.value.searchInputEndIconMode) {
            AVATAR -> {
                viewModelScope.launch(coroutineLaunchContext.io) {
                    if (dataRefreshTrackingFlow.isInProgress()) {
                        emitSideEffect(ShowToast(WAIT_FOR_DATA_REFRESH_FINISH))
                        dataRefreshTrackingFlow.awaitIdle()
                    }
                    updateViewState { copy(showAccountSwitchBottomSheet = true) }
                }
            }
            CLEAR -> searchQueryChanged("")
            NONE -> {
                // no-op
            }
        }
    }

    private fun trustNewMetadataKeyConfirmed(model: NewMetadataKeyToTrustModel) {
        updateViewState { copy(showProgress = true) }
        viewModelScope.launch(coroutineLaunchContext.io) {
            when (
                val output =
                    runAuthenticatedOperation {
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
        viewModelScope.launch(coroutineLaunchContext.io) {
            val otpResource = requireNotNull(moreMenuResource)
            when (val contentType = otpResource.resource.contentType()) {
                is Totp, V5TotpStandalone ->
                    deleteStandaloneTotpResource(otpResource.resource)
                is PasswordDescriptionTotp, V5DefaultWithTotp ->
                    if (editPermissionsConfirmationInteractor.shouldConfirmPermissions(otpResource.resource.resourceId)) {
                        Timber.d("Removing totp from a shared resource - navigating to permissions confirmation")
                        updateViewState { copy(pendingPermissionsConfirmationResource = otpResource.resource) }
                        emitSideEffect(NavigateToConfirmPermissions(otpResource.resource.resourceId))
                    } else {
                        downgradeToPasswordAndDescriptionResource(otpResource.resource)
                    }
                else ->
                    error("$contentType type should not be presented on totp list")
            }
            updateViewState { copy(showProgress = false) }
        }
    }

    private suspend fun deleteStandaloneTotpResource(otpResource: ResourceUiModel) {
        val resourceCommonActionsInteractor = get<ResourceCommonActionsInteractor> { parametersOf(otpResource) }
        performCommonResourceAction(
            action = { resourceCommonActionsInteractor.deleteResource() },
            doOnFailure = { emitSideEffect(ShowErrorSnackbar(FAILED_TO_DELETE_RESOURCE)) },
            doOnSuccess = {
                emitSideEffect(ShowSuccessSnackbar(RESOURCE_DELETED))
                emitSideEffect(InitiateDataRefresh)
            },
        )
    }

    private fun confirmedPermissionsReceived(confirmedPermissions: List<PermissionModelUi>) {
        val resource = viewState.value.pendingPermissionsConfirmationResource ?: return
        updateViewState { copy(pendingPermissionsConfirmationResource = null, showProgress = true) }
        viewModelScope.launch(coroutineLaunchContext.io) {
            downgradeToPasswordAndDescriptionResource(resource, confirmedPermissions)
            updateViewState { copy(showProgress = false) }
        }
    }

    private suspend fun downgradeToPasswordAndDescriptionResource(
        otpResource: ResourceUiModel,
        confirmedPermissions: List<PermissionModelUi>? = null,
    ) {
        val resourceUpdateActionInteractor = resourceUpdateActionsInteractorFactory.create(otpResource)
        performResourceUpdateAction(
            action = {
                if (confirmedPermissions == null) {
                    resourceUpdateActionInteractor.updateGenericResource(
                        UpdateAction.REMOVE_TOTP,
                        secretModification = { it.apply { totp = null } },
                    )
                } else {
                    resourceUpdateActionInteractor.updateGenericResourceWithConfirmedPermissions(
                        UpdateAction.REMOVE_TOTP,
                        confirmedPermissions,
                        secretModification = { it.apply { totp = null } },
                    )
                }
            },
            doOnPermissionsDrifted = { drifted ->
                updateViewState { copy(pendingPermissionsConfirmationResource = otpResource) }
                emitSideEffect(
                    NavigateToConfirmPermissions(otpResource.resourceId, driftedEntityNames = drifted.driftedEntityNames),
                )
            },
            doOnShareFailure = { emitSideEffect(ShowErrorSnackbar(SnackbarErrorType.SHARE_FAILED)) },
            doOnCryptoFailure = { emitSideEffect(ShowErrorSnackbar(SnackbarErrorType.ENCRYPTION_FAILURE)) },
            doOnFailure = { emitSideEffect(ShowErrorSnackbar(ERROR)) },
            doOnSuccess = {
                emitSideEffect(ShowSuccessSnackbar(RESOURCE_DELETED))
                emitSideEffect(InitiateDataRefresh)
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
        fetchTotp(otpItemWrapper.resource) { totp ->
            val otpParameters =
                totpParametersProvider.provideOtpParameters(
                    secretKey = totp.result.key,
                    digits = totp.result.digits,
                    period = totp.result.period,
                    algorithm = totp.result.algorithm,
                )

            when (otpParameters) {
                InvalidTotpInput -> stopRefreshingAndShowInvalidTotpError()
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
            emitSideEffect(InitiateDataRefresh)
            emitSideEffect(ShowSuccessSnackbar(RESOURCE_CREATED, intent.resourceName))
        }
        if (intent.resourceEdited) {
            emitSideEffect(InitiateDataRefresh)
            emitSideEffect(ShowSuccessSnackbar(RESOURCE_EDITED, intent.resourceName))
        }
    }

    private fun processOtpScanResult(intent: OtpQRScanReturned) {
        if (intent.otpCreated) {
            emitSideEffect(InitiateDataRefresh)
            emitSideEffect(ShowSuccessSnackbar(OTP_CREATED))
        } else {
            if (intent.otpManualCreationChosen) {
                emitSideEffect(NavigateToCreateResourceForm(leadingContentType = TOTP))
            }
        }
    }

    private fun searchQueryChanged(searchQuery: String) {
        if (searchQuery == searchQueryFlow.value) {
            return
        }
        searchQueryFlow.value = searchQuery
        updateViewState {
            copy(
                searchInputEndIconMode = if (searchQuery.isNotBlank()) CLEAR else AVATAR,
                isSearching = true,
            )
        }
    }

    @OptIn(FlowPreview::class)
    private fun observeSearchQuery() {
        viewModelScope.launch(coroutineLaunchContext.io) {
            searchQueryFlow
                .drop(1)
                .debounce(SEARCH_DEBOUNCE)
                .collectLatest { searchQuery ->
                    Timber.d("Applying search query (length: ${searchQuery.length})")
                    try {
                        val filteredOtps = getOtpResources(searchQuery)
                        updateViewState {
                            copy(searchQuery = searchQuery, filteredOtps = filteredOtps, isSearching = false)
                        }
                    } catch (exception: CancellationException) {
                        throw exception
                    } catch (exception: Exception) {
                        Timber.e(exception, "Failed to apply the search query")
                        updateViewState { copy(isSearching = false) }
                    }
                }
        }
    }

    private fun otpClick(resource: ResourceUiModel) {
        updateViewState { copy(showOtpMoreBottomSheet = false) }
        fetchTotp(resource) {
            showTotp(it, resource.resourceId)
        }
    }

    private fun fetchTotp(
        resource: ResourceUiModel,
        afterFetchAction: (SecretPropertyActionResult.Success<TotpSecret>) -> Unit,
    ) {
        fetchTotpJob?.cancel()
        fetchTotpJob =
            viewModelScope.launch(coroutineLaunchContext.io) {
                updateOtpLists { refreshingOnly(resource.resourceId) }

                val secretPropertiesActionsInteractor = secretPropertiesActionsInteractorFactory.create(resource)

                performSecretPropertyAction(
                    action = { secretPropertiesActionsInteractor.provideOtp() },
                    doOnDecryptionFailure = {
                        emitSideEffect(ShowErrorSnackbar(DECRYPTION_FAILURE))
                        updateOtpLists { refreshingNone() }
                    },
                    doOnFetchFailure = {
                        emitSideEffect(ShowErrorSnackbar(FETCH_FAILURE))
                        updateOtpLists { refreshingNone() }
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
                updateOtpLists {
                    revealed(
                        resourceId,
                        otpParameters.otpValue,
                        totp.result.period,
                        otpParameters.secondsValid,
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

    private suspend fun updateUniversalCountdown() {
        timerFactory.createInfiniteTimer(tickDuration = 1.seconds).collectLatest {
            updateViewState { copy(universalCountdownSeconds = currentRemainingCountdownSeconds()) }
        }
    }

    private suspend fun updateOtpsCounterTime() {
        timerFactory.createInfiniteTimer(tickDuration = 1.seconds).collectLatest {
            val visibleTotp = viewState.value.otps.findVisible()
            if (visibleTotp != null) {
                val updated = visibleTotp.copy(remainingSecondsCounter = (visibleTotp.remainingSecondsCounter!!) - 1)

                if (updated.isExpired()) {
                    updateOtpLists { allReset() }
                    fetchTotp(updated.resource) {
                        showTotp(it, updated.resource.resourceId)
                    }
                } else {
                    updateOtpLists { replaceOnId(updated) }
                }
            }
        }
    }

    private suspend fun synchronizeWithDataRefresh() {
        dataRefreshTrackingFlow.dataRefreshStatusFlow.collect {
            when (it) {
                is InProgress -> updateViewState { copy(isRefreshing = true, refreshProgress = it.progress) }
                FinishedWithFailure -> {
                    emitSideEffect(ShowErrorSnackbar(FAILED_TO_REFRESH_DATA))
                    updateViewState { copy(isRefreshing = false) }
                }
                FinishedWithSuccess -> {
                    val otps = getOtpResources()
                    val searchQuery = viewState.value.searchQuery
                    val filteredOtps = if (searchQuery.isNotEmpty()) getOtpResources(searchQuery) else emptyList()
                    updateViewState {
                        copy(
                            otps = otps,
                            filteredOtps = filteredOtps,
                            suggestedOtps = getSuggestedOtps(otps),
                            isRefreshing = false,
                        )
                    }
                }
                NotCompleted -> {
                    // do nothing
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

    private fun getSuggestedOtps(otps: List<OtpItemWrapper>): List<OtpItemWrapper> =
        when (showSuggestedModel) {
            is ShowSuggestedModel.DoNotShow -> emptyList()
            is ShowSuggestedModel.Show ->
                otps.filter { otpItem ->
                    val resourceUris =
                        otpItem.resource.metadataJsonModel.uris
                            .orEmpty() +
                            listOfNotNull(otpItem.resource.metadataJsonModel.uri)
                    autofillUriMatcher.isMatching(showSuggestedModel.suggestedUri, resourceUris)
                }
        }

    private suspend fun getOtpResources(searchQuery: String? = null): List<OtpItemWrapper> =
        getLocalResourcesUseCase
            .execute(GetLocalResourcesUseCase.Input(totpSlugs, searchQuery = searchQuery))
            .resources
            .map(ResourceUiModel::toOtpItemWrapper)

    private fun stopRefreshingAndShowError(message: String) {
        Timber.e(message)
        emitSideEffect(ShowErrorSnackbar(ERROR, message))
        updateOtpLists { refreshingNone() }
    }

    private fun stopRefreshingAndShowInvalidTotpError() {
        Timber.e("Invalid TOTP parameters")
        emitSideEffect(ShowErrorSnackbar(INVALID_TOTP_PARAMETERS))
        updateOtpLists { refreshingNone() }
    }

    companion object {
        val SEARCH_DEBOUNCE = 300.milliseconds
    }
}
