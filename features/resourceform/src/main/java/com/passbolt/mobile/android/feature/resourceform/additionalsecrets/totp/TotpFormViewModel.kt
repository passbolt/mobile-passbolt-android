/**
 * Passbolt - Open source password manager for teams
 * Copyright (c) 2026 Passbolt SA
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

package com.passbolt.mobile.android.feature.resourceform.additionalsecrets.totp

import com.passbolt.mobile.android.common.validation.StringIsBase32
import com.passbolt.mobile.android.common.validation.StringNotBlank
import com.passbolt.mobile.android.common.validation.validation
import com.passbolt.mobile.android.core.compose.SideEffectViewModel
import com.passbolt.mobile.android.feature.resourceform.additionalsecrets.totp.TotpFormIntent.AdvancedSettingsChanged
import com.passbolt.mobile.android.feature.resourceform.additionalsecrets.totp.TotpFormIntent.ApplyChanges
import com.passbolt.mobile.android.feature.resourceform.additionalsecrets.totp.TotpFormIntent.GoBack
import com.passbolt.mobile.android.feature.resourceform.additionalsecrets.totp.TotpFormIntent.IssuerChanged
import com.passbolt.mobile.android.feature.resourceform.additionalsecrets.totp.TotpFormIntent.MoreSettingsClick
import com.passbolt.mobile.android.feature.resourceform.additionalsecrets.totp.TotpFormIntent.RemoveTotp
import com.passbolt.mobile.android.feature.resourceform.additionalsecrets.totp.TotpFormIntent.ScanTotpClick
import com.passbolt.mobile.android.feature.resourceform.additionalsecrets.totp.TotpFormIntent.SecretChanged
import com.passbolt.mobile.android.feature.resourceform.additionalsecrets.totp.TotpFormIntent.TotpScanned
import com.passbolt.mobile.android.feature.resourceform.additionalsecrets.totp.TotpFormSideEffect.ApplyAndGoBack
import com.passbolt.mobile.android.feature.resourceform.additionalsecrets.totp.TotpFormSideEffect.NavigateBack
import com.passbolt.mobile.android.feature.resourceform.additionalsecrets.totp.TotpFormSideEffect.NavigateToAdvancedSettings
import com.passbolt.mobile.android.feature.resourceform.additionalsecrets.totp.TotpFormSideEffect.NavigateToScanTotp
import com.passbolt.mobile.android.feature.resourceform.additionalsecrets.totp.TotpSecretValidationError.MustBeBase32
import com.passbolt.mobile.android.feature.resourceform.additionalsecrets.totp.TotpSecretValidationError.MustNotBeEmpty
import com.passbolt.mobile.android.ui.ResourceFormMode
import com.passbolt.mobile.android.ui.TotpUiModel

internal class TotpFormViewModel(
    private val mode: ResourceFormMode,
    totpUiModel: TotpUiModel,
) : SideEffectViewModel<TotpFormState, TotpFormSideEffect>(
        initialState =
            TotpFormState(
                resourceFormMode = mode,
                secret = totpUiModel.secret,
                issuer = totpUiModel.issuer,
                expiry = totpUiModel.expiry,
                length = totpUiModel.length,
                algorithm = totpUiModel.algorithm,
            ),
    ) {
    fun onIntent(intent: TotpFormIntent) {
        when (intent) {
            is SecretChanged -> updateViewState { copy(secret = intent.secret) }
            is IssuerChanged -> updateViewState { copy(issuer = intent.issuer) }
            is TotpScanned -> handleTotpScanned(intent)
            is AdvancedSettingsChanged -> handleAdvancedSettingsChanged(intent)
            MoreSettingsClick -> handleMoreSettingsClick()
            ScanTotpClick -> emitSideEffect(NavigateToScanTotp)
            RemoveTotp -> emitSideEffect(ApplyAndGoBack(null))
            ApplyChanges -> applyChanges()
            GoBack -> emitSideEffect(NavigateBack)
        }
    }

    private fun handleTotpScanned(intent: TotpScanned) {
        if (intent.isManualCreationChosen) return

        intent.totpQr?.let {
            updateViewState {
                copy(
                    secret = it.secret,
                    issuer = it.issuer.orEmpty(),
                    algorithm = it.algorithm.name,
                    length = it.digits.toString(),
                    expiry = it.period.toString(),
                )
            }
        }
    }

    private fun handleAdvancedSettingsChanged(intent: AdvancedSettingsChanged) {
        intent.totpModel?.let {
            updateViewState {
                copy(
                    expiry = it.expiry,
                    length = it.length,
                    algorithm = it.algorithm,
                )
            }
        }
    }

    private fun handleMoreSettingsClick() {
        val state = viewState.value
        emitSideEffect(
            NavigateToAdvancedSettings(
                mode = mode,
                totpUiModel =
                    TotpUiModel(
                        secret = state.secret,
                        issuer = state.issuer,
                        expiry = state.expiry,
                        length = state.length,
                        algorithm = state.algorithm,
                    ),
            ),
        )
    }

    private fun applyChanges() {
        updateViewState { copy(secretValidationErrors = emptyList()) }
        val state = viewState.value
        validation {
            of(state.secret) {
                withRules(StringNotBlank) {
                    onInvalid {
                        updateViewState {
                            copy(secretValidationErrors = secretValidationErrors + MustNotBeEmpty)
                        }
                    }
                }
                withRules(StringIsBase32) {
                    onInvalid {
                        updateViewState {
                            copy(secretValidationErrors = secretValidationErrors + MustBeBase32)
                        }
                    }
                }
            }
            onValid {
                emitSideEffect(
                    ApplyAndGoBack(
                        TotpUiModel(
                            secret = state.secret,
                            issuer = state.issuer,
                            expiry = state.expiry,
                            length = state.length,
                            algorithm = state.algorithm,
                        ),
                    ),
                )
            }
        }
    }
}
