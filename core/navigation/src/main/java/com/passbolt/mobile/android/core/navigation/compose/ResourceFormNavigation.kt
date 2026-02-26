package com.passbolt.mobile.android.core.navigation.compose

import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.passbolt.mobile.android.core.navigation.compose.base.Feature.RESOURCE_FORM
import com.passbolt.mobile.android.core.navigation.compose.base.FeatureModuleNavigation
import com.passbolt.mobile.android.core.navigation.compose.keys.ResourceFormNavigationKey.MainResourceForm
import com.passbolt.mobile.android.core.navigation.compose.results.NavigationResultEventBus
import com.passbolt.mobile.android.core.navigation.compose.results.ResultEventBus
import com.passbolt.mobile.android.ui.ResourceFormMode
import org.koin.compose.koinInject
import org.koin.core.qualifier.named

@Composable
fun ResourceFormNavigation(
    mode: ResourceFormMode,
    hostNavigation: ResourceFormHostNavigation,
    navigator: AppNavigator = koinInject(),
) {
    val resultBus = remember { ResultEventBus() }

    rememberNavBackStack(MainResourceForm(mode)).let { backstack ->
        navigator.backStack = backstack
    }

    val featureModulesNavigation: Set<FeatureModuleNavigation> =
        setOf(
            koinInject<FeatureModuleNavigation>(named(RESOURCE_FORM)),
        )

    CompositionLocalProvider(
        NavigationResultEventBus provides resultBus,
        LocalResourceFormHostNavigation provides hostNavigation,
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
}
