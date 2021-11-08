package com.passbolt.mobile.android.feature.resources.new

import com.passbolt.mobile.android.common.validation.Validation
import com.passbolt.mobile.android.common.validation.validation
import com.passbolt.mobile.android.core.mvp.authentication.BaseAuthenticatedPresenter
import com.passbolt.mobile.android.core.mvp.coroutinecontext.CoroutineLaunchContext
import com.passbolt.mobile.android.database.usecase.GetResourceTypeWithFieldsUseCase
import com.passbolt.mobile.android.entity.resource.ResourceField
import com.passbolt.mobile.android.common.validation.Rule
import com.passbolt.mobile.android.common.validation.StringMaxLength
import com.passbolt.mobile.android.common.validation.StringNotBlank
import com.passbolt.mobile.android.core.commonresource.CreateResourceUseCase
import com.passbolt.mobile.android.core.mvp.session.runAuthenticatedOperation
import com.passbolt.mobile.android.core.security.PasswordGenerator
import com.passbolt.mobile.android.database.usecase.AddLocalResourceUseCase
import com.passbolt.mobile.android.storage.usecase.input.UserIdInput
import com.passbolt.mobile.android.storage.usecase.selectedaccount.GetSelectedAccountUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

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
class NewResourcePresenter(
    coroutineLaunchContext: CoroutineLaunchContext,
    private val getResourceTypeWithFieldsUseCase: GetResourceTypeWithFieldsUseCase,
    private val getSelectedAccountUseCase: GetSelectedAccountUseCase,
    private val passwordGenerator: PasswordGenerator,
    private val entropyViewMapper: EntropyViewMapper,
    private val createResourceUseCase: CreateResourceUseCase,
    private val addLocalResourceUseCase: AddLocalResourceUseCase
) : BaseAuthenticatedPresenter<NewResourceContract.View>(coroutineLaunchContext),
    NewResourceContract.Presenter {

    override var view: NewResourceContract.View? = null
    private val job = SupervisorJob()
    private val scope = CoroutineScope(job + coroutineLaunchContext.ui)
    private lateinit var fields: List<ResourceValue>
    private lateinit var resourceTypeId: String

    override fun viewCreated() {
        createFields()
    }

    private fun createFields() {
        scope.launch {
            val userId = requireNotNull(getSelectedAccountUseCase.execute(Unit).selectedAccount)
            val resourceType = getResourceTypeWithFieldsUseCase.execute(UserIdInput(userId))
            resourceTypeId = resourceType.resourceTypeId
            fields = resourceType.fields.map { ResourceValue(it, "") }
            fields.forEach {
                when (it.field.name) {
                    PASSWORD_FIELD -> view?.addPasswordInput(it.field.name)
                    DESCRIPTION_FIELD -> view?.addDescriptionInput(it.field.name, it.field.isSecret)
                    else -> view?.addTextInput(it.field.name, it.field.isSecret)
                }
            }
        }
    }

    override fun createClick() {
        validation {
            fields.forEach { pair ->
                val rules = getRules(pair.field)
                of(pair.value) {
                    rules.forEach { rule ->
                        validateRules(rule, pair)
                    }
                }
                onValid {
                    createResource()
                }
            }
        }
    }

    override fun textChanged(tag: String, value: String) {
        fields.find { it.field.name == tag }?.value = value
    }

    private fun createResource() {
        view?.showProgress()
        scope.launch {
            when (val result = runAuthenticatedOperation(needSessionRefreshFlow, sessionRefreshedFlow) {
                createResourceUseCase.execute(createResourceInput())
            }) {
                is CreateResourceUseCase.Output.Failure<*> -> view?.showError()
                is CreateResourceUseCase.Output.Success -> {
                    addLocalResourceUseCase.execute(AddLocalResourceUseCase.Input(result.resource))
                    view?.navigateBackWithSuccess()
                }
            }
            view?.hideProgress()
        }
    }

    private fun createResourceInput() =
        CreateResourceUseCase.Input(
            fields.find { it.field.name == NAME_FIELD }?.value!!,
            resourceTypeId,
            fields.find { it.field.name == PASSWORD_FIELD }?.value!!,
            fields.find { it.field.name == DESCRIPTION_FIELD }?.value.orEmpty(),
            fields.find { it.field.name == USERNAME_FIELD }?.value,
            fields.find { it.field.name == URI_FIELD }?.value
        )

    private fun Validation.ValueValidation<String>.validateRules(
        rule: Rule<String>,
        resourceValue: ResourceValue
    ) {
        withRules(rule) {
            when (rule) {
                is StringNotBlank -> {
                    onInvalid {
                        view?.showEmptyValueError(resourceValue.field.name)
                    }
                }
                is StringMaxLength -> {
                    onInvalid {
                        view?.showTooLongError(resourceValue.field.name)
                    }
                }
            }
        }
    }

    override fun passwordGenerateClick(tag: String) {
        val generatedPassword = passwordGenerator.generate()
        view?.showPassword(tag, generatedPassword)
    }

    override fun passwordTextChanged(tag: String, password: String) {
        fields.find { it.field.name == PASSWORD_FIELD }?.value = password
        val entropy = PasswordGenerator.Entropy.parse(passwordGenerator.getEntropy(password))
        view?.showPasswordStrength(tag, entropyViewMapper.map(entropy))
    }

    private fun getRules(field: ResourceField): List<Rule<String>> {
        val rules = mutableListOf<Rule<String>>()
        if (field.isRequired) {
            rules.add(StringNotBlank)
        }
        field.maxLength?.let {
            rules.add(StringMaxLength(it))
        }
        return rules
    }

    companion object {
        private const val PASSWORD_FIELD = "password"
        private const val DESCRIPTION_FIELD = "description"
        private const val USERNAME_FIELD = "username"
        private const val URI_FIELD = "uri"
        private const val NAME_FIELD = "name"
    }

    private class ResourceValue(
        val field: ResourceField,
        var value: String
    )
}
