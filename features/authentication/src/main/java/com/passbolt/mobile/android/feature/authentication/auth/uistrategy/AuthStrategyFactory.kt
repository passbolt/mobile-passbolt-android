package com.passbolt.mobile.android.feature.authentication.auth.uistrategy

import com.passbolt.mobile.android.core.navigation.AuthenticationType
import com.passbolt.mobile.android.feature.authentication.auth.AuthFragment

class AuthStrategyFactory {

    fun get(type: AuthenticationType, view: AuthFragment?) = when (type) {
        is AuthenticationType.Passphrase -> PassphraseAuthStrategy(view)
        is AuthenticationType.SignIn -> SignInAuthStrategy(view)
        is AuthenticationType.Refresh -> RefreshAuthStrategy(view)
    }
}