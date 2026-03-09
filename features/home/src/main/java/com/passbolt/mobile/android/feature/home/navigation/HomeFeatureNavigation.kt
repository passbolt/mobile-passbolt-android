package com.passbolt.mobile.android.feature.home.navigation

import PassboltTheme
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.remember
import com.passbolt.mobile.android.core.navigation.compose.AppNavigator
import com.passbolt.mobile.android.core.navigation.compose.base.EntryProviderInstaller
import com.passbolt.mobile.android.core.navigation.compose.base.FeatureModuleNavigation
import com.passbolt.mobile.android.core.navigation.compose.keys.HomeNavigationKey.Home
import com.passbolt.mobile.android.core.navigation.compose.results.CreateFolderCompleteResult
import com.passbolt.mobile.android.core.navigation.compose.results.OtpScanCompleteResult
import com.passbolt.mobile.android.core.navigation.compose.results.ResourceDetailsCompleteResult
import com.passbolt.mobile.android.core.navigation.compose.results.ResourceFormCompleteResult
import com.passbolt.mobile.android.core.navigation.compose.results.ResultEffect
import com.passbolt.mobile.android.core.navigation.compose.results.ShareCompleteResult
import com.passbolt.mobile.android.feature.home.screen.DefaultResourceHandlingStrategy
import com.passbolt.mobile.android.feature.home.screen.HomeIntent.FolderCreateReturned
import com.passbolt.mobile.android.feature.home.screen.HomeIntent.OtpQRScanReturned
import com.passbolt.mobile.android.feature.home.screen.HomeIntent.ResourceDetailsReturned
import com.passbolt.mobile.android.feature.home.screen.HomeIntent.ResourceFormReturned
import com.passbolt.mobile.android.feature.home.screen.HomeIntent.ResourceShareReturned
import com.passbolt.mobile.android.feature.home.screen.HomeScreen
import com.passbolt.mobile.android.feature.home.screen.HomeViewModel
import com.passbolt.mobile.android.feature.home.screen.ResourceHandlingStrategy
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

class HomeFeatureNavigation : FeatureModuleNavigation {
    override fun provideEntryProviderInstaller(): EntryProviderInstaller =
        {
            entry<Home> { key ->
                val navigator: AppNavigator = koinInject()
                val viewModel: HomeViewModel = koinViewModel()
                val activity = LocalActivity.current
                val resourceHandlingStrategy =
                    remember(activity, navigator) {
                        if (activity is ResourceHandlingStrategy) activity else DefaultResourceHandlingStrategy(navigator)
                    }
                val showSuggestedModel =
                    remember(resourceHandlingStrategy) {
                        resourceHandlingStrategy.showSuggestedModel()
                    }

                ResultEffect<ResourceFormCompleteResult> { result ->
                    viewModel.onIntent(ResourceFormReturned(result.resourceCreated, result.resourceEdited, result.resourceName))
                }
                ResultEffect<OtpScanCompleteResult> { result ->
                    viewModel.onIntent(OtpQRScanReturned(result.otpCreated, result.otpManualCreationChosen))
                }
                ResultEffect<ResourceDetailsCompleteResult> { result ->
                    viewModel.onIntent(ResourceDetailsReturned(result.resourceEdited, result.resourceDeleted, result.resourceName))
                }
                ResultEffect<ShareCompleteResult> { result ->
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
