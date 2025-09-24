package com.passbolt.mobile.android.core.navigation.compose

import com.passbolt.mobile.android.core.navigation.AppContext

sealed interface NavigationActivity {
    data class StartUp(
        val appContext: AppContext,
    ) : NavigationActivity

    object ManageAccounts : NavigationActivity

    object TransferAccount : NavigationActivity

    object AccountDetails : NavigationActivity
}
