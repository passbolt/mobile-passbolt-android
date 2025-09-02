package com.passbolt.mobile.android.core.navigation.compose

import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSavedStateNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import androidx.navigation3.ui.rememberSceneSetupNavEntryDecorator
import com.passbolt.mobile.android.core.navigation.compose.base.Feature
import com.passbolt.mobile.android.core.navigation.compose.base.FeatureModuleNavigation
import com.passbolt.mobile.android.core.navigation.compose.keys.SettingsNavigationKey
import org.koin.compose.koinInject
import org.koin.core.qualifier.named

@Composable
fun AppNavigation(navigator: AppNavigator = koinInject()) {
    rememberNavBackStack(SettingsNavigationKey.SettingsMain).let { backstack ->
        navigator.backStack = backstack
    }

    val featureModulesNavigation: Set<FeatureModuleNavigation> =
        setOf(
            koinInject<FeatureModuleNavigation>(named(Feature.SETTINGS)),
            koinInject<FeatureModuleNavigation>(named(Feature.LOGS)),
            koinInject<FeatureModuleNavigation>(named(Feature.ACCOUNT_DETAILS)),
        )

    NavDisplay(
        backStack = navigator.backStack,
        onBack = { navigator.navigateBack() },
        entryDecorators =
            listOf(
                rememberSceneSetupNavEntryDecorator(),
                rememberSavedStateNavEntryDecorator(),
                rememberViewModelStoreNavEntryDecorator(),
            ),
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
