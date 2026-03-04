package com.passbolt.mobile.android.feature.home.navigation

import PassboltTheme
import com.passbolt.mobile.android.core.navigation.compose.AppNavigator
import com.passbolt.mobile.android.core.navigation.compose.base.EntryProviderInstaller
import com.passbolt.mobile.android.core.navigation.compose.base.FeatureModuleNavigation
import com.passbolt.mobile.android.core.navigation.compose.keys.HomeNavigationKey.Home
import com.passbolt.mobile.android.core.navigation.compose.results.CreateFolderCompleteResult
import com.passbolt.mobile.android.core.navigation.compose.results.OtpScanCompleteResult
import com.passbolt.mobile.android.core.navigation.compose.results.PermissionsShareCompleteResult
import com.passbolt.mobile.android.core.navigation.compose.results.ResourceDetailsCompleteResult
import com.passbolt.mobile.android.core.navigation.compose.results.ResourceFormCompleteResult
import com.passbolt.mobile.android.core.navigation.compose.results.ResultEffect
import com.passbolt.mobile.android.feature.home.screen.HomeIntent.FolderCreateReturned
import com.passbolt.mobile.android.feature.home.screen.HomeIntent.OtpQRScanReturned
import com.passbolt.mobile.android.feature.home.screen.HomeIntent.ResourceDetailsReturned
import com.passbolt.mobile.android.feature.home.screen.HomeIntent.ResourceFormReturned
import com.passbolt.mobile.android.feature.home.screen.HomeIntent.ResourceShareReturned
import com.passbolt.mobile.android.feature.home.screen.HomeScreen
import com.passbolt.mobile.android.feature.home.screen.HomeViewModel
import com.passbolt.mobile.android.feature.home.screen.ResourceHandlingStrategy
import com.passbolt.mobile.android.feature.home.screen.ShowSuggestedModel
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

class HomeFeatureNavigation(
    private val resourceHandlingStrategy: ResourceHandlingStrategy,
    private val showSuggestedModel: ShowSuggestedModel,
) : FeatureModuleNavigation {
    override fun provideEntryProviderInstaller(): EntryProviderInstaller =
        {
            entry<Home> { key ->
                val navigator: AppNavigator = koinInject()
                val viewModel: HomeViewModel = koinViewModel()

                ResultEffect<ResourceFormCompleteResult> { result ->
                    viewModel.onIntent(ResourceFormReturned(result.resourceCreated, result.resourceEdited, result.resourceName))
                }
                ResultEffect<OtpScanCompleteResult> { result ->
                    viewModel.onIntent(OtpQRScanReturned(result.otpCreated, result.otpManualCreationChosen))
                }
                ResultEffect<ResourceDetailsCompleteResult> { result ->
                    viewModel.onIntent(ResourceDetailsReturned(result.resourceEdited, result.resourceDeleted, result.resourceName))
                }
                ResultEffect<PermissionsShareCompleteResult> { result ->
                    viewModel.onIntent(ResourceShareReturned(result.shared))
                }
                ResultEffect<CreateFolderCompleteResult> { result ->
                    viewModel.onIntent(FolderCreateReturned(result.folderName))
                }

                PassboltTheme {
                    HomeScreen(
                        resourceHandlingStrategy = resourceHandlingStrategy,
                        showSuggestedModel = showSuggestedModel,
                        homeView = key.homeDisplayViewModel,
                        navigator = navigator,
                        viewModel = viewModel,
                    )
                }
            }
        }
}
