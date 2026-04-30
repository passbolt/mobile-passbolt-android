package com.passbolt.mobile.android.feature.startup

import com.passbolt.mobile.android.core.navigation.AccountSetupDataModel

sealed class StartUpSideEffect {
    data class NavigateToSetup(
        val accountSetupDataModel: AccountSetupDataModel?,
    ) : StartUpSideEffect()

    data object NavigateToSignIn : StartUpSideEffect()
}
