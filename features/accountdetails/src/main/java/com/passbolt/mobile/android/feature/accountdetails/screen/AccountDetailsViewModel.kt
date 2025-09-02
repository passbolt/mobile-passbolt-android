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

package com.passbolt.mobile.android.feature.accountdetails.screen

import androidx.annotation.VisibleForTesting
import androidx.lifecycle.viewModelScope
import com.passbolt.mobile.android.common.validation.StringMaxLength
import com.passbolt.mobile.android.common.validation.StringNotBlank
import com.passbolt.mobile.android.common.validation.validation
import com.passbolt.mobile.android.core.accounts.usecase.accountdata.GetSelectedAccountDataUseCase
import com.passbolt.mobile.android.core.accounts.usecase.accountdata.UpdateAccountDataUseCase
import com.passbolt.mobile.android.core.accounts.usecase.selectedaccount.GetSelectedAccountUseCase
import com.passbolt.mobile.android.core.mvp.coroutinecontext.CoroutineLaunchContext
import com.passbolt.mobile.android.feature.accountdetails.screen.AccountDetailsIntent.GoBack
import com.passbolt.mobile.android.feature.accountdetails.screen.AccountDetailsIntent.SaveChanges
import com.passbolt.mobile.android.feature.accountdetails.screen.AccountDetailsIntent.StartTransferAccount
import com.passbolt.mobile.android.feature.accountdetails.screen.AccountDetailsIntent.UpdateLabel
import com.passbolt.mobile.android.feature.accountdetails.screen.AccountDetailsScreenSideEffect.NavigateToTransferAccount
import com.passbolt.mobile.android.feature.accountdetails.screen.AccountDetailsScreenSideEffect.NavigateUp
import com.passbolt.mobile.android.feature.accountdetails.screen.AccountDetailsValidationError.MaxLengthExceeded
import com.passbolt.mobile.android.feature.authentication.compose.AuthenticatedViewModel
import com.passbolt.mobile.android.mappers.AccountModelMapper
import kotlinx.coroutines.launch

internal class AccountDetailsViewModel(
    private val getSelectedAccountDataUseCase: GetSelectedAccountDataUseCase,
    private val updateAccountDataUseCase: UpdateAccountDataUseCase,
    private val getSelectedAccountUseCase: GetSelectedAccountUseCase,
    private val coroutineLaunchContext: CoroutineLaunchContext,
) : AuthenticatedViewModel<AccountDetailsState, AccountDetailsScreenSideEffect>(AccountDetailsState()) {
    init {
        loadInitialValues()
    }

    fun onIntent(intent: AccountDetailsIntent) {
        when (intent) {
            GoBack -> emitSideEffect(NavigateUp)
            StartTransferAccount -> emitSideEffect(NavigateToTransferAccount)
            is UpdateLabel -> updateViewState { copy(label = intent.label) }
            SaveChanges -> saveChanges()
        }
    }

    private fun saveChanges() {
        updateViewState { copy(labelValidationErrors = emptyList()) }
        validation {
            of(viewState.value.label) {
                withRules(StringNotBlank, StringMaxLength(LABEL_MAX_LENGTH))
                onInvalid {
                    updateViewState {
                        copy(
                            labelValidationErrors = labelValidationErrors + MaxLengthExceeded(LABEL_MAX_LENGTH),
                        )
                    }
                }
            }
            onValid {
                updateAccountDataUseCase.execute(
                    UpdateAccountDataUseCase.Input(
                        userId = requireNotNull(getSelectedAccountUseCase.execute(Unit).selectedAccount),
                        label = viewState.value.label,
                    ),
                )
                emitSideEffect(NavigateUp)
            }
        }
    }

    private fun loadInitialValues() {
        viewModelScope.launch(coroutineLaunchContext.io) {
            val data = getSelectedAccountDataUseCase.execute(Unit)
            val defaultLabel = AccountModelMapper.defaultLabel(data.firstName, data.lastName)
            val label = data.label ?: defaultLabel

            updateViewState {
                copy(
                    label = label,
                    name = "${data.firstName.orEmpty()} ${data.lastName.orEmpty()}",
                    email = data.email.orEmpty(),
                    role = data.role.orEmpty(),
                    organizationUrl = data.url,
                    avatarUrl = data.avatarUrl,
                )
            }
        }
    }

    companion object {
        @VisibleForTesting
        const val LABEL_MAX_LENGTH = 64
    }
}
