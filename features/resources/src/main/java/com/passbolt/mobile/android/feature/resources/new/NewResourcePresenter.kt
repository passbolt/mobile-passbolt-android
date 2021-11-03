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
    private val getSelectedAccountUseCase: GetSelectedAccountUseCase
) : BaseAuthenticatedPresenter<NewResourceContract.View>(coroutineLaunchContext),
    NewResourceContract.Presenter {

    override var view: NewResourceContract.View? = null
    private val job = SupervisorJob()
    private val scope = CoroutineScope(job + coroutineLaunchContext.ui)
    private lateinit var fields: List<Pair<ResourceField, String>>

    override fun viewCreated() {
        scope.launch {
            val userId = requireNotNull(getSelectedAccountUseCase.execute(Unit).selectedAccount)
            fields = getResourceTypeWithFieldsUseCase.execute(UserIdInput(userId)).fields.map { Pair(it, "") }
            fields.forEach {
                when {
                    it.first.name == PASSWORD_FIELD -> view?.addPasswordInput(it.first.name)
                    it.first.name == DESCRIPTION_FIELD -> view?.addDescriptionInput(it.first.name)
                    it.first.isSecret -> view?.addSecretInput(it.first.name)
                    else -> view?.addTextInput(it.first.name)
                }
            }
        }
    }

    override fun createClick() {
        validation {
            fields.forEach { pair ->
                val rules = getRules(pair.first)
                of(pair.second) {
                    rules.forEach { rule ->
                        validateRules(rule, pair)
                    }
                }
                onValid {
                    // TODO save resource
                }
            }
        }
    }

    private fun Validation.ValueValidation<String>.validateRules(
        rule: Rule<String>,
        pair: Pair<ResourceField, String>
    ) {
        withRules(rule) {
            when (rule) {
                is StringNotBlank -> {
                    onInvalid {
                        view?.showEmptyValueError(pair.first.name)
                    }
                }
                is StringMaxLength -> {
                    onInvalid {
                        view?.showTooLongError(pair.first.name)
                    }
                }
            }
        }
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
    }
}
