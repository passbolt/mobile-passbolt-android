package com.passbolt.mobile.android.core.resources.usecase

import com.passbolt.mobile.android.common.usecase.AsyncUseCase
import com.passbolt.mobile.android.metadata.usecase.GetMetadataTypesSettingsUseCase
import com.passbolt.mobile.android.supportedresourceTypes.ContentType
import com.passbolt.mobile.android.supportedresourceTypes.ContentType.PasswordAndDescription
import com.passbolt.mobile.android.supportedresourceTypes.ContentType.Totp
import com.passbolt.mobile.android.supportedresourceTypes.ContentType.V5Default
import com.passbolt.mobile.android.supportedresourceTypes.ContentType.V5Note
import com.passbolt.mobile.android.supportedresourceTypes.ContentType.V5TotpStandalone
import com.passbolt.mobile.android.ui.LeadingContentType
import com.passbolt.mobile.android.ui.LeadingContentType.CUSTOM_FIELDS
import com.passbolt.mobile.android.ui.LeadingContentType.PASSWORD
import com.passbolt.mobile.android.ui.LeadingContentType.STANDALONE_NOTE
import com.passbolt.mobile.android.ui.LeadingContentType.TOTP
import com.passbolt.mobile.android.ui.MetadataTypeModel
import com.passbolt.mobile.android.ui.MetadataTypeModel.V4
import com.passbolt.mobile.android.ui.MetadataTypeModel.V5
import timber.log.Timber

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
class GetDefaultCreateContentTypeUseCase(
    private val getMetadataTypesSettingsUseCase: GetMetadataTypesSettingsUseCase,
) : AsyncUseCase<GetDefaultCreateContentTypeUseCase.Input, GetDefaultCreateContentTypeUseCase.Output> {
    override suspend fun execute(input: Input): Output {
        val defaultMetadataType =
            getMetadataTypesSettingsUseCase
                .execute(Unit)
                .metadataTypesSettingsModel
                .defaultMetadataType

        return try {
            Output.CreationContentType(
                metadataType = defaultMetadataType,
                contentType =
                    when (defaultMetadataType) {
                        V4 ->
                            when (input.leadingContentType) {
                                TOTP -> Totp
                                PASSWORD -> PasswordAndDescription
                                CUSTOM_FIELDS -> error("Custom fields creation not supported for v4")
                                STANDALONE_NOTE -> error("Standalone note creation not supported for v4")
                            }
                        V5 ->
                            when (input.leadingContentType) {
                                TOTP -> V5TotpStandalone
                                PASSWORD -> V5Default
                                CUSTOM_FIELDS -> error("Custom fields creation not supported")
                                STANDALONE_NOTE -> V5Note
                            }
                    },
            )
        } catch (e: IllegalStateException) {
            Timber.e(e, "Failed to get default create content type")
            Output.NotPossibleNotCreateResource
        }
    }

    data class Input(
        val leadingContentType: LeadingContentType,
    )

    sealed class Output {
        data object NotPossibleNotCreateResource : Output()

        data class CreationContentType(
            val contentType: ContentType,
            val metadataType: MetadataTypeModel,
        ) : Output()
    }
}
