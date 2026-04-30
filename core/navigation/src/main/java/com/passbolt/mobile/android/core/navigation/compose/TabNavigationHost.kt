package com.passbolt.mobile.android.core.navigation.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ProvidedValue
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.passbolt.mobile.android.core.navigation.compose.base.FeatureModuleNavigation
import com.passbolt.mobile.android.core.navigation.compose.results.NavigationResultEventBus
import com.passbolt.mobile.android.core.navigation.compose.results.ResultEventBus
import org.koin.compose.koinInject

@Composable
fun TabNavigationHost(
    initialKey: NavKey,
    featureModulesNavigation: Set<FeatureModuleNavigation>,
    resultBus: ResultEventBus = remember { ResultEventBus() },
    additionalProviders: Array<ProvidedValue<*>> = emptyArray(),
    navigator: AppNavigator = koinInject(),
) {
    rememberNavBackStack(initialKey).let { backstack ->
        navigator.setActiveBackStack(backstack)
    }

    LaunchedEffect(Unit) {
        navigator.consumePendingNavigation()?.let { pendingKey ->
            navigator.navigateToKey(pendingKey)
        }
    }

    @Suppress("SpreadOperator")
    CompositionLocalProvider(
        NavigationResultEventBus provides resultBus,
        *additionalProviders,
    ) {
        NavDisplay(
            backStack = navigator.backStack,
            onBack = { navigator.navigateBack() },
            entryDecorators =
                listOf(
                    rememberSaveableStateHolderNavEntryDecorator(),
                    rememberViewModelStoreNavEntryDecorator(),
                ),
            entryProvider =
                entryProvider {
                    featureModulesNavigation.forEach { installer ->
                        installer.provideEntryProviderInstaller().invoke(this)
                    }
                },
            transitionSpec = { horizontalSlideTransition },
            popTransitionSpec = { horizontalSlidePopTransition },
            predictivePopTransitionSpec = { horizontalSlidePopTransition },
        )
    }
}
