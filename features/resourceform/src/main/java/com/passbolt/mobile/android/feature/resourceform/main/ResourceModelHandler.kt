package com.passbolt.mobile.android.feature.resourceform.main

import com.passbolt.mobile.android.core.mvp.authentication.UnauthenticatedReason
import com.passbolt.mobile.android.core.resources.actions.SecretPropertiesActionsInteractor
import com.passbolt.mobile.android.core.resources.actions.SecretPropertyActionResult
import com.passbolt.mobile.android.core.resources.usecase.db.GetLocalResourceUseCase
import com.passbolt.mobile.android.core.resourcetypes.graph.redesigned.ResourceTypesUpdatesAdjacencyGraph2
import com.passbolt.mobile.android.core.resourcetypes.graph.redesigned.UpdateAction2
import com.passbolt.mobile.android.core.secrets.usecase.decrypt.parser.SecretJsonModel
import com.passbolt.mobile.android.feature.resourceform.usecase.GetDefaultCreateContentTypeUseCase
import com.passbolt.mobile.android.feature.resourceform.usecase.GetEditContentTypeUseCase
import com.passbolt.mobile.android.jsonmodel.delegates.TotpSecret
import com.passbolt.mobile.android.supportedresourceTypes.ContentType
import com.passbolt.mobile.android.ui.LeadingContentType
import com.passbolt.mobile.android.ui.MetadataJsonModel
import com.passbolt.mobile.android.ui.MetadataTypeModel
import com.passbolt.mobile.android.ui.OtpParseResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.single
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koin.core.parameter.parametersOf
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
class ResourceModelHandler(
    private val getDefaultCreateContentTypeUseCase: GetDefaultCreateContentTypeUseCase,
    private val getEditContentTypeUseCase: GetEditContentTypeUseCase,
    private val resourceActionsGraph: ResourceTypesUpdatesAdjacencyGraph2,
    private val getLocalResourceUseCase: GetLocalResourceUseCase
) : KoinComponent {

    lateinit var resourceMetadata: MetadataJsonModel
    lateinit var resourceSecret: SecretJsonModel
    lateinit var metadataType: MetadataTypeModel
    lateinit var contentType: ContentType

    suspend fun initializeModelForCreation(leadingContentType: LeadingContentType) {
        val initialContentTypeToCreate = getDefaultCreateContentTypeUseCase.execute(
            GetDefaultCreateContentTypeUseCase.Input(leadingContentType)
        )
        contentType = initialContentTypeToCreate.contentType
        metadataType = initialContentTypeToCreate.metadataType

        resourceMetadata = MetadataJsonModel.empty()
        resourceSecret = when (leadingContentType) {
            LeadingContentType.TOTP -> SecretJsonModel.emptyTotp()
            LeadingContentType.PASSWORD -> SecretJsonModel.emptyPassword()
        }

        Timber.d("Initialized creation model with content type: $contentType and metadata type: $metadataType")
    }

    @Throws(Exception::class)
    suspend fun initializeModelForEdition(
        existingResourceId: String,
        needSessionRefreshFlow: MutableStateFlow<UnauthenticatedReason?>,
        sessionRefreshedFlow: StateFlow<Unit?>
    ) {
        try {
            // init resource
            val resource = getLocalResourceUseCase.execute(
                GetLocalResourceUseCase.Input(existingResourceId)
            ).resource
            resourceMetadata = resource.metadataJsonModel

            // init model fields
            val initialEditContentType = getEditContentTypeUseCase.execute(
                GetEditContentTypeUseCase.Input(resource.resourceTypeId)
            )
            contentType = initialEditContentType.contentType
            metadataType = initialEditContentType.metadataType

            // init secret
            val secretPropertiesActionsInteractor: SecretPropertiesActionsInteractor = get {
                parametersOf(resource, needSessionRefreshFlow, sessionRefreshedFlow)
            }
            val secret = secretPropertiesActionsInteractor.provideDecryptedSecret().single()
            resourceSecret = (secret as SecretPropertyActionResult.Success<SecretJsonModel>).result

            Timber.d("Initialized edition model with content type: $contentType and metadata type: $metadataType")
        } catch (e: Exception) {
            Timber.e(e, "Failed to initialize edition model")
            throw e
        }
    }

    fun applyModelChange(action: UpdateAction2, change: (MetadataJsonModel, SecretJsonModel) -> Unit) {
        if (processContentTypeEvent(action)) {
            change(resourceMetadata, resourceSecret)
            ensureRequiredFieldsAreSet()
            ensureNoAdditionalFields()
        }
    }

    private fun ensureRequiredFieldsAreSet() {
        if (contentType.hasTotp() && resourceSecret.totp == null) {
            resourceSecret.totp = TotpSecret(
                algorithm = OtpParseResult.OtpQr.Algorithm.DEFAULT.name,
                digits = OtpParseResult.OtpQr.TotpQr.DEFAULT_DIGITS,
                period = OtpParseResult.OtpQr.TotpQr.DEFAULT_PERIOD_SECONDS,
                key = ""
            )
        }
        if (contentType.hasNote() && resourceSecret.description == null) {
            resourceSecret.description = ""
        }
        if (contentType.hasPassword() && resourceSecret.getPassword(contentType) == null) {
            resourceSecret.setPassword(contentType, "")
        }
    }

    private fun ensureNoAdditionalFields() {
        if (!contentType.hasTotp() && resourceSecret.totp != null) {
            resourceSecret.totp = null
        }
        if (!contentType.hasNote() && resourceSecret.description != null) {
            resourceSecret.description = null
        }
        if (!contentType.hasPassword() && resourceSecret.getPassword(contentType) != null) {
            resourceSecret.setPassword(contentType, null)
        }
    }

    private fun processContentTypeEvent(action: UpdateAction2): Boolean {
        val finalAction = mergeActions(action)
        val allowedActions = resourceActionsGraph.getUpdateAction2sMetadata(contentType.slug).map { it.action }
        if (finalAction in allowedActions) {
            contentType = resourceActionsGraph.getResourceTypeSlugAfterUpdate(contentType.slug, finalAction)
            Timber.d("Allowed action $finalAction. Current content type is: $contentType")
            return true
        }
        Timber.d("Action $finalAction is not allowed for content type $contentType")
        return false
    }

    private fun mergeActions(action: UpdateAction2): UpdateAction2 {
        if (contentType in setOf(ContentType.PasswordDescriptionTotp, ContentType.V5DefaultWithTotp)) {
            if (action == UpdateAction2.REMOVE_PASSWORD && resourceHasNoNote()) {
                Timber.d("Merged REMOVE_PASSWORD into REMOVE_PASSWORD_AND_NOTE")
                return UpdateAction2.REMOVE_PASSWORD_AND_NOTE
            }
            if (action == UpdateAction2.REMOVE_NOTE && resourceHasNoPassword()) {
                Timber.d("Merged REMOVE_NOTE into REMOVE_PASSWORD_AND_NOTE")
                return UpdateAction2.REMOVE_PASSWORD_AND_NOTE
            }
        }
        return action
    }

    private fun resourceHasNoPassword(): Boolean =
        resourceSecret.getPassword(contentType).isNullOrBlank()

    private fun resourceHasNoNote(): Boolean =
        resourceSecret.description.isNullOrBlank()
}
