package com.passbolt.mobile.android.feature.resourceform.main

import com.passbolt.mobile.android.core.fulldatarefresh.base.DataRefreshViewReactivePresenter
import com.passbolt.mobile.android.core.mvp.coroutinecontext.CoroutineLaunchContext
import com.passbolt.mobile.android.core.passwordgenerator.SecretGenerator
import com.passbolt.mobile.android.core.passwordgenerator.codepoints.toCodepoints
import com.passbolt.mobile.android.core.passwordgenerator.entropy.EntropyCalculator
import com.passbolt.mobile.android.core.policies.usecase.GetPasswordPoliciesUseCase
import com.passbolt.mobile.android.core.secrets.usecase.decrypt.parser.SecretJsonModel
import com.passbolt.mobile.android.feature.resourceform.usecase.GetDefaultCreateContentTypeUseCase
import com.passbolt.mobile.android.ui.LeadingContentType.PASSWORD
import com.passbolt.mobile.android.ui.LeadingContentType.TOTP
import com.passbolt.mobile.android.ui.Mode.CREATE
import com.passbolt.mobile.android.ui.Mode.UPDATE
import com.passbolt.mobile.android.mappers.EntropyViewMapper
import com.passbolt.mobile.android.mappers.ResourceFormMapper
import com.passbolt.mobile.android.ui.Entropy
import com.passbolt.mobile.android.ui.LeadingContentType
import com.passbolt.mobile.android.ui.MetadataJsonModel
import com.passbolt.mobile.android.ui.MetadataTypeModel
import com.passbolt.mobile.android.ui.Mode
import com.passbolt.mobile.android.ui.OtpParseResult
import com.passbolt.mobile.android.ui.PasswordGeneratorTypeModel
import com.passbolt.mobile.android.ui.ResourceFormUiModel
import com.passbolt.mobile.android.ui.TotpUiModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch

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

