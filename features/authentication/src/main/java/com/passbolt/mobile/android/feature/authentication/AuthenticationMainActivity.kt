package com.passbolt.mobile.android.feature.authentication

import android.app.Activity
import android.os.Bundle
import androidx.navigation.NavOptions
import com.passbolt.mobile.android.common.lifecycleawarelazy.lifecycleAwareLazy
import com.passbolt.mobile.android.core.extension.findNavHostFragment
import com.passbolt.mobile.android.core.mvp.scoped.BindingScopedActivity
import com.passbolt.mobile.android.core.navigation.ActivityIntents
import com.passbolt.mobile.android.core.navigation.AppContext
import com.passbolt.mobile.android.core.security.flagsecure.FlagSecureSetter
import com.passbolt.mobile.android.feature.authentication.accountslist.AccountsListFragment
import com.passbolt.mobile.android.feature.authentication.auth.AuthFragment
import com.passbolt.mobile.android.feature.authentication.databinding.ActivityAuthenticationMainBinding
import org.koin.android.ext.android.inject
import com.passbolt.mobile.android.core.ui.R as CoreUiR

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

    private val navGraph by lifecycleAwareLazy {
        navController.navInflater.inflate(R.navigation.authentication)
    }

    private val authConfig by lifecycleAwareLazy {
        intent.getSerializableExtra(ActivityIntents.EXTRA_AUTH_CONFIG) as ActivityIntents.AuthConfig
    }

    private val context by lifecycleAwareLazy {
        intent.getSerializableExtra(ActivityIntents.EXTRA_CONTEXT) as AppContext
    }

    private val userId by lifecycleAwareLazy {
        intent.getStringExtra(ActivityIntents.EXTRA_USER_ID)
    }

    private val flagSecureSetter: FlagSecureSetter by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        flagSecureSetter.set(this)

        presenter.attach(this)
        presenter.bundleRetrieved(authConfig, userId)
        setResult(Activity.RESULT_CANCELED)
    }

    override fun onDestroy() {
        presenter.detach()
        super.onDestroy()
    }

    override fun initNavWithAccountList() {
        with(navGraph) {
            setStartDestination(R.id.accountsListFragment)
            navController.setGraph(this, AccountsListFragment.newBundle(authConfig, context))
        }
    }

    override fun initNavWithoutAccountList(currentAccount: String) {
        with(navGraph) {
            remove(requireNotNull(findNode(R.id.accountsListFragment)))
            setStartDestination(R.id.authFragment)
            navController.setGraph(this, AuthFragment.newBundle(authConfig, context, currentAccount))
        }
    }

    override fun navigateToSignIn(currentAccount: String) {
        navController.navigate(
            R.id.authFragment,
            AuthFragment.newBundle(authConfig, context, currentAccount),
            NavOptions.Builder()
                .setEnterAnim(CoreUiR.anim.slide_in_right)
                .setExitAnim(CoreUiR.anim.slide_out_left)
                .setPopEnterAnim(CoreUiR.anim.slide_in_left)
                .setPopExitAnim(CoreUiR.anim.slide_out_right)
                .build()
        )
    }
}
