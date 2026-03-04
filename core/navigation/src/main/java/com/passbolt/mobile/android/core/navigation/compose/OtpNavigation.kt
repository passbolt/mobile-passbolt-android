package com.passbolt.mobile.android.core.navigation.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.passbolt.mobile.android.core.navigation.compose.base.Feature.OTP
import com.passbolt.mobile.android.core.navigation.compose.base.Feature.RESOURCE_FORM
import com.passbolt.mobile.android.core.navigation.compose.base.Feature.RESOURCE_PICKER
import com.passbolt.mobile.android.core.navigation.compose.base.Feature.SCAN_OTP
import com.passbolt.mobile.android.core.navigation.compose.base.FeatureModuleNavigation
import com.passbolt.mobile.android.core.navigation.compose.keys.OtpNavigationKey.Otp
import com.passbolt.mobile.android.core.navigation.compose.keys.OtpNavigationKey.ScanOtp
import com.passbolt.mobile.android.core.navigation.compose.keys.OtpNavigationKey.ScanOtpMode
import com.passbolt.mobile.android.core.navigation.compose.results.ResourceFormCompleteResult
import com.passbolt.mobile.android.core.navigation.compose.results.ResultEventBus
import com.passbolt.mobile.android.ui.OtpParseResult
import org.koin.compose.koinInject
import org.koin.core.qualifier.named

@Composable
fun OtpNavigation(navigator: AppNavigator = koinInject()) {
    val resultBus = remember { ResultEventBus() }

    val hostNavigation =
        remember(navigator, resultBus) {
            OtpResourceFormHostNavigation(navigator, resultBus)
        }

    TabNavigationHost(
        initialKey = Otp,
        featureModulesNavigation =
            setOf(
                koinInject<FeatureModuleNavigation>(named(OTP)),
                koinInject<FeatureModuleNavigation>(named(SCAN_OTP)),
                koinInject<FeatureModuleNavigation>(named(RESOURCE_FORM)),
                koinInject<FeatureModuleNavigation>(named(RESOURCE_PICKER)),
            ),
        resultBus = resultBus,
        additionalProviders =
            arrayOf(
                LocalResourceFormHostNavigation provides hostNavigation,
            ),
        navigator = navigator,
    )
}

private class OtpResourceFormHostNavigation(
    private val navigator: AppNavigator,
    private val resultBus: ResultEventBus,
) : ResourceFormHostNavigation {
    override fun navigateBack() {
        navigator.navigateBack()
    }

    override fun navigateBackWithCreateSuccess(
        name: String,
        resourceId: String,
    ) {
        resultBus.sendResult(
            result =
                ResourceFormCompleteResult(
                    resourceCreated = true,
                    resourceEdited = false,
                    resourceName = name,
                ),
        )
        navigator.popToKey(Otp)
    }

    override fun navigateBackWithEditSuccess(name: String) {
        resultBus.sendResult(
            result =
                ResourceFormCompleteResult(
                    resourceCreated = false,
                    resourceEdited = true,
                    resourceName = name,
                ),
        )
        navigator.popToKey(Otp)
    }

    override fun navigateToScanOtp(resultCallback: (Boolean, OtpParseResult.OtpQr.TotpQr?) -> Unit) {
        navigator.navigateToKey(
            ScanOtp(ScanOtpMode.SCAN_FOR_RESULT),
        )
    }
}
