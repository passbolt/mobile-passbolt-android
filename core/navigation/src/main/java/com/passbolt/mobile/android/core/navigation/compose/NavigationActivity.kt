package com.passbolt.mobile.android.core.navigation.compose

import com.passbolt.mobile.android.core.navigation.AccountSetupDataModel
import com.passbolt.mobile.android.core.navigation.AppContext

sealed interface NavigationActivity {
    data class AuthenticationStartUp(
        val appContext: AppContext,
    ) : NavigationActivity

    object AuthenticationSignIn : NavigationActivity

    object AuthenticationManageAccounts : NavigationActivity

    object Home : NavigationActivity

    object Start : NavigationActivity

    object Setup : NavigationActivity

    data class SetupWithPredefinedAccountData(
        val accountSetupDataModel: AccountSetupDataModel?,
    ) : NavigationActivity

    object AutofillReorderToFront : NavigationActivity
}
