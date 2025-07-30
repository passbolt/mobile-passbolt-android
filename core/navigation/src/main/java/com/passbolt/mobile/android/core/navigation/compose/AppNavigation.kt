package com.passbolt.mobile.android.core.navigation.compose

import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import com.passbolt.mobile.android.core.navigation.compose.base.Feature
import com.passbolt.mobile.android.core.navigation.compose.base.FeatureModuleNavigation
import org.koin.compose.koinInject
import org.koin.core.qualifier.named

@Composable
fun AppNavigation(navigator: AppNavigator = koinInject()) {
    val featureModulesNavigation: Set<FeatureModuleNavigation> =
        setOf(
            koinInject<FeatureModuleNavigation>(named(Feature.SETTINGS)),
            koinInject<FeatureModuleNavigation>(named(Feature.LOGS)),
        )

    NavDisplay(
        backStack = navigator.backStack,
        onBack = { navigator.navigateBack() },
        entryProvider =
            entryProvider {
                featureModulesNavigation.forEach { installer ->
                    installer.provideEntryProviderInstaller().invoke(this)
                }
            },
        transitionSpec = {
            slideInHorizontally(initialOffsetX = { it }) togetherWith
                slideOutHorizontally(targetOffsetX = { -it })
        },
        popTransitionSpec = {
            slideInHorizontally(initialOffsetX = { -it }) togetherWith
                slideOutHorizontally(targetOffsetX = { it })
        },
        predictivePopTransitionSpec = {
            slideInHorizontally(initialOffsetX = { -it }) togetherWith
                slideOutHorizontally(targetOffsetX = { it })
        },
    )
}
