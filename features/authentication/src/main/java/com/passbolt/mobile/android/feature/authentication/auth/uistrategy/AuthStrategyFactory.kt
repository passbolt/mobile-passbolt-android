package com.passbolt.mobile.android.feature.authentication.auth.uistrategy

import com.passbolt.mobile.android.core.navigation.ActivityIntents
import com.passbolt.mobile.android.feature.authentication.auth.AuthFragment

class AuthStrategyFactory {

    fun get(type: ActivityIntents.AuthConfig, view: AuthFragment?) = when (type) {
        is ActivityIntents.AuthConfig.Startup -> StartupAuthStrategy(view)
        is ActivityIntents.AuthConfig.Setup -> SetupAuthStrategy(view)
        is ActivityIntents.AuthConfig.ManageAccount -> StartupAuthStrategy(view)
        is ActivityIntents.AuthConfig.RefreshFull -> SignInAuthStrategy(view)
        is ActivityIntents.AuthConfig.RefreshPassphrase -> PassphraseAuthStrategy(view)
        is ActivityIntents.AuthConfig.Mfa -> PassphraseAuthStrategy(view)
    }
}
