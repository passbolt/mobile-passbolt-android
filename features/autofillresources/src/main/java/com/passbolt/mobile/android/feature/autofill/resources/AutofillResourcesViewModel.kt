package com.passbolt.mobile.android.feature.autofill.resources

import androidx.lifecycle.viewModelScope
import com.passbolt.mobile.android.core.accounts.usecase.accounts.GetAccountsUseCase
import com.passbolt.mobile.android.core.compose.SideEffectViewModel
import com.passbolt.mobile.android.core.mvp.coroutinecontext.CoroutineLaunchContext
import com.passbolt.mobile.android.core.resources.actions.SecretPropertiesActionsInteractor
import com.passbolt.mobile.android.core.resources.actions.performSecretPropertyAction
import com.passbolt.mobile.android.core.resources.usecase.db.GetLocalResourceUseCase
import com.passbolt.mobile.android.feature.autofill.resources.AutofillResourcesIntent.NewResourceCreated
import com.passbolt.mobile.android.feature.autofill.resources.AutofillResourcesIntent.SelectAutofillItem
import com.passbolt.mobile.android.feature.autofill.resources.AutofillResourcesIntent.UserAuthenticated
import com.passbolt.mobile.android.feature.autofill.resources.AutofillResourcesSideEffect.AutofillReturn
import com.passbolt.mobile.android.feature.autofill.resources.AutofillResourcesSideEffect.NavigateToAuth
import com.passbolt.mobile.android.feature.autofill.resources.AutofillResourcesSideEffect.NavigateToSetup
import com.passbolt.mobile.android.feature.autofill.resources.AutofillResourcesSideEffect.ShowToast
import com.passbolt.mobile.android.feature.autofill.resources.ToastType.DECRYPTION_FAILURE
import com.passbolt.mobile.android.feature.autofill.resources.ToastType.FETCH_FAILURE
import com.passbolt.mobile.android.ui.ResourceModel
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koin.core.parameter.parametersOf

class AutofillResourcesViewModel(
    getAccountsUseCase: GetAccountsUseCase,
    private val uri: String?,
    private val getLocalResourceUseCase: GetLocalResourceUseCase,
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

    private fun selectAutofillItem(resourceModel: ResourceModel) {
        updateViewState { copy(showProgress = true) }
        val secretPropertiesActionsInteractor: SecretPropertiesActionsInteractor =
            get { parametersOf(resourceModel) }
        viewModelScope.launch(coroutineLaunchContext.io) {
            performSecretPropertyAction(
                action = { secretPropertiesActionsInteractor.providePassword() },
                doOnFetchFailure = { emitSideEffect(ShowToast(FETCH_FAILURE)) },
                doOnDecryptionFailure = { emitSideEffect(ShowToast(DECRYPTION_FAILURE)) },
                doOnSuccess = {
                    emitSideEffect(
                        AutofillReturn(
                            username = resourceModel.metadataJsonModel.username.orEmpty(),
                            password = it.result.orEmpty(),
                            uri = uri,
                        ),
                    )
                },
            )
            updateViewState { copy(showProgress = false) }
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
