package com.passbolt.mobile.android.core.navigation.compose

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.passbolt.mobile.android.core.navigation.compose.base.Feature.AUTOFILL_ENCOURAGEMENTS
import com.passbolt.mobile.android.core.navigation.compose.base.Feature.LOGS
import com.passbolt.mobile.android.core.navigation.compose.base.Feature.SETUP
import com.passbolt.mobile.android.core.navigation.compose.base.FeatureModuleNavigation
import com.passbolt.mobile.android.core.navigation.compose.keys.SetupNavigationKey.Welcome
import org.koin.compose.koinInject
import org.koin.core.qualifier.named

@Composable
fun SetupNavigation(navigator: AppNavigator = koinInject()) {
    rememberNavBackStack(Welcome).let { backstack ->
        navigator.setActiveBackStack(backstack)
    }

    val featureModulesNavigation: Set<FeatureModuleNavigation> =
        setOf(
            koinInject<FeatureModuleNavigation>(named(SETUP)),
            koinInject<FeatureModuleNavigation>(named(LOGS)),
            koinInject<FeatureModuleNavigation>(named(AUTOFILL_ENCOURAGEMENTS)),
        )

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
