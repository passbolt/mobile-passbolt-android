package com.passbolt.mobile.android.core.navigation.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.passbolt.mobile.android.core.navigation.compose.base.Feature.GROUP_DETAILS
import com.passbolt.mobile.android.core.navigation.compose.base.Feature.PERMISSIONS
import com.passbolt.mobile.android.core.navigation.compose.base.FeatureModuleNavigation
import com.passbolt.mobile.android.core.navigation.compose.keys.PermissionsNavigationKey.Permissions
import com.passbolt.mobile.android.core.navigation.compose.results.NavigationResultEventBus
import com.passbolt.mobile.android.core.navigation.compose.results.ResultEventBus
import com.passbolt.mobile.android.ui.PermissionsItem
import com.passbolt.mobile.android.ui.PermissionsMode
import org.koin.compose.koinInject
import org.koin.core.qualifier.named

@Composable
fun PermissionsNavigation(
    id: String,
    mode: PermissionsMode,
    permissionsItem: PermissionsItem,
    hostNavigation: PermissionsHostNavigation,
    navigator: AppNavigator = koinInject(),
) {
    val resultBus = remember { ResultEventBus() }

    rememberNavBackStack(Permissions(id, mode, permissionsItem)).let { backstack ->
        navigator.backStack = backstack
    }

    val featureModulesNavigation: Set<FeatureModuleNavigation> =
        setOf(
            koinInject<FeatureModuleNavigation>(named(PERMISSIONS)),
            koinInject<FeatureModuleNavigation>(named(GROUP_DETAILS)),
        )

    CompositionLocalProvider(
        NavigationResultEventBus provides resultBus,
        LocalPermissionsHostNavigation provides hostNavigation,
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
