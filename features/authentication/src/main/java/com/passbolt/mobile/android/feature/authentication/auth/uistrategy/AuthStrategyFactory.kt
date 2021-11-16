package com.passbolt.mobile.android.feature.authentication.auth.uistrategy

import com.passbolt.mobile.android.core.navigation.ActivityIntents
import com.passbolt.mobile.android.core.navigation.AppContext
import com.passbolt.mobile.android.feature.authentication.auth.AuthFragment

class AuthStrategyFactory {

    fun get(type: ActivityIntents.AuthConfig, context: AppContext, view: AuthFragment?) = when (type) {
        is ActivityIntents.AuthConfig.Startup -> StartupAuthStrategy(view, context)
        is ActivityIntents.AuthConfig.Setup -> SetupAuthStrategy(view, context)
        is ActivityIntents.AuthConfig.ManageAccount -> StartupAuthStrategy(view, context)
        is ActivityIntents.AuthConfig.RefreshFull -> SignInAuthStrategy(view, context)
        is ActivityIntents.AuthConfig.RefreshPassphrase -> PassphraseAuthStrategy(view, context)
        is ActivityIntents.AuthConfig.Mfa -> PassphraseAuthStrategy(view, context)
    }
}
