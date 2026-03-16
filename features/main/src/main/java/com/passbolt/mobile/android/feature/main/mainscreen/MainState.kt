package com.passbolt.mobile.android.feature.main.mainscreen

import com.passbolt.mobile.android.core.navigation.compose.BottomTab
import com.passbolt.mobile.android.feature.main.mainscreen.bottomnavigation.MainBottomNavigationModel

data class MainState(
    val bottomNavigationModel: MainBottomNavigationModel? = null,
    val showChromeNativeAutofillDialog: Boolean = false,
    val selectedTab: BottomTab = BottomTab.HOME,
)
