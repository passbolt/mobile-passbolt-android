package com.passbolt.mobile.android.feature.resourceform.additionalsecrets.password

import com.passbolt.mobile.android.core.mvp.coroutinecontext.CoroutineLaunchContext
import com.passbolt.mobile.android.core.passwordgenerator.SecretGenerator
import com.passbolt.mobile.android.core.passwordgenerator.codepoints.toCodepoints
import com.passbolt.mobile.android.core.passwordgenerator.entropy.EntropyCalculator
import com.passbolt.mobile.android.core.policies.usecase.GetPasswordPoliciesUseCase
import com.passbolt.mobile.android.mappers.EntropyViewMapper
import com.passbolt.mobile.android.ui.Entropy
import com.passbolt.mobile.android.ui.PasswordGeneratorTypeModel
import com.passbolt.mobile.android.ui.PasswordUiModel
import com.passbolt.mobile.android.ui.ResourceFormMode
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

class PasswordFormPresenter(
    private val entropyViewMapper: EntropyViewMapper,
    private val entropyCalculator: EntropyCalculator,
    private val getPasswordPoliciesUseCase: GetPasswordPoliciesUseCase,
    private val secretGenerator: SecretGenerator,
    coroutineLaunchContext: CoroutineLaunchContext
) : PasswordFormContract.Presenter {

    override var view: PasswordFormContract.View? = null
    private val job = SupervisorJob()
    private val scope = CoroutineScope(job + coroutineLaunchContext.ui)

    private lateinit var passwordModel: PasswordUiModel

    override fun argsRetrieved(mode: ResourceFormMode, passwordModel: PasswordUiModel) {
        this.passwordModel = passwordModel
        when (mode) {
            is ResourceFormMode.Create -> view?.showCreateTitle()
            is ResourceFormMode.Edit -> view?.showEditTitle(mode.resourceName)
        }

        scope.launch {
            view?.showPasswordMainUri(passwordModel.mainUri)
            view?.showPasswordUsername(passwordModel.username)
            showPassword(passwordModel.password)
        }
    }

    override fun detach() {
        scope.coroutineContext.cancelChildren()
        super.detach()
    }

    private suspend fun showPassword(password: String) {
        val entropy = entropyCalculator.getSecretEntropy(password)
        view?.showPassword(password.toCodepoints(), entropy, entropyViewMapper.map(Entropy.parse(entropy)))
    }

    override fun passwordMainUriTextChanged(mainUri: String) {
        this.passwordModel = passwordModel.copy(mainUri = mainUri)
    }

    override fun passwordUsernameTextChanged(username: String) {
        this.passwordModel = passwordModel.copy(username = username)
    }

    override fun passwordTextChanged(password: String) {
        this.passwordModel = passwordModel.copy(password = password)

        scope.launch {
            val entropy = entropyCalculator.getSecretEntropy(password)
            view?.showPasswordStrength(entropyViewMapper.map(Entropy.parse(entropy)), entropy)
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

    override fun applyClick() {
        view?.goBackWithResult(passwordModel)
    }
}
