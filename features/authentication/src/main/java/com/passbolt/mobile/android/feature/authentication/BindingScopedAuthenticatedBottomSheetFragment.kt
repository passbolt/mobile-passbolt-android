package com.passbolt.mobile.android.feature.authentication

import android.app.Activity
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.viewbinding.ViewBinding
import com.passbolt.mobile.android.core.authenticationcore.session.GetSessionUseCase
import com.passbolt.mobile.android.core.mvp.authentication.AuthenticationState.Unauthenticated.Reason
import com.passbolt.mobile.android.core.mvp.authentication.AuthenticationState.Unauthenticated.Reason.Mfa.MfaProvider.DUO
import com.passbolt.mobile.android.core.mvp.authentication.AuthenticationState.Unauthenticated.Reason.Mfa.MfaProvider.TOTP
import com.passbolt.mobile.android.core.mvp.authentication.AuthenticationState.Unauthenticated.Reason.Mfa.MfaProvider.YUBIKEY
import com.passbolt.mobile.android.core.mvp.authentication.BaseAuthenticatedContract
import com.passbolt.mobile.android.core.mvp.scoped.BindingScopedBottomSheetFragment
import com.passbolt.mobile.android.core.navigation.ActivityIntents
import com.passbolt.mobile.android.feature.authentication.mfa.duo.AuthWithDuoDialog
import com.passbolt.mobile.android.feature.authentication.mfa.duo.AuthWithDuoListener
import com.passbolt.mobile.android.feature.authentication.mfa.totp.EnterTotpDialog
import com.passbolt.mobile.android.feature.authentication.mfa.totp.EnterTotpListener
import com.passbolt.mobile.android.feature.authentication.mfa.unknown.UnknownProviderDialog
import com.passbolt.mobile.android.feature.authentication.mfa.youbikey.ScanYubikeyDialog
import com.passbolt.mobile.android.feature.authentication.mfa.youbikey.ScanYubikeyListener
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

abstract class BindingScopedAuthenticatedBottomSheetFragment
<T : ViewBinding, V : BaseAuthenticatedContract.View>(viewInflater: (LayoutInflater, ViewGroup?, Boolean) -> T) :
    BindingScopedBottomSheetFragment<T>(viewInflater), BaseAuthenticatedContract.View, EnterTotpListener,
    ScanYubikeyListener, AuthWithDuoListener {

    abstract val presenter: BaseAuthenticatedContract.Presenter<V>
    private val getSessionUseCase: GetSessionUseCase by inject()

    private val authenticationResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == Activity.RESULT_OK) {
            presenter.authenticationRefreshed()
        }
    }

    override fun showRefreshPassphraseAuth() {
        authenticationResult.launch(
            ActivityIntents.authentication(requireContext(), ActivityIntents.AuthConfig.RefreshPassphrase)
        )
    }

    override fun showSignInAuth() {
        authenticationResult.launch(
            ActivityIntents.authentication(requireContext(), ActivityIntents.AuthConfig.SignIn)
        )
    }

    override fun showMfaAuth(mfaReason: Reason.Mfa.MfaProvider?, hasMultipleProviders: Boolean) {
        when (mfaReason) {
            YUBIKEY -> showYubikeyDialog(hasMultipleProviders)
            TOTP -> showTotpDialog(hasMultipleProviders)
            DUO -> showDuoDialog(hasMultipleProviders)
            null -> showUnknownProvider()
        }
    }

    override fun showTotpDialog(hasOtherProviders: Boolean) {
        EnterTotpDialog.newInstance(
            token = getSessionUseCase.execute(Unit).accessToken,
            hasOtherProvider = hasOtherProviders
        ).show(
            childFragmentManager, EnterTotpDialog::class.java.name
        )
    }

    override fun showYubikeyDialog(hasOtherProviders: Boolean) {
        ScanYubikeyDialog.newInstance(
            token = getSessionUseCase.execute(Unit).accessToken,
            hasOtherProvider = hasOtherProviders
        ).show(
            childFragmentManager, ScanYubikeyDialog::class.java.name
        )
    }

    override fun showDuoDialog(hasOtherProviders: Boolean) {
        AuthWithDuoDialog.newInstance(
            token = getSessionUseCase.execute(Unit).accessToken,
            hasOtherProvider = hasOtherProviders
        ).show(
            childFragmentManager, AuthWithDuoDialog::class.java.name
        )
    }

    override fun showUnknownProvider() {
        UnknownProviderDialog().show(
            childFragmentManager, UnknownProviderDialog::class.java.name
        )
    }

    override fun totpOtherProviderClick(bearer: String) {
        presenter.otherProviderClick(TOTP)
    }

    override fun totpVerificationSucceeded(mfaHeader: String?) {
        presenter.authenticationRefreshed()
    }

    override fun yubikeyOtherProviderClick(jwtToken: String?) {
        presenter.otherProviderClick(YUBIKEY)
    }

    override fun yubikeyVerificationSucceeded(mfaHeader: String?) {
        presenter.authenticationRefreshed()
    }

    override fun duoOtherProviderClick(jwtToken: String?) {
        presenter.otherProviderClick(DUO)
    }

    override fun duoAuthSucceeded(mfaHeader: String?) {
        presenter.authenticationRefreshed()
    }
}
