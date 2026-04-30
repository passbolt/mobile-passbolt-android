package com.passbolt.mobile.android.resourcepicker.navigation

import PassboltTheme
import com.passbolt.mobile.android.core.navigation.compose.base.EntryProviderInstaller
import com.passbolt.mobile.android.core.navigation.compose.base.FeatureModuleNavigation
import com.passbolt.mobile.android.core.navigation.compose.keys.OtpNavigationKey.ResourcePicker
import com.passbolt.mobile.android.resourcepicker.screen.ResourcePickerScreen

class ResourcePickerFeatureNavigation : FeatureModuleNavigation {
    override fun provideEntryProviderInstaller(): EntryProviderInstaller =
        {
            entry<ResourcePicker> { key ->
                PassboltTheme {
                    ResourcePickerScreen(suggestionUri = key.suggestionUri)
                }
            }
        }
}
