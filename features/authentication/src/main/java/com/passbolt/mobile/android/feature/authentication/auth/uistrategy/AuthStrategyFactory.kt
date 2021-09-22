package com.passbolt.mobile.android.feature.authentication.auth.uistrategy

import com.passbolt.mobile.android.core.navigation.ActivityIntents
import com.passbolt.mobile.android.feature.authentication.auth.AuthFragment

class AuthStrategyFactory {

    fun get(type: ActivityIntents.AuthConfig, view: AuthFragment?) = when (type) {
        ActivityIntents.AuthConfig.STARTUP -> StartupAuthStrategy(view)
        ActivityIntents.AuthConfig.SETUP -> SetupAuthStrategy(view)
        ActivityIntents.AuthConfig.MANAGE_ACCOUNT -> StartupAuthStrategy(view)
        ActivityIntents.AuthConfig.REFRESH_FULL -> SignInAuthStrategy(view)
        ActivityIntents.AuthConfig.REFRESH_PASSPHRASE -> PassphraseAuthStrategy(view)
    }
}
