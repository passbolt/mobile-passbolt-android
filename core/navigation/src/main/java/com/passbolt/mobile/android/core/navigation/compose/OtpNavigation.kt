package com.passbolt.mobile.android.core.navigation.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.passbolt.mobile.android.core.navigation.compose.base.Feature.ACCOUNT_DETAILS
import com.passbolt.mobile.android.core.navigation.compose.base.Feature.OTP
import com.passbolt.mobile.android.core.navigation.compose.base.Feature.RESOURCE_FORM
import com.passbolt.mobile.android.core.navigation.compose.base.Feature.RESOURCE_PICKER
import com.passbolt.mobile.android.core.navigation.compose.base.Feature.SCAN_OTP
import com.passbolt.mobile.android.core.navigation.compose.base.Feature.TRANSFER_ACCOUNT_TO_ANOTHER_DEVICE
import com.passbolt.mobile.android.core.navigation.compose.base.FeatureModuleNavigation
import com.passbolt.mobile.android.core.navigation.compose.keys.OtpNavigationKey.Otp
import com.passbolt.mobile.android.core.navigation.compose.results.ResultEventBus
import org.koin.compose.koinInject
import org.koin.core.qualifier.named

@Composable
fun OtpNavigation(navigator: AppNavigator = koinInject()) {
    val resultBus = remember { ResultEventBus() }

    TabNavigationHost(
        initialKey = Otp,
        featureModulesNavigation =
            setOf(
                koinInject<FeatureModuleNavigation>(named(OTP)),
                koinInject<FeatureModuleNavigation>(named(SCAN_OTP)),
                koinInject<FeatureModuleNavigation>(named(RESOURCE_FORM)),
                koinInject<FeatureModuleNavigation>(named(RESOURCE_PICKER)),
                koinInject<FeatureModuleNavigation>(named(ACCOUNT_DETAILS)),
                koinInject<FeatureModuleNavigation>(named(TRANSFER_ACCOUNT_TO_ANOTHER_DEVICE)),
            ),
        resultBus = resultBus,
        navigator = navigator,
    )
}
