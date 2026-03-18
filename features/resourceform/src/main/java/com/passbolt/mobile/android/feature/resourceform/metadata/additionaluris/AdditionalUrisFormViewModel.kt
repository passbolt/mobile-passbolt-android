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

package com.passbolt.mobile.android.feature.resourceform.metadata.additionaluris

import com.passbolt.mobile.android.common.UuidProvider
import com.passbolt.mobile.android.core.compose.SideEffectViewModel
import com.passbolt.mobile.android.feature.resourceform.metadata.additionaluris.AdditionalUrisFormIntent.AddAdditionalUri
import com.passbolt.mobile.android.feature.resourceform.metadata.additionaluris.AdditionalUrisFormIntent.AdditionalUriChanged
import com.passbolt.mobile.android.feature.resourceform.metadata.additionaluris.AdditionalUrisFormIntent.ApplyChanges
import com.passbolt.mobile.android.feature.resourceform.metadata.additionaluris.AdditionalUrisFormIntent.GoBack
import com.passbolt.mobile.android.feature.resourceform.metadata.additionaluris.AdditionalUrisFormIntent.Initialize
import com.passbolt.mobile.android.feature.resourceform.metadata.additionaluris.AdditionalUrisFormIntent.MainUriChanged
import com.passbolt.mobile.android.feature.resourceform.metadata.additionaluris.AdditionalUrisFormIntent.RemoveAdditionalUri
import com.passbolt.mobile.android.feature.resourceform.metadata.additionaluris.AdditionalUrisFormSideEffect.ApplyAndGoBack
import com.passbolt.mobile.android.feature.resourceform.metadata.additionaluris.AdditionalUrisFormSideEffect.NavigateUp
import com.passbolt.mobile.android.feature.resourceform.metadata.additionaluris.AdditionalUrisFormSideEffect.ScrollToItem
import com.passbolt.mobile.android.feature.resourceform.metadata.additionaluris.AdditionalUrisFormSideEffect.ShowErrorSnackbar
import com.passbolt.mobile.android.feature.resourceform.metadata.additionaluris.LimitChecker.LimitCheckResult.CanAdd
import com.passbolt.mobile.android.feature.resourceform.metadata.additionaluris.LimitChecker.LimitCheckResult.LimitReached
import com.passbolt.mobile.android.ui.AdditionalUrisUiModel
import java.util.UUID

internal class AdditionalUrisFormViewModel(
    private val uuidProvider: UuidProvider,
    private val formValidator: FormValidator<AdditionalUrisValidationInput>,
    private val limitChecker: LimitChecker,
) : SideEffectViewModel<AdditionalUrisFormState, AdditionalUrisFormSideEffect>(AdditionalUrisFormState()) {
    fun onIntent(intent: AdditionalUrisFormIntent) {
        when (intent) {
            is Initialize -> initialize(intent)
            is MainUriChanged -> mainUriChanged(intent.uri)
            is AdditionalUriChanged -> additionalUriChanged(intent.uriId, intent.uri)
            AddAdditionalUri -> addAdditionalUri()
            is RemoveAdditionalUri -> removeAdditionalUri(intent.uriId)
            ApplyChanges -> applyChanges()
            GoBack -> emitSideEffect(NavigateUp)
        }
    }

    private fun initialize(intent: Initialize) {
        val additionalUrisMap =
            intent.additionalUris.additionalUris
                .associateByTo(linkedMapOf(), { UUID.fromString(uuidProvider.get()) }) { uri ->
                    AdditionalUriItemState(uri = uri)
                }

        updateViewState {
            copy(
                resourceFormMode = intent.mode,
                mainUri = intent.additionalUris.mainUri,
                additionalUris = additionalUrisMap,
            )
        }
    }

    private fun mainUriChanged(uri: String) {
        updateViewState {
            copy(
                mainUri = uri.trim(),
                mainUriError = null,
            )
        }
    }

    private fun additionalUriChanged(
        uriId: UUID,
        uri: String,
    ) {
        updateViewState {
            val updatedUris = LinkedHashMap(additionalUris)
            updatedUris[uriId] = AdditionalUriItemState(uri = uri.trim(), error = null)
            copy(additionalUris = updatedUris)
        }
    }

    private fun addAdditionalUri() {
        val currentState = viewState.value
        when (val result = limitChecker.checkLimit(currentState.additionalUris.size)) {
            is LimitReached ->
                emitSideEffect(
                    ShowErrorSnackbar(
                        type = SnackbarErrorType.MAX_URIS_EXCEEDED,
                        message = result.maxLimit.toString(),
                    ),
                )
            CanAdd -> {
                updateViewState {
                    val updatedUris = LinkedHashMap(additionalUris)
                    updatedUris[UUID.fromString(uuidProvider.get())] = AdditionalUriItemState()
                    copy(additionalUris = updatedUris)
                }
            }
        }
    }

    private fun removeAdditionalUri(uriId: UUID) {
        updateViewState {
            val updatedUris = LinkedHashMap(additionalUris)
            updatedUris.remove(uriId)
            copy(additionalUris = updatedUris)
        }
    }

    private fun applyChanges() {
        val state = viewState.value
        val validationResult =
            formValidator.validateAll(
                AdditionalUrisValidationInput(state.mainUri, state.additionalUris),
            )

        if (validationResult.hasErrors) {
            updateViewState {
                copy(
                    mainUriError = validationResult.mainUriError,
                    additionalUris = validationResult.additionalUris,
                )
            }
            findFirstErrorIndex(validationResult)?.let { index ->
                emitSideEffect(ScrollToItem(index))
            }
        } else {
            emitSideEffect(
                ApplyAndGoBack(
                    AdditionalUrisUiModel(
                        mainUri = state.mainUri,
                        additionalUris = state.additionalUris.values.map { it.uri },
                    ),
                ),
            )
        }
    }

    private fun findFirstErrorIndex(validationResult: FormValidator.ValidationResult): Int? {
        if (validationResult.mainUriError != null) return 0
        validationResult.additionalUris.entries.forEachIndexed { index, (_, itemState) ->
            if (itemState.error != null) return index
        }
        return null
    }
}
