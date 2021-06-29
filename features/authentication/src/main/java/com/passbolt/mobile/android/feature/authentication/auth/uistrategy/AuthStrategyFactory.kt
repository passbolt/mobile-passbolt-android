package com.passbolt.mobile.android.feature.authentication.auth.uistrategy

import com.passbolt.mobile.android.feature.authentication.AuthenticationType
import com.passbolt.mobile.android.feature.authentication.auth.AuthFragment

class AuthStrategyFactory {

    fun get(type: AuthenticationType, view: AuthFragment?) = when (type) {
        AuthenticationType.PASSPHRASE -> PassphraseAuthStrategy(view)
        AuthenticationType.SIGN_IN -> SignInAuthStrategy(view)
    }
}
