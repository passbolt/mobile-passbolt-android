package com.passbolt.mobile.android.feature.authentication

import android.app.Activity
import android.view.LayoutInflater
import androidx.activity.result.contract.ActivityResultContracts
import androidx.viewbinding.ViewBinding
import com.passbolt.mobile.android.core.mvp.authentication.BaseAuthenticatedContract
import com.passbolt.mobile.android.core.mvp.authentication.AuthenticationState.Unauthenticated.Reason
import com.passbolt.mobile.android.core.mvp.authentication.UnauthenticatedReason
import com.passbolt.mobile.android.core.mvp.viewbinding.BindingActivity
import com.passbolt.mobile.android.core.navigation.ActivityIntents
import com.passbolt.mobile.android.feature.authentication.mfa.unknown.UnknownProviderDialog
import com.passbolt.mobile.android.feature.authentication.mfa.totp.EnterTotpDialog
import com.passbolt.mobile.android.feature.authentication.mfa.totp.EnterTotpListener
import com.passbolt.mobile.android.feature.authentication.mfa.youbikey.ScanYubikeyDialog
import com.passbolt.mobile.android.feature.authentication.mfa.youbikey.ScanYubikeyListener
import com.passbolt.mobile.android.storage.usecase.session.GetSessionUseCase
import org.koin.android.ext.android.inject
import org.koin.android.scope.AndroidScopeComponent
import org.koin.androidx.scope.activityScope
import org.koin.core.scope.Scope
import java.lang.IllegalStateException

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
    BindingActivity<T>(viewInflater), AndroidScopeComponent, BaseAuthenticatedContract.View, EnterTotpListener,
    ScanYubikeyListener {

    override val scope: Scope by activityScope()
    abstract val presenter: BaseAuthenticatedContract.Presenter<V>
    private val mfaProviderHandler: MfaProviderHandler by inject()
    private val getSessionUseCase: GetSessionUseCase by inject()

    private val authenticationResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == Activity.RESULT_OK) {
            presenter.authenticationRefreshed()
        }
    }

    override fun showAuth(reason: UnauthenticatedReason) {
        if (reason is Reason.Mfa) {
            mfaProviderHandler.run(reason, ::showYubikeyDialog, ::showTotpDialog, ::showUnknownProvider)
        } else {
            val authType = when (reason) {
                Reason.Passphrase -> ActivityIntents.AuthConfig.RefreshPassphrase
                Reason.Session -> ActivityIntents.AuthConfig.SignIn
                else -> {
                    throw IllegalStateException("Wrong reason")
                }
            }
            authenticationResult.launch(
                ActivityIntents.authentication(this, authType)
            )
        }
    }

    private fun showUnknownProvider() {
        UnknownProviderDialog().show(
            supportFragmentManager, UnknownProviderDialog::class.java.name
        )
    }

    private fun showTotpDialog(hasYubikeyProvider: Boolean) {
        EnterTotpDialog.newInstance(
            token = getSessionUseCase.execute(Unit).accessToken,
            hasYubikeyProvider = hasYubikeyProvider
        ).show(
            supportFragmentManager, EnterTotpDialog::class.java.name
        )
    }

    private fun showYubikeyDialog(hasTotpProvider: Boolean) {
        ScanYubikeyDialog.newInstance(
            token = getSessionUseCase.execute(Unit).accessToken,
            hasTotpProvider = hasTotpProvider
        ).show(
            supportFragmentManager, EnterTotpDialog::class.java.name
        )
    }

    override fun changeProviderToYubikey(bearer: String) {
        showYubikeyDialog(true)
    }

    override fun totpVerificationSucceeded(mfaHeader: String?) {
        presenter.authenticationRefreshed()
    }

    override fun changeProviderToTotp(jwtToken: String?) {
        showTotpDialog(true)
    }

    override fun yubikeyVerificationSucceeded(mfaHeader: String?) {
        presenter.authenticationRefreshed()
    }
}
