package com.passbolt.mobile.android.feature.authentication

import android.app.Activity
import android.os.Bundle
import com.passbolt.mobile.android.common.lifecycleawarelazy.lifecycleAwareLazy
import com.passbolt.mobile.android.core.extension.findNavHostFragment
import com.passbolt.mobile.android.core.mvp.scoped.BindingScopedActivity
import com.passbolt.mobile.android.core.navigation.ActivityIntents
import com.passbolt.mobile.android.feature.authentication.accountslist.AccountsListFragmentDirections
import com.passbolt.mobile.android.feature.authentication.databinding.ActivityAuthenticationMainBinding
import org.koin.android.ext.android.inject

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
class AuthenticationMainActivity :
    BindingScopedActivity<ActivityAuthenticationMainBinding>(ActivityAuthenticationMainBinding::inflate),
    AuthenticationMainContract.View {

    private val presenter: AuthenticationMainContract.Presenter by inject()
    private val navController by lifecycleAwareLazy {
        findNavHostFragment(binding.fragmentContainer.id).navController
    }

    private val authStrategy by lifecycleAwareLazy {
        val ordinal = intent.getIntExtra(ActivityIntents.EXTRA_AUTH_STRATEGY_TYPE, -1)
        AuthenticationType.values()[ordinal]
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        presenter.attach(this)
        presenter.bundleRetrieved(authStrategy)
        setResult(Activity.RESULT_CANCELED)
    }

    override fun navigateToAuth(
        userId: String,
        authenticationStrategy: AuthenticationType
    ) {
        navController.navigate(
            AccountsListFragmentDirections.actionAccountsListFragmentToAuthFragment(userId, authenticationStrategy)
        )
    }
}
