package com.passbolt.mobile.android.feature.autofill.resources

import androidx.lifecycle.viewModelScope
import com.passbolt.mobile.android.core.compose.SideEffectViewModel
import com.passbolt.mobile.android.core.mvp.coroutinecontext.CoroutineLaunchContext
import com.passbolt.mobile.android.core.otpcore.TotpParametersProvider
import com.passbolt.mobile.android.core.otpcore.TotpParametersProvider.OtpParametersResult.InvalidTotpInput
import com.passbolt.mobile.android.core.otpcore.TotpParametersProvider.OtpParametersResult.OtpParameters
import com.passbolt.mobile.android.core.navigation.AutofillType
import com.passbolt.mobile.android.domain.accounts.usecase.GetAccountsUseCase
import com.passbolt.mobile.android.domain.preferences.usecase.GetGlobalPreferencesUseCase
import com.passbolt.mobile.android.domain.resources.actions.SecretPropertiesActionsInteractor
import com.passbolt.mobile.android.domain.resources.actions.performSecretPropertyAction
import com.passbolt.mobile.android.domain.resources.usecase.db.GetLocalResourceUseCase
import com.passbolt.mobile.android.domain.secrets.model.SecretJsonModel
import com.passbolt.mobile.android.feature.autofill.resources.AutofillResourcesIntent.NewResourceCreated
import com.passbolt.mobile.android.feature.autofill.resources.AutofillResourcesIntent.SelectAutofillItem
import com.passbolt.mobile.android.feature.autofill.resources.AutofillResourcesIntent.UserAuthenticated
import com.passbolt.mobile.android.feature.autofill.resources.AutofillResourcesSideEffect.AutofillReturn
import com.passbolt.mobile.android.feature.autofill.resources.AutofillResourcesSideEffect.NavigateToAuth
import com.passbolt.mobile.android.feature.autofill.resources.AutofillResourcesSideEffect.NavigateToSetup
import com.passbolt.mobile.android.feature.autofill.resources.AutofillResourcesSideEffect.ShowToast
import com.passbolt.mobile.android.feature.autofill.resources.ToastType.DECRYPTION_FAILURE
import com.passbolt.mobile.android.feature.autofill.resources.ToastType.FETCH_FAILURE
import com.passbolt.mobile.android.feature.autofill.resources.ToastType.INVALID_TOTP_PARAMETERS
import com.passbolt.mobile.android.feature.autofill.resources.datasetstrategy.AutofillPayload
import com.passbolt.mobile.android.jsonmodel.delegates.TotpSecret
import com.passbolt.mobile.android.ui.ResourceUiModel
import com.passbolt.mobile.android.ui.contentType
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koin.core.parameter.parametersOf
import timber.log.Timber

class AutofillResourcesViewModel(
    getAccountsUseCase: GetAccountsUseCase,
    private val uri: String?,
    private val autofillType: AutofillType?,
    private val getGlobalPreferencesUseCase: GetGlobalPreferencesUseCase,
    private val getLocalResourceUseCase: GetLocalResourceUseCase,
    private val totpParametersProvider: TotpParametersProvider,
    private val coroutineLaunchContext: CoroutineLaunchContext,
) : SideEffectViewModel<AutofillResourcesState, AutofillResourcesSideEffect>(AutofillResourcesState()),
    KoinComponent {
    init {
        if (getAccountsUseCase.execute(Unit).users.isNotEmpty()) {
            emitSideEffect(NavigateToAuth)
        } else {
            emitSideEffect(NavigateToSetup)
        }
    }

    fun onIntent(intent: AutofillResourcesIntent) {
        when (intent) {
            is UserAuthenticated -> userAuthenticated()
            is SelectAutofillItem -> selectAutofillItem(intent.resourceModel)
            is NewResourceCreated -> newResourceCreated(intent.resourceId)
        }
    }

    private fun userAuthenticated() {
        updateViewState { copy(showHome = true) }
    }

    private fun selectAutofillItem(resource: ResourceUiModel) {
        updateViewState { copy(showProgress = true) }
        viewModelScope.launch(coroutineLaunchContext.io) {
            val payload = buildPayload(resource)
            if (payload != null) {
                emitSideEffect(AutofillReturn(payload))
            }
            updateViewState { copy(showProgress = false) }
        }
    }

    private suspend fun buildPayload(resource: ResourceUiModel): AutofillPayload? {
        val contentType = resource.contentType()
        val username = resource.metadataJsonModel.username
        val secret = fetchDecryptedSecret(resource)

        val password = secret?.getPassword(contentType)
        val totpCode = secret?.totp?.let { totpCode(it) }

        return if (username == null && password == null && totpCode == null) {
            null
        } else {
            AutofillPayload(
                username = username,
                password = password,
                totpCode = totpCode,
                uri = uri,
                totpCodeToCopy = totpCode?.takeIf { shouldCopyTotpToClipboard() },
            )
        }
    }

    private suspend fun fetchDecryptedSecret(resource: ResourceUiModel): SecretJsonModel? {
        val interactor: SecretPropertiesActionsInteractor = get { parametersOf(resource) }
        var secret: SecretJsonModel? = null
        performSecretPropertyAction(
            action = { interactor.provideDecryptedSecret() },
            doOnFetchFailure = { emitSideEffect(ShowToast(FETCH_FAILURE)) },
            doOnDecryptionFailure = { emitSideEffect(ShowToast(DECRYPTION_FAILURE)) },
            doOnSuccess = { secret = it.result },
        )
        return secret
    }

    // Copy the TOTP to the clipboard only when the user asked for it and the
    // form did not have a TOTP field of its own - if it had, the code has just
    // been filled in directly and a second copy would only overwrite whatever
    // the user had in the clipboard.
    private fun shouldCopyTotpToClipboard(): Boolean =
        autofillType == AutofillType.CREDENTIALS &&
            getGlobalPreferencesUseCase.execute(Unit).isCopyTotpOnAutofillEnabled

    private fun totpCode(totp: TotpSecret): String? =
        when (
            val parameters =
                totpParametersProvider.provideOtpParameters(
                    secretKey = totp.key,
                    digits = totp.digits,
                    period = totp.period,
                    algorithm = totp.algorithm,
                )
        ) {
            is OtpParameters -> parameters.otpValue
            InvalidTotpInput -> {
                Timber.e("Invalid TOTP parameters")
                emitSideEffect(ShowToast(INVALID_TOTP_PARAMETERS))
                null
            }
        }

    private fun newResourceCreated(resourceId: String) {
        viewModelScope.launch(coroutineLaunchContext.io) {
            selectAutofillItem(
                getLocalResourceUseCase
                    .execute(
                        GetLocalResourceUseCase.Input(resourceId),
                    ).resource,
            )
        }
    }
}
