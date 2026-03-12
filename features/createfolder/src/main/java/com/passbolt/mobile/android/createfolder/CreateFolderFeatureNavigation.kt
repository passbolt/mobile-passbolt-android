package com.passbolt.mobile.android.createfolder

import PassboltTheme
import com.passbolt.mobile.android.core.navigation.compose.base.EntryProviderInstaller
import com.passbolt.mobile.android.core.navigation.compose.base.FeatureModuleNavigation
import com.passbolt.mobile.android.core.navigation.compose.keys.CreateFolderNavigationKey.CreateFolder

class CreateFolderFeatureNavigation : FeatureModuleNavigation {
    override fun provideEntryProviderInstaller(): EntryProviderInstaller =
        {
            entry<CreateFolder> { key ->
                PassboltTheme {
                    CreateFolderScreen(
                        parentFolderId = key.parentFolderId,
                    )
                }
            }
        }
}
