package com.passbolt.mobile.android.core.navigation.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.passbolt.mobile.android.core.navigation.compose.base.Feature.CREATE_FOLDER
import com.passbolt.mobile.android.core.navigation.compose.base.Feature.FOLDER_DETAILS
import com.passbolt.mobile.android.core.navigation.compose.base.Feature.GROUP_DETAILS
import com.passbolt.mobile.android.core.navigation.compose.base.Feature.LOCATION_DETAILS
import com.passbolt.mobile.android.core.navigation.compose.base.Feature.PERMISSIONS
import com.passbolt.mobile.android.core.navigation.compose.base.Feature.RESOURCE_DETAILS
import com.passbolt.mobile.android.core.navigation.compose.base.Feature.RESOURCE_FORM
import com.passbolt.mobile.android.core.navigation.compose.base.Feature.RESOURCE_PICKER
import com.passbolt.mobile.android.core.navigation.compose.base.Feature.SCAN_OTP
import com.passbolt.mobile.android.core.navigation.compose.base.Feature.TAGS_DETAILS
import com.passbolt.mobile.android.core.navigation.compose.base.FeatureModuleNavigation
import com.passbolt.mobile.android.core.navigation.compose.keys.HomeNavigationKey.Home
import com.passbolt.mobile.android.core.navigation.compose.keys.OtpNavigationKey.ScanOtp
import com.passbolt.mobile.android.core.navigation.compose.keys.OtpNavigationKey.ScanOtpMode
import com.passbolt.mobile.android.core.navigation.compose.keys.PermissionsNavigationKey.Permissions
import com.passbolt.mobile.android.core.navigation.compose.results.PermissionsShareCompleteResult
import com.passbolt.mobile.android.core.navigation.compose.results.ResourceFormCompleteResult
import com.passbolt.mobile.android.core.navigation.compose.results.ResultEventBus
import com.passbolt.mobile.android.ui.HomeDisplayViewModel
import com.passbolt.mobile.android.ui.OtpParseResult
import com.passbolt.mobile.android.ui.PermissionsItem
import com.passbolt.mobile.android.ui.PermissionsMode
import org.koin.compose.koinInject
import org.koin.core.qualifier.named

@Composable
@Suppress("ktlint:compose:vm-forwarding-check", "ViewModelForwarding")
fun HomeNavigation(
    initialHomeDisplay: HomeDisplayViewModel,
    homeFeatureNavigation: FeatureModuleNavigation,
    navigator: AppNavigator = koinInject(),
) {
    val resultBus = remember { ResultEventBus() }

    val hostNavigation =
        remember(navigator, resultBus) {
            HomeResourceFormHostNavigation(navigator, resultBus)
        }
    val permissionsHostNavigation =
        remember(navigator, resultBus) {
            HomePermissionsHostNavigation(navigator, resultBus)
        }

    TabNavigationHost(
        initialKey = Home(initialHomeDisplay),
        featureModulesNavigation =
            setOf(
                homeFeatureNavigation,
                koinInject<FeatureModuleNavigation>(named(RESOURCE_FORM)),
                koinInject<FeatureModuleNavigation>(named(SCAN_OTP)),
                koinInject<FeatureModuleNavigation>(named(RESOURCE_PICKER)),
                koinInject<FeatureModuleNavigation>(named(PERMISSIONS)),
                koinInject<FeatureModuleNavigation>(named(GROUP_DETAILS)),
                koinInject<FeatureModuleNavigation>(named(RESOURCE_DETAILS)),
                koinInject<FeatureModuleNavigation>(named(FOLDER_DETAILS)),
                koinInject<FeatureModuleNavigation>(named(CREATE_FOLDER)),
                koinInject<FeatureModuleNavigation>(named(TAGS_DETAILS)),
                koinInject<FeatureModuleNavigation>(named(LOCATION_DETAILS)),
            ),
        resultBus = resultBus,
        additionalProviders =
            arrayOf(
                LocalResourceFormHostNavigation provides hostNavigation,
                LocalPermissionsHostNavigation provides permissionsHostNavigation,
            ),
        navigator = navigator,
    )
}

private class HomeResourceFormHostNavigation(
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
        navigator.navigateBack()
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
        navigator.navigateBack()
    }

    override fun navigateToScanOtp(resultCallback: (Boolean, OtpParseResult.OtpQr.TotpQr?) -> Unit) {
        navigator.navigateToKey(
            ScanOtp(ScanOtpMode.SCAN_FOR_RESULT),
        )
    }
}

private class HomePermissionsHostNavigation(
    private val navigator: AppNavigator,
    private val resultBus: ResultEventBus,
) : PermissionsHostNavigation {
    override fun navigateBack() {
        navigator.navigateBack()
    }

    override fun navigateToSelfWithMode(
        id: String,
        mode: PermissionsMode,
    ) {
        navigator.navigateToKey(
            Permissions(
                id,
                mode,
                PermissionsItem.RESOURCE,
            ),
        )
    }

    override fun closeWithShareSuccessResult() {
        resultBus.sendResult(
            result = PermissionsShareCompleteResult(shared = true),
        )
        navigator.navigateBack()
    }

    override fun navigateToHome() {
        navigator.popToKey(navigator.backStack.first())
    }
}
