package com.passbolt.mobile.android.folderdetails

import PassboltTheme
import com.passbolt.mobile.android.core.navigation.compose.base.EntryProviderInstaller
import com.passbolt.mobile.android.core.navigation.compose.base.FeatureModuleNavigation
import com.passbolt.mobile.android.core.navigation.compose.keys.FolderDetailsNavigationKey.FolderDetails

class FolderDetailsFeatureNavigation : FeatureModuleNavigation {
    override fun provideEntryProviderInstaller(): EntryProviderInstaller =
        {
            entry<FolderDetails> { key ->
                PassboltTheme {
                    FolderDetailsScreen(folderId = key.folderId)
                }
            }
        }
}
