package com.passbolt.mobile.android.core.navigation.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.passbolt.mobile.android.core.navigation.ActivityIntents
import com.passbolt.mobile.android.core.navigation.AppContext
import com.passbolt.mobile.android.core.navigation.compose.base.Feature.AUTHENTICATION
import com.passbolt.mobile.android.core.navigation.compose.base.Feature.LOGS
import com.passbolt.mobile.android.core.navigation.compose.base.FeatureModuleNavigation
import com.passbolt.mobile.android.core.navigation.compose.keys.AuthenticationNavigationKey.AccountsList
import com.passbolt.mobile.android.core.navigation.compose.keys.AuthenticationNavigationKey.Auth
import com.passbolt.mobile.android.core.navigation.compose.results.NavigationResultEventBus
import com.passbolt.mobile.android.core.navigation.compose.results.ResultEventBus
import org.koin.compose.koinInject
import org.koin.core.qualifier.named

@Composable
fun AuthenticationNavigation(
    authConfig: ActivityIntents.AuthConfig,
    appContext: AppContext,
    skipAccountsList: Boolean,
    initialUserId: String?,
    navigator: AppNavigator = koinInject(),
) {
    val resultBus = remember { ResultEventBus() }
    val authParams = remember { AuthenticationParams(authConfig, appContext) }

    val initialKeys: Array<NavKey> =
        when {
            skipAccountsList && initialUserId != null -> arrayOf(Auth(initialUserId))
            initialUserId != null -> arrayOf(AccountsList, Auth(initialUserId))
            else -> arrayOf(AccountsList)
        }

    @Suppress("SpreadOperator")
    rememberNavBackStack(*initialKeys).let { backstack ->
        navigator.backStack = backstack
    }

    val featureModulesNavigation: Set<FeatureModuleNavigation> =
        setOf(
            koinInject<FeatureModuleNavigation>(named(AUTHENTICATION)),
            koinInject<FeatureModuleNavigation>(named(LOGS)),
        )

    CompositionLocalProvider(
        NavigationResultEventBus provides resultBus,
        LocalAuthenticationParams provides authParams,
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