class ResourceFormPresenter(
    private val getPasswordPoliciesUseCase: GetPasswordPoliciesUseCase,
    private val secretGenerator: SecretGenerator,
    private val entropyViewMapper: EntropyViewMapper,
    private val entropyCalculator: EntropyCalculator,
    private val getDefaultCreateContentTypeUseCase: GetDefaultCreateContentTypeUseCase,
    private val resourceFormMapper: ResourceFormMapper,
    coroutineLaunchContext: CoroutineLaunchContext
) : DataRefreshViewReactivePresenter<ResourceFormContract.View>(coroutineLaunchContext),
    ResourceFormContract.Presenter {

    override var view: ResourceFormContract.View? = null
    private val job = SupervisorJob()
    private val scope = CoroutineScope(job + coroutineLaunchContext.ui)

    private lateinit var mode: Mode
    private lateinit var uiModel: ResourceFormUiModel

    private lateinit var resourceMetadata: MetadataJsonModel
    private lateinit var resourceSecret: SecretJsonModel
    private lateinit var metadataType: MetadataTypeModel

    private var argsConsumed = false
    private var areAdvancedSettingsExpanded = false

    override fun argsRetrieved(mode: Mode, leadingContentType: LeadingContentType, parentFolderId: String?) {
        this.mode = mode

        scope.launch {
            if (!argsConsumed) {
                initializeModel(leadingContentType)
                argsConsumed = true
            }

            view?.showName(resourceMetadata.name)
            setupAdvancedSettings()
            setupLeadingContentType(uiModel.leadingContentType)
            setupPrimaryButton(mode)
        }
    }

    // TODO update for edit - set data based on the existing resource
    private suspend fun initializeModel(leadingContentType: LeadingContentType) {
        val defaultContentTypeToCreate = getDefaultCreateContentTypeUseCase.execute(
            GetDefaultCreateContentTypeUseCase.Input(leadingContentType)
        )
        val contentType = defaultContentTypeToCreate.contentType
        metadataType = defaultContentTypeToCreate.metadataType
        uiModel = resourceFormMapper.map(contentType)

        // TODO create default value
        resourceMetadata = MetadataJsonModel(
            """
                {"name": ""}
            """.trimIndent()
        )
        // TODO create default value
        resourceSecret = SecretJsonModel(
            """
                {
                    "password": "",
                    "totp": {
                        "secret_key": "",
                        "period": ${OtpParseResult.OtpQr.TotpQr.DEFAULT_PERIOD_SECONDS},
                        "digits": ${OtpParseResult.OtpQr.TotpQr.DEFAULT_DIGITS},
                        "algorithm": ${OtpParseResult.OtpQr.Algorithm.DEFAULT.name}
                    }
                }
            """.trimIndent()
        )
    }

    private fun setupAdvancedSettings() {
        if (areAdvancedSettingsExpanded) {
            view?.setupAdditionalSecrets(uiModel.supportedAdditionalSecrets)
            view?.setupMetadata(uiModel.supportedMetadata)
            view?.hideAdvancedSettings()
        }
    }

    override fun detach() {
        scope.coroutineContext.cancelChildren()
        super<DataRefreshViewReactivePresenter>.detach()
    }

    override fun advancedSettingsClick() {
        view?.setupAdditionalSecrets(uiModel.supportedAdditionalSecrets)
        view?.setupMetadata(uiModel.supportedMetadata)
        view?.hideAdvancedSettings()
        areAdvancedSettingsExpanded = true
    }

    private fun setupPrimaryButton(mode: Mode) {
        when (mode) {
            CREATE -> view?.showCreateButton()
            UPDATE -> view?.showSaveButton()
        }
    }

    private suspend fun setupLeadingContentType(leadingContentType: LeadingContentType) {
        when (leadingContentType) {
            TOTP -> {
                view?.showCreateTotpTitle()
                view?.addTotpLeadingForm(
                    resourceFormMapper.mapToUiModel(resourceSecret.totp, resourceMetadata.name)
                )
                view?.showTotpIssuer(resourceMetadata.getMainUri(metadataType))
                view?.showTotpSecret(resourceSecret.totp?.key.orEmpty())
            }
            PASSWORD -> {
                view?.showCreatePasswordTitle()
                view?.addPasswordLeadingForm()
                view?.showPasswordUsername(resourceMetadata.username.orEmpty())
                view?.showPasswordMainUri(resourceMetadata.getMainUri(metadataType))
                showPassword(resourceSecret.secret)
            }
        }
    }

    override fun passwordGenerateClick() {
        scope.launch {
            val passwordPolicies = getPasswordPoliciesUseCase.execute(Unit)
            val secretGenerationResult = when (passwordPolicies.defaultGenerator) {
                PasswordGeneratorTypeModel.PASSWORD ->
                    secretGenerator.generatePassword(passwordPolicies.passwordGeneratorSettings)
                PasswordGeneratorTypeModel.PASSPHRASE ->
                    secretGenerator.generatePassphrase(passwordPolicies.passphraseGeneratorSettings)
            }
            when (secretGenerationResult) {
                is SecretGenerator.SecretGenerationResult.FailedToGenerateLowEntropy ->
                    view?.showUnableToGeneratePassword(secretGenerationResult.minimumEntropyBits)
                is SecretGenerator.SecretGenerationResult.Success -> view?.showPassword(
                    secretGenerationResult.password,
                    secretGenerationResult.entropy,
                    entropyViewMapper.map(Entropy.parse(secretGenerationResult.entropy))
                )
            }
        }
    }

    override fun nameTextChanged(name: String) {
        resourceMetadata.name = name
    }

    override fun passwordTextChanged(password: String) {
        scope.launch {
            resourceSecret.secret = password

            val entropy = entropyCalculator.getSecretEntropy(password)
            view?.showPasswordStrength(entropyViewMapper.map(Entropy.parse(entropy)), entropy)
        }
    }

    private suspend fun showPassword(password: String) {
        val entropy = entropyCalculator.getSecretEntropy(password)
        view?.showPassword(password.toCodepoints(), entropy, entropyViewMapper.map(Entropy.parse(entropy)))
    }

    override fun passwordMainUriTextChanged(mainUri: String) {
        resourceMetadata.setMainUri(metadataType, mainUri)
    }

    override fun passowrdUsernameTextChanged(username: String) {
        resourceMetadata.username = username
    }

    override fun metadataDescriptionChanged(metadataDescription: String?) {
        resourceMetadata.description = metadataDescription
    }

    override fun additionalSecureNoteClick() {
        view?.navigateToSecureNote(resourceSecret.description.orEmpty())
    }

    override fun totpSecretChanged(secret: String) {
        resourceSecret.totp = requireNotNull(resourceSecret.totp).copy(key = secret)
    }

    override fun totpUrlChanged(url: String) {
        resourceMetadata.setMainUri(metadataType, url)
    }

    override fun totpAdvancedSettingsChanged(totpAdvancedSettings: TotpUiModel?) {
        val settings = totpAdvancedSettings ?: TotpUiModel.emptyWithDefaults(resourceMetadata.getMainUri(metadataType))
        resourceSecret.totp = requireNotNull(resourceSecret.totp).copy(
            algorithm = settings.algorithm,
            digits = settings.length.toInt(),
            period = settings.expiry.toLong()
        )
    }

    override fun additionalTotpClick() {
        view?.navigateToTotp(
            resourceFormMapper.mapToUiModel(resourceSecret.totp, resourceMetadata.getMainUri(metadataType))
        )
    }

    override fun metadataDescriptionClick() {
        view?.navigateToMetadataDescription(resourceMetadata.description.orEmpty())
    }

    override fun secureNoteChanged(secureNote: String?) {
        resourceSecret.description = secureNote
    }

    override fun totpChanged(totpUiModel: TotpUiModel?) {
        // TODO ensure validated before mapping
        resourceSecret.totp = resourceFormMapper.mapToJsonModel(totpUiModel)!!
        resourceMetadata.setMainUri(metadataType, totpUiModel?.issuer.orEmpty())
    }

    override fun refreshSuccessAction() {
        // TODO("Not yet implemented")
    }

    override fun refreshFailureAction() {
        // TODO("Not yet implemented")
    }

    override fun createResourceClick() {
//        TODO("Not yet implemented")
    }

    override fun updateResourceClick() {
//        TODO("Not yet implemented")
    }
}
