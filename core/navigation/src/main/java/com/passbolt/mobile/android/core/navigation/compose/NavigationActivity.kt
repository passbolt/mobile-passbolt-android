package com.passbolt.mobile.android.core.navigation.compose

sealed interface NavigationActivity {
    object StartUp : NavigationActivity

    object ManageAccounts : NavigationActivity

    object TransferAccount : NavigationActivity

    object AccountDetails : NavigationActivity
}
