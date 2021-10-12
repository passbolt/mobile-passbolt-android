package com.passbolt.mobile.android.feature.authentication.auth.presenter

import com.passbolt.mobile.android.core.navigation.ActivityIntents
import com.passbolt.mobile.android.feature.authentication.auth.AuthContract

class AuthReasonMapper {

    fun map(authConfig: ActivityIntents.AuthConfig) = when (authConfig) {
        is ActivityIntents.AuthConfig.RefreshPassphrase -> AuthContract.View.RefreshAuthReason.PASSPHRASE
        is ActivityIntents.AuthConfig.RefreshFull -> AuthContract.View.RefreshAuthReason.SESSION
        else -> {
            null /* reason is shown only for session and passphrase refresh*/
        }
    }
}
