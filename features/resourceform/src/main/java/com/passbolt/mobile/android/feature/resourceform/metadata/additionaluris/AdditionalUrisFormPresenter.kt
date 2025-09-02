package com.passbolt.mobile.android.feature.resourceform.metadata.additionaluris

import androidx.annotation.VisibleForTesting
import com.passbolt.mobile.android.common.validation.StringMaxLength
import com.passbolt.mobile.android.common.validation.validation
import com.passbolt.mobile.android.ui.AdditionalUrisUiModel
import com.passbolt.mobile.android.ui.ResourceFormMode
import java.util.UUID

/**
 * Passbolt - Open source password manager for teams
 * Copyright (c) 2021 Passbolt SA
 *
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General
 * Public License (AGPL) as published by the Free Software Foundation version 3.
 *
 *
 * The name "Passbolt" is a registered trademark of Passbolt SA, and Passbolt SA hereby declines to grant a trademark
 * license to "Passbolt" pursuant to the GNU Affero General Public License version 3 Section 7(e), without a separate
 * agreement with Passbolt SA.
 *
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See GNU Affero General Public License for more details.
 *
 *
 * You should have received a copy of the GNU Affero General Public License along with this program. If not,
 * see GNU Affero General Public License v3 (http://www.gnu.org/licenses/agpl-3.0.html).
 *
 * @copyright Copyright (c) Passbolt SA (https://www.passbolt.com)
 * @license https://opensource.org/licenses/AGPL-3.0 AGPL License
 * @link https://www.passbolt.com Passbolt (tm)
 * @since v1.0
 */

class AdditionalUrisFormPresenter : AdditionalUrisFormContract.Presenter {
    override var view: AdditionalUrisFormContract.View? = null

    private var mainUri: String = ""
    private var additionalUrisUiModel = LinkedHashMap<UUID, String>()

    override fun argsRetrieved(
        mode: ResourceFormMode,
        additionalUris: AdditionalUrisUiModel,
    ) {
        mainUri = additionalUris.mainUri
        additionalUrisUiModel =
            additionalUris.additionalUris.associateByTo(additionalUrisUiModel) {
                UUID.randomUUID()
            }

        when (mode) {
            is ResourceFormMode.Create -> view?.showCreateTitle()
            is ResourceFormMode.Edit -> view?.showEditTitle(mode.resourceName)
        }

        view?.showMainUri(additionalUris.mainUri)
        view?.showAdditionalUris(additionalUrisUiModel)
    }

    override fun mainUriChanged(mainUri: String) {
        this.mainUri = mainUri.trim()
    }

    override fun addAdditionalUriClick() {
        if (additionalUrisUiModel.size >= MAX_ADDITIONAL_URIS) {
            view?.showMaxUriLimitExceeded(MAX_ADDITIONAL_URIS)
        } else {
            additionalUrisUiModel[UUID.randomUUID()] = ""
            view?.showAdditionalUris(additionalUrisUiModel)
        }
    }

    override fun additionalUriChanged(
        uiTag: UUID,
        uri: String,
    ) {
        additionalUrisUiModel[uiTag] = uri.trim()
    }

    override fun additionalUriRemoved(uiTag: UUID) {
        additionalUrisUiModel.remove(uiTag)
    }

    override fun applyClick() {
        view?.clearValidationErrors()
        validation {
            of(mainUri) {
                withRules(StringMaxLength(URI_MAX_LENGTH)) {
                    onInvalid { view?.showMainUriMaxLengthError(URI_MAX_LENGTH) }
                }
            }
            additionalUrisUiModel.forEach {
                of(it.value) {
                    withRules(StringMaxLength(URI_MAX_LENGTH)) {
                        onInvalid {
                            view?.showAdditionalUriMaxLengthError(it.key, URI_MAX_LENGTH)
                            view?.scrollToAdditionalUriWithError(it.key)
                        }
                    }
                }
            }

            onValid {
                view?.goBackWithResult(
                    AdditionalUrisUiModel(
                        mainUri = mainUri,
                        additionalUris = additionalUrisUiModel.values.toList(),
                    ),
                )
            }
        }
    }

    companion object {
        private const val MAX_ADDITIONAL_URIS = 19

        @VisibleForTesting
        val URI_MAX_LENGTH = 1024
    }
}
