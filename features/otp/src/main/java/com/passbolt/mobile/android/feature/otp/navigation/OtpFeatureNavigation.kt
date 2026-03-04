package com.passbolt.mobile.android.feature.otp.navigation

import PassboltTheme
import com.passbolt.mobile.android.core.navigation.compose.AppNavigator
import com.passbolt.mobile.android.core.navigation.compose.base.EntryProviderInstaller
import com.passbolt.mobile.android.core.navigation.compose.base.FeatureModuleNavigation
import com.passbolt.mobile.android.core.navigation.compose.keys.OtpNavigationKey.Otp
import com.passbolt.mobile.android.core.navigation.compose.results.OtpScanCompleteResult
import com.passbolt.mobile.android.core.navigation.compose.results.ResourceFormCompleteResult
import com.passbolt.mobile.android.core.navigation.compose.results.ResultEffect
import com.passbolt.mobile.android.feature.otp.screen.OtpIntent.OtpQRScanReturned
import com.passbolt.mobile.android.feature.otp.screen.OtpIntent.ResourceFormReturned
import com.passbolt.mobile.android.feature.otp.screen.OtpScreen
import com.passbolt.mobile.android.feature.otp.screen.OtpViewModel
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

class OtpFeatureNavigation : FeatureModuleNavigation {
    override fun provideEntryProviderInstaller(): EntryProviderInstaller =
        {
            entry<Otp> {
                val navigator: AppNavigator = koinInject()
                val viewModel: OtpViewModel = koinViewModel()

                ResultEffect<OtpScanCompleteResult> { result ->
                    viewModel.onIntent(OtpQRScanReturned(result.otpCreated, result.otpManualCreationChosen))
                }
                ResultEffect<ResourceFormCompleteResult> { result ->
                    viewModel.onIntent(ResourceFormReturned(result.resourceCreated, result.resourceEdited, result.resourceName))
                }

                PassboltTheme {
                    OtpScreen(navigator = navigator, viewModel = viewModel)
                }
            }
        }
}
