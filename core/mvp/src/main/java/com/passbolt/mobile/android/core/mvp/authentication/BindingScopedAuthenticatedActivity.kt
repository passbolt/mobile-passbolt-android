package com.passbolt.mobile.android.core.mvp.authentication

import android.app.Activity
import android.view.LayoutInflater
import androidx.activity.result.contract.ActivityResultContracts
import androidx.viewbinding.ViewBinding
import com.passbolt.mobile.android.core.mvp.session.AuthenticationState
import com.passbolt.mobile.android.core.mvp.session.UnauthenticatedReason
import com.passbolt.mobile.android.core.mvp.viewbinding.BindingActivity
import com.passbolt.mobile.android.core.navigation.ActivityIntents
import com.passbolt.mobile.android.core.navigation.AuthenticationType
import org.koin.android.scope.AndroidScopeComponent
import org.koin.androidx.scope.activityScope
import org.koin.core.scope.Scope

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

abstract class BindingScopedAuthenticatedActivity<T : ViewBinding,
        V : BaseAuthenticatedContract.View>(viewInflater: (LayoutInflater) -> T) :
    BindingActivity<T>(viewInflater), AndroidScopeComponent, BaseAuthenticatedContract.View {

    override val scope: Scope by activityScope()
    abstract val presenter: BaseAuthenticatedContract.Presenter<V>

    private val authenticationResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == Activity.RESULT_OK) {
            presenter.authenticationRefreshed()
        }
    }

    override fun showAuth(reason: UnauthenticatedReason) {
        val authType = when (reason) {
            AuthenticationState.Unauthenticated.Reason.PASSPHRASE -> AuthenticationType.Passphrase
            AuthenticationState.Unauthenticated.Reason.SESSION -> AuthenticationType.Refresh
        }
        authenticationResult.launch(
            ActivityIntents.refreshAuthentication(this, authType)
        )
    }
}
