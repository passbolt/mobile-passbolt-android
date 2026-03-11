package com.passbolt.mobile.android.feature.settings.navigation

import PassboltTheme
import com.passbolt.mobile.android.core.navigation.compose.base.EntryProviderInstaller
import com.passbolt.mobile.android.core.navigation.compose.base.FeatureModuleNavigation
import com.passbolt.mobile.android.core.navigation.compose.keys.SettingsNavigationKey
import com.passbolt.mobile.android.feature.settings.screen.appsettings.autofill.autofillenabled.AutofillEnabledScreen
import com.passbolt.mobile.android.feature.settings.screen.appsettings.autofill.encourageaccessibility.EncourageAccessibilityScreen
import com.passbolt.mobile.android.feature.settings.screen.appsettings.autofill.encouragenativeautofill.EncourageNativeAutofillScreen

class AutofillEncouragementsFeatureNavigation : FeatureModuleNavigation {
    override fun provideEntryProviderInstaller(): EntryProviderInstaller =
        {
            entry<SettingsNavigationKey.EncourageNativeAutofill> {
                PassboltTheme { EncourageNativeAutofillScreen(dismissBehavior = it.dismissBehavior) }
            }
            entry<SettingsNavigationKey.AutofillEnabled> {
                PassboltTheme { AutofillEnabledScreen() }
            }
            entry<SettingsNavigationKey.EncourageAccessibilityAutofill> {
                PassboltTheme { EncourageAccessibilityScreen() }
            }
        }
}
