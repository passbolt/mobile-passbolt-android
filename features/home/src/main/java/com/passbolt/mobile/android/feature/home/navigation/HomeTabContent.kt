package com.passbolt.mobile.android.feature.home.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.passbolt.mobile.android.core.navigation.compose.HomeNavigation
import com.passbolt.mobile.android.core.preferences.usecase.GetHomeDisplayViewPrefsUseCase
import com.passbolt.mobile.android.mappers.HomeDisplayViewMapper
import org.koin.compose.koinInject

@Composable
fun HomeTabContent() {
    val filterPreferencesUseCase: GetHomeDisplayViewPrefsUseCase = koinInject()
    val homeDisplayMapper: HomeDisplayViewMapper = koinInject()
    val initialHomeDisplay =
        remember {
            val prefs = filterPreferencesUseCase.execute(Unit)
            homeDisplayMapper.map(prefs.userSetHomeView, prefs.lastUsedHomeView)
        }
    HomeNavigation(initialHomeDisplay)
}
