package com.passbolt.mobile.android.core.navigation.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.passbolt.mobile.android.core.navigation.compose.base.Feature.ACCOUNT_DETAILS
import com.passbolt.mobile.android.core.navigation.compose.base.Feature.CREATE_FOLDER
import com.passbolt.mobile.android.core.navigation.compose.base.Feature.FOLDER_DETAILS
import com.passbolt.mobile.android.core.navigation.compose.base.Feature.GROUP_DETAILS
import com.passbolt.mobile.android.core.navigation.compose.base.Feature.HOME
import com.passbolt.mobile.android.core.navigation.compose.base.Feature.LOCATION_DETAILS
import com.passbolt.mobile.android.core.navigation.compose.base.Feature.PERMISSIONS
import com.passbolt.mobile.android.core.navigation.compose.base.Feature.RESOURCE_DETAILS
import com.passbolt.mobile.android.core.navigation.compose.base.Feature.RESOURCE_FORM
import com.passbolt.mobile.android.core.navigation.compose.base.Feature.RESOURCE_PICKER
import com.passbolt.mobile.android.core.navigation.compose.base.Feature.SCAN_OTP
import com.passbolt.mobile.android.core.navigation.compose.base.Feature.TAGS_DETAILS
import com.passbolt.mobile.android.core.navigation.compose.base.Feature.TRANSFER_ACCOUNT_TO_ANOTHER_DEVICE
import com.passbolt.mobile.android.core.navigation.compose.base.FeatureModuleNavigation
import com.passbolt.mobile.android.core.navigation.compose.keys.HomeNavigationKey.Home
import com.passbolt.mobile.android.core.navigation.compose.results.ResultEventBus
import com.passbolt.mobile.android.ui.HomeDisplayViewModel
import org.koin.compose.koinInject
import org.koin.core.qualifier.named

@Composable
@Suppress("ktlint:compose:vm-forwarding-check", "ViewModelForwarding")
fun HomeNavigation(
    initialHomeDisplay: HomeDisplayViewModel,
    navigator: AppNavigator = koinInject(),
) {
    val resultBus = remember { ResultEventBus() }

    TabNavigationHost(
        initialKey = Home(initialHomeDisplay),
        featureModulesNavigation =
            setOf(
                koinInject<FeatureModuleNavigation>(named(HOME)),
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
                koinInject<FeatureModuleNavigation>(named(ACCOUNT_DETAILS)),
                koinInject<FeatureModuleNavigation>(named(TRANSFER_ACCOUNT_TO_ANOTHER_DEVICE)),
            ),
        resultBus = resultBus,
        navigator = navigator,
    )
}
