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

package com.passbolt.mobile.android.core.policies.usecase

import com.passbolt.mobile.android.core.policies.validation.PasswordPoliciesValidator
import com.passbolt.mobile.android.ui.PasswordPolicies

class PasswordPoliciesInteractor(
    private val fetchPasswordPoliciesUseCase: FetchPasswordPoliciesUseCase,
    private val savePasswordPoliciesUseCase: SavePasswordPoliciesUseCase,
    private val passwordPoliciesValidator: PasswordPoliciesValidator,
) {
    suspend fun fetchAndSavePasswordPolicies(): Output =
        when (val response = fetchPasswordPoliciesUseCase.execute(Unit)) {
            is FetchPasswordPoliciesUseCase.Output.Failure<*> -> Output.Failure.FetchFailure
            is FetchPasswordPoliciesUseCase.Output.Success ->
                validatePasswordPolicies(response.passwordPolicies)
        }

    private suspend fun validatePasswordPolicies(passwordPolicies: PasswordPolicies): Output {
        val arePasswordPoliciesValid = passwordPoliciesValidator.arePasswordPoliciesValid(passwordPolicies)
        return if (arePasswordPoliciesValid) {
            savePasswordPolicies(passwordPolicies)
        } else {
            Output.Failure.ValidationFailure
        }
    }

    private suspend fun savePasswordPolicies(passwordPolicies: PasswordPolicies): Output {
        savePasswordPoliciesUseCase.execute(SavePasswordPoliciesUseCase.Input(passwordPolicies))
        return Output.Success(passwordPolicies)
    }

    sealed class Output {
        data class Success(
            val passwordPolicies: PasswordPolicies,
        ) : Output()

        sealed class Failure : Output() {
            data object FetchFailure : Failure()

            data object ValidationFailure : Failure()
        }
    }
}
