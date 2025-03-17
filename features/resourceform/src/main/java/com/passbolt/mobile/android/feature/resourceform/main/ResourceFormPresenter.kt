package com.passbolt.mobile.android.feature.resourceform.main

import com.passbolt.mobile.android.core.fulldatarefresh.base.DataRefreshViewReactivePresenter
import com.passbolt.mobile.android.core.mvp.coroutinecontext.CoroutineLaunchContext
import com.passbolt.mobile.android.core.passwordgenerator.SecretGenerator
import com.passbolt.mobile.android.core.passwordgenerator.entropy.EntropyCalculator
import com.passbolt.mobile.android.core.policies.usecase.GetPasswordPoliciesUseCase
import com.passbolt.mobile.android.feature.resourceform.main.LeadingContentType.PASSWORD
import com.passbolt.mobile.android.feature.resourceform.main.LeadingContentType.TOTP
import com.passbolt.mobile.android.feature.resourceform.main.Mode.CREATE
import com.passbolt.mobile.android.feature.resourceform.main.Mode.UPDATE
import com.passbolt.mobile.android.mappers.EntropyViewMapper
import com.passbolt.mobile.android.ui.Entropy
import com.passbolt.mobile.android.ui.PasswordGeneratorTypeModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
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
    coroutineLaunchContext: CoroutineLaunchContext
) : DataRefreshViewReactivePresenter<ResourceFormContract.View>(coroutineLaunchContext),
    ResourceFormContract.Presenter {

    override var view: ResourceFormContract.View? = null
    private val job = SupervisorJob()
    private val scope = CoroutineScope(job + coroutineLaunchContext.ui)

    override fun argsRetrieved(mode: Mode, leadingContentType: LeadingContentType, parentFolderId: String?) {
        setupLeadingContentType(leadingContentType)
        setupPrimaryButton(mode)
    }

    private fun setupPrimaryButton(mode: Mode) {
        when (mode) {
            CREATE -> view?.showCreateButton()
            UPDATE -> view?.showSaveButton()
        }
    }

    private fun setupLeadingContentType(leadingContentType: LeadingContentType) {
        when (leadingContentType) {
            TOTP -> {
                view?.showCreateTotpTitle()
                view?.addTotpLeadingForm()
            }
            PASSWORD -> {
                view?.showCreatePasswordTitle()
                view?.addPasswordLeadingForm()
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

    override fun passwordTextChanged(password: String) {
        scope.launch {
            // TODO update resource model with changed password

            val entropy = entropyCalculator.getSecretEntropy(password)
            view?.showPasswordStrength(entropyViewMapper.map(Entropy.parse(entropy)), entropy)
        }
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
