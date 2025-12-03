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

package com.passbolt.mobile.android.feature.settings.screen.accounts.keyinspector

import androidx.lifecycle.viewModelScope
import com.passbolt.mobile.android.core.accounts.usecase.accountdata.GetSelectedAccountDataUseCase
import com.passbolt.mobile.android.core.mvp.coroutinecontext.CoroutineLaunchContext
import com.passbolt.mobile.android.core.ui.formatter.DateFormatter
import com.passbolt.mobile.android.core.ui.formatter.FingerprintFormatter
import com.passbolt.mobile.android.core.users.user.FetchCurrentUserUseCase
import com.passbolt.mobile.android.feature.authentication.compose.AuthenticatedViewModel
import com.passbolt.mobile.android.feature.authentication.session.runAuthenticatedOperation
import com.passbolt.mobile.android.feature.settings.screen.accounts.keyinspector.KeyInspectorIntent.CloseMoreMenu
import com.passbolt.mobile.android.feature.settings.screen.accounts.keyinspector.KeyInspectorIntent.CopyFingerprint
import com.passbolt.mobile.android.feature.settings.screen.accounts.keyinspector.KeyInspectorIntent.CopyUid
import com.passbolt.mobile.android.feature.settings.screen.accounts.keyinspector.KeyInspectorIntent.GoBack
import com.passbolt.mobile.android.feature.settings.screen.accounts.keyinspector.KeyInspectorIntent.OpenMoreMenu
import com.passbolt.mobile.android.feature.settings.screen.accounts.keyinspector.KeyInspectorScreenSideEffect.AddFingerprintToClipboard
import com.passbolt.mobile.android.feature.settings.screen.accounts.keyinspector.KeyInspectorScreenSideEffect.AddUidToClipboard
import com.passbolt.mobile.android.feature.settings.screen.accounts.keyinspector.KeyInspectorScreenSideEffect.ErrorSnackbarType.FAILED_TO_FETCH_KEY
import com.passbolt.mobile.android.feature.settings.screen.accounts.keyinspector.KeyInspectorScreenSideEffect.NavigateUp
import com.passbolt.mobile.android.feature.settings.screen.accounts.keyinspector.KeyInspectorScreenSideEffect.ShowErrorSnackbar
import com.passbolt.mobile.android.mappers.AccountModelMapper
import kotlinx.coroutines.launch
import timber.log.Timber

internal class KeyInspectorViewModel(
    private val fetchCurrentUserUseCase: FetchCurrentUserUseCase,
    private val getSelectedAccountDataUseCase: GetSelectedAccountDataUseCase,
    private val dateFormatter: DateFormatter,
    private val fingerprintFormatter: FingerprintFormatter,
    coroutineLaunchContext: CoroutineLaunchContext,
) : AuthenticatedViewModel<KeyInspectorState, KeyInspectorScreenSideEffect>(KeyInspectorState()) {
    init {
        Timber.e("new view mdoel")
        viewModelScope.launch(coroutineLaunchContext.default) {
            updateViewState { copy(showProgress = true) }
            loadInitialValues()
            updateViewState { copy(showProgress = false) }
        }
    }

    fun onIntent(intent: KeyInspectorIntent) {
        when (intent) {
            CopyFingerprint -> emitSideEffect(AddFingerprintToClipboard(viewState.value.fingerprint))
            CopyUid -> emitSideEffect(AddUidToClipboard(viewState.value.uid))
            GoBack -> emitSideEffect(NavigateUp)
            OpenMoreMenu -> updateViewState { copy(showBottomSheet = true) }
            CloseMoreMenu -> updateViewState { copy(showBottomSheet = false) }
        }
    }

    private suspend fun loadInitialValues() {
        showAccountData()
        fetchKeyData()
    }

    private suspend fun fetchKeyData() {
        when (val keyData = runAuthenticatedOperation { fetchCurrentUserUseCase.execute(Unit) }) {
            is FetchCurrentUserUseCase.Output.Failure<*> -> emitSideEffect(ShowErrorSnackbar(FAILED_TO_FETCH_KEY, keyData.message))
            is FetchCurrentUserUseCase.Output.Success -> {
                val keyData = keyData.userModel.gpgKey
                updateViewState {
                    copy(
                        fingerprint = fingerprintFormatter.format(keyData.fingerprint, appendMiddleSpacing = false).orEmpty(),
                        keyLength = keyData.bits,
                        uid = keyData.uid.orEmpty(),
                        created = keyData.keyCreationDate?.let { dateFormatter.format(it) }.orEmpty(),
                        expires = keyData.keyExpirationDate?.let { dateFormatter.format(it) }.orEmpty(),
                        algorithm = keyData.type.orEmpty(),
                    )
                }
            }
        }
    }

    private fun showAccountData() {
        val accountData = getSelectedAccountDataUseCase.execute(Unit)
        val defaultLabel = AccountModelMapper.defaultLabel(accountData.firstName, accountData.lastName)
        updateViewState {
            copy(avatarUrl = accountData.avatarUrl, label = accountData.label ?: defaultLabel)
        }
    }
}
