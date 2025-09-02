package com.passbolt.mobile.android.feature.settings.screen.accounts.keyinspector.keyinspectormoremenu

import androidx.lifecycle.viewModelScope
import com.passbolt.mobile.android.core.accounts.usecase.privatekey.GetSelectedUserPrivateKeyUseCase
import com.passbolt.mobile.android.core.compose.SideEffectViewModel
import com.passbolt.mobile.android.core.mvp.coroutinecontext.CoroutineLaunchContext
import com.passbolt.mobile.android.feature.settings.screen.accounts.keyinspector.keyinspectormoremenu.KeyInspectorBottomSheetIntent.Close
import com.passbolt.mobile.android.feature.settings.screen.accounts.keyinspector.keyinspectormoremenu.KeyInspectorBottomSheetIntent.ExportPrivateKey
import com.passbolt.mobile.android.feature.settings.screen.accounts.keyinspector.keyinspectormoremenu.KeyInspectorBottomSheetIntent.ExportPublicKey
import com.passbolt.mobile.android.feature.settings.screen.accounts.keyinspector.keyinspectormoremenu.KeyInspectorBottomSheetIntent.RefreshedPassphrase
import com.passbolt.mobile.android.feature.settings.screen.accounts.keyinspector.keyinspectormoremenu.KeyInspectorBottomSheetSideEffect.ConfirmPassphrase
import com.passbolt.mobile.android.feature.settings.screen.accounts.keyinspector.keyinspectormoremenu.KeyInspectorBottomSheetSideEffect.Dismiss
import com.passbolt.mobile.android.feature.settings.screen.accounts.keyinspector.keyinspectormoremenu.KeyInspectorBottomSheetSideEffect.ErrorSnackbarType.FAILED_TO_GENERATE_PUBLIC_KEY
import com.passbolt.mobile.android.feature.settings.screen.accounts.keyinspector.keyinspectormoremenu.KeyInspectorBottomSheetSideEffect.ShowErrorSnackbar
import com.passbolt.mobile.android.feature.settings.screen.accounts.keyinspector.keyinspectormoremenu.KeyInspectorBottomSheetSideEffect.ShowTextShareSheet
import com.passbolt.mobile.android.feature.settings.screen.accounts.keyinspector.keyinspectormoremenu.KeyInspectorBottomSheetState.CurrentFlow.EXPORT_PRIVATE_KEY
import com.passbolt.mobile.android.feature.settings.screen.accounts.keyinspector.keyinspectormoremenu.KeyInspectorBottomSheetState.CurrentFlow.EXPORT_PUBLIC_KEY
import com.passbolt.mobile.android.gopenpgp.OpenPgp
import com.passbolt.mobile.android.gopenpgp.exception.OpenPgpResult.Error
import com.passbolt.mobile.android.gopenpgp.exception.OpenPgpResult.Result
import kotlinx.coroutines.launch
import timber.log.Timber

internal class KeyInspectorBottomSheetViewModel(
    private val getPrivateKeyUseCase: GetSelectedUserPrivateKeyUseCase,
    private val openPgp: OpenPgp,
    private val coroutineLaunchContext: CoroutineLaunchContext,
) : SideEffectViewModel<KeyInspectorBottomSheetState, KeyInspectorBottomSheetSideEffect>(KeyInspectorBottomSheetState()) {
    fun onIntent(intent: KeyInspectorBottomSheetIntent) {
        when (intent) {
            Close -> emitSideEffect(Dismiss)
            ExportPrivateKey -> {
                updateViewState { copy(currentExportFlow = EXPORT_PRIVATE_KEY) }
                emitSideEffect(ConfirmPassphrase)
            }
            ExportPublicKey -> {
                updateViewState { copy(currentExportFlow = EXPORT_PUBLIC_KEY) }
                emitSideEffect(ConfirmPassphrase)
            }
            RefreshedPassphrase -> refreshedPassphrase()
        }
    }

    private fun refreshedPassphrase() {
        when (viewState.value.currentExportFlow) {
            EXPORT_PRIVATE_KEY -> sharePrivateKey()
            EXPORT_PUBLIC_KEY -> sharePublicKey()
            null -> {
                Timber.e("Authentication succeeded but no export flow is set.")
                // no-op
            }
        }
    }

    private fun sharePublicKey() {
        viewModelScope.launch(coroutineLaunchContext.io) {
            getPrivateKeyUseCase.execute(Unit).privateKey?.let {
                when (val publicKeyResult = openPgp.generatePublicKey(it)) {
                    is Error ->
                        emitSideEffect(
                            ShowErrorSnackbar(
                                FAILED_TO_GENERATE_PUBLIC_KEY,
                                publicKeyResult.error.message,
                            ),
                        )
                    is Result -> {
                        emitSideEffect(Dismiss)
                        emitSideEffect(ShowTextShareSheet(publicKeyResult.result))
                    }
                }
            }
        }
    }

    private fun sharePrivateKey() {
        viewModelScope.launch(coroutineLaunchContext.io) {
            getPrivateKeyUseCase.execute(Unit).privateKey?.let {
                emitSideEffect(Dismiss)
                emitSideEffect(ShowTextShareSheet(it))
            }
        }
    }
}
