package com.passbolt.mobile.android.core.navigation.compose

import androidx.compose.runtime.compositionLocalOf
import com.passbolt.mobile.android.core.navigation.ActivityIntents
import com.passbolt.mobile.android.core.navigation.AppContext

data class AuthenticationParams(
    val authConfig: ActivityIntents.AuthConfig,
    val appContext: AppContext,
)

val LocalAuthenticationParams =
    compositionLocalOf<AuthenticationParams> { error("No AuthenticationParams provided") }
